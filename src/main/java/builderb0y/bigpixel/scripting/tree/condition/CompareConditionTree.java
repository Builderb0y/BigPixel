package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;
import builderb0y.bigpixel.scripting.tree.ConstantInsnTree;
import builderb0y.bigpixel.scripting.tree.InsnTree;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.types.VectorType.GroupShape;

public class CompareConditionTree extends ConditionTree {

	public final InsnTree left, right;
	public final CompareMode mode;

	public CompareConditionTree(InsnTree left, InsnTree right, CompareMode mode) {
		if (left.type() != right.type()) {
			throw new IllegalArgumentException("Can't compare " + left + " and " + right);
		}
		if (left.type().shape != GroupShape.UNIT) {
			throw new IllegalArgumentException("Comparing vectors is not yet supported");
		}
		this.left = left;
		this.right = right;
		this.mode = mode;
	}

	public static ConditionTree create(InsnTree left, InsnTree right, CompareMode mode) {
		return new CompareConditionTree(left, right, mode);
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		this.left.emitBytecode(context);
		if (this.right instanceof ConstantInsnTree constant && constant.type() == VectorType.INT && constant.<Integer>get() == 0) {
			this.mode.emitIntZero(context, ifTrue, ifFalse);
		}
		else {
			this.right.emitBytecode(context);
			switch (this.left.type().componentType) {
				case INT -> this.mode.emitInt(context, ifTrue, ifFalse);
				case LONG -> this.mode.emitLong(context, ifTrue, ifFalse);
				case FLOAT -> this.mode.emitFloat(context, ifTrue, ifFalse);
				case DOUBLE -> this.mode.emitDouble(context, ifTrue, ifFalse);
				default -> throw new IllegalStateException(this.left.type().toString());
			}
		}
	}
}