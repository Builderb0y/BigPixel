package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;

import builderb0y.bigpixel.scripting.tree.InsnTree;
import builderb0y.bigpixel.scripting.types.VectorType;

public class ConditionInsnTree extends InsnTree {

	public final ConditionTree condition;

	public ConditionInsnTree(ConditionTree condition) {
		super(VectorType.BOOLEAN);
		this.condition = condition;
	}

	@Override
	public void emitBytecode(Context context) {
		Label
			zero = context.codeBuilder.newLabel(),
			end = context.codeBuilder.newLabel();
		this.condition.emitBytecode(context, null, zero);
		context.codeBuilder.iconst_1().goto_(end).labelBinding(zero).iconst_0().labelBinding(end);
	}

	@Override
	public ConditionTree toCondition() {
		return this.condition;
	}
}