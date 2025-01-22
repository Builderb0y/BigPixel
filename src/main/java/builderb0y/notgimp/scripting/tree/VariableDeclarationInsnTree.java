package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;

public class VariableDeclarationInsnTree extends InsnTree {

	public final String name;
	public final InsnTree initializer;

	public VariableDeclarationInsnTree(String name, InsnTree initializer) {
		super(VectorType.VOID);
		this.initializer = initializer;
		this.name = name;
	}

	@Override
	public void emitBytecode(Context context) {
		int index = context.allocateLocal(this.name, this.initializer.type).index();
		this.initializer.emitBytecode(context);
		switch (this.initializer.type) {
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