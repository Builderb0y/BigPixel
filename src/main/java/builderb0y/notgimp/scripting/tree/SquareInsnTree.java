package builderb0y.notgimp.scripting.tree;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import jdk.incubator.vector.Vector;

import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.VectorType;

public class SquareInsnTree extends InsnTree {

	public final InsnTree operand;

	public SquareInsnTree(InsnTree operand) {
		VectorType type = operand.type();
		if (type == VectorType.VOID) {
			throw new IllegalArgumentException("Can't square void");
		}
		super(type);
		this.operand = operand;
	}

	@Override
	public void emitBytecode(Context context) {
		this.operand.emitBytecode(context);
		VectorType type = this.type();
		if (type.isReallyDoubleWidth()) context.codeBuilder.dup2();
		else context.codeBuilder.dup();
		switch (type.shape) {
			case UNIT -> {
				switch (type.componentType) {
					case INT -> context.codeBuilder.imul();
					case LONG -> context.codeBuilder.lmul();
					case FLOAT -> context.codeBuilder.fmul();
					case DOUBLE -> context.codeBuilder.dmul();
					default -> throw new IllegalStateException(type.toString());
				}
			}
			case VEC2, VEC3, VEC4 -> {
				ClassDesc vecType = Util.desc(type.holderClass());
				context.codeBuilder.invokevirtual(
					vecType,
					"mul",
					MethodTypeDesc.of(vecType, Util.desc(Vector.class))
				);
			}
			/*
			case MAT2, MAT4, MAT8 -> {
				ClassDesc matType = Util.desc(this.type.holderClass());
				context.codeBuilder.getstatic(Util.desc(VectorOperators.class), "MUL", Util.desc(VectorOperators.Associative.class));
				context.codeBuilder.invokevirtual(
					matType,
					"lanewise",
					MethodTypeDesc.of(matType, matType, Util.desc(VectorOperators.Binary.class))
				);
			}
			*/
		}
	}
}