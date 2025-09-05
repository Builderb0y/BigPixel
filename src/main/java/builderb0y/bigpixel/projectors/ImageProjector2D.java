package builderb0y.bigpixel.projectors;

import java.util.Arrays;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.ZoomableImage;

public abstract class ImageProjector2D extends ImageProjector {

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
	public double offsetX, offsetY;
	public SimpleIntegerProperty zoomIndex = new SimpleIntegerProperty(12); //1.0
	public ObservableValue<Double> zoom = this.zoomIndex.map((Number index) -> ZOOMS[index.intValue()]);

	public ImageProjector2D(Type type, ZoomableImage zoomableImage) {
		super(type, zoomableImage);
		this.zoom.addListener(Util.change(zoomableImage.f3::updateZoom));
	}

	@Override
	public @Nullable Texcoord project(double x, double y) {
		double zoom = this.zoom.getValue();
		int projectedX = (int)(Math.floor((x - this.offsetX) / zoom));
		int projectedY = (int)(Math.floor((y - this.offsetY) / zoom));
		return this.handleEdge(projectedX, projectedY);
	}

	public abstract @Nullable Texcoord handleEdge(int projectedX, int projectedY);

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
				(this.offsetX - x) * (newZoom / oldZoom) + x,
				(this.offsetY - y) * (newZoom / oldZoom) + y
			);
		}
	}

	public void setPosition(double posX, double posY) {
		LayerNode layer = this.zoomableImage.openImage.layerGraph.visibleLayerProperty.getValue();
		if (layer == null) return;
		HDRImage image = layer.image;
		double zoom = this.zoom.getValue();
		Canvas canvas = this.zoomableImage.display.display;
		if (posX + image.width  * zoom < 0) posX = image.width  * -zoom;
		if (posY + image.height * zoom < 0) posY = image.height * -zoom;
		if (posX > canvas.getWidth ()) posX = canvas.getWidth ();
		if (posY > canvas.getHeight()) posY = canvas.getHeight();
		this.offsetX = posX;
		this.offsetY = posY;
		this.zoomableImage.redraw();
	}

	@Override
	public void drag(double deltaX, double deltaY) {
		this.setPosition(this.offsetX + deltaX, this.offsetY + deltaY);
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
		this.offsetX = (this.canvasWidth  - this.layerWidth  * ZOOMS[zoomIndex]) * 0.5D;
		this.offsetY = (this.canvasHeight - this.layerHeight * ZOOMS[zoomIndex]) * 0.5D;
	}

	@Override
	public void drawOutline(
		byte[] pixels,
		int x1,
		int y1,
		int x2,
		int y2,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double zoom = this.zoom.getValue();
		x1 = (int)(Math.ceil(this.offsetX + x1 * zoom)) - 1;
		y1 = (int)(Math.ceil(this.offsetY + y1 * zoom)) - 1;
		x2 = (int)(Math.ceil(this.offsetX + x2 * zoom));
		y2 = (int)(Math.ceil(this.offsetY + y2 * zoom));
		for (int x = x1; x <= x2; x++) {
			setColorSafe(pixels, x, y1, canvasWidth, canvasHeight, ((x ^ y1) & 8) == 0 ? dark : light);
			setColorSafe(pixels, x, y2, canvasWidth, canvasHeight, ((x ^ y2) & 8) == 0 ? dark : light);
		}
		for (int y = y1; ++y < y2;) {
			setColorSafe(pixels, x1, y, canvasWidth, canvasHeight, ((x1 ^ y) & 8) == 0 ? dark : light);
			setColorSafe(pixels, x2, y, canvasWidth, canvasHeight, ((x2 ^ y) & 8) == 0 ? dark : light);
		}
	}
}