package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.*;
import builderb0y.notgimp.RateLimiter.NonPeriodicRateLimiter;
import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.json.JsonValue;
import builderb0y.notgimp.scripting.parsing.ExpressionParser;
import builderb0y.notgimp.scripting.parsing.ScriptEnvironment;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.UsageTracker;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.Vec;

public class DerivedLayerSource extends LayerSource {

	public TextArea textArea = new TextArea();
	public RateLimiter recompiler = new NonPeriodicRateLimiter(500L, () -> this.doRecompile(true));
	public @Nullable DerivedImageScript script;
	public Map<String, Layer> watching = Collections.emptyMap();
	public boolean isAnimated;
	public boolean loading;

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("type", "derived");
		JsonArray source = new JsonArray();
		this.textArea.getText().lines().forEachOrdered(source::add);
		map.add("code", source);
		return map;
	}

	@Override
	public void load(JsonMap map) {
		this.loading = true;
		try {
			this.textArea.setText(map.getArray("code").stream().map(JsonValue::asString).collect(Collectors.joining(System.lineSeparator())));
		}
		finally {
			this.loading = false;
		}
	}

	public DerivedLayerSource(LayerSources sources) {
		super(sources, "Derived");
		this.textArea.setFont(Font.font("monospace"));
		this.textArea.textProperty().addListener(Util.change(this::recompile));
	}

	public void init(boolean fromSave) {}

	public void copyFrom(DerivedLayerSource that) {
		this.textArea.setText(that.textArea.getText());
	}

	@Override
	public Collection<Layer> getDependencies() {
		return this.watching.values();
	}

	@Override
	public boolean isAnimated() {
		return this.isAnimated;
	}

	@Override
	public Node getRootNode() {
		return this.textArea;
	}

	@Override
	public void onDeselected() {
		super.onDeselected();
		this.script = null;
		this.setWatching(Collections.emptyMap(), false);
	}

	@Override
	public void invalidateStructure() {
		this.recompile();
	}

	public void setWatching(Map<String, Layer> watching, boolean animated) {
		this.watching = watching;
		this.isAnimated = animated;
	}

	@Override
	public void doRedraw() throws RedrawException {
		if (this.script == null) throw new RedrawException("Script failed to compile");
		AnimationSource animation = this.sources.layer.openImage.animationSource;
		HDRImage image = this.sources.layer.image;
		int width = image.width;
		int height = image.height;
		IntVector iResolution = VectorOperations.int2_from_int_int(width, height);
		FloatVector fRcpResolution = VectorOperations.float2_from_float(1.0F).div(VectorOperations.float2_from_int2(iResolution));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				IntVector UV = VectorOperations.int2_from_int_int(x, height - y - 1);
				FloatVector uv = VectorOperations.float2_from_int2(UV).add(0.5F).mul(fRcpResolution);
				this.script.compute(
					UV,
					uv,
					iResolution,
					animation.frames.get(),
					animation.frame.get(),
					animation.seconds.get(),
					animation.fraction.get()
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
			ExpressionParser<DerivedImageScript> parser = new ExpressionParser<>(this.textArea.getText(), DerivedImageScript.class);
			Map<String, Layer> layerMap = this.sources.layer.openImage.layerMap;
			UsageTracker animationTracer = new UsageTracker();
			this.script = (
				parser
				.addBuiltins()
				.addLayers(this.sources.layer.item)
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
			this.setWatching(parser.usedLayers.keySet().stream().collect(Collectors.toMap(Function.identity(), layerMap::get)), animationTracer.used);
			if (redraw) this.requestRedraw();
			this.sources.layer.redrawException.set(null);
		}
		catch (Throwable throwable) {
			this.script = null;
			this.setWatching(Collections.emptyMap(), false);
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