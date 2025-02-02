package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.scripting.types.RngOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.GroupShape;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class RandomInsnTree extends InsnTree {

	public final InsnTree[] arguments;

	public RandomInsnTree(InsnTree... arguments) {
		for (InsnTree argument : arguments) {
			for (VectorType type : argument.types()) {
				if (type.shape != GroupShape.UNIT) {
					throw new IllegalArgumentException("Not a scalar: " + argument);
				}
			}
		}
		super(VectorType.LONG);
		this.arguments = arguments;
	}

	@Override
	public void emitBytecode(Context context) {
		for (InsnTree argument : this.arguments) {
			argument.emitBytecode(context);
		}
		context.codeBuilder.lconst_0();
		for (int argIndex = this.arguments.length; --argIndex >= 0;) {
			VectorType[] argTypes = this.arguments[argIndex].types();
			for (int typeIndex = argTypes.length; --typeIndex >= 0;) {
				new MethodInfo(RngOperations.class, "permute_" + argTypes[typeIndex].name).emitBytecode(context);
			}
		}
	}
}