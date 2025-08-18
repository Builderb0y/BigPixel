package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;
import builderb0y.bigpixel.scripting.tree.InsnTree;

public abstract class ConditionTree {

	public abstract void emitBytecode(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse);

	public ConditionTree not() {
		return new NotConditionTree(this);
	}

	public InsnTree toInsn() {
		return new ConditionInsnTree(this);
	}

	public static void checkLabels(@Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (ifTrue == ifFalse) { //also catches the case where both labels are null.
			throw new IllegalArgumentException("ifTrue and ifFalse cannot both point to the same location.");
		}
	}
}