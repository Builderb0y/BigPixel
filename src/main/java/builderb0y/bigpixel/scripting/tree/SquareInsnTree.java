package builderb0y.bigpixel.scripting.tree;

import jdk.incubator.vector.Vector;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import builderb0y.bigpixel.scripting.types.VectorType;

import static org.objectweb.asm.Opcodes.*;

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
				context.codeBuilder.math(GeneratorAdapter.MUL, type.componentType.unitDesc);
			}
			case VEC2, VEC3, VEC4 -> {
				Type vecType = Type.getType(type.holderClass());
				context.method.visitMethodInsn(
					INVOKEVIRTUAL,
					vecType.getInternalName(),
					"mul",
					Type.getMethodDescriptor(vecType, Type.getType(Vector.class)),
					false
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