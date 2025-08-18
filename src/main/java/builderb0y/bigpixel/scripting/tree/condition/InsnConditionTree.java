package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;
import builderb0y.bigpixel.scripting.tree.InsnTree;

public class InsnConditionTree extends ConditionTree {

	public final InsnTree condition;

	public InsnConditionTree(InsnTree condition) {
		this.condition = condition;
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.condition.emitBytecode(context);
		if (ifTrue != null) {
			context.codeBuilder.ifne(ifTrue);
			if (ifFalse != null) {
				context.codeBuilder.goto_(ifFalse);
			}
		}
		else {
			context.codeBuilder.ifeq(ifFalse);
		}
	}

	@Override
	public InsnTree toInsn() {
		return this.condition;
	}
}