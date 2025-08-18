package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.scripting.types.VectorType;

public class DupInsnTree extends InsnTree {

	public final InsnTree value;
	public final int times;

	public DupInsnTree(InsnTree value, int times) {
		super(Util.fill(new VectorType[times], value.type()));
		this.value = value;
		this.times = times;
	}

	@Override
	public void emitBytecode(Context context) {
		this.value.emitBytecode(context);
		if (this.value.type().isReallyDoubleWidth()) {
			for (int i = 1; i < this.times; i++) {
				context.codeBuilder.dup2();
			}
		}
		else {
			for (int i = 1; i < this.times; i++) {
				context.codeBuilder.dup();
			}
		}
	}
}