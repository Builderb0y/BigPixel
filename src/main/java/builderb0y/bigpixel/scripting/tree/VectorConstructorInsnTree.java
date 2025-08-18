package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;

public class VectorConstructorInsnTree extends InvokeInsnTree {

	public VectorConstructorInsnTree(VectorType type, InsnTree[] arguments, MethodInfo method) {
		super(type, arguments, method);
	}
}