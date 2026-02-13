package builderb0y.bigpixel.scripting.tree.condition;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;

import static org.objectweb.asm.Opcodes.*;

public enum CompareMode {
	GT(">"),
	GE(">="),
	LT("<"),
	LE("<="),
	EQ("=="),
	NE("!=");

	public final String name = this.name().toLowerCase(Locale.ROOT);
	public final String operator;

	CompareMode(String operator) {
		this.operator = operator;
	}

	public CompareMode not() {
		return switch (this) {
			case GT -> LE;
			case GE -> LT;
			case LT -> GE;
			case LE -> GT;
			case EQ -> NE;
			case NE -> EQ;
		};
	}

	public void ifNonZero(CodeEmitter.Context context, Label label) {
		switch (this) {
			case GT -> context.codeBuilder.ifICmp(IFGT, label);
			case GE -> context.codeBuilder.ifICmp(IFGE, label);
			case LT -> context.codeBuilder.ifICmp(IFLT, label);
			case LE -> context.codeBuilder.ifICmp(IFLE, label);
			case EQ -> context.codeBuilder.ifICmp(IFEQ, label);
			case NE -> context.codeBuilder.ifICmp(IFNE, label);
		}
	}

	public void ifZero(CodeEmitter.Context context, Label label) {
		switch (this) {
			case GT -> context.codeBuilder.ifZCmp(IFGT, label);
			case GE -> context.codeBuilder.ifZCmp(IFGE, label);
			case LT -> context.codeBuilder.ifZCmp(IFLT, label);
			case LE -> context.codeBuilder.ifZCmp(IFLE, label);
			case EQ -> context.codeBuilder.ifZCmp(IFEQ, label);
			case NE -> context.codeBuilder.ifZCmp(IFNE, label);
		}
	}

	public void emitInt(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (ifTrue != null) {
			this.ifNonZero(context, ifTrue);
			if (ifFalse != null) {
				context.codeBuilder.goTo(ifFalse);
			}
		}
		else {
			this.not().ifNonZero(context, ifFalse);
		}
	}

	public void emitIntZero(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (ifTrue != null) {
			this.ifZero(context, ifTrue);
			if (ifFalse != null) {
				context.codeBuilder.goTo(ifFalse);
			}
		}
		else {
			this.not().ifZero(context, ifFalse);
		}
	}

	public void emitLong(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		context.codeBuilder.visitInsn(LCMP);
		this.emitIntZero(context, ifTrue, ifFalse);
	}

	public void emitFloat(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		context.codeBuilder.visitInsn(this == LT || this == LE ? FCMPG : FCMPL);
		this.emitIntZero(context, ifTrue, ifFalse);
	}

	public void emitDouble(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		context.codeBuilder.visitInsn(this == LT || this == LE ? DCMPG : DCMPL);
		this.emitIntZero(context, ifTrue, ifFalse);
	}
}