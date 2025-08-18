package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;

public class OrConditionTree extends ConditionTree {

	public final ConditionTree left, right;

	public OrConditionTree(ConditionTree left, ConditionTree right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		boolean madeTrue = ifTrue == null;
		if (madeTrue) ifTrue = context.codeBuilder.newLabel();
		this.left.emitBytecode(context, ifTrue, null);
		this.right.emitBytecode(context, ifTrue, null);
		if (ifFalse != null) context.codeBuilder.goto_(ifFalse);
		if (madeTrue) context.codeBuilder.labelBinding(ifTrue);
	}
}