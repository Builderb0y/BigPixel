package builderb0y.notgimp.scripting.tree;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

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
		switch (this.type()) {
			case INT -> {
				context.codeBuilder.constantInstruction(this.<Integer>get());
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
				context.codeBuilder.constantInstruction(this.<Long>get());
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
				context.codeBuilder.constantInstruction(this.<Float>get());
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
				context.codeBuilder.constantInstruction(this.<Double>get());
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
		}
	}

	@Override
	public @Nullable InsnTree cast(VectorType... types) {
		if (Arrays.equals(this.types, types)) return this;
		if (types.length != 1) return null;
		if (this.types[0].componentType.isFloatingPoint() == types[0].componentType.isFloatingPoint()) {
			return new ConstantInsnTree(types[0], this.value);
		}
		else {
			return null;
		}
	}
}