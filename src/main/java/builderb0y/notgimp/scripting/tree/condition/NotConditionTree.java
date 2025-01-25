package builderb0y.notgimp.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.scripting.tree.CodeEmitter;

public class NotConditionTree extends ConditionTree {

	public final ConditionTree tree;

	public NotConditionTree(ConditionTree tree) {
		this.tree = tree;
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		this.tree.emitBytecode(context, ifFalse, ifTrue);
	}

	@Override
	public ConditionTree not() {
		return this.tree;
	}
}