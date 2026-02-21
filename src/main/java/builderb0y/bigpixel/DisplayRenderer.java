package builderb0y.bigpixel;

import java.util.function.Function;
import java.util.stream.IntStream;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.tools.Tool;
import builderb0y.bigpixel.tools.Tool.Selection;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;

public class DisplayRenderer {

	public final ZoomableImage zoomableImage;
	public final ObservableValue<LayerNode> visibleLayer, selectedLayer;
	public final ObservableIntegerValue width, height;
	public final ListBinding<CachedImage> frames;
	public final ObservableValue<WritableImage> currentFrame;
	public @Nullable Selection previousSelection;

	public DisplayRenderer(ZoomableImage display) {
		this.zoomableImage = display;
		this.visibleLayer = display.openImage.layerGraph.visibleLayerProperty;
		this.selectedLayer = display.openImage.layerGraph.selectedLayer;
		this.width = Util.toInt(display.canvasHolder.display.widthProperty(), 0);
		this.height = Util.toInt(display.canvasHolder.display.heightProperty(), 0);
		ObservableValue<Number> frameCountValue = DisplayRenderer.this.visibleLayer.flatMap((LayerNode layer) -> layer.animation.frameCount).orElse(1);
		this.frames = new ListBinding<>() {

			{
				this.bind(
					DisplayRenderer.this.visibleLayer,
					frameCountValue,
					DisplayRenderer.this.selectedLayer,
					DisplayRenderer.this.width,
					DisplayRenderer.this.height
				);
			}

			@Override
			public ObservableList<CachedImage> computeValue() {
				int frameCount = frameCountValue.getValue().intValue();
				CachedImage[] images = new CachedImage[frameCount];
				int width = DisplayRenderer.this.width.get();
				int height = DisplayRenderer.this.height.get();
				for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
					images[frameIndex] = new CachedImage(width > 0 && height > 0 ? new WritableImage(width, height) : null, frameIndex);
				}
				return FXCollections.observableArrayList(images);
			}
		};
		this.currentFrame = this.frames.valueAt(Util.toInt(this.visibleLayer.flatMap((LayerNode layer) -> layer.animation.currentFrameIndex), 0)).flatMap(Function.identity());
	}

	public void invalidateAll() {
		if (this.frames.isValid()) {
			this.frames.forEach(CachedImage::invalidate);
		}
	}

	public void draw(int frameIndex, WritableImage image) {
		int width = (int)(image.getWidth());
		int height = (int)(image.getHeight());
		PixelWriter writer = image.getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		LayerNode visibleLayer = this.visibleLayer.getValue();
		int layerWidth = visibleLayer.imageWidth();
		int layerHeight = visibleLayer.imageHeight();
		byte[] pixels = new byte[width * height * 4];
		LayerView view = visibleLayer.views.currentView();
		if (view != null) {
			view.beforeRedraw(layerWidth, layerHeight, width, height);
			IntStream.range(0, height).parallel().forEach((int y) -> {
				for (int x = 0; x < width; x++) {
					float r = 0.0F, g = 0.0F, b = 0.0F, a = 0.0F;
					ProjectionResult mapped = view.project(x, y, frameIndex);
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
			if (view.drawOutline.isSelected()) {
				view.drawLayerOutline(
					pixels,
					layerWidth,
					layerHeight,
					width,
					height
				);
			}
			LayerNode selectedLayer = this.selectedLayer.getValue();
			if (selectedLayer != null && selectedLayer.sources.selectedValue.get() instanceof ManualLayerSource manual) {
				Selection selection = new Selection();
				Tool<?> tool = manual.toolWithoutColorPicker.get();
				if (tool != null && tool.getSelection(selection)) {
					this.previousSelection = selection;
					view.drawSelectionOutline(
						visibleLayer,
						pixels,
						selection.minX,
						selection.minY,
						selection.maxX + 1,
						selection.maxY + 1,
						layerWidth,
						layerHeight,
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

	public class CachedImage extends ObjectBinding<WritableImage> {

		public final WritableImage image;
		public final int frameIndex;

		public CachedImage(WritableImage image, int frameIndex) {
			this.image = image;
			this.frameIndex = frameIndex;
		}

		@Override
		public WritableImage computeValue() {
			if (this.image != null) DisplayRenderer.this.draw(this.frameIndex, this.image);
			return this.image;
		}
	}
}