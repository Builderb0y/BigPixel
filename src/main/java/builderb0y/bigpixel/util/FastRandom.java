package builderb0y.bigpixel.util;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.Range;

public class FastRandom implements RandomGenerator {

	public static final long PHI64 = 0x9E3779B97F4A7C15L;

	public long seed;

	public FastRandom(long seed) {
		this.seed = seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public FastRandom withSeed(long seed) {
		this.seed = seed;
		return this;
	}

	public int nextIntBits(@Range(from = 0, to = 32) int numberOfBits) {
		if (numberOfBits == 32) return this.nextInt();
		return this.nextInt() & ((1 << numberOfBits) - 1);
	}

	public long nextLongBits(@Range(from = 0, to = 64) int numberOfBits) {
		if (numberOfBits == 64) return this.nextLong();
		return this.nextLong() & ((1L << numberOfBits) - 1);
	}

	@Override
	public int nextInt() {
		return (int)(stafford(this.seed += PHI64));
	}

	@Override
	public long nextLong() {
		return stafford(this.seed += PHI64);
	}

	@Override
	public float nextFloat() {
		return toPositiveFloat(this.nextLong());
	}

	@Override
	public double nextDouble() {
		return toPositiveDouble(this.nextLong());
	}

	public static long stafford(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	public static int nextUniformInt(long seed) {
		return (int)(stafford(seed));
	}

	public static int toUniformInt(long permutedSeed) {
		return (int)(permutedSeed);
	}

	public static int nextPositiveInt(long seed) {
		return nextUniformInt(seed) & 0x7FFF_FFFF;
	}

	public static int toPositiveInt(long permutedSeed) {
		return ((int)(permutedSeed)) & 0x7FFF_FFFF;
	}

	public static long nextUniformLong(long seed) {
		return stafford(seed);
	}

	public static long toUniformLong(long permutedSeed) {
		return permutedSeed;
	}

	public static long nextPositiveLong(long seed) {
		return nextUniformLong(seed) & 0x7FFF_FFFF_FFFF_FFFFL;
	}

	public static long toPositiveLong(long permutedSeed) {
		return permutedSeed & 0x7FFF_FFFF_FFFF_FFFFL;
	}

	public static float nextPositiveFloat(long seed) {
		return (nextUniformLong(seed) >>> (64 - 24)) * 0x1.0p-24F;
	}

	public static float toPositiveFloat(long permutedSeed) {
		return (permutedSeed >>> (64 - 24)) * 0x1.0p-24F;
	}

	public static float nextUniformFloat(long seed) {
		return (nextUniformLong(seed) >> (64 - 25)) * 0x1.0p-24F;
	}

	public static float toUniformFloat(long permutedSeed) {
		return (permutedSeed >> (64 - 25)) * 0x1.0p-24F;
	}

	public static double nextPositiveDouble(long seed) {
		return (nextUniformLong(seed) >>> (64 - 53)) * 0x1.0p-53D;
	}

	public static double toPositiveDouble(long permutedSeed) {
		return (permutedSeed >>> (64 - 53)) * 0x1.0p-53D;
	}

	public static double nextUniformDouble(long seed) {
		return (nextUniformLong(seed) >> (64 - 54)) * 0x1.0p-53D;
	}

	public static double toUniformDouble(long permutedSeed) {
		return (permutedSeed >> (64 - 54)) * 0x1.0p-53D;
	}

	public static boolean nextBoolean(long seed) {
		return nextUniformInt(seed) < 0;
	}

	public static boolean toBoolean(long permutedSeed) {
		return toUniformInt(permutedSeed) < 0;
	}

	public static boolean nextChancedBoolean(long seed, float chance) {
		return chance > 0.0F && (chance >= 1.0F || nextPositiveFloat(seed) < chance);
	}

	public static boolean toChancedBoolean(long permutedSeed, float chance) {
		return chance > 0.0F && (chance >= 1.0F || toPositiveFloat(permutedSeed) < chance);
	}

	public static boolean nextChancedBoolean(long seed, double chance) {
		return chance > 0.0D && (chance >= 1.0D || nextPositiveDouble(seed) < chance);
	}

	public static boolean toChancedBoolean(long permutedSeed, double chance) {
		return chance > 0.0D && (chance >= 1.0D || toPositiveDouble(permutedSeed) < chance);
	}
}