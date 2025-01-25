package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class IndexInsnTree extends InvokeInsnTree {

	public IndexInsnTree(VectorType type, InsnTree receiver, InsnTree[] arguments, MethodInfo method) {
		super(type, receiver, arguments, method);
	}
}