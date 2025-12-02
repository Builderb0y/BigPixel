package builderb0y.bigpixel.scripting.tree;

import org.objectweb.asm.Type;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;

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
		context.codeBuilder.loadThis();
		context.codeBuilder.getField(context.self, this.layerName, Type.getType(Sampler.class));
		for (InsnTree argument : this.arguments) {
			argument.emitBytecode(context);
		}
		this.method.emitBytecode(context);
	}
}