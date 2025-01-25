package builderb0y.notgimp.scripting.tree;

import java.lang.invoke.MethodHandle;

import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorOperators.Binary;

import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.BinaryCombiner.BinaryValue;
import builderb0y.notgimp.scripting.util.ConstantFolder;
import builderb0y.notgimp.scripting.util.VectorOpCompiler;

public class BinaryInsnTree extends InsnTree {

	public final InsnTree[] trees;
	public final Binary operator;
	public final CodeEmitter emitter;

	public BinaryInsnTree(VectorType type, InsnTree[] trees, Binary operator, CodeEmitter emitter) {
		super(type);
		this.emitter = emitter;
		this.trees = trees;
		this.operator = operator;
	}

	public BinaryInsnTree(InsnTree left, InsnTree right, Binary operator) {
		BinaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.binary(left.type(), right.type(), operator);
		InsnTree left2 = left.cast(emitter.left());
		InsnTree right2 = right.cast(emitter.right());
		if (left2 == null || right2 == null) {
			throw new AssertionError(STR."\{left} \{operator.operatorName()} \{right} = null");
		}
		this(emitter.out(), new InsnTree[] { left2, right2 }, operator, emitter.value());
	}

	public BinaryInsnTree(InsnTree both, Binary operator) {
		BinaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.binary(both.types()[0], both.types()[1], operator);
		InsnTree both2 = both.cast(emitter.left(), emitter.right());
		if (both2 == null) {
			throw new AssertionError(STR."\{operator.operatorName()}(\{both}) = null");
		}
		this(emitter.out(), new InsnTree[] { both2 }, operator, emitter.value());
	}

	public static InsnTree create(InsnTree left, InsnTree right, VectorOperators.Binary operator) {
		if (left instanceof ConstantInsnTree l && right instanceof ConstantInsnTree r) {
			BinaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.binary(left.type(), right.type(), operator);
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

	public static InsnTree createUnpacked(InsnTree both, VectorOperators.Binary operator) {
		if (both.types().length != 2) throw new ArityException("2 operands are required");
		return new BinaryInsnTree(both, operator);
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
				BinaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.binary(left.type(), left.type(), VectorOperators.MUL);
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
		for (InsnTree tree : this.trees) {
			tree.emitBytecode(context);
		}
		this.emitter.emitBytecode(context);
	}
}