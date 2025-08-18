package builderb0y.bigpixel.scripting.tree.condition;

import java.lang.classfile.Label;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;

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
			case GT -> context.codeBuilder.if_icmpgt(label);
			case GE -> context.codeBuilder.if_icmpge(label);
			case LT -> context.codeBuilder.if_icmplt(label);
			case LE -> context.codeBuilder.if_icmple(label);
			case EQ -> context.codeBuilder.if_icmpeq(label);
			case NE -> context.codeBuilder.if_icmpne(label);
		}
	}

	public void ifZero(CodeEmitter.Context context, Label label) {
		switch (this) {
			case GT -> context.codeBuilder.ifgt(label);
			case GE -> context.codeBuilder.ifge(label);
			case LT -> context.codeBuilder.iflt(label);
			case LE -> context.codeBuilder.ifle(label);
			case EQ -> context.codeBuilder.ifeq(label);
			case NE -> context.codeBuilder.ifne(label);
		}
	}

	public void emitInt(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (ifTrue != null) {
			this.ifNonZero(context, ifTrue);
			if (ifFalse != null) {
				context.codeBuilder.goto_(ifFalse);
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
				context.codeBuilder.goto_(ifFalse);
			}
		}
		else {
			this.not().ifZero(context, ifFalse);
		}
	}

	public void emitLong(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		context.codeBuilder.lcmp();
		this.emitIntZero(context, ifTrue, ifFalse);
	}

	public void emitFloat(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (this == LT || this == LE) context.codeBuilder.fcmpg();
		else context.codeBuilder.fcmpl();
		this.emitIntZero(context, ifTrue, ifFalse);
	}

	public void emitDouble(CodeEmitter.Context context, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (this == LT || this == LE) context.codeBuilder.dcmpg();
		else context.codeBuilder.dcmpl();
		this.emitIntZero(context, ifTrue, ifFalse);
	}
}