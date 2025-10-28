package builderb0y.bigpixel.views;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ConfigParameter;
import builderb0y.bigpixel.F3Menu;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;

public abstract class LayerView implements OrganizedSelection.Value<LayerView.Type> {

	public final Type type;
	public final LayerViews views;
	public final ViewParameters parameters = new ViewParameters(this);
	public CheckBox drawOutline = this.parameters.addCheckbox("draw_outline", "Draw Outline", true);
	public double canvasWidth, canvasHeight;
	public int layerWidth, layerHeight;

	public LayerView(Type type, LayerViews views) {
		this.type = type;
		this.views = views;
		super();
	}

	@Override
	public LayerNode getLayer() {
		return this.views.layer;
	}

	public void beforeRedraw(Canvas canvas, LayerNode layer) {
		this.canvasWidth  = canvas.getWidth();
		this.canvasHeight = canvas.getHeight();
		this.layerWidth   = layer.image.width;
		this.layerHeight  = layer.image.height;
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
	public void load(JsonMap root) {
		for (ConfigParameter<?, ?> parameter : this.parameters) {
			parameter.load(root);
		}
		this.getDependencies().load(root.getMap("dependencies"));
	}

	public abstract Node getRootConfigPane();

	@Override
	public void redrawLater() {
		this.views.layer.graph.openImage.imageDisplay.redrawLater();
	}

	public abstract @Nullable ProjectionResult project(double x, double y);

	public static class ProjectionResult {

		public final LayerSourceInput layer;
		public final int x;
		public final int y;

		public ProjectionResult(LayerSourceInput layer, int x, int y) {
			this.layer = layer;
			this.x = x;
			this.y = y;
		}

		public FloatVector sample() {
			return this.layer.getColor(this.x, this.y);
		}

		@Override
		public String toString() {
			return this.x + ", " + this.y;
		}
	}

	public abstract void zoom(double x, double y, boolean zoomIn);

	public abstract void drag(double deltaX, double deltaY);

	public abstract void center();

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
	public Type getType() {
		return this.type;
	}

	public static enum Category implements OrganizedSelection.Category<Category> {
		ROOT;

		@Override
		public @Nullable Category getParent() {
			return null;
		}

		@Override
		public String toString() {
			return "View Modes";
		}
	}

	public static enum Type implements OrganizedSelection.Type<LayerView, Category> {
		FLAT_CLAMPED("flat_clamped", "Flat (Clamped)", FlatClampedLayerView::new),
		FLAT_TILING("flat_tiling", "Flat (Tiling)", FlatTilingLayerView::new),
		CUBE("cube", "Cube", CubeLayerView::new);

		public static final Type[] VALUES = values();
		public static final Map<String, Type> BY_SAVE_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(Type::getSaveName, Function.identity()));

		public final String saveName, displayName;
		public final Function<LayerViews, LayerView> constructor;

		Type(String saveName, String name, Function<LayerViews, LayerView> constructor) {
			this.saveName = saveName;
			this.displayName = name;
			this.constructor = constructor;
		}

		@Override
		public LayerView create(LayerNode layer) {
			return this.constructor.apply(layer.views);
		}

		@Override
		public Category getCategory() {
			return Category.ROOT;
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