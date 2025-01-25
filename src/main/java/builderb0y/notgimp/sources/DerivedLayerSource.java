package builderb0y.notgimp.sources;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.MapChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.RateLimiter;
import builderb0y.notgimp.RateLimiter.NonPeriodicRateLimiter;
import builderb0y.notgimp.scripting.parsing.ExpressionParser;
import builderb0y.notgimp.scripting.parsing.ScriptEnvironment;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.Vec;

public class DerivedLayerSource extends LayerSource implements MapChangeListener<String, Layer> {

	public TextArea textArea = new TextArea();
	public RateLimiter recompiler = new NonPeriodicRateLimiter(500L, this::doRecompile);
	public @Nullable DerivedImageScript script;
	public Map<String, Layer> watching = Collections.emptyMap();
	public InvalidationListener layerListener = (Observable _) -> this.redraw();

	public DerivedLayerSource(LayerSources sources) {
		super(sources);
		this.textArea.setFont(Font.font("monospace"));
		this.textArea.textProperty().addListener((Observable _) -> this.recompiler.run());
	}

	public void init() {
		this.sources.layer.openImage.layerMap.addListener(this);
	}

	public void copyFrom(DerivedLayerSource that) {
		this.textArea.setText(that.textArea.getText());
	}

	@Override
	public Node getRootNode() {
		return this.textArea;
	}

	@Override
	public void onChanged(Change<? extends String, ? extends Layer> change) {
		if (change.wasRemoved()) {
			if (this.watching.containsKey(change.getKey())) {
				this.script = null;
				this.setWatching(Collections.emptyMap());
			}
		}
		else {
			if (this.script == null && this.isSelected()) {
				this.recompiler.run();
			}
		}
	}

	public void setWatching(Map<String, Layer> watching) {
		for (Layer layer : this.watching.values()) {
			layer.image.value.removeListener(this.layerListener);
		}
		for (Layer layer : watching.values()) {
			layer.image.value.addListener(this.layerListener);
		}
		this.watching = watching;
	}

	@Override
	public void redraw() {
		if (this.script == null) return;
		HDRImage image = this.sources.layer.image;
		int width = image.width;
		int height = image.height;
		IntVector iResolution = VectorOperations.int2_from_int_int(width, height);
		FloatVector fRcpResolution = VectorOperations.float2_from_float(1.0F).div(VectorOperations.float2_from_int2(iResolution));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				IntVector UV = VectorOperations.int2_from_int_int(x, height - y - 1);
				FloatVector uv = VectorOperations.float2_from_int2(UV).add(0.5F).mul(fRcpResolution);
				this.script.compute(UV, uv, iResolution).intoArray(image.pixels, image.baseIndex(x, y));
			}
		}
		image.markDirty();
	}

	public void doRecompile() {
		try {
			Map<String, Layer> layers = this.sources.layer.openImage.layerMap;
			ExpressionParser<DerivedImageScript> parser = new ExpressionParser<>(this.textArea.getText(), DerivedImageScript.class);
			this.script = (
				parser
				.addBuiltins()
				.addLayers(layers)
				.configureEnvironment((ScriptEnvironment environment) -> {
					environment
					.addKeyword("return", KeywordHandler.returner(VectorType.FLOAT4))
					.addVariable("uv", VariableHandler.builtinParameter("uv", VectorType.FLOAT2))
					.addVariable("UV", VariableHandler.builtinParameter("UV", VectorType.INT2))
					.addVariable("resolution", VariableHandler.builtinParameter("resolution", VectorType.INT2))
					;
				})
				.parse(layers)
			);
			this.setWatching(parser.usedLayers.keySet().stream().collect(Collectors.toMap(Function.identity(), layers::get)));
		}
		catch (Throwable throwable) {
			this.script = null;
			this.setWatching(Collections.emptyMap());
			throwable.printStackTrace();
			return;
		}
		this.redraw();
	}

	@FunctionalInterface
	public static interface DerivedImageScript {

		public abstract FloatVector compute(
			@Vec(2) IntVector UV,
			@Vec(2) FloatVector uv,
			@Vec(2) IntVector resolution
		);
	}
}