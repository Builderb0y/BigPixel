package builderb0y.bigpixel.scripting.tree.condition;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;
import builderb0y.bigpixel.scripting.tree.InsnTree;

import static org.objectweb.asm.Opcodes.*;

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
			context.codeBuilder.ifZCmp(IFNE, ifTrue);
			if (ifFalse != null) {
				context.codeBuilder.goTo(ifFalse);
			}
		}
		else {
			context.codeBuilder.ifZCmp(IFEQ, ifFalse);
		}
	}

	@Override
	public InsnTree toInsn() {
		return this.condition;
	}
}