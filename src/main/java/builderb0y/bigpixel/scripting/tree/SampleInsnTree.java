package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;

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
		.getfield(context.self, this.layerName, Util.desc(LayerNode.class));
		for (InsnTree argument : this.arguments) {
			argument.emitBytecode(context);
		}
		this.method.emitBytecode(context);
	}
}