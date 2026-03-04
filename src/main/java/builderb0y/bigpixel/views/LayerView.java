package builderb0y.bigpixel.views;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.ZoomableImage.DrawParams;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView.LayerViewType;

public abstract class LayerView implements OrganizedSelection.Value<LayerViewType> {

	public final LayerViewType type;
	public final LayerViews views;
	public final ConfigParameters parameters;
	public final CheckBox drawOutline;
	public final CheckBox showAlpha;
	public double canvasWidth, canvasHeight;
	public int layerWidth, layerHeight;

	public LayerView(LayerViewType type, LayerViews views) {
		this.type = type;
		this.views = views;
		OpenImage image = this.getLayer().graph.openImage;
		this.parameters = new ConfigParameters(image.parameterSet, Util.change(image.imageDisplay::redrawLater));
		this.drawOutline = this.parameters.addCheckbox("draw_outline", "Draw Outline", true);
		this.showAlpha = this.parameters.addCheckbox("show_alpha", "Show Alpha", true);
	}

	@Override
	public LayerNode getLayer() {
		return this.views.layer;
	}

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap().with("type", this.type.saveName);
		for (ConfigParameter<?> parameter : this.parameters) {
			parameter.save(map);
		}
		return map.with("dependencies", this.getDependencies().save());
	}

	@Override
	public void load(JsonMap root) {
		for (ConfigParameter<?> parameter : this.parameters) {
			parameter.load(root);
		}
		this.getDependencies().load(root.getMap("dependencies"));
	}

	public abstract Node getRootConfigPane();

	public void beforeRedraw(int imageWidth, int imageHeight, int canvasWidth, int canvasHeight) {
		this.layerWidth   = imageWidth;
		this.layerHeight  = imageHeight;
		this.canvasWidth  = canvasWidth;
		this.canvasHeight = canvasHeight;
	}

	@Override
	public void redrawLater() {
		this.views.layer.graph.openImage.imageDisplay.redrawLater();
	}

	public abstract @Nullable ProjectionResult project(double x, double y, int frameIndex);

	public static record ProjectionResult(
		SamplerProvider input,
		double rawX,
		double rawY,
		int x,
		int y,
		float rawR,
		float rawG,
		float rawB,
		float rawA,
		float r,
		float g,
		float b,
		float a
	) {

		public static final DecimalFormat COLOR_FORMAT = new DecimalFormat();
		static {
			COLOR_FORMAT.setMaximumFractionDigits(3);
		}

		public ProjectionResult(
			SamplerProvider input,
			double rawX,
			double rawY,
			int x,
			int y,
			float r,
			float g,
			float b,
			float a,
			float shade
		) {
			this(input, rawX, rawY, x, y, r, g, b, a, r * shade, g * shade, b * shade, a);
		}

		public ProjectionResult(SamplerProvider input, int x, int y) {
			this(input, x, y, x, y, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		}

		@Override
		public String toString() {
			return "(" + this.x + ", " + this.y + ") -> [ " + COLOR_FORMAT.format(this.r) + ", " + COLOR_FORMAT.format(this.g) + ", " + COLOR_FORMAT.format(this.b) + ", " + COLOR_FORMAT.format(this.a) + " ]";
		}
	}

	public abstract void zoom(double x, double y, boolean zoomIn);

	public abstract void drag(double deltaX, double deltaY);

	public abstract void center();

	public abstract ObservableValue<DrawParams> drawParamsProperty();

	public void updateF3(F3Menu f3) {
		//no-op by default.
	}

	public static final int
		LAYER_OUTLINE_DARK = 0xFF7F7F3F,
		LAYER_OUTLINE_LIGHT = 0xFFFFFFBF,
		SELECTION_OUTLINE_DARK = 0xFF000000,
		SELECTION_OUTLINE_LIGHT = 0xFFFFFFFF;

	public abstract void drawLayerOutline(
		byte[] pixels,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight
	);

	public abstract void drawSelectionOutline(
		LayerNode layer,
		byte[] pixels,
		int x1,
		int y1,
		int x2,
		int y2,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight
	);

	public static void setColorSafe(byte[] pixels, int x, int y, int width, int height, int argb) {
		if (x >= 0 && x < width && y >= 0 && y < height) {
			int baseIndex = (y * width + x) << 2;
			pixels[baseIndex    ] = (byte)(argb       );
			pixels[baseIndex | 1] = (byte)(argb >>>  8);
			pixels[baseIndex | 2] = (byte)(argb >>> 16);
			pixels[baseIndex | 3] = (byte)(argb >>> 24);
		}
	}

	@Override
	public LayerViewType getType() {
		return this.type;
	}

	public static enum LayerViewCategory implements OrganizedSelection.Category<LayerViewCategory> {
		ROOT;

		@Override
		public @Nullable LayerView.LayerViewCategory getParent() {
			return null;
		}

		@Override
		public String toString() {
			return "View Modes";
		}
	}

	public static enum LayerViewType implements OrganizedSelection.Type<LayerView, LayerViewCategory> {
		FLAT_CLAMPED("flat_clamped", "Flat (Clamped)", FlatClampedLayerView::new),
		FLAT_TILING("flat_tiling", "Flat (Tiling)", FlatTilingLayerView::new),
		CUBE("cube", "Cube", CubeLayerView::new);

		public static final LayerViewType[] VALUES = values();
		public static final Map<String, LayerViewType> BY_SAVE_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(LayerViewType::getSaveName, Function.identity()));

		public final String saveName, displayName;
		public final Function<LayerViews, LayerView> constructor;

		LayerViewType(String saveName, String name, Function<LayerViews, LayerView> constructor) {
			this.saveName = saveName;
			this.displayName = name;
			this.constructor = constructor;
		}

		@Override
		public LayerView create(LayerNode layer) {
			return this.constructor.apply(layer.views);
		}

		@Override
		public LayerViewCategory getCategory() {
			return LayerViewCategory.ROOT;
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