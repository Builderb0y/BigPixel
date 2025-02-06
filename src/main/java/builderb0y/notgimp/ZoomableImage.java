package builderb0y.notgimp;

import java.util.Arrays;
import java.util.stream.IntStream;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import builderb0y.notgimp.RateLimiter.PeriodicRateLimiter;
import builderb0y.notgimp.sources.ManualLayerSource;
import builderb0y.notgimp.tools.SourcelessTool;
import builderb0y.notgimp.tools.Tool;
import builderb0y.notgimp.tools.Tool.Selection;

import static builderb0y.notgimp.HDRImage.*;

public class ZoomableImage {

	public static final double[] ZOOMS = {
		1.0D / 64.0D,
		1.0D / 48.0D,
		1.0D / 32.0D,
		1.0D / 24.0D,
		1.0D / 16.0D,
		1.0D / 12.0D,
		1.0D /  8.0D,
		1.0D /  6.0D,
		1.0D /  4.0D,
		1.0D /  3.0D,
		1.0D /  2.0D,
		1.0D,
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

	public OpenImage openImage;
	public CanvasHelper display;
	public double offsetX, offsetY;
	public IntegerProperty zoomIndex = new SimpleIntegerProperty(11); //1.0
	public ObservableValue<Double> zoom = this.zoomIndex.map((Number index) -> ZOOMS[index.intValue()]);
	public ChangeListener<Number> centerer;
	public RateLimiter redrawer;
	public HdrImageWatcher watcher = (HDRImage image, boolean fromAnimation) -> this.redraw();

	public ZoomableImage(OpenImage openImage) {
		this.openImage = openImage;
		this.display = new CanvasHelper().checkerboard().resizeable((Canvas canvas) -> this.redraw());
		this.centerer = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> this.center();
		this.redrawer = new PeriodicRateLimiter(20L, this::doRedraw);
	}

	public void init() {
		this.display.canvas. widthProperty().addListener(this.centerer);
		this.display.canvas.heightProperty().addListener(this.centerer);
		Canvas canvas = this.display.canvas;
		this.openImage.wrap.addListener(Util.change(this::redraw));
		this.openImage.showingLayerProperty.addListener(Util.change((Layer oldLayer, Layer newLayer) -> {
			oldLayer.image.removeWatcher(this.watcher);
			newLayer.image.addWatcher(this.watcher);
			this.redraw();
		}));
		this.openImage.showingLayerProperty.getValue().image.addWatcher(this.watcher);
		canvas.setOnScroll((ScrollEvent event) -> {
			int oldZoomIndex = this.zoomIndex.get();
			int newZoomIndex;
			if (event.getDeltaY() > 0.0D) {
				newZoomIndex = Math.max(oldZoomIndex - 1, 0);
			}
			else if (event.getDeltaY() < 0.0D) {
				newZoomIndex = Math.min(oldZoomIndex + 1, ZOOMS.length - 1);
			}
			else {
				return;
			}
			if (newZoomIndex != oldZoomIndex) {
				this.zoomIndex.set(newZoomIndex);
				double oldZoom = ZOOMS[oldZoomIndex];
				double newZoom = ZOOMS[newZoomIndex];
				this.setPosition(
					(this.offsetX - event.getX()) * (oldZoom / newZoom) + event.getX(),
					(this.offsetY - event.getY()) * (oldZoom / newZoom) + event.getY()
				);
				this.redraw();
			}
		});
		this.display.getRootPane().cursorProperty().bind(this.openImage.cursorProperty);
		EventHandler<MouseEvent> handler = new EventHandler<>() {

			public double pressX, pressY;
			public int toolX, toolY;

			@Override
			public void handle(MouseEvent event) {
				Canvas canvas = ZoomableImage.this.display.canvas;
				canvas.requestFocus();
				if (event.getButton() == MouseButton.MIDDLE) {
					if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
						this.pressX = event.getX();
						this.pressY = event.getY();
						canvas.setCursor(Cursor.CROSSHAIR);
					}
					else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
						ZoomableImage.this.setPosition(
							ZoomableImage.this.offsetX + (event.getX() - this.pressX),
							ZoomableImage.this.offsetY + (event.getY() - this.pressY)
						);
						this.pressX = event.getX();
						this.pressY = event.getY();
						ZoomableImage.this.redraw();
					}
					else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
						ZoomableImage.this.redraw();
						canvas.setCursor(null);
					}
				}
				else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
					Layer layer = ZoomableImage.this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue();
					SourcelessTool<?> tool = ZoomableImage.this.openImage.toolWithColorPicker.get();
					if (tool != null) {
						double zoom = ZoomableImage.this.zoom.getValue();
						int x = (int)(Math.floor((event.getX() - ZoomableImage.this.offsetX) * zoom));
						int y = (int)(Math.floor((event.getY() - ZoomableImage.this.offsetY) * zoom));
						if (ZoomableImage.this.openImage.wrap.get()) {
							x = Math.floorMod(x, layer.image.width);
							y = Math.floorMod(y, layer.image.height);
						}
						if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
							this.toolX = x;
							this.toolY = y;
							tool.mouseDown(x, y, event.getButton());
						}
						else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
							if (this.toolX != x || this.toolY != y) {
								this.toolX = x;
								this.toolY = y;
								tool.mouseDragged(x, y, event.getButton());
							}
						}
					}
				}
			}
		};
		canvas.setOnMousePressed(handler);
		canvas.setOnMouseDragged(handler);
		canvas.setOnMouseReleased(handler);
	}

	public void setPosition(double posX, double posY) {
		HDRImage image = this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue().image;
		double zoom = this.zoom.getValue();
		if (posX + image.width  / zoom < 0) posX = image.width  / -zoom;
		if (posY + image.height / zoom < 0) posY = image.height / -zoom;
		if (posX > this.display.canvas.getWidth ()) posX = this.display.canvas.getWidth ();
		if (posY > this.display.canvas.getHeight()) posY = this.display.canvas.getHeight();
		this.offsetX = posX;
		this.offsetY = posY;
	}

	public void center() {
		//hacky code to only center once.
		if (this.centerer == null) return;
		Canvas canvas = this.display.canvas;
		if (canvas.getWidth() == 0.0D || canvas.getHeight() == 0.0D) return;
		HDRImage image = this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue().image;
		int zoomIndex = Arrays.binarySearch(
			ZOOMS,
			Math.max(
				image.width  / canvas.getWidth(),
				image.height / canvas.getHeight()
			)
		);
		if (zoomIndex < 0) zoomIndex = Math.min(~zoomIndex, ZOOMS.length - 1);

		this.zoomIndex.set(zoomIndex);
		this.offsetX = (canvas.getWidth () - image.width  / ZOOMS[zoomIndex]) * 0.5D;
		this.offsetY = (canvas.getHeight() - image.height / ZOOMS[zoomIndex]) * 0.5D;
		canvas. widthProperty().removeListener(this.centerer);
		canvas.heightProperty().removeListener(this.centerer);
		this.centerer = null;
	}

	public void redraw() {
		this.redrawer.run();
	}

	public void doRedraw() {
		HDRImage image = this.openImage.showingLayerProperty.getValue().image;
		Canvas canvas = this.display.canvas;
		PixelWriter writer = canvas.getGraphicsContext2D().getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		double zoom = this.zoom.getValue();
		int width = (int)(canvas.getWidth());
		int height = (int)(canvas.getHeight());
		byte[] pixels = new byte[width * height * 4];
		boolean wrap = this.openImage.wrap.get();
		IntStream.range(0, height).parallel().forEach((int y) -> {
			int mappedY = (int)(Math.floor((y - this.offsetY) * zoom));
			if (wrap) {
				mappedY = Math.floorMod(mappedY, image.height);
			}
			for (int x = 0; x < width; x++) {
				int mappedX = (int)(Math.floor((x - this.offsetX) * zoom));
				if (wrap) {
					mappedX = Math.floorMod(mappedX, image.width);
				}
				float red = 0.0F, green = 0.0F, blue = 0.0F, alpha = 0.0F;
				if (mappedX >= 0 && mappedX < image.width && mappedY >= 0 && mappedY < image.height) {
					int baseIndex = image.baseIndex(mappedX, mappedY);
					red   = image.pixels[baseIndex |   RED_OFFSET];
					green = image.pixels[baseIndex | GREEN_OFFSET];
					blue  = image.pixels[baseIndex |  BLUE_OFFSET];
					alpha = image.pixels[baseIndex | ALPHA_OFFSET];
				}
				int baseIndex = (y * width + x) << 2;
				pixels[baseIndex    ] = Util.clampB(blue  * alpha);
				pixels[baseIndex | 1] = Util.clampB(green * alpha);
				pixels[baseIndex | 2] = Util.clampB(red   * alpha);
				pixels[baseIndex | 3] = Util.clampB(        alpha);
			}
		});
		int
			x1 = 0,
			y1 = 0,
			x2 = image.width,
			y2 = image.height;
		if (this.openImage.getSelectedLayer().sources.getCurrentSource() instanceof ManualLayerSource manual) {
			Selection selection = new Selection();
			Tool<?> tool = manual.toolWithoutColorPicker.get();
			if (tool != null && tool.getSelection(selection)) {
				x1 = selection.minX;
				y1 = selection.minY;
				x2 = selection.maxX + 1;
				y2 = selection.maxY + 1;
			}
		}
		x1 = (int)(Math.ceil(this.offsetX + x1 / zoom)) - 1;
		y1 = (int)(Math.ceil(this.offsetY + y1 / zoom)) - 1;
		x2 = (int)(Math.ceil(this.offsetX + x2 / zoom));
		y2 = (int)(Math.ceil(this.offsetY + y2 / zoom));
		for (int x = x1; x <= x2; x++) {
			setGrayscaleSafe(pixels, x, y1, width, height, ((x ^ y1) & 8) == 0 ? 0.0F : 1.0F);
			setGrayscaleSafe(pixels, x, y2, width, height, ((x ^ y2) & 8) == 0 ? 0.0F : 1.0F);
		}
		for (int y = y1; ++y < y2;) {
			setGrayscaleSafe(pixels, x1, y, width, height, ((x1 ^ y) & 8) == 0 ? 0.0F : 1.0F);
			setGrayscaleSafe(pixels, x2, y, width, height, ((x2 ^ y) & 8) == 0 ? 0.0F : 1.0F);
		}
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
	}

	public static void setGrayscaleSafe(byte[] pixels, int x, int y, int width, int height, float grayscale) {
		if (x >= 0 && x < width && y >= 0 && y < height) {
			int baseIndex = (y * width + x) << 2;
			byte value = Util.clampB(grayscale);
			pixels[baseIndex    ] = value;
			pixels[baseIndex | 1] = value;
			pixels[baseIndex | 2] = value;
			pixels[baseIndex | 3] = -1;
		}
	}
}