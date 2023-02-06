package szumszum.stages;

import java.util.HashMap;

import szumszum.Status;
import szumszum.Terminal;

public class Benchmark implements IStage {

	public static Benchmark instance;

	private HashMap<Integer, Integer> memo;

	private int fib(int n) {
		int a = 0, b = 1;

		for (int i = 0; i < n; ++i) {
			int c = b;
			b += a;
			a = c;
		}

		int res = a;
		memo.put(n, res);
		return res;
	}

	private long modpow(long base, long pow, long mod) {
		if (pow == 0) {
			return 1;
		}
		else if (pow % 2 == 0) {
			long x = modpow(base, pow / 2, mod);
			return x * x % mod;
		}
		else {
			return modpow(base, pow - 1, mod) * base % mod;
		}
	}

	private double computeScaleFactor() {
		long start = System.nanoTime();

		for (int j = 0; j < 2; ++j) {
			memo = new HashMap<>();
			int N = 49_999;
			for (int i = 0; i < N; ++i) {
				fib((int)(i * modpow(2, N - 2, N) % N) + 1);
			}
		}

		long end2 = System.nanoTime();

		return (end2 - start) / 1e9;
	}

	private double factor1 = -1, factor2 = -1;

	public boolean run() {
		if (factor1 == -1) {
			Terminal.enterStage("Running first benchmark");
			Terminal.flush();
			factor1 = computeScaleFactor();
		}
		else {
			Terminal.enterStage("Running second benchmark");
			Terminal.flush();
			factor2 = computeScaleFactor();
			Terminal.enterStage("Running second benchmark (result = " + scaleFactor() + ")");
		}

		Terminal.updateStatus(Status.DONE);
		Terminal.exitStage();

		return true;
	}

	public double scaleFactor() {
		return (factor1 + factor2) / 2;
	}

}
