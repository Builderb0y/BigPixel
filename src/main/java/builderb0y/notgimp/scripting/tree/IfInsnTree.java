package builderb0y.notgimp.scripting.tree;

import java.lang.classfile.Label;

import builderb0y.notgimp.scripting.tree.condition.ConditionTree;

public class IfInsnTree extends InsnTree {

	public final ConditionTree condition;
	public final InsnTree body;

	public IfInsnTree(ConditionTree condition, InsnTree body) {
		super();
		this.condition = condition;
		this.body = body.castToVoid();
	}

	@Override
	public void emitBytecode(Context context) {
		Label end = context.codeBuilder.newLabel();
		this.condition.emitBytecode(context, null, end);
		this.body.emitBytecode(context);
		context.codeBuilder.labelBinding(end);
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}