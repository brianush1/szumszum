package szumszum.stages;

import szumszum.Status;
import szumszum.Terminal;

public class CheckUpdates implements IStage {

	public boolean run() {
		Terminal.enterStage("Checking for updates");

		Terminal.updateStatus(Status.FAIL, "not implemented");
		Terminal.exitStage();

		return true;
	}

}
