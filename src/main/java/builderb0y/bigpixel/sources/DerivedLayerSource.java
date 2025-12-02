package builderb0y.bigpixel.sources;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import builderb0y.bigpixel.AnimationSource;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.scripting.parsing.ExpressionParser;
import builderb0y.bigpixel.scripting.parsing.ScriptEnvironment;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.UsageTracker;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptParsingException;
import builderb0y.bigpixel.scripting.tree.InsnTree;
import builderb0y.bigpixel.scripting.tree.SampleInsnTree;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.types.VectorType.Vec;
import builderb0y.bigpixel.sources.dependencies.DerivedLayerDependencies;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.util.RateLimiter;
import builderb0y.bigpixel.util.RateLimiter.NonPeriodicRateLimiter;
import builderb0y.bigpixel.util.Util;

import static org.objectweb.asm.Opcodes.*;

public class DerivedLayerSource extends LayerSource {

	public TextArea textArea = this.parameters.addCode("code");
	public RateLimiter recompiler = new NonPeriodicRateLimiter(500L, () -> this.doRecompile(true));
	public DerivedLayerDependencies dependencies = new DerivedLayerDependencies(this);
	public @Nullable DerivedImageScriptFactory scriptFactory;
	public boolean loading;

	@Override
	public void load(JsonMap map) {
		this.loading = true;
		try {
			super.load(map);
		}
		finally {
			this.loading = false;
		}
	}

	public DerivedLayerSource(LayerSources sources) {
		super(LayerSourceType.DERIVED, sources);
		this.textArea.textProperty().addListener(Util.change(this::recompile));
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public Node getConfigNode() {
		return this.textArea;
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		if (this.scriptFactory == null) throw new RedrawException("Script failed to compile");
		Map<String, LayerNode> layersByName = this.sources.layer.graph.layersByName;
		DerivedImageScript script = this.scriptFactory.create((String name) -> {
			return layersByName.get(name).createInvertedInput(frame);
		});
		AnimationSource animation = this.sources.layer.graph.openImage.animationSource;
		HDRImage destination = this.sources.layer.getFrame(frame);
		int width = destination.width;
		int height = destination.height;
		IntVector iResolution = VectorOperations.int2_from_int_int(width, height);
		FloatVector fRcpResolution = VectorOperations.float2_from_float(1.0F).div(VectorOperations.float2_from_int2(iResolution));
		int frames = animation.frameCount.get();
		float seconds = animation.getSeconds(frame);
		float fraction = animation.getFraction(frame);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				IntVector UV = VectorOperations.int2_from_int_int(x, height + ~y);
				FloatVector uv = VectorOperations.float2_from_int2(UV).add(0.5F).mul(fRcpResolution);
				script.compute(
					UV,
					uv,
					iResolution,
					frames,
					frame,
					seconds,
					fraction
				)
				.intoArray(destination.pixels, destination.baseIndex(x, y));
			}
		}
	}

	public void recompile() {
		if (this.loading) this.doRecompile(false);
		else this.recompiler.run();
	}

	public void doRecompile(boolean redraw) {
		try {
			this.assembleScriptFactory();
			if (redraw) this.redrawLater();
			this.sources.layer.redrawException.set(null);
		}
		catch (Throwable throwable) {
			//throwable.printStackTrace();
			this.scriptFactory = null;
			this.dependencies.setActualDependencies(Collections.emptySet(), false);
			//throwable.printStackTrace();
			this.sources.layer.redrawException.set(throwable);
		}
	}

	public void assembleScriptFactory() throws ScriptParsingException {
		Map<String, LayerNode> layerMap = this.sources.layer.graph.layersByName;
		DerivedParser parser = new DerivedParser(this.textArea.getText());
		UsageTracker animationTracer = new UsageTracker();
		this.scriptFactory = (
			parser
			.addBuiltins()
			.addLayers(this.dependencies.potentialDependencies.values())
			.configureEnvironment((ScriptEnvironment environment) -> {
				environment
				.addKeyword("return", KeywordHandler.returner(VectorType.FLOAT4))
				.addVariable("uv", VariableHandler.builtinParameter("uv", VectorType.FLOAT2))
				.addVariable("UV", VariableHandler.builtinParameter("UV", VectorType.INT2))
				.addVariable("resolution", VariableHandler.builtinParameter("resolution", VectorType.INT2))
				.addVariable("animationFrame", VariableHandler.builtinParameter("animationFrame", VectorType.INT, animationTracer))
				.addVariable("animationFrames", VariableHandler.builtinParameter("animationFrames", VectorType.INT, animationTracer))
				.addVariable("animationSeconds", VariableHandler.builtinParameter("animationSeconds", VectorType.FLOAT, animationTracer))
				.addVariable("animationFraction", VariableHandler.builtinParameter("animationFraction", VectorType.FLOAT, animationTracer))
				;
			})
			.parse()
		);
		this.dependencies.setActualDependencies(parser.usedLayers.keySet().stream().map(layerMap::get).collect(Collectors.toSet()), animationTracer.used);
	}

	public static class DerivedParser extends ExpressionParser<DerivedImageScript> {

		public Map<String, Integer> usedLayers;

		public DerivedParser(String source) {
			super(source, DerivedImageScript.class);
			this.usedLayers = new HashMap<>();
		}

		@Override
		public DerivedParser addBuiltins() {
			return (DerivedParser)(super.addBuiltins());
		}

		@Override
		public DerivedParser configureEnvironment(Consumer<ScriptEnvironment> configurator) {
			return (DerivedParser)(super.configureEnvironment(configurator));
		}

		public DerivedParser addLayers(Collection<LayerNode> layers) {
			for (LayerNode layer : layers) {
				this.scope.environment.addFunction(
					layer.getDisplayName(),
					(ExpressionParser<?> parser, String name, InsnTree[] params) -> {
						VectorType[] types = InsnTree.flattenTypes(params);
						DerivedParser derived = (DerivedParser)(parser);
						int index = derived.usedLayers.computeIfAbsent(name, (String _) -> derived.usedLayers.size());
						return switch (types.length) {
							case 1 -> {
								InsnTree[] castParams = ScriptHandlers.multiCast(params, VectorType.INT2);
								if (castParams == null) yield null;
								yield new SampleInsnTree(castParams, "layer" + index, getPixelWrappedOneArg);
							}
							case 2 -> {
								InsnTree[] castParams = ScriptHandlers.multiCast(params, VectorType.INT, VectorType.INT);
								if (castParams == null) yield null;
								yield new SampleInsnTree(castParams, "layer" + index, getPixelWrappedTwoArgs);
							}
							default -> {
								yield null;
							}
						};
					}
				);
			}
			return this;
		}

		@Override
		public ClassNode parseBasic() throws ScriptParsingException {
			ClassNode clazz = super.parseBasic();
			MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ScriptLayerAccess.class)), null, null);
			constructor.visitVarInsn(ALOAD, 0);
			constructor.visitMethodInsn(INVOKESPECIAL, clazz.superName, "<init>", "()V", false);
			for (Map.Entry<String, Integer> entry : this.usedLayers.entrySet()) {
				FieldNode field = new FieldNode(ACC_PUBLIC | ACC_FINAL, "layer" + entry.getValue(), Type.getDescriptor(Sampler.class), null, null);
				clazz.fields.add(field);
				constructor.visitVarInsn(ALOAD, 0);
				constructor.visitVarInsn(ALOAD, 1);
				constructor.visitLdcInsn(entry.getKey());
				constructor.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ScriptLayerAccess.class), "get", Type.getMethodDescriptor(Type.getType(Sampler.class), Type.getType(String.class)), true);
				constructor.visitFieldInsn(PUTFIELD, clazz.name, field.name, field.desc);
			}
			constructor.visitInsn(RETURN);
			clazz.methods.addFirst(constructor);
			return clazz;
		}

		public DerivedImageScriptFactory parse() throws ScriptParsingException {
			ClassNode clazz = this.parseBasic();
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			clazz.accept(writer);
			byte[] bytes = writer.toByteArray();
			try {
				//note: must use defineClass() instead of defineHiddenClass(),
				//because LambdaMetafactory fails for hidden classes
				//with an obscure error deep in the classfile API.
				Class<?> definedClass = MethodHandles.lookup().defineClass(bytes);
				MethodHandle constructor = MethodHandles.lookup().findConstructor(definedClass, MethodType.methodType(void.class, ScriptLayerAccess.class));
				return (DerivedImageScriptFactory)(
					LambdaMetafactory.metafactory(
						MethodHandles.lookup(),
						"create",
						MethodType.methodType(DerivedImageScriptFactory.class),
						MethodType.methodType(DerivedImageScript.class, ScriptLayerAccess.class),
						constructor,
						MethodType.methodType(DerivedImageScript.class, ScriptLayerAccess.class)
					)
					.getTarget()
					.invokeExact()
				);
			}
			catch (Throwable throwable) {
				StringWriter stringWriter = new StringWriter(1024);
				PrintWriter printWriter = new PrintWriter(stringWriter);
				TraceClassVisitor tracer = new TraceClassVisitor(printWriter);
				clazz.accept(tracer);
				ScriptParsingException toThrow = new ScriptParsingException("Exception defining class!\n\n" + stringWriter, throwable, null);
				toThrow.printStackTrace();
				throw toThrow;
			}
		}
	}

	@FunctionalInterface
	public static interface DerivedImageScript {

		public abstract FloatVector compute(
			@Vec(2) IntVector UV,
			@Vec(2) FloatVector uv,
			@Vec(2) IntVector resolution,
			int animationFrames,
			int animationFrame,
			float animationSeconds,
			float animationFraction
		);
	}

	@FunctionalInterface
	public static interface DerivedImageScriptFactory {

		public abstract DerivedImageScript create(ScriptLayerAccess layers);
	}

	@FunctionalInterface
	public static interface ScriptLayerAccess {

		public abstract Sampler get(String name);
	}
}