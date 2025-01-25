package builderb0y.notgimp.scripting.tree;

public class NoopInsnTree extends InsnTree {

	public static final NoopInsnTree INSTANCE = new NoopInsnTree();

	public NoopInsnTree() {
		super();
	}

	@Override
	public void emitBytecode(Context context) {
		//no-op.
	}
}