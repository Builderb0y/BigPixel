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

public abstract class LayerSource implements OrganizedSelection.Value<LayerSource.Type> {

	public final Type type;
	public final LayerSources sources;
	public final SourceParameters parameters;

	public LayerSource(Type type, LayerSources sources) {
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

	public void redrawImmediately() {
		try {
			this.doRedraw();
			this.sources.layer.redrawException.set(null);
		}
		catch (RedrawException exception) {
			this.sources.layer.redrawException.set(exception);
		}
		catch (Throwable exception) {
			exception.printStackTrace();
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

	@Override
	public Type getType() {
		return this.type;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}

	public static enum Category implements OrganizedSelection.Category<Category> {
		ROOT(null, "All Effects"),
		SIMPLE(ROOT, "Simple Effects"),
		MEDIUM(ROOT, "Medium Effects"),
		COMPLEX(ROOT, "Complex Effects");

		public final Category parent;
		public final String displayName;

		Category(Category parent, String displayName) {
			this.parent = parent;
			this.displayName = displayName;
		}

		@Override
		public @Nullable Category getParent() {
			return this.parent;
		}

		@Override
		public String toString() {
			return this.displayName;
		}
	}

	public static enum Type implements OrganizedSelection.Type<LayerSource, Category> {
		MANUAL        (Category.ROOT,    "manual",         "Manual",                ManualLayerSource::new),
		PASSTHROUGH   (Category.ROOT,    "passthrough",    "Passthrough",      PassthroughLayerSource::new),

		ALPHA         (Category.SIMPLE,  "alpha",          "Alpha Blend",            AlphaLayerSource::new),
		ADD           (Category.SIMPLE,  "add",            "Addition",                 AddLayerSource::new),
		AVERAGE       (Category.SIMPLE,  "avg",            "Average",              AverageLayerSource::new),
		MULTIPLY      (Category.SIMPLE,  "mul",            "Multiply",            MultiplyLayerSource::new),
		SCREEN        (Category.SIMPLE,  "screen",         "Screen",                ScreenLayerSource::new),
		MIN           (Category.SIMPLE,  "min",            "Min",                      MinLayerSource::new),
		MAX           (Category.SIMPLE,  "max",            "Max",                      MaxLayerSource::new),
		INVERT        (Category.SIMPLE,  "invert",         "Invert",                InvertLayerSource::new),
		CLAMP         (Category.SIMPLE,  "clamp",          "Clamp",                  ClampLayerSource::new),

		NORMALIZE     (Category.MEDIUM,  "normalize",      "Normalize",          NormalizeLayerSource::new),
		GRADIENT_REMAP(Category.MEDIUM,  "gradient_remap", "Gradient Remap", GradientRemapLayerSource::new),
		COLOR_MATRIX  (Category.MEDIUM,  "color_matrix",   "Color Matrix",     ColorMatrixLayerSource::new),
		CLIFF_CURVE   (Category.MEDIUM,  "cliff",          "Cliff Curve",       CliffCurveLayerSource::new),
		TILE          (Category.MEDIUM,  "tile",           "Tile",                    TileLayerSource::new),

		CONVOLVE      (Category.COMPLEX, "convolve",       "Convolve",            ConvolveLayerSource::new),
		K_MEANS       (Category.COMPLEX, "kmeans",         "K-Means",               KMeansLayerSource::new),
		DENOISE       (Category.COMPLEX, "denoise",        "De-noise",             DeNoiseLayerSource::new),
		WFC           (Category.COMPLEX, "wfc",            "Wave Function Collapse",   WFCLayerSource::new),

		DERIVED       (Category.ROOT,    "derived",        "Derived",              DerivedLayerSource::new);

		public static final Type[] VALUES = values();
		public static final Map<String, Type> BY_SAVE_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(Type::getSaveName, Function.identity()));

		public final Category category;
		public final String saveName, displayName;
		public final Function<LayerSources, LayerSource> factory;

		Type(Category category, String saveName, String displayName, Function<LayerSources, LayerSource> factory) {
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
		public Category getCategory() {
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