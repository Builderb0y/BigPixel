package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;

public class ReassignInsnTree extends InsnTree {

	public final String name;
	public final InsnTree value;

	public ReassignInsnTree(String name, InsnTree value) {
		if (value.type() == VectorType.VOID) {
			throw new IllegalArgumentException("Can't assign void to variable");
		}
		super();
		this.name = name;
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		int index = context.getVariable(this.name).index();
		this.value.emitBytecode(context);
		switch (this.value.type()) {
			case BOOLEAN, INT -> context.codeBuilder.istore(index);
			case LONG -> context.codeBuilder.lstore(index);
			case FLOAT -> context.codeBuilder.fstore(index);
			case DOUBLE -> context.codeBuilder.dstore(index);
			default -> context.codeBuilder.astore(index);
		}
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}