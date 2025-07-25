package builderb0y.notgimp.scripting.types;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.VectorType.Vec;

@SuppressWarnings({ "unused", "MethodParameterNamingConvention", "StandardVariableNames" })
public class UtilityOperations {

	public static final FloatVector
		ignCoefficients = VectorOperations.float2_from_float_float(3.555713358F, 0.3092692451F);
	public static final DoubleVector
		//cos(golden angle) -sin(golden angle)
		//sin(golden angle)  cos(golden angle)
		ROT_PHI_X = VectorOperations.double2_from_double_double(-0.7373688780783197D,  0.6754902942615238D),
		ROT_PHI_Y = VectorOperations.double2_from_double_double(-0.6754902942615238D, -0.7373688780783197D);

	//////////////////////////////// noise ////////////////////////////////

	public static float ign_int2(@Vec(2) IntVector uv) {
		float dot = ((FloatVector)(uv.convert(VectorOperators.I2F, 0))).mul(ignCoefficients).reduceLanes(VectorOperators.ADD);
		return dot - (float)(Math.floor(dot));
	}

	public static double sinNoise(long rng, @Vec(2) DoubleVector uv) {
		double angle = RngOperations.rng_to_bounded_double(rng, Math.TAU);
		DoubleVector unit = VectorOperations.double2_from_double_double(Math.cos(angle), Math.sin(angle));
		double result = 0.0D;
		for (int iteration = 0; true;) {
			result += Math.sin(unit.mul(uv).reduceLanes(VectorOperators.ADD) + RngOperations.rng_to_bounded_double(RngOperations.stafford(rng += RngOperations.PHI64), Math.TAU));
			if (++iteration >= 64) break;
			unit = ROT_PHI_X.mul(unit.lane(0)).add(ROT_PHI_Y.mul(unit.lane(1)));
		}
		return result;
	}

	//////////////////////////////// hue ////////////////////////////////

	public static @Vec(3) FloatVector hue_float(float hue) {
		hue *= 6.0F;
		return VectorOperations.float3_from_float_float_float(
			Util.clampF(Math.abs(hue - 3.0F) - 1.0F),
			Util.clampF(2.0F - Math.abs(hue - 2.0F)),
			Util.clampF(2.0F - Math.abs(hue - 4.0F))
		);
	}

	public static @Vec(3) FloatVector smoothHue_float(float hue) {
		FloatVector result = VectorOperations.float3_from_float(hue * ((float)(Math.TAU)));
		result = result.sub(VectorOperations.float3_from_float_float_float(0.0F, (float)(Math.TAU / 3.0F), (float)(Math.TAU * 2.0D / 3.0D)));
		result = result.lanewise(VectorOperators.COS);
		result = result.fma(0.5F, 0.5F);
		result = result.mul(result);
		result = VectorOperations.normalize_float3(result);
		result = result.sqrt();
		return result;
	}

	//////////////////////////////// cliff ////////////////////////////////

	public static float cliff_float_float(float value, float coefficient) {
		float cx = value * coefficient;
		return cx / (cx - value + 1.0F);
	}

	public static @Vec(2) FloatVector cliff_float2_float(@Vec(2) FloatVector value, float coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static @Vec(3) FloatVector cliff_float3_float(@Vec(3) FloatVector value, float coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static @Vec(4) FloatVector cliff_float4_float(@Vec(4) FloatVector value, float coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static @Vec(2) FloatVector cliff_float2_float2(@Vec(2) FloatVector value, @Vec(2) FloatVector coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static @Vec(3) FloatVector cliff_float3_float3(@Vec(3) FloatVector value, @Vec(3) FloatVector coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static @Vec(4) FloatVector cliff_float4_float4(@Vec(4) FloatVector value, @Vec(4) FloatVector coefficient) {
		FloatVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0F));
	}

	public static double cliff_double_double(double value, double coefficient) {
		double cx = value * coefficient;
		return cx / (cx - value + 1.0D);
	}

	public static @Vec(2) DoubleVector cliff_double2_double(@Vec(2) DoubleVector value, double coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	public static @Vec(3) DoubleVector cliff_double3_double(@Vec(3) DoubleVector value, double coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	public static @Vec(4) DoubleVector cliff_double4_double(@Vec(4) DoubleVector value, double coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	public static @Vec(2) DoubleVector cliff_double2_double2(@Vec(2) DoubleVector value, @Vec(2) DoubleVector coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	public static @Vec(3) DoubleVector cliff_double3_double3(@Vec(3) DoubleVector value, @Vec(3) DoubleVector coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	public static @Vec(4) DoubleVector cliff_double4_double4(@Vec(4) DoubleVector value, @Vec(4) DoubleVector coefficient) {
		DoubleVector cx = value.mul(coefficient);
		return cx.div(cx.sub(value).add(1.0D));
	}

	//////////////////////////////// smooth ////////////////////////////////

	public static float smooth_float(float x) {
		return x * x * (x * -2.0F + 3.0F);
	}

	public static @Vec(2) FloatVector smooth_float2(@Vec(2) FloatVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static @Vec(3) FloatVector smooth_float3(@Vec(3) FloatVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static @Vec(4) FloatVector smooth_float4(@Vec(4) FloatVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static double smooth_double(double x) {
		return x * x * (x * -2.0F + 3.0F);
	}

	public static @Vec(2) DoubleVector smooth_double2(@Vec(2) DoubleVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static @Vec(3) DoubleVector smooth_double3(@Vec(3) DoubleVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static @Vec(4) DoubleVector smooth_double4(@Vec(4) DoubleVector x) {
		return x.mul(x).mul(x.mul(-2.0F).add(3.0F));
	}

	public static float smoother_float(float x) {
		return ((x * 6.0F - 15.0F) * x + 10.0F) * x * x * x;
	}

	public static @Vec(2) FloatVector smoother_float2(@Vec(2) FloatVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	public static @Vec(3) FloatVector smoother_float3(@Vec(3) FloatVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	public static @Vec(4) FloatVector smoother_float4(@Vec(4) FloatVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	public static double smoother_double(double x) {
		return ((x * 6.0F - 15.0F) * x + 10.0F) * x * x * x;
	}

	public static @Vec(2) DoubleVector smoother_double2(@Vec(2) DoubleVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	public static @Vec(3) DoubleVector smoother_double3(@Vec(3) DoubleVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	public static @Vec(4) DoubleVector smoother_double4(@Vec(4) DoubleVector x) {
		return x.mul(6.0F).sub(15.0F).mul(x).add(10.0F).mul(x).mul(x).mul(x);
	}

	//////////////////////////////// projections ////////////////////////////////

	public static float projectLineFrac_float_float_float(float a, float b, float f) {
		return (f - a) / (b - a);
	}

	public static float projectLineDist_float_float_float(float a, float b, float f) {
		return f - a;
	}

	public static float projectLinePos_float_float_float(float a, float b, float f) {
		return f;
	}

	public static float projectLineOffsetToPos_float_float_float(float a, float b, float f) {
		return 0.0F;
	}

	public static float projectLineOffsetFromPos_float_float_float(float a, float b, float f) {
		return 0.0F;
	}

	public static float projectLineOffsetDist_float_float_float(float a, float b, float f) {
		return 0.0F;
	}

	public static float projectLineFrac_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float2_float2(f, b) / VectorOperations.lengthSquared_float2(b);
	}

	public static float projectLineDist_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float2_float2(f, b) / VectorOperations.length_float2(b);
	}

	public static @Vec(2) FloatVector projectLinePos_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float2_float2(f, b) / VectorOperations.lengthSquared_float2(b)).add(a);
	}

	public static @Vec(2) FloatVector projectLineOffsetToPos_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float2_float2(f, b) / VectorOperations.lengthSquared_float2(b)).sub(f);
	}

	public static @Vec(2) FloatVector projectLineOffsetFromPos_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_float2_float2(f, b) / VectorOperations.lengthSquared_float2(b)));
	}

	public static float projectLineOffsetDist_float2_float2_float2(@Vec(2) FloatVector a, @Vec(2) FloatVector b, @Vec(2) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_float2(f.sub(b.mul(VectorOperations.dot_float2_float2(f, b) / VectorOperations.lengthSquared_float2(b))));
	}

	public static float projectLineFrac_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float3_float3(f, b) / VectorOperations.lengthSquared_float3(b);
	}

	public static float projectLineDist_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float3_float3(f, b) / VectorOperations.length_float3(b);
	}

	public static @Vec(3) FloatVector projectLinePos_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float3_float3(f, b) / VectorOperations.lengthSquared_float3(b)).add(a);
	}

	public static @Vec(3) FloatVector projectLineOffsetToPos_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float3_float3(f, b) / VectorOperations.lengthSquared_float3(b)).sub(f);
	}

	public static @Vec(3) FloatVector projectLineOffsetFromPos_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_float3_float3(f, b) / VectorOperations.lengthSquared_float3(b)));
	}

	public static float projectLineOffsetDist_float3_float3_float3(@Vec(3) FloatVector a, @Vec(3) FloatVector b, @Vec(3) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_float3(f.sub(b.mul(VectorOperations.dot_float3_float3(f, b) / VectorOperations.lengthSquared_float3(b))));
	}

	public static float projectLineFrac_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float4_float4(f, b) / VectorOperations.lengthSquared_float4(b);
	}

	public static float projectLineDist_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_float4_float4(f, b) / VectorOperations.length_float4(b);
	}

	public static @Vec(4) FloatVector projectLinePos_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float4_float4(f, b) / VectorOperations.lengthSquared_float4(b)).add(a);
	}

	public static @Vec(4) FloatVector projectLineOffsetToPos_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_float4_float4(f, b) / VectorOperations.lengthSquared_float4(b)).sub(f);
	}

	public static @Vec(4) FloatVector projectLineOffsetFromPos_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_float4_float4(f, b) / VectorOperations.lengthSquared_float4(b)));
	}

	public static float projectLineOffsetDist_float4_float4_float4(@Vec(4) FloatVector a, @Vec(4) FloatVector b, @Vec(4) FloatVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_float4(f.sub(b.mul(VectorOperations.dot_float4_float4(f, b) / VectorOperations.lengthSquared_float4(b))));
	}

	public static double projectLineFrac_double_double_double(double a, double b, double f) {
		return (f - a) / (b - a);
	}

	public static double projectLineDist_double_double_double(double a, double b, double f) {
		return f - a;
	}

	public static double projectLinePos_double_double_double(double a, double b, double f) {
		return f;
	}

	public static double projectLineOffsetToPos_double_double_double(double a, double b, double f) {
		return 0.0F;
	}

	public static double projectLineOffsetFromPos_double_double_double(double a, double b, double f) {
		return 0.0F;
	}

	public static double projectLineOffsetDist_double_double_double(double a, double b, double f) {
		return 0.0F;
	}

	public static double projectLineFrac_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double2_double2(f, b) / VectorOperations.lengthSquared_double2(b);
	}

	public static double projectLineDist_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double2_double2(f, b) / VectorOperations.length_double2(b);
	}

	public static @Vec(2) DoubleVector projectLinePos_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double2_double2(f, b) / VectorOperations.lengthSquared_double2(b)).add(a);
	}

	public static @Vec(2) DoubleVector projectLineOffsetToPos_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double2_double2(f, b) / VectorOperations.lengthSquared_double2(b)).sub(f);
	}

	public static @Vec(2) DoubleVector projectLineOffsetFromPos_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_double2_double2(f, b) / VectorOperations.lengthSquared_double2(b)));
	}

	public static double projectLineOffsetDist_double2_double2_double2(@Vec(2) DoubleVector a, @Vec(2) DoubleVector b, @Vec(2) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_double2(f.sub(b.mul(VectorOperations.dot_double2_double2(f, b) / VectorOperations.lengthSquared_double2(b))));
	}

	public static double projectLineFrac_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double3_double3(f, b) / VectorOperations.lengthSquared_double3(b);
	}

	public static double projectLineDist_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double3_double3(f, b) / VectorOperations.length_double3(b);
	}

	public static @Vec(3) DoubleVector projectLinePos_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double3_double3(f, b) / VectorOperations.lengthSquared_double3(b)).add(a);
	}

	public static @Vec(3) DoubleVector projectLineOffsetToPos_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double3_double3(f, b) / VectorOperations.lengthSquared_double3(b)).sub(f);
	}

	public static @Vec(3) DoubleVector projectLineOffsetFromPos_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_double3_double3(f, b) / VectorOperations.lengthSquared_double3(b)));
	}

	public static double projectLineOffsetDist_double3_double3_double3(@Vec(3) DoubleVector a, @Vec(3) DoubleVector b, @Vec(3) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_double3(f.sub(b.mul(VectorOperations.dot_double3_double3(f, b) / VectorOperations.lengthSquared_double3(b))));
	}

	public static double projectLineFrac_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double4_double4(f, b) / VectorOperations.lengthSquared_double4(b);
	}

	public static double projectLineDist_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.dot_double4_double4(f, b) / VectorOperations.length_double4(b);
	}

	public static @Vec(4) DoubleVector projectLinePos_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double4_double4(f, b) / VectorOperations.lengthSquared_double4(b)).add(a);
	}

	public static @Vec(4) DoubleVector projectLineOffsetToPos_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return b.mul(VectorOperations.dot_double4_double4(f, b) / VectorOperations.lengthSquared_double4(b)).sub(f);
	}

	public static @Vec(4) DoubleVector projectLineOffsetFromPos_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return f.sub(b.mul(VectorOperations.dot_double4_double4(f, b) / VectorOperations.lengthSquared_double4(b)));
	}

	public static double projectLineOffsetDist_double4_double4_double4(@Vec(4) DoubleVector a, @Vec(4) DoubleVector b, @Vec(4) DoubleVector f) {
		b = b.sub(a); f = f.sub(a); return VectorOperations.length_double4(f.sub(b.mul(VectorOperations.dot_double4_double4(f, b) / VectorOperations.lengthSquared_double4(b))));
	}
}