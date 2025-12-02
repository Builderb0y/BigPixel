package builderb0y.bigpixel.scripting.tree;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigpixel.scripting.tree.condition.ConditionTree;
import builderb0y.bigpixel.scripting.types.VectorType;

public class IfElseInsnTree extends InsnTree {

	public final ConditionTree condition;
	public final InsnTree ifTrue, ifFalse;
	public final boolean statement;

	public IfElseInsnTree(ConditionTree condition, InsnTree ifTrue, InsnTree ifFalse, boolean statement) {
		if (statement) {
			ifTrue = ifTrue.castToVoid();
			ifFalse = ifFalse.castToVoid();
		}
		else {
			if (!Arrays.equals(ifTrue.types(), ifFalse.types())) {
				throw new IllegalArgumentException("Mismatched types: " + ifTrue + " and " + ifFalse);
			}
		}
		super(ifTrue.types());
		this.condition = condition;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
		this.statement = statement;
	}

	@Override
	public void emitBytecode(Context context) {
		Label falseBranch = context.codeBuilder.newLabel(), end = context.codeBuilder.newLabel();
		this.condition.emitBytecode(context, null, falseBranch);
		this.ifTrue.emitBytecode(context);
		if (!this.ifTrue.jumpsUnconditionally()) {
			context.codeBuilder.goTo(end);
		}
		context.codeBuilder.visitLabel(falseBranch);
		this.ifFalse.emitBytecode(context);
		context.codeBuilder.visitLabel(end);
	}

	@Override
	public boolean canBeStatement() {
		return this.statement;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.ifTrue.jumpsUnconditionally() && this.ifFalse.jumpsUnconditionally();
	}

	@Override
	public @Nullable InsnTree cast(VectorType... types) {
		if (Arrays.equals(this.types(), types)) return this;
		if (this.statement) return null;
		InsnTree ifTrue = this.ifTrue.cast(types);
		if (ifTrue == null) return null;
		InsnTree ifFalse = this.ifFalse.cast(types);
		if (ifFalse == null) return null;
		return new IfElseInsnTree(this.condition, ifTrue, ifFalse, false);
	}
}