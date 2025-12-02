package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ConfigParameter;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.LayerSource.LayerSourceType;

public abstract class LayerSource implements OrganizedSelection.Value<LayerSourceType> {

	public final LayerSourceType type;
	public final LayerSources sources;
	public final SourceParameters parameters;

	public LayerSource(LayerSourceType type, LayerSources sources) {
		this.type = type;
		this.sources = sources;
		this.parameters = new SourceParameters(this);
	}

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap().with("type", this.type.saveName);
		for (ConfigParameter<?, ?> parameter : this.parameters) {
			parameter.save(map);
		}
		return map.with("dependencies", this.getDependencies().save());
	}

	@Override
	public void load(JsonMap map) {
		for (ConfigParameter<?, ?> parameter : this.parameters) {
			parameter.load(map);
		}
		this.getDependencies().load(map.getMap("dependencies"));
	}

	@Override
	public LayerNode getLayer() {
		return this.sources.layer;
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

	@Override
	public void redrawLater() {
		this.sources.layer.requestRedraw();
	}

	public abstract void doRedraw(int frame) throws RedrawException;

	@Override
	public String toString() {
		return this.type.displayName;
	}

	@Override
	public LayerSourceType getType() {
		return this.type;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}

	public static enum LayerSourceCategory implements OrganizedSelection.Category<LayerSourceCategory> {
		ROOT(null, "All Effects"),
		SIMPLE(ROOT, "Simple Effects"),
		MEDIUM(ROOT, "Medium Effects"),
		COMPLEX(ROOT, "Complex Effects");

		public final LayerSourceCategory parent;
		public final String displayName;

		LayerSourceCategory(LayerSourceCategory parent, String displayName) {
			this.parent = parent;
			this.displayName = displayName;
		}

		@Override
		public @Nullable LayerSourceCategory getParent() {
			return this.parent;
		}

		@Override
		public String toString() {
			return this.displayName;
		}
	}

	public static enum LayerSourceType implements OrganizedSelection.Type<LayerSource, LayerSourceCategory> {
		MANUAL        (LayerSourceCategory.ROOT,    "manual",         "Manual",                ManualLayerSource::new),
		PASSTHROUGH   (LayerSourceCategory.ROOT,    "passthrough",    "Passthrough",      PassthroughLayerSource::new),

		ALPHA         (LayerSourceCategory.SIMPLE,  "alpha",          "Alpha Blend",            AlphaLayerSource::new),
		ADD           (LayerSourceCategory.SIMPLE,  "add",            "Addition",                 AddLayerSource::new),
		AVERAGE       (LayerSourceCategory.SIMPLE,  "avg",            "Average",              AverageLayerSource::new),
		MULTIPLY      (LayerSourceCategory.SIMPLE,  "mul",            "Multiply",            MultiplyLayerSource::new),
		SCREEN        (LayerSourceCategory.SIMPLE,  "screen",         "Screen",                ScreenLayerSource::new),
		MIN           (LayerSourceCategory.SIMPLE,  "min",            "Min",                      MinLayerSource::new),
		MAX           (LayerSourceCategory.SIMPLE,  "max",            "Max",                      MaxLayerSource::new),
		INVERT        (LayerSourceCategory.SIMPLE,  "invert",         "Invert",                InvertLayerSource::new),
		CLAMP         (LayerSourceCategory.SIMPLE,  "clamp",          "Clamp",                  ClampLayerSource::new),

		NORMALIZE     (LayerSourceCategory.MEDIUM,  "normalize",      "Normalize",          NormalizeLayerSource::new),
		GRADIENT_REMAP(LayerSourceCategory.MEDIUM,  "gradient_remap", "Gradient Remap", GradientRemapLayerSource::new),
		COLOR_MATRIX  (LayerSourceCategory.MEDIUM,  "color_matrix",   "Color Matrix",     ColorMatrixLayerSource::new),
		CLIFF_CURVE   (LayerSourceCategory.MEDIUM,  "cliff",          "Cliff Curve",       CliffCurveLayerSource::new),
		TILE          (LayerSourceCategory.MEDIUM,  "tile",           "Tile",                    TileLayerSource::new),

		CONVOLVE      (LayerSourceCategory.COMPLEX, "convolve",       "Convolve",            ConvolveLayerSource::new),
		K_MEANS       (LayerSourceCategory.COMPLEX, "kmeans",         "K-Means",               KMeansLayerSource::new),
		DENOISE       (LayerSourceCategory.COMPLEX, "denoise",        "De-noise",             DeNoiseLayerSource::new),
		WFC           (LayerSourceCategory.COMPLEX, "wfc",            "Wave Function Collapse",   WFCLayerSource::new),

		DERIVED       (LayerSourceCategory.ROOT,    "derived",        "Derived",              DerivedLayerSource::new);

		public static final LayerSourceType[] VALUES = values();
		public static final Map<String, LayerSourceType> BY_SAVE_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(LayerSourceType::getSaveName, Function.identity()));

		public final LayerSourceCategory category;
		public final String saveName, displayName;
		public final Function<LayerSources, LayerSource> factory;

		LayerSourceType(LayerSourceCategory category, String saveName, String displayName, Function<LayerSources, LayerSource> factory) {
			this.category = category;
			this.saveName = saveName;
			this.displayName = displayName;
			this.factory = factory;
		}

		@Override
		public LayerSource create(LayerNode layer) {
			return this.factory.apply(layer.sources);
		}

		@Override
		public LayerSourceCategory getCategory() {
			return this.category;
		}

		@Override
		public String toString() {
			return this.displayName;
		}

		@Override
		public String getSaveName() {
			return this.saveName;
		}
	}
}