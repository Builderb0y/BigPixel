package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;

public abstract class LayerSource {

	public final Type type;
	public final LayerSources sources;
	public final SourceParameters parameters;

	public LayerSource(Type type, LayerSources sources) {
		this.type = type;
		this.sources = sources;
		this.parameters = new SourceParameters(this);
	}

	public JsonMap save() {
		JsonMap map = new JsonMap().with("type", this.type.saveName);
		for (SourceParameter<?, ?> parameter : this.parameters) {
			parameter.save(map);
		}
		return map.with("dependencies", this.getDependencies().save());
	}

	public void load(JsonMap map) {
		for (SourceParameter<?, ?> parameter : this.parameters) {
			parameter.load(map);
		}
		this.getDependencies().load(map.getMap("dependencies"));
	}

	public void copyFrom(LayerSource that) {
		if (this.getClass() != that.getClass()) {
			throw new IllegalArgumentException("Class mismatch: attempting to copy " + that.getClass() + " to " + this.getClass());
		}
		this.parameters.copyFrom(that.parameters);
	}

	public Node getConfigNode() {
		return this.getDependencies().getConfigPane();
	}

	public abstract LayerDependencies getDependencies();

	public void requestRedraw() {
		this.sources.layer.requestRedraw();
	}

	public void redrawImmediately() {
		try {
			this.doRedraw();
			this.sources.layer.redrawException.set(null);
		}
		catch (Throwable exception) {
			while (exception.getCause() != null) {
				exception = exception.getCause();
			}
			this.sources.layer.redrawException.set(exception);
		}
	}

	public abstract void doRedraw() throws RedrawException;

	@Override
	public String toString() {
		return this.type.displayName;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}

	public static enum Type {
		MANUAL        ("manual",         "Manual",                ManualLayerSource::new),
		PASSTHROUGH   ("passthrough", "Passthrough",         PassthroughLayerSource::new),
		ALPHA         ("alpha",          "Alpha Blend",            AlphaLayerSource::new),
		ADD           ("add",            "Addition",                 AddLayerSource::new),
		AVERAGE       ("avg",            "Average",              AverageLayerSource::new),
		MULTIPLY      ("mul",            "Multiply",            MultiplyLayerSource::new),
		SCREEN        ("screen",         "Screen",                ScreenLayerSource::new),
		MIN           ("min",            "Min",                      MinLayerSource::new),
		MAX           ("max",            "Max",                      MaxLayerSource::new),
		INVERT        ("invert",         "Invert",                InvertLayerSource::new),
		NORMALIZE     ("normalize",      "Normalize",          NormalizeLayerSource::new),
		CLAMP         ("clamp",          "Clamp",                  ClampLayerSource::new),
		GRADIENT_REMAP("gradient_remap", "Gradient Remap", GradientRemapLayerSource::new),
		COLOR_MATRIX  ("color_matrix",   "Color Matrix",     ColorMatrixLayerSource::new),
		CLIFF_CURVE   ("cliff",          "Cliff Curve",       CliffCurveLayerSource::new),
		CONVOLVE      ("convolve",       "Convolve",            ConvolveLayerSource::new),
		K_MEANS       ("kmeans",         "K-Means",               KMeansLayerSource::new),
		TILE          ("tile",           "Tile",                    TileLayerSource::new),
		WFC           ("wfc",            "Wave Function Collapse",   WFCLayerSource::new),
		DERIVED       ("derived",        "Derived",              DerivedLayerSource::new);

		public static final Type[] VALUES = values();
		public static final Map<String, Type> BY_SAVE_NAME = Arrays.stream(VALUES).collect(Collectors.toMap((Type type) -> type.saveName, Function.identity()));

		public final String saveName, displayName;
		public final Function<LayerSources, LayerSource> factory;

		Type(String saveName, String displayName, Function<LayerSources, LayerSource> factory) {
			this.saveName = saveName;
			this.displayName = displayName;
			this.factory = factory;
		}

		@Override
		public String toString() {
			return this.displayName;
		}
	}
}