package builderb0y.bigpixel;

import java.util.EnumMap;
import java.util.stream.IntStream;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.projectors.ImageProjector;
import builderb0y.bigpixel.projectors.ImageProjector.Texcoord;
import builderb0y.bigpixel.RateLimiter.PeriodicRateLimiter;
import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.tools.SourcelessTool;
import builderb0y.bigpixel.tools.Tool;
import builderb0y.bigpixel.tools.Tool.Selection;

import static builderb0y.bigpixel.HDRImage.*;

public class ZoomableImage {

	public OpenImage openImage;
	public CanvasHelper display;
	public F3Menu f3;
	public StackPane displayWithF3;
	public SimpleObjectProperty<ImageProjector.Type> currentProjectorType;
	public EnumMap<ImageProjector.Type, ImageProjector> projectors;
	public double lastMouseX, lastMouseY;
	public ChangeListener<Number> centerer;
	public RateLimiter redrawer;

	public ZoomableImage(OpenImage openImage) {
		this.openImage = openImage;
		this.display = new CanvasHelper().checkerboard().resizeable((CanvasHelper _) -> this.redraw());
		this.f3 = new F3Menu();
		this.displayWithF3 = new StackPane(this.display.getRootPane(), this.f3.rootNode());
		this.centerer = Util.change(this::centerOnce);
		this.redrawer = new PeriodicRateLimiter(20L, this::doRedraw);
		this.currentProjectorType = new SimpleObjectProperty<>(
			this,
			"currentProjectorType",
			ImageProjector.Type.FLAT_CLAMPED
		);
		this.projectors = new EnumMap<>(ImageProjector.Type.class);
		for (ImageProjector.Type type : ImageProjector.Type.VALUES) {
			this.projectors.put(type, type.constructor.apply(this));
		}
	}

	public void init() {
		this.display.display. widthProperty().addListener(this.centerer);
		this.display.display.heightProperty().addListener(this.centerer);
		Canvas canvas = this.display.display;
		ChangeListener<Object> redrawer = Util.change(this::redraw);
		this.currentProjectorType.addListener(redrawer);
		this.openImage.layerGraph.visibleLayerProperty.addListener(redrawer);
		canvas.setOnScroll((ScrollEvent event) -> {
			if (event.getDeltaY() > 0.0D) {
				this.getProjector().zoom(event.getX(), event.getY(), true);
			}
			else if (event.getDeltaY() < 0.0D) {
				this.getProjector().zoom(event.getX(), event.getY(), false);
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
						ZoomableImage.this.getProjector().drag(
							event.getX() - this.pressX,
							event.getY() - this.pressY
						);
						this.pressX = event.getX();
						this.pressY = event.getY();
					}
					else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
						ZoomableImage.this.redraw();
						canvas.setCursor(null);
					}
				}
				else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
					SourcelessTool<?> tool = ZoomableImage.this.openImage.toolWithColorPicker.get();
					if (tool != null) {
						Texcoord projected = ZoomableImage.this.getProjector().project(event.getX(), event.getY());
						if (projected != null) {
							int x = projected.x;
							int y = projected.y;
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
			}
		};
		canvas.setOnMousePressed(handler);
		canvas.setOnMouseDragged(handler);
		canvas.setOnMouseReleased(handler);
		canvas.setOnMouseMoved((MouseEvent event) -> {
			Texcoord projected = this.getProjector().project(
				this.lastMouseX = event.getX(),
				this.lastMouseY = event.getY()
			);
			if (projected != null) {
				this.f3.updatePos(projected.x, projected.y);
			}
			else {
				this.f3.clearHoverPos();
			}
		});
		canvas.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.F3) {
				Pane pane = this.f3.rootNode();
				pane.setVisible(!pane.isVisible());
			}
		});
	}

	public ImageProjector getProjector() {
		return this.projectors.get(this.currentProjectorType.get());
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
		LayerNode layer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		if (layer == null) return false;
		if (this.centerer != null) {
			for (ImageProjector.Type type : ImageProjector.Type.VALUES) {
				ImageProjector projector = this.projectors.get(type);
				projector.beforeRedraw(canvas, layer);
				projector.center();
			}
		}
		else {
			ImageProjector projector = this.getProjector();
			projector.beforeRedraw(canvas, layer);
			projector.center();
		}
		this.redraw();
		return true;
	}

	public void redraw() {
		this.redrawer.run();
	}

	public void doRedraw() {
		Canvas canvas = this.display.display;
		int width = (int)(canvas.getWidth());
		int height = (int)(canvas.getHeight());
		PixelWriter writer = canvas.getGraphicsContext2D().getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		LayerNode visibleLayer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		if (visibleLayer == null) {
			byte[] row = new byte[width * 4];
			writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), row, 0, 0);
			return;
		}
		HDRImage image = visibleLayer.image;
		ImageProjector projector = this.getProjector();
		projector.beforeRedraw(canvas, visibleLayer);
		byte[] pixels = this.display.pixels;
		IntStream.range(0, height).parallel().forEach((int y) -> {
			for (int x = 0; x < width; x++) {
				FloatVector color = Util.INVISIBLACK;
				Texcoord mapped = projector.project(x, y);
				if (mapped != null) {
					color = mapped.sample(image);
				}
				int baseIndex = (y * width + x) << 2;
				float clampedAlpha = Util.clampF(color.lane(ALPHA_OFFSET));
				FloatVector premultiplied = color.mul(clampedAlpha);
				pixels[baseIndex    ] = Util.clampB(premultiplied.lane( BLUE_OFFSET));
				pixels[baseIndex | 1] = Util.clampB(premultiplied.lane(GREEN_OFFSET));
				pixels[baseIndex | 2] = Util.clampB(premultiplied.lane(  RED_OFFSET));
				pixels[baseIndex | 3] = Util.clampB(        color.lane(ALPHA_OFFSET));
			}
		});
		projector.drawOutline(
			pixels,
			0,
			0,
			image.width,
			image.height,
			image.width,
			image.height,
			width,
			height,
			0xFF7F7F3F,
			0xFFFFFFBF
		);
		LayerNode selectedLayer = this.openImage.layerGraph.selectedLayer.get();
		if (selectedLayer != null && selectedLayer.sources.getCurrentSource() instanceof ManualLayerSource manual) {
			Selection selection = new Selection();
			Tool<?> tool = manual.toolWithoutColorPicker.get();
			if (tool != null && tool.getSelection(selection)) {
				this.f3.updateSelection(selection);
				projector.drawOutline(
					pixels,
					selection.minX,
					selection.minY,
					selection.maxX + 1,
					selection.maxY + 1,
					image.width,
					image.height,
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
}