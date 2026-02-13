package builderb0y.bigpixel;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.tools.SourcelessTool;
import builderb0y.bigpixel.util.BaseCanvasHelper;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerView.LayerViewType;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;
import builderb0y.bigpixel.views.LayerViews;

public class ZoomableImage {

	public OpenImage openImage;
	public BaseCanvasHelper canvasHolder;
	public F3Menu f3;
	public StackPane displayWithF3;
	public double previousMouseX, previousMouseY;
	public @Nullable ProjectionResult previousProjectionResult;
	public ChangeListener<Number> centerer;
	public DisplayRenderer displayRenderer;
	public ObservableValue<DrawParams> drawParams;
	public Runnable redrawer;
	public boolean redrawRequested;

	public ZoomableImage(OpenImage openImage) {
		this.openImage = openImage;
		this.canvasHolder = new BaseCanvasHelper() {

			@Override
			public void redraw() {
				ZoomableImage.this.redrawLater();
			}
		}
		.checkerboard()
		.resizeable();
		this.f3 = new F3Menu();
		this.displayWithF3 = new StackPane(this.canvasHolder.getRootPane(), this.f3.rootNode());
		this.centerer = Util.change(this::centerOnce);
		this.displayRenderer = new DisplayRenderer(this);
		this.redrawer = this::redrawImmediately;
		this.drawParams = (
			openImage
			.layerGraph
			.visibleLayerProperty
			.flatMap((LayerNode layer) -> layer.views.selectedValue)
			.flatMap(LayerView::drawParamsProperty)
		);
		this.drawParams.addListener(Util.change(this.displayRenderer::invalidateAll));
		this.displayRenderer.currentFrame.addListener((Observable _) -> this.redrawLater());
	}

	public void init() {
		this.canvasHolder.innerPane. widthProperty().addListener(this.centerer);
		this.canvasHolder.innerPane.heightProperty().addListener(this.centerer);
		Canvas canvas = this.canvasHolder.display;
		canvas.setOnScroll((ScrollEvent event) -> {
			LayerView projector = this.getView();
			if (projector != null) {
				if (event.getDeltaY() > 0.0D) {
					projector.zoom(event.getX(), event.getY(), true);
				}
				else if (event.getDeltaY() < 0.0D) {
					projector.zoom(event.getX(), event.getY(), false);
				}
			}
		});
		this.canvasHolder.getRootPane().cursorProperty().bind(this.openImage.cursorProperty);
		EventHandler<MouseEvent> handler = new EventHandler<>() {

			public double pressX, pressY;
			public int toolX, toolY;

			@Override
			public void handle(MouseEvent event) {
				Canvas canvas = ZoomableImage.this.canvasHolder.display;
				canvas.requestFocus();
				if (event.getButton() == MouseButton.MIDDLE) {
					if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
						this.pressX = event.getX();
						this.pressY = event.getY();
						canvas.setCursor(Cursor.CROSSHAIR);
					}
					else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
						LayerView projector = ZoomableImage.this.getView();
						if (projector != null) projector.drag(
							event.getX() - this.pressX,
							event.getY() - this.pressY
						);
						this.pressX = event.getX();
						this.pressY = event.getY();
					}
					else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
						//ZoomableImage.this.redrawLater();
						canvas.setCursor(null);
					}
				}
				else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
					SourcelessTool<?> tool = ZoomableImage.this.openImage.toolWithColorPicker.get();
					if (tool != null) {
						LayerView projector = ZoomableImage.this.getView();
						if (projector != null) {
							ProjectionResult projected = projector.project(event.getX(), event.getY(), 0);
							if (projected != null) {
								int x = projected.x();
								int y = projected.y();
								if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
									this.toolX = x;
									this.toolY = y;
									tool.mouseDown(projected, event.getButton());
								}
								else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
									if (this.toolX != x || this.toolY != y) {
										this.toolX = x;
										this.toolY = y;
										tool.mouseDragged(projected, event.getButton());
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
			this.updateProjectionResult(
				this.previousMouseX = event.getX(),
				this.previousMouseY = event.getY()
			);
		});
		canvas.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.F3) {
				Pane pane = this.f3.rootNode();
				pane.setVisible(!pane.isVisible());
			}
		});
	}

	public void updateProjectionResult(double mouseX, double mouseY) {
		LayerView view = this.getView();
		if (view != null) {
			LayerNode layer = this.openImage.layerGraph.getVisibleLayer();
			view.beforeRedraw(
				layer.imageWidth(),
				layer.imageHeight(),
				(int)(this.canvasHolder.display.getWidth()),
				(int)(this.canvasHolder.display.getHeight())
			);
			this.previousProjectionResult = view.project(mouseX, mouseY, layer.animation.getFrameIndex());
		}
		else {
			this.previousProjectionResult = null;
		}
	}

	public @Nullable LayerView getView() {
		LayerNode visibleLayer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		return visibleLayer != null ? visibleLayer.views.selectedValue.get() : null;
	}

	public void centerOnce() {
		if (this.centerer != null && this.center()) {
			BorderPane canvas = this.canvasHolder.innerPane;
			canvas. widthProperty().removeListener(this.centerer);
			canvas.heightProperty().removeListener(this.centerer);
			this.centerer = null;
		}
	}

	public boolean center() {
		Canvas canvas = this.canvasHolder.display;
		int canvasWidth = (int)(canvas.getWidth());
		int canvasHeight = (int)(canvas.getHeight());
		if (canvasWidth == 0 || canvasHeight == 0) return false;
		if (this.centerer != null) {
			for (LayerNode layer : this.openImage.layerGraph.layerList) {
				LayerViews views = layer.views;
				int layerWidth = layer.imageWidth();
				int layerHeight = layer.imageHeight();
				for (LayerViewType type : LayerViewType.VALUES) {
					LayerView projector = views.getOrCreateValue(type);
					projector.beforeRedraw(layerWidth, layerHeight, canvasWidth, canvasHeight);
					projector.center();
				}
			}
		}
		else {
			LayerNode layer = this.openImage.layerGraph.getVisibleLayer();
			if (layer == null) return false;
			int layerWidth = layer.imageWidth();
			int layerHeight = layer.imageHeight();
			LayerView projector = this.getView();
			if (projector != null) {
				projector.beforeRedraw(layerWidth, layerHeight, canvasWidth, canvasHeight);
				projector.center();
			}
		}
		//this.redrawLater();
		return true;
	}

	public void redrawLater() {
		if (!this.redrawRequested) {
			this.redrawRequested = true;
			Platform.runLater(this.redrawer);
		}
	}

	public void redrawImmediately() {
		this.redrawRequested = false;
		WritableImage image = this.displayRenderer.currentFrame.getValue();
		this.canvasHolder.blit(image);
	}

	public void updateF3(F3Menu f3) {
		this.updateProjectionResult(this.previousMouseX, this.previousMouseY);
		f3.add("Hover: " + this.previousProjectionResult);
		f3.add("Selection: " + this.displayRenderer.previousSelection);
		LayerView projector = this.getView();
		if (projector != null) projector.updateF3(f3);
	}

	public static interface DrawParams {}
}