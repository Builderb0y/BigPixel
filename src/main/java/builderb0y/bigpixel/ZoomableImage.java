package builderb0y.bigpixel;

import java.util.stream.IntStream;

import javafx.application.Platform;
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
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;
import builderb0y.bigpixel.tools.SourcelessTool;
import builderb0y.bigpixel.tools.Tool;
import builderb0y.bigpixel.tools.Tool.Selection;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;
import builderb0y.bigpixel.views.LayerViews;

import static builderb0y.bigpixel.HDRImage.*;

public class ZoomableImage {

	public OpenImage openImage;
	public CanvasHelper display;
	public F3Menu f3;
	public StackPane displayWithF3;
	public @Nullable ProjectionResult previousProjectionResult;
	public Selection previousSelection;
	public ChangeListener<Number> centerer;
	public Runnable redrawer = this::redrawImmediately;
	public boolean redrawRequested;

	public ZoomableImage(OpenImage openImage) {
		this.openImage = openImage;
		this.display = new CanvasHelper().checkerboard().resizeable((CanvasHelper _) -> this.redrawLater());
		this.f3 = new F3Menu();
		this.displayWithF3 = new StackPane(this.display.getRootPane(), this.f3.rootNode());
		this.centerer = Util.change(this::centerOnce);
	}

	public void init() {
		this.display.display. widthProperty().addListener(this.centerer);
		this.display.display.heightProperty().addListener(this.centerer);
		Canvas canvas = this.display.display;
		this.openImage.layerGraph.visibleLayerProperty.addListener(Util.change(this::redrawLater));
		canvas.setOnScroll((ScrollEvent event) -> {
			LayerView projector = this.getProjector();
			if (projector != null) {
				if (event.getDeltaY() > 0.0D) {
					projector.zoom(event.getX(), event.getY(), true);
				}
				else if (event.getDeltaY() < 0.0D) {
					projector.zoom(event.getX(), event.getY(), false);
				}
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
						LayerView projector = ZoomableImage.this.getProjector();
						if (projector != null) projector.drag(
							event.getX() - this.pressX,
							event.getY() - this.pressY
						);
						this.pressX = event.getX();
						this.pressY = event.getY();
					}
					else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
						ZoomableImage.this.redrawLater();
						canvas.setCursor(null);
					}
				}
				else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
					SourcelessTool<?> tool = ZoomableImage.this.openImage.toolWithColorPicker.get();
					if (tool != null) {
						LayerView projector = ZoomableImage.this.getProjector();
						if (projector != null) {
							ProjectionResult projected = projector.project(event.getX(), event.getY());
							if (projected != null && projected.layer instanceof VaryingLayerSourceInput varying) {
								int x = projected.x;
								int y = projected.y;
								if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
									this.toolX = x;
									this.toolY = y;
									tool.mouseDown(x, y, varying.getBackingLayer(), event.getButton());
								}
								else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
									if (this.toolX != x || this.toolY != y) {
										this.toolX = x;
										this.toolY = y;
										tool.mouseDragged(x, y, varying.getBackingLayer(), event.getButton());
									}
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
			LayerView projector = this.getProjector();
			this.previousProjectionResult = (
				projector != null ? projector.project(event.getX(), event.getY()) : null
			);
		});
		canvas.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.F3) {
				Pane pane = this.f3.rootNode();
				pane.setVisible(!pane.isVisible());
			}
		});
	}

	public @Nullable LayerView getProjector() {
		LayerNode visibleLayer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		return visibleLayer != null ? visibleLayer.views.selectedValue.get() : null;
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
			LayerViews views = layer.views;
			for (LayerView.Type type : LayerView.Type.VALUES) {
				LayerView projector = views.getOrCreateValue(type);
				projector.beforeRedraw(canvas, layer);
				projector.center();
			}
		}
		else {
			LayerView projector = this.getProjector();
			if (projector != null) {
				projector.beforeRedraw(canvas, layer);
				projector.center();
			}
		}
		this.redrawLater();
		return true;
	}

	public void redrawLater() {
		if (!this.redrawRequested) {
			this.redrawRequested = true;
			Platform.runLater(this.redrawer);
		}
	}

	public void redrawImmediately() {
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
		byte[] pixels = this.display.pixels;
		LayerView projector = this.getProjector();
		if (projector != null) {
			projector.beforeRedraw(canvas, visibleLayer);
			IntStream.range(0, height).parallel().forEach((int y) -> {
				for (int x = 0; x < width; x++) {
					FloatVector color = Util.INVISIBLACK;
					ProjectionResult mapped = projector.project(x, y);
					if (mapped != null) {
						color = mapped.sample();
					}
					int baseIndex = (y * width + x) << 2;
					float clampedAlpha = Util.clampF(color.lane(ALPHA_OFFSET));
					FloatVector premultiplied = color.mul(clampedAlpha);
					pixels[baseIndex] = Util.clampB(premultiplied.lane(BLUE_OFFSET));
					pixels[baseIndex | 1] = Util.clampB(premultiplied.lane(GREEN_OFFSET));
					pixels[baseIndex | 2] = Util.clampB(premultiplied.lane(RED_OFFSET));
					pixels[baseIndex | 3] = Util.clampB(color.lane(ALPHA_OFFSET));
				}
			});
			if (projector.drawOutline.isSelected()) {
				projector.drawLayerOutline(
					pixels,
					image.width,
					image.height,
					width,
					height
				);
			}
			LayerNode selectedLayer = this.openImage.layerGraph.selectedLayer.get();
			if (selectedLayer != null && selectedLayer.sources.selectedValue.get() instanceof ManualLayerSource manual) {
				Selection selection = new Selection();
				Tool<?> tool = manual.toolWithoutColorPicker.get();
				if (tool != null && tool.getSelection(selection)) {
					this.previousSelection = selection;
					projector.drawSelectionOutline(
						visibleLayer,
						pixels,
						selection.minX,
						selection.minY,
						selection.maxX + 1,
						selection.maxY + 1,
						image.width,
						image.height,
						width,
						height
					);
				}
				else {
					this.previousSelection = null;
				}
			}
			else {
				this.previousSelection = null;
			}
		}
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
		this.redrawRequested = false;
	}

	public void updateF3(F3Menu f3) {
		f3.add("Hover: " + this.previousProjectionResult);
		f3.add("Selection: " + this.previousSelection);
		LayerView projector = this.getProjector();
		if (projector != null) projector.updateF3(f3);
	}
}