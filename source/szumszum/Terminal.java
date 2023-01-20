package szumszum;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class Terminal {

	private static PrintWriter output;

	private static String[] args;

	private Terminal() {}

	public static String[] getArgs() {
		return args;
	}

	public static boolean hasArg(String arg) {
		return Arrays.asList(args).contains(arg);
	}

	public static void initialize(String[] args) {
		output = new PrintWriter(System.out);

		Terminal.args = args;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (output != null) {
					output.println();
					output.println();
					output.println();
					output.close();
				}
			}
		});
	}

	public static void close() {
		output.close();
	}

	private static String currentStage;

	public static void enterStage(String title) {
		hasSpacing = false;
		currentStage = title.replaceAll("\u001B.*?m", "");
		output.print("\u001B[1G\u001B[2K"
				+ " ".repeat(2) + ".... " + title + "\n\n\n"
				+ "\u001B[3A"
				+ "\u001B[" + (currentStage.length() + 8) + "G");
	}

	public static void exitStage() {
		output.println();
	}

	public static void updateStatus(Status status) {
		updateStatus(status, null);
	}

	public static void updateStatus(Status status, String explanation) {
		String str = status.toString();

		int color = 39; // default

		if (status == Status.DONE) {
			color = 34; // blue
		} else if (status == Status.FAIL) {
			color = 31; // red
		} else if (status == Status.TIME) {
			color = 31; // red
		} else if (status == Status.PASS) {
			color = 32; // green
		}

		if (explanation != null) {
			output.print("\u001B[3G" // move to appropriate column
					+ "\u001B[1;" + color + "m" // foreground color
					+ str
					+ "\u001B[0m" // reset
					+ "\u001B[" + (currentStage.length() + 9) + "G"); // move back

			String toPrint = "";
			for (String line : explanation.split("\n")) {
				toPrint += "\n" + " ".repeat(6 - str.length()) + line;
			}
			output.print(toPrint);
			addSpacing();
		} else {
			output.print("\u001B[3G" // move to appropriate column
					+ "\u001B[1;" + color + "m" // foreground color
					+ str
					+ "\u001B[0m" // reset
					+ "\u001B[" + (currentStage.length() + 9) + "G"); // move back
		}
	}

	public static void flush() {
		output.flush();
	}

	private static boolean hasSpacing = false;

	public static void addSpacing() {
		if (!hasSpacing) {
			hasSpacing = true;
			output.println();
		}
	}

}
