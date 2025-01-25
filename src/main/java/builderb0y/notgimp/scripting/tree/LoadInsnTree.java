package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;

public class LoadInsnTree extends InsnTree {

	public final String name;

	public LoadInsnTree(VectorType type, String name) {
		super(type);
		this.name = name;
	}

	@Override
	public void emitBytecode(Context context) {
		int index = context.getVariable(this.name).index();
		switch (this.type()) {
			case INT, BOOLEAN -> context.codeBuilder.iload(index);
			case LONG -> context.codeBuilder.lload(index);
			case FLOAT -> context.codeBuilder.fload(index);
			case DOUBLE -> context.codeBuilder.dload(index);
			default -> context.codeBuilder.aload(index);
		}
	}

	@Override
	public Assigner assigner() {
		return (InsnTree value) -> {
			if (this.type() == value.type()) {
				return new ReassignInsnTree(this.name, value);
			}
			else {
				throw new IllegalArgumentException("Can't store " + value.type() + " in variable of type " + this.type());
			}
		};
	}
}