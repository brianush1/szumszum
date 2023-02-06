package szumszum.stages;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import szumszum.Hasher;
import szumszum.MethodSignature;
import szumszum.Remote;
import szumszum.Status;
import szumszum.Terminal;
import szumszum.TestResult;
import szumszum.cases.CaseGroup;
import szumszum.cases.TestCase;

public class RunTests implements IStage {

	private Class<?> mainClass;

	private Path testsPath;

	private boolean downloadTests() {
		String file = DetectAssignment.getAssignment() + ".tests.zip";
		testsPath = Path.of(file);

		if (Files.exists(testsPath)) {
			Terminal.enterStage("\u001B[3m" + file + "\u001B[0m already exists; checking for updates");

			try {
				Terminal.flush();

				String expected;

				try {
					expected = Remote.downloadString(file + ".sha1").trim();
				}
				catch (IOException ex) {
					Terminal.updateStatus(Status.FAIL, "A network error occurred; attempting to keep going");
					Terminal.exitStage();

					return true;
				}

				String actual = Hasher.getSHA1(testsPath);

				if (actual.equals(expected)) {
					Terminal.enterStage("\u001B[3m" + file + "\u001B[0m already exists and is up-to-date");
					Terminal.updateStatus(Status.DONE);
					Terminal.exitStage();

					return true;
				}
			}
			catch (IOException ex) {
				Terminal.updateStatus(Status.FAIL, "An I/O error occurred; attempting to keep going");
				Terminal.exitStage();

				return true;
			}
		}

		Terminal.enterStage("Downloading \u001B[3m" + file + "\u001B[0m");

		try {
			Terminal.flush();
			Remote.downloadFile(file, testsPath);
		}
		catch (IOException ex) {
			Terminal.updateStatus(Status.FAIL, "A network error occurred while downloading test cases");
			Terminal.exitStage();

			return false;
		}

		Terminal.updateStatus(Status.DONE);
		Terminal.exitStage();

		return true;
	}

	private boolean compile() throws IOException, InterruptedException {
		String file = DetectAssignment.getAssignment() + ".java";
		Terminal.enterStage("Compiling \u001B[3m" + file + "\u001B[0m");

		Terminal.flush();
		if (!new File(file).isFile()) {
			Terminal.updateStatus(Status.FAIL, "File not found; are you in the right directory?");
			Terminal.exitStage();
			return false;
		} else {
			Process process = Runtime.getRuntime().exec("javac -parameters " + file);

			int exitCode = process.waitFor();

			if (exitCode != 0) {
				Terminal.updateStatus(Status.FAIL, "Compilation returned with error code " + exitCode
						+ "\n\n" + new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
				Terminal.exitStage();
				return false;
			}
		}

		try {
			String url = "file:" + System.getProperty("user.dir");
			if (!url.endsWith("/")) {
				// force URLClassLoader to see this path as a directory
				url += "/";
			}

			mainClass = Class.forName(DetectAssignment.getAssignment(), true,
					new URLClassLoader(new URL[] { new URL(url) }));
		} catch (ClassNotFoundException e) {
			Terminal.updateStatus(Status.FAIL, "Could not load compiled class");
			Terminal.exitStage();
			return false;
		}

		Terminal.updateStatus(Status.DONE);
		Terminal.exitStage();

		return true;
	}

	private HashMap<String, Method> methodMap;

	private boolean checkInterface(Path testsRoot) {
		Method[] methods = mainClass.getDeclaredMethods();
		methodMap = new HashMap<>();
		TreeSet<String> otherMethods = new TreeSet<>();

		for (Method method : methods) {
			String name = method.getName();
			method.setAccessible(true);
			methodMap.put(name, method);
			otherMethods.add(name);
		}

		boolean interfaceCheck = true;

		MethodSignature[] expectedMethods = null;

		try {
			ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(testsRoot.resolve("interface")));
			expectedMethods = (MethodSignature[]) stream.readObject();
			stream.close();
		} catch (IOException ex) {
			throw new Error(ex);
		} catch (Exception ex) {
			Terminal.enterStage("Corrupted data");
			Terminal.updateStatus(Status.FAIL);
			Terminal.exitStage();

			return false;
		}

		for (MethodSignature expected : expectedMethods) {
			Method method = methodMap.getOrDefault(expected.name, null);
			if (method != null) {
				otherMethods.remove(expected.name);

				MethodSignature actual = new MethodSignature(method);

				Terminal.enterStage("Check " + expected.highlight());

				if (!expected.checkCovariance(actual)) {
					interfaceCheck = false;
					Terminal.updateStatus(Status.FAIL, "You wrote: " + actual.highlight());
				}
				else if (!Arrays.equals(expected.paramNames, actual.paramNames)) {
					String explanation = "";

					boolean[] underlined = new boolean[expected.numParams];
					for (int j = 0; j < expected.numParams; j++) {
						if (!expected.paramNames[j].equals(actual.paramNames[j])) {
							// explanation += "\n • Expected " + expected.paramNames[j]
							// 	+ ", got " + actual.paramNames[j];
							underlined[j] = true;
						}
					}

					Terminal.enterStage("Check " + expected.highlight(underlined));

					explanation += "You wrote: " + actual.highlight(underlined);
					explanation += "\n\u001B[1;33mWarning:\u001B[0m Parameter names don't match";

					Terminal.updateStatus(Status.PASS, explanation);
				}
				else {
					Terminal.updateStatus(Status.PASS);
				}

			}
			else {
				Terminal.enterStage("Check " + expected.highlight());

				interfaceCheck = false;
				Terminal.updateStatus(Status.FAIL, "Method is missing; check for typos");
			}

			Terminal.exitStage();
		}

		ArrayList<String> nonprivate = new ArrayList<>();

		for (String name : otherMethods) {
			Method method = methodMap.get(name);

			if (!Modifier.isPrivate(method.getModifiers())) {
				nonprivate.add(new MethodSignature(method).highlight());
			}
		}

		if (nonprivate.size() == 0) {
			Terminal.enterStage("All other methods marked \u001B[1;34mprivate\u001B[0m");
			Terminal.updateStatus(Status.PASS);
			Terminal.exitStage();
		}
		else {
			String explanation = "";

			for (String s : nonprivate) {
				explanation += "\n • " + s;
			}

			Terminal.enterStage("The following methods should be marked \u001B[1;34mprivate\u001B[0m:");
			Terminal.updateStatus(Status.FAIL, explanation.substring(1));
			Terminal.exitStage();
		}

		return interfaceCheck;
	}

	private class RunTestResult {
		Status success;
		String explanation;
		Object returnValue;
	}

	private RunTestResult runTest(double timeLimit, TestCase test) {
		Method method = methodMap.get(test.methodName);
		Status success;
		String explanation = null;

		var threadFactory = new ThreadFactory() {

			private ArrayList<Thread> threads = new ArrayList<>();

			public Thread newThread(Runnable r) {
				Thread result = new Thread(r);
				threads.add(result);
				return result;
			}

			public void forceKill() {
				// TODO: figure this out
				threads.clear();
			}

		};
		ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);

		Future<TestResult> future = null;

		Object _returnValue = null;
		try {
			future = service.submit(() -> {
				try {
					Object result = method.invoke(null, test.args);

					TestResult returnValue = new TestResult();
					returnValue.isError = false;
					returnValue.result = result;
					return returnValue;
				}
				catch (IllegalAccessException ex) {
					throw new Error(ex);
				}
				catch (InvocationTargetException ex) {
					TestResult returnValue = new TestResult();
					returnValue.isError = true;
					returnValue.result = ex.getCause();
					return returnValue;
				}
			});

			TestResult result = future.get((long)(timeLimit * 1_000_000L), TimeUnit.NANOSECONDS);

			if (result.isError) {
				success = Status.FAIL;

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				try (PrintStream pstream = new PrintStream(stream, true, StandardCharsets.UTF_8)) {
					((Throwable) result.result).printStackTrace(pstream);
				}
				explanation = stream.toString(StandardCharsets.UTF_8);

				String[] lines = explanation.split("\n", -1);
				explanation = "";
				for (String line : lines) {
					if ((line.contains("jdk.internal.reflect.NativeMethodAccessorImpl")
					|| line.contains("jdk.internal.reflect.GeneratedMethodAccessor")) && line.contains("invoke")) {
						break; // prettier stack trace
					}

					explanation += "\n" + line;
				}

				if (explanation.length() > 0)
					explanation = explanation.substring(1);
			}
			else {
				_returnValue = result.result;
				if (test.returnChecker.check(result.result)) {
					success = Status.PASS;
				}
				else {
					success = Status.FAIL;
				}
			}
		}
		catch (TimeoutException ex) {
			success = Status.TIME;
		}
		catch (ExecutionException ex) {
			throw new Error(ex);
		}
		catch (InterruptedException ex) {
			throw new Error(ex);
		}
		finally {
			threadFactory.forceKill();
			service.shutdownNow();
		}

		RunTestResult result = new RunTestResult();
		result.success = success;
		result.explanation = explanation;
		result.returnValue = _returnValue;
		return result;
	}

	public boolean run() {
		boolean downloaded = downloadTests();
		if (!downloaded) {
			return false;
		}

		try {
			boolean compiled = compile();
			if (!compiled) {
				return false;
			}
		} catch (InterruptedException ex) {
			Terminal.updateStatus(Status.FAIL, "Compilation interrupted");
			Terminal.exitStage();

			return false;
		} catch (IOException ex) {
			Terminal.updateStatus(Status.FAIL, "I/O error; try again");
			Terminal.exitStage();

			return false;
		}

		Terminal.addSpacing();

		Path testsRoot;

		try {
			testsRoot = FileSystems.newFileSystem(testsPath, null)
				.getRootDirectories()
				.iterator()
				.next();
		} catch (IOException ex) {
			Terminal.enterStage("I/O error; try again");
			Terminal.updateStatus(Status.FAIL);
			Terminal.exitStage();

			return false;
		}

		boolean interfaceCheck = checkInterface(testsRoot);
		if (!interfaceCheck) {
			return false;
		}

		Terminal.addSpacing();

		Benchmark.instance.run();
		double scaleFactor = Benchmark.instance.scaleFactor();

		int numCases = 0;
		int passed = 0, failed = 0, timedOut = 0;

		for (int caseIndex = 1, caseFileIndex = 1;; caseFileIndex++) {
			Path path = testsRoot.resolve(String.format("case%02d", caseFileIndex));
			if (!Files.exists(path)) {
				break;
			}

			Terminal.enterStage("Test Case " + caseIndex + ": Loading...");
			Terminal.flush();

			CaseGroup group;

			try {
				ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(path));
				group = (CaseGroup) stream.readObject();
				stream.close();
			} catch (IOException ex) {
				throw new Error(ex);
			} catch (Exception ex) {
				Terminal.enterStage("Test Case " + caseIndex + ": ???");
				Terminal.updateStatus(Status.DONE, "Corrupted test data");
				Terminal.exitStage();

				continue;
			}

			String header;
			if (group.subCases.length > 1) {
				header = "Test Cases " + caseIndex + "-"
					+ (caseIndex + group.subCases.length - 1) + ": ";
			}
			else {
				header = "Test Case " + caseIndex + ": ";
			}

			Terminal.enterStage(header + group.name);
			Terminal.flush();

			Status overallSuccess = Status.PASS;
			String latestExplanation = null;

			long startTime = System.nanoTime();

			int numTestsRun = 0;

			for (int i = 0; i < group.subCases.length; i++) {
				numTestsRun += 1;
				numCases += 1;

				ByteArrayOutputStream output = new ByteArrayOutputStream();
				System.setOut(new PrintStream(output, false, StandardCharsets.UTF_8));
				System.setErr(new PrintStream(output, false, StandardCharsets.UTF_8));

				RunTestResult result = runTest(group.timeLimitPerCase * scaleFactor, group.subCases[i]);
				Status success = result.success;
				latestExplanation = result.explanation;

				if (success != Status.PASS) { // wrong answer
					String stringified = Arrays.deepToString(group.subCases[i].args);

					String msg;

					if (stringified.length() > 5000 && !Terminal.hasArg("--verbose")) {
						msg = "Large case data not shown; re-run as szumszum.jar --verbose to see all case data";
					}
					else {
						msg = "Called " + group.subCases[i].methodName
							+ " with arguments:\n";
						msg += stringified;
						msg += "\nExpected output: ";
						msg += group.subCases[i].returnChecker.toString();
						msg += "\nYour solution's output: ";
						if (result.returnValue == null) {
							msg += "ERROR";
						}
						else {
							msg += result.returnValue.toString();
						}
					}

					if (latestExplanation == null) {
						latestExplanation = msg;
					}
					else {
						latestExplanation = msg + "\n" + latestExplanation;
					}
				}

				boolean continueAnyway = false;
				if (output.size() != 0) {
					if (success == Status.PASS) {
						success = Status.FAIL;
						continueAnyway = true;
						latestExplanation = "Correct answer produced, with extraneous output:\n" + output.toString(StandardCharsets.UTF_8);
					}
					else {
						String msg = "Extraneous output:\n" + output.toString(StandardCharsets.UTF_8);

						if (latestExplanation == null) {
							latestExplanation = msg;
						}
						else {
							latestExplanation = latestExplanation + "\n" + msg;
						}
					}
				}

				if (success == Status.PASS) {
					passed += 1;
				}
				else if (success == Status.TIME) {
					timedOut += 1;
				}
				else {
					failed += 1;
				}

				if (success != Status.PASS && (!continueAnyway || i == group.subCases.length - 1)) {
					overallSuccess = success;

					break;
				}
			}

			long endTime = System.nanoTime();

			if (group.subCases.length > 1) {
				Terminal.enterStage(header + group.name
					+ (overallSuccess == Status.TIME
						? String.format(" (took over %.3fs)", group.timeLimitPerCase * scaleFactor / 1000.0)
						: String.format(" (took %.3fs on average)", (endTime - startTime)
							/ (double) numTestsRun / 1_000_000_000.0))
				);
				String msg = "Stopped on case " + (caseIndex + numTestsRun - 1);
				if (overallSuccess != Status.PASS) {
					if (latestExplanation != null) {
						latestExplanation = msg + "\n" + latestExplanation;
					}
					else {
						latestExplanation = msg;
					}
				}
				Terminal.updateStatus(overallSuccess, latestExplanation);
				Terminal.exitStage();
			}
			else {
				Terminal.enterStage(header + group.name
					+ (overallSuccess == Status.TIME
						? String.format(" (took over %.3fs)", group.timeLimitPerCase * scaleFactor / 1000.0)
						: String.format(" (took %.3fs)", (endTime - startTime) / 1_000_000_000.0))
				);
				Terminal.updateStatus(overallSuccess, latestExplanation);
				Terminal.exitStage();
			}

			caseIndex += group.subCases.length;
		}

		Terminal.addSpacing();

		Terminal.enterStage(numCases + " test cases");
		if (passed == numCases) {
			Terminal.updateStatus(Status.PASS);
		}
		else {
			Terminal.updateStatus(Status.FAIL, passed + " test cases passed"
				+ "\n" + failed + " failed"
				+ "\n" + timedOut + " timed out");
		}
		Terminal.exitStage();

		Terminal.addSpacing();

		return true;
	}

}
