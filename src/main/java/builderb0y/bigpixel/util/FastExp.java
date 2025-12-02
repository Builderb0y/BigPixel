package builderb0y.bigpixel.util;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;

public class FastExp {

	public static final double
		LOGE2, //ln(2)
		LOG2E, //log2(e)
		TERM3, //coefficient for x^3
		TERM2, //coefficient for x^2
		TERM1, //coefficient for x^1
		TERM0; //coefficient for x^0

	static {
		final double ln2 = Math.log(2.0D);
		LOGE2 = ln2;
		LOG2E = 1.0D / ln2;
		TERM3 = 3.0D * ln2 - 2.0D;
		TERM2 = -4.0D * ln2 + 3.0D;
		TERM1 = ln2;
		TERM0 = 1.0D;
	}

	public static double fastExp2(double value) {
		if (Double.isNaN(value)) return Double.NaN;
		if (value < Double.MIN_EXPONENT) return 0.0F;
		//Double.MAX_VALUE would be closer to the true mathematical value,
		//but we return Double.POSITIVE_INFINITY instead to ensure consistency with Math.exp().
		if (value >= Double.MAX_EXPONENT + 1) return Double.POSITIVE_INFINITY;

		double floor = Math.floor(value);
		value -= floor;
		double cubicCurve = ((TERM3 * value + TERM2) * value + TERM1) * value + TERM0;
		long bits = Double.doubleToRawLongBits(cubicCurve);
		bits += ((long)(floor)) << 52;
		return Double.longBitsToDouble(bits);
	}

	public static double fastExp(double value) {
		return fastExp2(value * LOG2E);
	}

	public static DoubleVector fastExp2(DoubleVector vector) {
		//no VectorOperators.FLOOR :(
		double[] floorArray = vector.toArray();
		for (int index = 0, length = floorArray.length; index < length; index++) {
			floorArray[index] = Math.floor(floorArray[index]);
		}
		DoubleVector floor = DoubleVector.fromArray(vector.species(), floorArray, 0);
		DoubleVector result = vector.sub(floor);
		return (
			result
			.mul(TERM3)
			.add(TERM2)
			.mul(result)
			.add(TERM1)
			.mul(result)
			.add(TERM0)
			.reinterpretAsLongs()
			.add(
				floor
				.convert(VectorOperators.D2L, 0)
				.lanewise(VectorOperators.LSHL, 52L)
			)
			.reinterpretAsDoubles()
			.blend(0.0D, vector.compare(VectorOperators.LT, Double.MIN_EXPONENT))
			.blend(Double.POSITIVE_INFINITY, vector.compare(VectorOperators.GE, Double.MAX_EXPONENT + 1))
			.blend(Double.NaN, vector.test(VectorOperators.IS_NAN))
		);
	}

	public static DoubleVector fastExp(DoubleVector vector) {
		return fastExp2(vector.mul(LOG2E));
	}
}