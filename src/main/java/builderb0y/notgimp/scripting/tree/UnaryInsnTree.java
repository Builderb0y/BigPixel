package builderb0y.notgimp.scripting.tree;

import java.lang.invoke.MethodHandle;

import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.scripting.parsing.ScriptHandlers;
import builderb0y.notgimp.scripting.util.BinaryCombiner.UnaryValue;
import builderb0y.notgimp.scripting.util.ConstantFolder;
import builderb0y.notgimp.scripting.util.VectorOpCompiler;

public class UnaryInsnTree extends InsnTree {

	public final InsnTree operand;
	public final VectorOperators.Unary operator;
	public final CodeEmitter emitter;

	public UnaryInsnTree(InsnTree operand, VectorOperators.Unary operator) {
		UnaryValue<CodeEmitter> emitter = VectorOpCompiler.INSTANCE.unary(operand.type(), operator);
		super(emitter.out());
		this.operand = operand.cast(emitter.in());
		this.operator = operator;
		this.emitter = emitter.value();
		if (this.operand == null) {
			throw new AssertionError(STR."\{operator.operatorName()}\{operand} = null");
		}
	}

	public static InsnTree create(InsnTree operand, VectorOperators.Unary operator) {
		if (operand instanceof ConstantInsnTree constant) {
			UnaryValue<MethodHandle> handle = ConstantFolder.INSTANCE.unary(operand.type(), operator);
			try {
				return new ConstantInsnTree(handle.out(), handle.value().invoke(constant.value));
			}
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
		else {
			return new UnaryInsnTree(operand, operator);
		}
	}

	public static InsnTree negate(InsnTree operand) {
		return create(operand, VectorOperators.NEG);
	}

	public static InsnTree not(InsnTree operand) {
		return create(operand, VectorOperators.NOT);
	}

	@Override
	public void emitBytecode(Context context) {
		this.operand.emitBytecode(context);
		this.emitter.emitBytecode(context);
	}
}