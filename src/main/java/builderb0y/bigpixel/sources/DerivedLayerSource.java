package builderb0y.bigpixel.sources;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.RateLimiter.NonPeriodicRateLimiter;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.scripting.parsing.ExpressionParser;
import builderb0y.bigpixel.scripting.parsing.ScriptEnvironment;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.UsageTracker;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.types.VectorType.Vec;
import builderb0y.bigpixel.sources.dependencies.DerivedLayerDependencies;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;

public class DerivedLayerSource extends LayerSource {

	public TextArea textArea = this.parameters.addCode("code");
	public RateLimiter recompiler = new NonPeriodicRateLimiter(500L, () -> this.doRecompile(true));
	public @Nullable DerivedImageScript script;
	public DerivedLayerDependencies dependencies = new DerivedLayerDependencies(this);
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
		super(Type.DERIVED, sources);
		this.textArea.setFont(Font.font("monospace"));
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
	public void doRedraw() throws RedrawException {
		if (this.script == null) throw new RedrawException("Script failed to compile");
		AnimationSource animation = this.sources.layer.graph.openImage.animationSource;
		HDRImage image = this.sources.layer.image;
		int width = image.width;
		int height = image.height;
		IntVector iResolution = VectorOperations.int2_from_int_int(width, height);
		FloatVector fRcpResolution = VectorOperations.float2_from_float(1.0F).div(VectorOperations.float2_from_int2(iResolution));
		int frames = animation.frames.get();
		int frame = animation.frame.get();
		float seconds = animation.seconds.get();
		float fraction = animation.fraction.get();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				IntVector UV = VectorOperations.int2_from_int_int(x, height - y - 1);
				FloatVector uv = VectorOperations.float2_from_int2(UV).add(0.5F).mul(fRcpResolution);
				this.script.compute(
					UV,
					uv,
					iResolution,
					frames,
					frame,
					seconds,
					fraction
				)
				.intoArray(image.pixels, image.baseIndex(x, y));
			}
		}
	}

	public void recompile() {
		if (this.loading) this.doRecompile(false);
		else this.recompiler.run();
	}

	public void doRecompile(boolean redraw) {
		try {
			Map<String, LayerNode> layerMap = this.sources.layer.graph.layersByName;
			ExpressionParser<DerivedImageScript> parser = new ExpressionParser<>(this.textArea.getText(), DerivedImageScript.class);
			UsageTracker animationTracer = new UsageTracker();
			this.script = (
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
				.parse(layerMap)
			);
			this.dependencies.setActualDependencies(parser.usedLayers.keySet().stream().map(layerMap::get).collect(Collectors.toSet()));
			this.dependencies.animated = animationTracer.used;
			if (redraw) this.redrawLater();
			this.sources.layer.redrawException.set(null);
		}
		catch (Throwable throwable) {
			this.script = null;
			this.dependencies.setActualDependencies(Collections.emptySet());
			this.dependencies.animated = false;
			//throwable.printStackTrace();
			this.sources.layer.redrawException.set(throwable);
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
}