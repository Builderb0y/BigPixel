package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class VectorConstructorInsnTree extends InvokeInsnTree {

	public VectorConstructorInsnTree(VectorType type, InsnTree[] arguments, MethodInfo method) {
		super(type, arguments, method);
	}
}