package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;

public class NoopInsnTree extends InsnTree {

	public static final NoopInsnTree INSTANCE = new NoopInsnTree();

	public NoopInsnTree() {
		super(VectorType.VOID);
	}

	@Override
	public void emitBytecode(Context context) {
		//no-op.
	}
}