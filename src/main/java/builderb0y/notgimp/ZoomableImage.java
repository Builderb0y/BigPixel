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
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import builderb0y.notgimp.RateLimiter.PeriodicRateLimiter;
import builderb0y.notgimp.sources.ManualLayerSource;
import builderb0y.notgimp.tools.SourcelessTool;
import builderb0y.notgimp.tools.Tool;
import builderb0y.notgimp.tools.Tool.Selection;

import static builderb0y.notgimp.HDRImage.*;

public class ZoomableImage {

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

	public OpenImage openImage;
	public CanvasHelper display;
	public F3Menu f3;
	public StackPane displayWithF3;
	public double offsetX, offsetY;
	public double lastMouseX, lastMouseY;
	public SimpleIntegerProperty zoomIndex = new SimpleIntegerProperty(12); //1.0
	public ObservableValue<Double> zoom = this.zoomIndex.map((Number index) -> ZOOMS[index.intValue()]);
	public ChangeListener<Number> centerer;
	public RateLimiter redrawer;

	public ZoomableImage(OpenImage openImage) {
		this.openImage = openImage;
		this.display = new CanvasHelper().checkerboard().resizeable((Canvas canvas) -> this.redraw());
		this.f3 = new F3Menu();
		this.displayWithF3 = new StackPane(this.display.getRootPane(), this.f3.rootNode());
		this.centerer = Util.change(this::centerOnce);
		this.redrawer = new PeriodicRateLimiter(20L, this::doRedraw);
	}

	public void init() {
		this.display.display. widthProperty().addListener(this.centerer);
		this.display.display.heightProperty().addListener(this.centerer);
		this.zoom.addListener(Util.change((Double zoom) -> {
			this.f3.updateZoom(zoom);
		}));
		Canvas canvas = this.display.display;
		ChangeListener<Object> redrawer = Util.change(this::redraw);
		this.openImage.wrap.addListener(redrawer);
		this.openImage.showingLayerProperty.addListener(redrawer);
		canvas.setOnScroll((ScrollEvent event) -> {
			int oldZoomIndex = this.zoomIndex.get();
			int newZoomIndex;
			if (event.getDeltaY() < 0.0D) {
				newZoomIndex = Math.max(oldZoomIndex - 1, 0);
			}
			else if (event.getDeltaY() > 0.0D) {
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
					(this.offsetX - event.getX()) * (newZoom / oldZoom) + event.getX(),
					(this.offsetY - event.getY()) * (newZoom / oldZoom) + event.getY()
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
				Canvas canvas = ZoomableImage.this.display.display;
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
						int x = (int)(Math.floor((event.getX() - ZoomableImage.this.offsetX) / zoom));
						int y = (int)(Math.floor((event.getY() - ZoomableImage.this.offsetY) / zoom));
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
		canvas.setOnMouseMoved((MouseEvent event) -> {
			this.lastMouseX = event.getX();
			this.lastMouseY = event.getY();
			double zoom = this.zoom.getValue();
			int x = (int)(Math.floor((event.getX() - this.offsetX) / zoom));
			int y = (int)(Math.floor((event.getY() - this.offsetY) / zoom));
			if (this.openImage.wrap.get()) {
				Layer layer = this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue();
				x = Math.floorMod(x, layer.image.width);
				y = Math.floorMod(y, layer.image.height);
			}
			this.f3.updatePos(x, y);
		});
		canvas.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.F3) {
				Pane pane = this.f3.rootNode();
				pane.setVisible(!pane.isVisible());
			}
		});
	}

	public void setPosition(double posX, double posY) {
		HDRImage image = this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue().image;
		double zoom = this.zoom.getValue();
		if (posX + image.width  * zoom < 0) posX = image.width  * -zoom;
		if (posY + image.height * zoom < 0) posY = image.height * -zoom;
		if (posX > this.display.display.getWidth ()) posX = this.display.display.getWidth ();
		if (posY > this.display.display.getHeight()) posY = this.display.display.getHeight();
		this.offsetX = posX;
		this.offsetY = posY;
	}

	public void centerOnce() {
		if (this.centerer != null && this.center()) {
			Canvas canvas = this.display.display;
			canvas. widthProperty().removeListener(this.centerer);
			canvas.heightProperty().removeListener(this.centerer);
			this.centerer = null;
		}
	}

	public boolean center() {
		Canvas canvas = this.display.display;
		if (canvas.getWidth() == 0.0D || canvas.getHeight() == 0.0D) return false;
		HDRImage image = this.openImage.layerTree.getSelectionModel().getSelectedItem().getValue().image;
		int zoomIndex = Arrays.binarySearch(
			ZOOMS,
			Math.min(
				canvas.getWidth () / image.width,
				canvas.getHeight() / image.height
			)
		);
		if (zoomIndex < 0) zoomIndex = Math.min(-1 + ~zoomIndex, ZOOMS.length - 1);

		this.zoomIndex.set(zoomIndex);
		this.offsetX = (canvas.getWidth () - image.width  * ZOOMS[zoomIndex]) * 0.5D;
		this.offsetY = (canvas.getHeight() - image.height * ZOOMS[zoomIndex]) * 0.5D;
		return true;
	}

	public void redraw() {
		this.redrawer.run();
	}

	public void doRedraw() {
		HDRImage image = this.openImage.showingLayerProperty.getValue().image;
		Canvas canvas = this.display.display;
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
			int mappedY = (int)(Math.floor((y - this.offsetY) / zoom));
			if (wrap) {
				mappedY = Math.floorMod(mappedY, image.height);
			}
			for (int x = 0; x < width; x++) {
				int mappedX = (int)(Math.floor((x - this.offsetX) / zoom));
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
				float clampedAlpha = Util.clampF(alpha);
				pixels[baseIndex    ] = Util.clampB(blue  * clampedAlpha);
				pixels[baseIndex | 1] = Util.clampB(green * clampedAlpha);
				pixels[baseIndex | 2] = Util.clampB(red   * clampedAlpha);
				pixels[baseIndex | 3] = Util.clampB(               alpha);
			}
		});
		this.drawOutline(
			pixels,
			0,
			0,
			image.width,
			image.height,
			width,
			height,
			0xFF7F7F3F,
			0xFFFFFFBF
		);
		if (this.openImage.getSelectedLayer().sources.getCurrentSource() instanceof ManualLayerSource manual) {
			Selection selection = new Selection();
			Tool<?> tool = manual.toolWithoutColorPicker.get();
			if (tool != null && tool.getSelection(selection)) {
				this.f3.updateSelection(selection);
				this.drawOutline(
					pixels,
					selection.minX,
					selection.minY,
					selection.maxX + 1,
					selection.maxY + 1,
					width,
					height,
					0xFF000000,
					0xFFFFFFFF
				);
			}
			else {
				this.f3.updateSelection(null);
			}
		}
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
	}

	public void drawOutline(byte[] pixels, int x1, int y1, int x2, int y2, int canvasWidth, int canvasHeight, int dark, int light) {
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

	public static void setColorSafe(byte[] pixels, int x, int y, int width, int height, int argb) {
		if (x >= 0 && x < width && y >= 0 && y < height) {
			int baseIndex = (y * width + x) << 2;
			pixels[baseIndex    ] = (byte)(argb);
			pixels[baseIndex | 1] = (byte)(argb >>> 8);
			pixels[baseIndex | 2] = (byte)(argb >>> 16);
			pixels[baseIndex | 3] = (byte)(argb >>> 24);
		}
	}
}