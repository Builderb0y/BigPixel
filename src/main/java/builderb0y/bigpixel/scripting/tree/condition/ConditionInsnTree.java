package builderb0y.bigpixel.scripting.tree.condition;

import org.objectweb.asm.Label;

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
		context.codeBuilder.push(1);
		context.codeBuilder.goTo(end);
		context.codeBuilder.visitLabel(zero);
		context.codeBuilder.push(0);
		context.codeBuilder.visitLabel(end);
	}

	@Override
	public ConditionTree toCondition() {
		return this.condition;
	}
}