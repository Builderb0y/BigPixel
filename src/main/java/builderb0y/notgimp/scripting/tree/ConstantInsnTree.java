package builderb0y.notgimp.scripting.tree;

import java.math.BigDecimal;
import java.math.BigInteger;

import builderb0y.notgimp.scripting.types.VectorType;

public class ConstantInsnTree extends InsnTree {

	public static final ConstantInsnTree
		TRUE = new ConstantInsnTree(VectorType.BOOLEAN, Boolean.TRUE),
		FALSE = new ConstantInsnTree(VectorType.BOOLEAN, Boolean.FALSE);

	public final Object value;

	public ConstantInsnTree(VectorType type, Object value) {
		super(type);
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	public <T> T get() {
		return (T)(this.value);
	}

	@Override
	public void emitBytecode(Context context) {
		switch (this.type) {
			case INT -> {
				int intValue = this.<BigInteger>get().intValueExact();
				switch (intValue) {
					case -1 -> context.codeBuilder.iconst_m1();
					case  0 -> context.codeBuilder.iconst_0();
					case  1 -> context.codeBuilder.iconst_1();
					case  2 -> context.codeBuilder.iconst_2();
					case  3 -> context.codeBuilder.iconst_3();
					case  4 -> context.codeBuilder.iconst_4();
					case  5 -> context.codeBuilder.iconst_5();
					default -> context.codeBuilder.bipush(intValue);
				}
			}
			case INT2 -> {
				throw new UnsupportedOperationException();
			}
			case INT3 -> {
				throw new UnsupportedOperationException();
			}
			case INT4 -> {
				throw new UnsupportedOperationException();
			}
			/*
			case INT2X2 -> {
				throw new UnsupportedOperationException();
			}
			case INT4X4 -> {
				throw new UnsupportedOperationException();
			}
			case INT8X8 -> {
				throw new UnsupportedOperationException();
			}
			*/
			case LONG -> {
				long longValue = this.<BigInteger>get().longValueExact();
				if (longValue == 0L) context.codeBuilder.lconst_0();
				else if (longValue == 1L) context.codeBuilder.lconst_1();
				else context.codeBuilder.ldc(longValue);
			}
			case LONG2 -> {
				throw new UnsupportedOperationException();
			}
			case LONG3 -> {
				throw new UnsupportedOperationException();
			}
			case LONG4 -> {
				throw new UnsupportedOperationException();
			}
			/*
			case LONG2X2 -> {
				throw new UnsupportedOperationException();
			}
			case LONG4X4 -> {
				throw new UnsupportedOperationException();
			}
			case LONG8X8 -> {
				throw new UnsupportedOperationException();
			}
			*/
			case FLOAT -> {
				float floatValue = this.<BigDecimal>get().floatValue();
				if (Float.floatToRawIntBits(floatValue) == 0) context.codeBuilder.fconst_0();
				else if (floatValue == 1.0F) context.codeBuilder.fconst_1();
				else if (floatValue == 2.0F) context.codeBuilder.fconst_2();
				else context.codeBuilder.ldc(floatValue);
			}
			case FLOAT2 -> {
				throw new UnsupportedOperationException();
			}
			case FLOAT3 -> {
				throw new UnsupportedOperationException();
			}
			case FLOAT4 -> {
				throw new UnsupportedOperationException();
			}
			/*
			case FLOAT2X2 -> {
				throw new UnsupportedOperationException();
			}
			case FLOAT4X4 -> {
				throw new UnsupportedOperationException();
			}
			case FLOAT8X8 -> {
				throw new UnsupportedOperationException();
			}
			*/
			case DOUBLE -> {
				double doubleValue = this.<BigDecimal>get().doubleValue();
				if (Double.doubleToRawLongBits(doubleValue) == 0) context.codeBuilder.dconst_0();
				else if (doubleValue == 1.0D) context.codeBuilder.dconst_1();
				else context.codeBuilder.ldc(doubleValue);
			}
			case DOUBLE2 -> {
				throw new UnsupportedOperationException();
			}
			case DOUBLE3 -> {
				throw new UnsupportedOperationException();
			}
			case DOUBLE4 -> {
				throw new UnsupportedOperationException();
			}
			/*
			case DOUBLE2X2 -> {
				throw new UnsupportedOperationException();
			}
			case DOUBLE4X4 -> {
				throw new UnsupportedOperationException();
			}
			case DOUBLE8X8 -> {
				throw new UnsupportedOperationException();
			}
			*/
			case BOOLEAN -> {
				if (this.<Boolean>get()) {
					context.codeBuilder.iconst_1();
				}
				else {
					context.codeBuilder.iconst_0();
				}
			}
			case BOOLEAN2 -> {
				throw new UnsupportedOperationException();
			}
			case BOOLEAN3 -> {
				throw new UnsupportedOperationException();
			}
			case BOOLEAN4 -> {
				throw new UnsupportedOperationException();
			}
			case BIGINT -> {
				throw new IllegalStateException("BigInteger not coerced");
			}
			case BIGDEC -> {
				throw new IllegalStateException("BigDecimal not coerced");
			}
		}
	}
}