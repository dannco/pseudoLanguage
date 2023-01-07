package utils;

import java.util.Random;
import java.util.stream.IntStream;

public class Rng {

	private Rng() { }

	public static Random rng = new Random();
	public static void setRng(long seed) {
		rng.setSeed(seed);
	}

	public static int intInRange(int min, int max) {
		if (min < 0 || max < min) throw new IllegalArgumentException(
				"invalid bounds for min/max"
		);
		return rng.nextInt(1 + max - min) + min;
	}

	public static int Roll(int d, int v) {
		return IntStream.range(0, d).map(i -> intInRange(1, v)).sum();
	}


}
