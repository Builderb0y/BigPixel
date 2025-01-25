package builderb0y.notgimp.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.scripting.tree.CodeEmitter;
import builderb0y.notgimp.scripting.tree.ConstantInsnTree;
import builderb0y.notgimp.scripting.tree.InsnTree;

public class ConstantConditionTree extends ConditionTree {

	public static final ConstantConditionTree
		TRUE = new ConstantConditionTree(true),
		FALSE = new ConstantConditionTree(false);

	public final boolean value;

	public ConstantConditionTree(boolean value) {
		this.value = value;
	}

	public static ConstantConditionTree of(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public InsnTree toInsn() {
		return this.value ? ConstantInsnTree.TRUE : ConstantInsnTree.FALSE;
	}

	@Override
	public ConditionTree not() {
		return of(!this.value);
	}

	@Override
	public void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		Label label = this.value ? ifTrue : ifFalse;
		if (label != null) context.codeBuilder.goto_(label);
	}
}