package builderb0y.notgimp.scripting.tree;

public class VariableDeclarationInsnTree extends ReassignInsnTree {

	public VariableDeclarationInsnTree(String name, InsnTree initializer) {
		super(name, initializer);
	}

	@Override
	public void emitBytecode(Context context) {
		context.allocateLocal(this.name, this.value.type()).index();
		super.emitBytecode(context);
	}
}