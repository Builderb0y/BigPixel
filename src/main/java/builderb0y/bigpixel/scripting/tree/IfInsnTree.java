package builderb0y.bigpixel.scripting.tree;

import org.objectweb.asm.Label;

import builderb0y.bigpixel.scripting.tree.condition.ConditionTree;

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
		context.codeBuilder.visitLabel(end);
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}