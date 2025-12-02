package builderb0y.bigpixel;

import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.tools.SourcelessTool;
import builderb0y.bigpixel.tools.Tool;
import builderb0y.bigpixel.tools.Tool.Selection;
import builderb0y.bigpixel.util.BaseCanvasHelper;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerView.LayerViewType;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;
import builderb0y.bigpixel.views.LayerViews;

public class ZoomableImage extends AnimationView {

	public OpenImage openImage;
	public BaseCanvasHelper display;
	public F3Menu f3;
	public StackPane displayWithF3;
	public double previousMouseX, previousMouseY;
	public @Nullable ProjectionResult previousProjectionResult;
	public Selection previousSelection;
	public ListBinding<HDRImage> visibleAnimation;
	public ObservableValue<DrawParams> drawParams;
	public ChangeListener<Number> centerer;
	public InvalidationListener redrawListener;
	public Runnable redrawer;
	public boolean redrawRequested;

	public ZoomableImage(OpenImage openImage) {
		super(openImage.animationSource);
		this.openImage = openImage;
		this.display = new BaseCanvasHelper().checkerboard().resizeable((BaseCanvasHelper _) -> this.redrawLater());
		this.f3 = new F3Menu();
		this.displayWithF3 = new StackPane(this.display.getRootPane(), this.f3.rootNode());
		this.centerer = Util.change(this::centerOnce);
		this.redrawer = this::redrawImmediately;
		this.redrawListener = (Observable _) -> this.redrawLater();
		this.visibleAnimation = new ListBinding<>() {

			{
				this.bind(openImage.layerGraph.visibleLayerProperty);
			}

			@Override
			public ObservableList<HDRImage> computeValue() {
				return openImage.layerGraph.getVisibleLayer().animation.frames;
			}
		};
		this.drawParams = (
			openImage
			.layerGraph
			.visibleLayerProperty
			.flatMap((LayerNode layer) -> layer.views.selectedValue)
			.flatMap(LayerView::drawParamsProperty)
		);
	}

	public void init() {
		this.display.innerPane. widthProperty().addListener(this.centerer);
		this.display.innerPane.heightProperty().addListener(this.centerer);
		Canvas canvas = this.display.display;
		ObservableValue<HDRImage> imageBeingDisplayed = this.openImage.layerGraph.visibleLayerProperty.flatMap((LayerNode layer) -> layer.animation.currentFrame);
		imageBeingDisplayed.getValue().addListener(this.redrawListener);
		imageBeingDisplayed.addListener(Util.change((HDRImage oldImage, HDRImage newImage) -> {
			oldImage.removeListener(this.redrawListener);
			newImage.addListener(this.redrawListener);
			this.redrawLater();
		}));
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
						LayerView projector = ZoomableImage.this.getView();
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
						LayerView projector = ZoomableImage.this.getView();
						if (projector != null) {
							ProjectionResult projected = projector.project(event.getX(), event.getY());
							if (projected != null && projected.input() instanceof VaryingSamplerProvider varying) {
								int x = projected.x();
								int y = projected.y();
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
				(int)(this.display.display.getWidth()),
				(int)(this.display.display.getHeight())
			);
			this.previousProjectionResult = view.project(mouseX, mouseY);
		}
		else {
			this.previousProjectionResult = null;
		}
	}

	public @Nullable LayerView getView() {
		LayerNode visibleLayer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		return visibleLayer != null ? visibleLayer.views.selectedValue.get() : null;
	}

	public WritableImage getImage() {
		int width = (int)(this.display.display.getWidth());
		int height = (int)(this.display.display.getHeight());
		if (width > 0 && height > 0) return this.getImage(new DrawKey(
			width,
			height,
			this.animationSource.getFrameIndex(),
			this.drawParams.getValue()
		));
		else return null;
	}

	@Override
	public CachedImage createCachedImage(int frame) {
		CachedImage cached = super.createCachedImage(frame);
		ObjectBinding<HDRImage> binding = this.visibleAnimation.valueAt(frame);
		binding.get().addListener(cached);
		binding.addListener(Util.change((HDRImage oldImage, HDRImage newImage) -> {
			oldImage.removeListener(cached);
			newImage.addListener(cached);
		}));
		return cached;
	}

	public void centerOnce() {
		if (this.centerer != null && this.center()) {
			BorderPane canvas = this.display.innerPane;
			canvas. widthProperty().removeListener(this.centerer);
			canvas.heightProperty().removeListener(this.centerer);
			this.centerer = null;
		}
	}

	public boolean center() {
		LayerNode layer = this.openImage.layerGraph.getVisibleLayer();
		if (layer == null) return false;
		Canvas canvas = this.display.display;
		int canvasWidth = (int)(canvas.getWidth());
		int canvasHeight = (int)(canvas.getHeight());
		if (canvasWidth == 0 || canvasHeight == 0) return false;
		int layerWidth = layer.imageWidth();
		int layerHeight = layer.imageHeight();
		if (this.centerer != null) {
			LayerViews views = layer.views;
			for (LayerViewType type : LayerViewType.VALUES) {
				LayerView projector = views.getOrCreateValue(type);
				projector.beforeRedraw(layerWidth, layerHeight, canvasWidth, canvasHeight);
				projector.center();
			}
		}
		else {
			LayerView projector = this.getView();
			if (projector != null) {
				projector.beforeRedraw(layerWidth, layerHeight, canvasWidth, canvasHeight);
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

	@Override
	public void invalidateAll() {
		super.invalidateAll();
		this.redrawLater();
	}

	@Override
	public void invalidate(int frame) {
		super.invalidate(frame);
		if (this.animationSource.getFrameIndex() == frame) {
			this.redrawLater();
		}
	}

	@Override
	public void draw(DrawKey key, WritableImage image) {
		int width = key.width();
		int height = key.height();
		PixelWriter writer = image.getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		LayerNode visibleLayer = this.openImage.layerGraph.visibleLayerProperty.getValue();
		if (visibleLayer == null) {
			byte[] row = new byte[width * 4];
			writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), row, 0, 0);
			return;
		}
		int imageWidth = visibleLayer.imageWidth();
		int imageHeight = visibleLayer.imageHeight();
		byte[] pixels = new byte[width * height * 4];
		LayerView projector = this.getView();
		if (projector != null) {
			projector.beforeRedraw(imageWidth, imageHeight, width, height);
			IntStream.range(0, height).parallel().forEach((int y) -> {
				for (int x = 0; x < width; x++) {
					float r = 0.0F, g = 0.0F, b = 0.0F, a = 0.0F;
					ProjectionResult mapped = projector.project(x, y);
					if (mapped != null) {
						r = mapped.r();
						g = mapped.g();
						b = mapped.b();
						a = mapped.a();
					}
					int baseIndex = (y * width + x) << 2;
					float clampedAlpha = Util.clampF(a);
					r *= clampedAlpha;
					g *= clampedAlpha;
					b *= clampedAlpha;
					pixels[baseIndex    ] = Util.clampB(b);
					pixels[baseIndex | 1] = Util.clampB(g);
					pixels[baseIndex | 2] = Util.clampB(r);
					pixels[baseIndex | 3] = Util.clampB(a);
				}
			});
			if (projector.drawOutline.isSelected()) {
				projector.drawLayerOutline(
					pixels,
					imageWidth,
					imageHeight,
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
						imageWidth,
						imageHeight,
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
	}

	public void redrawImmediately() {
		this.redrawRequested = false;
		WritableImage image = this.getImage();
		if (image == null) return;
		this.display.blit(image);
	}

	public void updateF3(F3Menu f3) {
		this.updateProjectionResult(this.previousMouseX, this.previousMouseY);
		f3.add("Hover: " + this.previousProjectionResult);
		f3.add("Selection: " + this.previousSelection);
		LayerView projector = this.getView();
		if (projector != null) projector.updateF3(f3);
	}
}