package builderb0y.bigpixel.scripting.tree.condition;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;

public class AndConditionTree extends ConditionTree {

	public final ConditionTree left, right;

	public AndConditionTree(ConditionTree left, ConditionTree right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		boolean madeFalse = ifFalse == null;
		if (madeFalse) ifFalse = context.codeBuilder.newLabel();
		this.left.emitBytecode(context, null, ifFalse);
		this.right.emitBytecode(context, null, ifFalse);
		if (ifTrue != null) context.codeBuilder.goTo(ifTrue);
		if (madeFalse) context.codeBuilder.visitLabel(ifFalse);
	}
}