package builderb0y.bigpixel.scripting.tree;

public class UnpackedInsnTree extends InsnTree {

	public final InsnTree[] values;

	public UnpackedInsnTree(InsnTree[] values) {
		super(InsnTree.flattenTypes(values));
		this.values = values;
	}

	@Override
	public void emitBytecode(Context context) {
		for (InsnTree value : this.values) {
			value.emitBytecode(context);
		}
	}
}