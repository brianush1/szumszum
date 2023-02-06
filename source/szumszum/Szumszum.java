package szumszum;
import szumszum.stages.Benchmark;
import szumszum.stages.DetectAssignment;
import szumszum.stages.IStage;
import szumszum.stages.RunTests;

public class Szumszum {

	public static void main(String[] args) {
		Terminal.initialize(args);

		IStage[] stages = {
			Benchmark.instance = new Benchmark(),
			new DetectAssignment(),
			new RunTests()
		};

		Terminal.addSpacing();

		for (IStage stage : stages) {
			boolean proceed = stage.run();
			if (!proceed) {
				break;
			}
		}

		Terminal.addSpacing();

		Terminal.close();

		System.exit(0);
	}

}
