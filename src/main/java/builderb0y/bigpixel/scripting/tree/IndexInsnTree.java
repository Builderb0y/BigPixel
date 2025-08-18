package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;

public class IndexInsnTree extends InvokeInsnTree {

	public IndexInsnTree(VectorType type, InsnTree receiver, InsnTree[] arguments, MethodInfo method) {
		super(type, receiver, arguments, method);
	}
}