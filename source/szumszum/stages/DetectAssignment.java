package szumszum.stages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import szumszum.Status;
import szumszum.Terminal;

public class DetectAssignment implements IStage {

	private static String assignment;

	public static String getAssignment() {
		return assignment;
	}

	public boolean run() {
		Terminal.enterStage("Detecting assignment name");

		if (Terminal.getArgs().length == 1) {
			assignment = Terminal.getArgs()[0];
			Terminal.updateStatus(Status.DONE, "Manually specified: \u001B[3m" + assignment + "\u001B[0m");
			Terminal.exitStage();

			return true;
		}

		if (Files.exists(Path.of("test-all.sh"))) {
			try {
				Terminal.flush();

				String[] tokens = Files.readString(Path.of("test-all.sh")).split(" ");

				HashMap<String, Integer> frequency = new HashMap<>();
				int maxFrequency = -1;
				String maxFrequencyToken = null;
				for (String token : tokens) {
					if (token.matches("^[A-Za-z0-9_]+\\.java$")) {
						int thisFrequency = frequency.getOrDefault(token, 0) + 1;
						if (thisFrequency > maxFrequency) {
							maxFrequency = thisFrequency;
							maxFrequencyToken = token;
						}
						frequency.put(token, thisFrequency);
					}
				}

				assignment = maxFrequencyToken.split("\\.")[0];

				Terminal.enterStage("Assignment name: \u001B[3m" + assignment + "\u001B[0m");
				Terminal.updateStatus(Status.DONE);
				Terminal.exitStage();

				return true;
			}
			catch (IOException ex) {}
		}

		Terminal.updateStatus(Status.FAIL, "Could not detect assignment name from test-all.sh, please specify manually:"
			+ "\n    ./szumszum.jar AssignmentName");
		Terminal.exitStage();

		return false;
	}

}
