package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.GroupShape;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class SwizzleInsnTree extends InsnTree {

	public final InsnTree vector;
	public final int[] indices;
	public final MethodInfo lane;

	public SwizzleInsnTree(InsnTree vector, int[] indices) {
		if (vector.type().shape == GroupShape.UNIT) {
			throw new IllegalArgumentException("Can't swizzle scalars");
		}
		if (indices.length > 4) {
			throw new IllegalArgumentException("Invalid swizzle length: " + indices.length);
		}
		for (int index : indices) {
			if (index < 0 || index > 3) {
				throw new IllegalArgumentException("Invalid swizzle: " + index);
			}
		}

		super(Util.fill(new VectorType[indices.length], VectorType.get(vector.type().componentType, GroupShape.UNIT)));
		this.vector = vector;
		this.indices = indices;
		this.lane = new MethodInfo(vector.type().holderClass(), "lane", int.class);
	}

	public static InsnTree swizzle(InsnTree vector, String swizzle) {
		if (vector.type().shape == GroupShape.UNIT) {
			return null;
		}
		int swizzleLength = swizzle.length();
		if (swizzleLength == 0 || swizzleLength > 4) return null;
		int[] indices = new int[swizzleLength];
		for (int swizzleIndex = 0; swizzleIndex < swizzleLength; swizzleIndex++) {
			switch (swizzle.charAt(swizzleIndex)) {
				case 'x', 'r' -> indices[swizzleIndex] = 0;
				case 'y', 'g' -> indices[swizzleIndex] = 1;
				case 'z', 'b' -> indices[swizzleIndex] = 2;
				case 'w', 'a' -> indices[swizzleIndex] = 3;
				default -> { return null; }
			}
		}
		SwizzleInsnTree unwrapped = new SwizzleInsnTree(vector, indices);
		if (swizzleLength == 1) return unwrapped;
		StringBuilder fullName = new StringBuilder();
		VectorType rebuiltType = VectorType.get(
			vector.type().componentType,
			switch (swizzleLength) {
				case 2 -> GroupShape.VEC2;
				case 3 -> GroupShape.VEC3;
				case 4 -> GroupShape.VEC4;
				default -> throw new AssertionError();
			}
		);
		fullName.append(rebuiltType.name).append("_from");
		VectorType unit = VectorType.get(vector.type().componentType, GroupShape.UNIT);
		for (int i = 0; i < swizzleLength; i++) {
			fullName.append('_').append(unit.name);
		}
		MethodInfo method = new MethodInfo(VectorOperations.class, fullName.toString());
		return new VectorConstructorInsnTree(rebuiltType, new InsnTree[] { unwrapped }, method);
	}

	public static InsnTree unpack(InsnTree vector) {
		//todo: fix "uv.xxx" causing this code to break.
		/*
		if (vector instanceof VectorConstructorInsnTree constructor) {
			if (constructor.arguments.length == 1) {
				return new DupInsnTree(constructor.arguments[0], constructor.type().shape.rows);
			}
			else {
				return new UnpackedInsnTree(constructor.arguments);
			}
		}
		*/
		GroupShape shape = vector.type().shape;
		if (shape == GroupShape.UNIT) return vector;
		int rows = shape.rows;
		int[] iota = new int[rows];
		for (int index = 0; index < rows; index++) {
			iota[index] = index;
		}
		return new SwizzleInsnTree(vector, iota);
	}

	@Override
	public void emitBytecode(Context context) {
		this.vector.emitBytecode(context);
		boolean doubleWidth = this.types()[0].componentType.isDoubleWidth;
		for (int laneIndex = 0, lanes = this.indices.length; laneIndex < lanes; laneIndex++) {
			if (laneIndex != lanes - 1) {
				context.codeBuilder.dup();
			}
			int constant = this.indices[laneIndex];
			switch (constant) {
				case 0 -> context.codeBuilder.iconst_0();
				case 1 -> context.codeBuilder.iconst_1();
				case 2 -> context.codeBuilder.iconst_2();
				case 3 -> context.codeBuilder.iconst_3();
				default -> throw new IllegalStateException("Swizzle out of range: " + constant);
			}
			this.lane.emitBytecode(context);
			if (laneIndex != lanes - 1) {
				if (doubleWidth) {
					context.codeBuilder.dup2_x1();
					context.codeBuilder.pop2();
				}
				else {
					context.codeBuilder.swap();
				}
			}
		}
	}
}