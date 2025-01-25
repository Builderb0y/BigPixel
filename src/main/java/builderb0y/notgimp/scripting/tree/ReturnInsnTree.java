package builderb0y.notgimp.scripting.tree;

public class ReturnInsnTree extends InsnTree {

	public final InsnTree value;

	public ReturnInsnTree(InsnTree value) {
		value.type();
		super();
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		this.value.emitBytecode(context);
		switch (this.value.type()) {
			case INT, BOOLEAN -> context.codeBuilder.ireturn();
			case LONG -> context.codeBuilder.lreturn();
			case FLOAT -> context.codeBuilder.freturn();
			case DOUBLE -> context.codeBuilder.dreturn();
			case VOID -> context.codeBuilder.return_();
			default -> context.codeBuilder.areturn();
		}
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return true;
	}
}