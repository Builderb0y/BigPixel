package builderb0y.bigpixel.views;

import java.util.Arrays;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableDoubleValue;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.F3Menu;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.ZoomableImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.SingletonDependencies;
import builderb0y.bigpixel.util.Util;

public abstract class LayerView2D extends LayerView {

	public static final double[] ZOOMS = {
		0.015625D,
		0.0234375D,
		0.03125D,
		0.046875D,
		0.0625D,
		0.09375D,
		0.125D,
		0.1875D,
		0.25D,
		0.375D,
		0.5D,
		0.75D,
		1.0D,
		1.5D,
		2.0D,
		3.0D,
		4.0D,
		6.0D,
		8.0D,
		12.0D,
		16.0D,
		24.0D,
		32.0D,
		48.0D,
		64.0D,
	};
	public final SimpleDoubleProperty
		offsetX = new SimpleDoubleProperty(this, "offsetX"),
		offsetY = new SimpleDoubleProperty(this, "offsetY");
	public final SimpleIntegerProperty zoomIndex = new SimpleIntegerProperty(12); //1.0
	public final ObservableDoubleValue zoom = Util.toDouble(this.zoomIndex.map((Number index) -> ZOOMS[index.intValue()]), 1.0D);
	public final SingletonDependencies dependencies;

	public LayerView2D(LayerViewType type, LayerViews views) {
		super(type, views);
		this.dependencies = new SingletonDependencies(views.layer);
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public @Nullable ProjectionResult project(double x, double y, int frameIndex) {
		double zoom = this.zoom.get();
		double rawX = (x - this.offsetX.get()) / zoom;
		double rawY = (y - this.offsetY.get()) / zoom;
		int projectedX = (int)(Math.floor(rawX));
		int projectedY = (int)(Math.floor(rawY));
		return this.handleEdge(rawX, rawY, projectedX, projectedY, frameIndex);
	}

	public abstract @Nullable ProjectionResult handleEdge(double rawX, double rawY, int projectedX, int projectedY, int frameIndex);

	@Override
	public void zoom(double x, double y, boolean zoomIn) {
		int oldZoomIndex = this.zoomIndex.get();
		int newZoomIndex;
		if (zoomIn) {
			newZoomIndex = Math.min(oldZoomIndex + 1, ZOOMS.length - 1);
		}
		else {
			newZoomIndex = Math.max(oldZoomIndex - 1, 0);
		}
		if (newZoomIndex != oldZoomIndex) {
			this.zoomIndex.set(newZoomIndex);
			double oldZoom = ZOOMS[oldZoomIndex];
			double newZoom = ZOOMS[newZoomIndex];
			this.setPosition(
				(this.offsetX.get() - x) * (newZoom / oldZoom) + x,
				(this.offsetY.get() - y) * (newZoom / oldZoom) + y
			);
		}
	}

	public void setPosition(double posX, double posY) {
		LayerNode layer = this.views.layer;
		if (layer == null) return;
		double width = layer.imageWidth();
		double height = layer.imageHeight();
		double zoom = this.zoom.get();
		ZoomableImage zoomableImage = this.views.layer.graph.openImage.imageDisplay;
		double canvasWidth = zoomableImage.canvasHolder.display.getWidth();
		double canvasHeight = zoomableImage.canvasHolder.display.getHeight();
		if (posX + width  * zoom < 0) posX = width  * -zoom;
		if (posY + height * zoom < 0) posY = height * -zoom;
		if (posX > canvasWidth ) posX = canvasWidth ;
		if (posY > canvasHeight) posY = canvasHeight;
		this.offsetX.set(posX);
		this.offsetY.set(posY);
		//zoomableImage.redrawLater();
	}

	@Override
	public void drag(double deltaX, double deltaY) {
		this.setPosition(this.offsetX.get() + deltaX, this.offsetY.get() + deltaY);
	}

	@Override
	public void center() {
		int zoomIndex = Arrays.binarySearch(
			ZOOMS,
			Math.min(
				this.canvasWidth / this.layerWidth,
				this.canvasHeight / this.layerHeight
			)
		);
		if (zoomIndex < 0) zoomIndex = Math.min(-1 + ~zoomIndex, ZOOMS.length - 1);

		this.zoomIndex.set(zoomIndex);
		this.offsetX.set((this.canvasWidth  - this.layerWidth  * ZOOMS[zoomIndex]) * 0.5D);
		this.offsetY.set((this.canvasHeight - this.layerHeight * ZOOMS[zoomIndex]) * 0.5D);
	}

	@Override
	public void drawLayerOutline(
		byte[] pixels,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight
	) {
		this.drawOutline(
			pixels,
			0,
			0,
			layerWidth,
			layerHeight,
			canvasWidth,
			canvasHeight,
			LAYER_OUTLINE_DARK,
			LAYER_OUTLINE_LIGHT
		);
	}

	@Override
	public void drawSelectionOutline(
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
	) {
		this.drawOutline(
			pixels,
			x1,
			y1,
			x2,
			y2,
			canvasWidth,
			canvasHeight,
			SELECTION_OUTLINE_DARK,
			SELECTION_OUTLINE_LIGHT
		);
	}

	public void drawOutline(
		byte[] pixels,
		int x1,
		int y1,
		int x2,
		int y2,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double zoom = this.zoom.get();
		x1 = (int)(Math.ceil(this.offsetX.get() + x1 * zoom)) - 1;
		y1 = (int)(Math.ceil(this.offsetY.get() + y1 * zoom)) - 1;
		x2 = (int)(Math.ceil(this.offsetX.get() + x2 * zoom));
		y2 = (int)(Math.ceil(this.offsetY.get() + y2 * zoom));
		for (int x = x1; x <= x2; x++) {
			setColorSafe(pixels, x, y1, canvasWidth, canvasHeight, ((x ^ y1) & 8) == 0 ? dark : light);
			setColorSafe(pixels, x, y2, canvasWidth, canvasHeight, ((x ^ y2) & 8) == 0 ? dark : light);
		}
		for (int y = y1; ++y < y2;) {
			setColorSafe(pixels, x1, y, canvasWidth, canvasHeight, ((x1 ^ y) & 8) == 0 ? dark : light);
			setColorSafe(pixels, x2, y, canvasWidth, canvasHeight, ((x2 ^ y) & 8) == 0 ? dark : light);
		}
	}

	@Override
	public void updateF3(F3Menu f3) {
		f3.add("Zoom: " + this.zoom.getValue());
	}
}