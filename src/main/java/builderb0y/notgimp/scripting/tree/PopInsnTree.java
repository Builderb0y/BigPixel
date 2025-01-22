package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;

public class PopInsnTree extends InsnTree {

	public final InsnTree value;

	public PopInsnTree(InsnTree value) {
		super(VectorType.VOID);
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		this.value.emitBytecode(context);
		if (this.value.type != VectorType.VOID) {
			if (this.value.type.isReallyDoubleWidth()) {
				context.codeBuilder.pop2();
			}
			else {
				context.codeBuilder.pop();
			}
		}
	}
}