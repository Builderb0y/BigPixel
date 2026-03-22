package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ConfigParameters;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.LayerSource.LayerSourceType;
import builderb0y.bigpixel.util.Util;

public abstract class LayerSource implements OrganizedSelection.Value<LayerSourceType> {

	public final LayerSourceType type;
	public final LayerSources sources;
	public final ConfigParameters parameters;
	public final CheckBox clampRGB, clampAlpha;
	public final VBox extraSettingsPane;
	public final PopOver extraSettingsPopOver;
	public final AtomicInteger currentProgress = new AtomicInteger();
	public int maxProgress = 0;

	public LayerSource(LayerSourceType type, LayerSources sources) {
		this.type = type;
		this.sources = sources;
		this.parameters = new ConfigParameters(sources.layer.graph.openImage.parameterSet, Util.change(this::redrawLater));
		this.clampRGB = this.parameters.addCheckbox("clampRGB", "Clamp RGB", true);
		this.clampAlpha = this.parameters.addCheckbox("clampA", "Clamp Alpha", true);
		this.extraSettingsPane = new VBox(this.clampRGB, this.clampAlpha);
		this.extraSettingsPane.setSpacing(4.0D);
		this.extraSettingsPopOver = Util.setupPopOver(new PopOver(this.extraSettingsPane));
		ChangeListener<Object> listener = Util.change(this::redrawLater);
		this.clampRGB.selectedProperty().addListener(listener);
		this.clampAlpha.selectedProperty().addListener(listener);
	}

	public abstract int computeMaxProgress(int width, int height);

	public void startProgressing() {
		LayerNode layer = this.sources.layer;
		this.currentProgress.set(0);
		this.maxProgress = this.computeMaxProgress(layer.imageWidth(), layer.imageHeight());
		if (this.getDependencies().animatedProperty().get()) {
			this.maxProgress *= layer.animation.getFrameCount();
		}
		this.getLayer().progressChangedAsync();
	}

	public void incrementProgress() {
		int progress = this.currentProgress.incrementAndGet();
		if (progress > this.maxProgress) {
			new Throwable("Too much progress! " + progress + " > " + this.maxProgress).printStackTrace();
		}
		this.getLayer().progressChangedAsync();
	}

	public void setProgress(int oneIndexedProgress) {
		this.currentProgress.set(oneIndexedProgress);
		this.getLayer().progressChangedAsync();
	}

	public double getProgress() {
		if (this.maxProgress == 0) return 0.0D;
		return this.currentProgress.doubleValue() / this.maxProgress;
	}

	public static FloatVector clamp(FloatVector color, boolean clampRGB, boolean clampAlpha) {
		if (clampRGB) {
			if (clampAlpha) {
				color = color.max(0.0F).min(1.0F);
				return color.blend(0.0F, color.test(VectorOperators.IS_NAN));
			}
			else {
				color = color.lanewise(VectorOperators.MAX, 0.0F, Util.RGB_MASK).lanewise(VectorOperators.MIN, 1.0F, Util.RGB_MASK);
				return color.blend(0.0F, color.test(VectorOperators.IS_NAN).and(Util.RGB_MASK));
			}
		}
		else {
			if (clampAlpha) {
				float alpha = color.lane(HDRImage.ALPHA_OFFSET);
				if (!(alpha >= 0.0F)) alpha = 0.0F;
				if (!(alpha <= 1.0F)) alpha = 1.0F;
				return color.withLane(HDRImage.ALPHA_OFFSET, alpha);
			}
			else {
				return color;
			}
		}
	}

	public FloatVector clampColor(FloatVector color) {
		return clamp(color, this.clampRGB.isSelected(), this.clampAlpha.isSelected());
	}

	public void clampImage(HDRImage destination) {
		boolean clampRGB = this.clampRGB.isSelected();
		boolean clampA = this.clampAlpha.isSelected();
		if (clampRGB || clampA) {
			IntStream.range(0, destination.pixels.length >> 2).parallel().map((int index) -> index << 2).forEach((int index) -> {
				clamp(FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, index), clampRGB, clampA).intoArray(destination.pixels, index);
			});
		}
	}

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap().with("type", this.type.saveName);
		this.parameters.save(map);
		return map.with("dependencies", this.getDependencies().save());
	}

	@Override
	public void load(JsonMap map) {
		this.parameters.load(map);
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

	@Override
	public void redrawLater() {
		this.sources.layer.requestRedraw();
	}

	public void resizeIfNecessary() throws RedrawException {}

	public abstract void doRedraw(int frame) throws RedrawException;

	public Node getConfigPane() {
		return this.getDependencies().getConfigPane();
	}

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
		RESCALE       (LayerSourceCategory.MEDIUM,  "rescale",        "Rescale",              RescaleLayerSource::new),

		MODE_BLUR     (LayerSourceCategory.COMPLEX, "mode_blur",      "Mode Blur",           ModeBlurLayerSource::new),
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