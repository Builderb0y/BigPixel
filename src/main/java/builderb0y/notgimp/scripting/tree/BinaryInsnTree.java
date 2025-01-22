package builderb0y.notgimp.scripting.tree;

import java.lang.invoke.MethodHandle;

import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorOperators.Binary;

import builderb0y.notgimp.scripting.parsing.ScriptHandlers;
import builderb0y.notgimp.scripting.util.BinaryCombiner.BinaryValue;
import builderb0y.notgimp.scripting.util.ConstantFolder;
import builderb0y.notgimp.scripting.util.VectorOpCompiler;

public class BinaryInsnTree extends InsnTree {

	public final InsnTree left, right;
	public final Binary operator;
	public final CodeEmitter emitter;

	public BinaryInsnTree(InsnTree left, InsnTree right, Binary operator) {
		BinaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.binary(left.type, right.type, operator);
		super(emitter.out());
		this.left = ScriptHandlers.cast(left, emitter.left());
		this.right = ScriptHandlers.cast(right, emitter.right());
		this.operator = operator;
		this.emitter = emitter.value();
		if (this.left == null || this.right == null) {
			throw new AssertionError(STR."\{left} \{operator.operatorName()} \{right} = null");
		}
	}

	public static InsnTree create(InsnTree left, InsnTree right, VectorOperators.Binary operator) {
		if (left instanceof ConstantInsnTree l && right instanceof ConstantInsnTree r) {
			BinaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.binary(left.type, right.type, operator);
			try {
				return new ConstantInsnTree(handle.out(), handle.value().invoke(l.value, r.value));
			}
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
		else {
			return new BinaryInsnTree(left, right, operator);
		}
	}

	public static InsnTree add(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.ADD);
	}

	public static InsnTree sub(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.SUB);
	}

	public static InsnTree mul(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.MUL);
	}

	public static InsnTree div(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.DIV);
	}

	public static InsnTree shl(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.LSHL);
	}

	public static InsnTree shr(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.ASHR);
	}

	public static InsnTree ushr(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.LSHR);
	}

	public static InsnTree and(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.AND);
	}

	public static InsnTree or(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.OR);
	}

	public static InsnTree xor(InsnTree left, InsnTree right) {
		return create(left, right, VectorOperators.XOR);
	}

	public static InsnTree pow(InsnTree left, InsnTree right) {
		if (right instanceof ConstantInsnTree rconst && rconst.value instanceof Number number && number.doubleValue() == 2.0D) {
			if (left instanceof ConstantInsnTree lconst) {
				BinaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.binary(left.type, left.type, VectorOperators.MUL);
				try {
					return new ConstantInsnTree(handle.out(), handle.value().invoke(lconst.value, lconst.value));
				}
				catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			}
			else {
				return new SquareInsnTree(left);
			}
		}
		else {
			return create(left, right, VectorOperators.POW);
		}
	}

	@Override
	public void emitBytecode(Context context) {
		this.left.emitBytecode(context);
		this.right.emitBytecode(context);
		this.emitter.emitBytecode(context);
	}
}