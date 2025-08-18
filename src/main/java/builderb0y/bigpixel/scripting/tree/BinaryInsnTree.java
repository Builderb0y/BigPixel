package builderb0y.bigpixel.scripting.tree;

import java.lang.invoke.MethodHandle;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.BinaryCombiner.BinaryValue;
import builderb0y.bigpixel.scripting.util.BinaryOperatorWrapper;
import builderb0y.bigpixel.scripting.util.ConstantFolder;
import builderb0y.bigpixel.scripting.util.VectorOpCompiler;

public class BinaryInsnTree extends InsnTree {

	public final InsnTree[] trees;
	public final CodeEmitter emitter;

	public BinaryInsnTree(VectorType type, InsnTree[] trees, CodeEmitter emitter) {
		super(type);
		this.emitter = emitter;
		this.trees = trees;
	}

	public BinaryInsnTree(InsnTree left, InsnTree right, BinaryOperatorWrapper operator) {
		BinaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.binary(left.type(), right.type(), operator);
		InsnTree left2 = left.cast(emitter.left());
		InsnTree right2 = right.cast(emitter.right());
		if (left2 == null || right2 == null) {
			throw new AssertionError(left + " " + operator.name() + " " + right + " = null");
		}
		this(emitter.out(), new InsnTree[] { left2, right2 }, emitter.value());
	}

	public BinaryInsnTree(InsnTree both, BinaryOperatorWrapper operator) {
		BinaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.binary(both.types()[0], both.types()[1], operator);
		InsnTree both2 = both.cast(emitter.left(), emitter.right());
		if (both2 == null) {
			throw new AssertionError(operator.name() + " (" + both + ") = null");
		}
		this(emitter.out(), new InsnTree[] { both2 }, emitter.value());
	}

	public static InsnTree create(InsnTree left, InsnTree right, BinaryOperatorWrapper operator) {
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

	public static InsnTree createUnpacked(InsnTree both, BinaryOperatorWrapper operator) {
		if (both.types().length != 2) throw new ArityException("2 operands are required");
		return new BinaryInsnTree(both, operator);
	}

	public static InsnTree add(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.ADD);
	}

	public static InsnTree sub(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.SUB);
	}

	public static InsnTree mul(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.MUL);
	}

	public static InsnTree div(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.DIV);
	}

	public static InsnTree mod(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.MOD);
	}

	public static InsnTree shl(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.LSHL);
	}

	public static InsnTree shr(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.ASHR);
	}

	public static InsnTree ushr(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.LSHR);
	}

	public static InsnTree and(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.AND);
	}

	public static InsnTree or(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.OR);
	}

	public static InsnTree xor(InsnTree left, InsnTree right) {
		return create(left, right, BinaryOperatorWrapper.XOR);
	}

	public static InsnTree pow(InsnTree left, InsnTree right) {
		if (right instanceof ConstantInsnTree rconst && rconst.value instanceof Number number && number.doubleValue() == 2.0D) {
			if (left instanceof ConstantInsnTree lconst) {
				BinaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.binary(left.type(), left.type(), BinaryOperatorWrapper.MUL);
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
			return create(left, right, BinaryOperatorWrapper.POW);
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