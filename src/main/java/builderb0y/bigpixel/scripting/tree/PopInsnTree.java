package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.scripting.types.VectorType;

public class PopInsnTree extends InsnTree {

	public final InsnTree value;

	public PopInsnTree(InsnTree value) {
		super();
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		this.value.emitBytecode(context);
		VectorType[] types = this.value.types;
		for (int index = types.length; --index >= 0;) {
			VectorType type = types[index];
			if (type != VectorType.VOID) {
				if (type.isReallyDoubleWidth()) {
					context.codeBuilder.pop2();
				}
				else {
					context.codeBuilder.pop();
				}
			}
		}
	}

	@Override
	public boolean canBeStatement() {
		return this.value.canBeStatement();
	}
}