package builderb0y.notgimp.scripting.tree;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class SampleInsnTree extends InsnTree {

	public final String layerName;
	public final InsnTree[] arguments;
	public final MethodInfo method;

	public SampleInsnTree(InsnTree[] arguments, String layerName, MethodInfo method) {
		super(VectorType.FLOAT4);
		this.arguments = arguments;
		this.layerName = layerName;
		this.method = method;
	}

	@Override
	public void emitBytecode(Context context) {
		context
		.codeBuilder
		.aload(context.codeBuilder.receiverSlot())
		.getfield(context.self, this.layerName, Util.desc(Layer.class));
		for (InsnTree argument : this.arguments) {
			argument.emitBytecode(context);
		}
		this.method.emitBytecode(context);
	}
}