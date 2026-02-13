package builderb0y.bigpixel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.DisplayRenderer.CachedImage;
import builderb0y.bigpixel.util.Util;

public class Thumbnail {

	public final LayerNode layer;
	public final SimpleIntegerProperty targetSize;
	public final IntegerBinding width, height;
	public final ListBinding<WritableImage> frames;
	public final ObjectBinding<WritableImage> currentFrame;

	public Thumbnail(LayerNode layer, int targetSize) {
		this.layer = layer;
		this.targetSize = new SimpleIntegerProperty(this, "targetSize", targetSize);
		this.width = Bindings.createIntegerBinding(
			() -> {
				int width = layer.animation.width();
				int height = layer.animation.height();
				return (
					width >= height
					? this.targetSize.get()
					: Math.ceilDiv(width * this.targetSize.get(), height)
				);
			},
			layer.animation.width,
			layer.animation.height,
			this.targetSize
		);
		this.height = Bindings.createIntegerBinding(
			() -> {
				int width = layer.animation.width();
				int height = layer.animation.height();
				return (
					height >= width
					? this.targetSize.get()
					: Math.ceilDiv(height * this.targetSize.get(), width)
				);
			},
			layer.animation.width,
			layer.animation.height,
			this.targetSize
		);
		this.frames = new ListBinding<WritableImage>() {

			public FutureState futureState = new FutureState();

			{
				this.bind(
					Thumbnail.this.layer.animation.frameCount,
					Thumbnail.this.width,
					Thumbnail.this.height
				);
			}

			public void restartDraw(Function<FutureState, CompletableFuture<Void>> future) {
				this.futureState = FutureState.push(this.futureState, future);
			}

			@Override
			public ObservableList<WritableImage> computeValue() {
				int frameCount = Thumbnail.this.layer.animation.frameCount.get();
				WritableImage[] images = new WritableImage[frameCount];
				int width = Thumbnail.this.width.get();
				int height = Thumbnail.this.height.get();
				for (int index = 0; index < frameCount; index++) {
					images[index] = new WritableImage(width, height);
				}
				if (frameCount == 1) {
					this.restartDraw((FutureState state) -> {
						return Thumbnail.this.draw(state, 0, images[0]);
					});
				}
				else {
					this.restartDraw((FutureState state) -> {
						return CompletableFuture.allOf(
							IntStream.range(0, frameCount).mapToObj((int frameIndex) -> {
								return Thumbnail.this.draw(state, frameIndex, images[frameIndex]);
							})
							.toArray(CompletableFuture[]::new)
						);
					});
				}
				return FXCollections.observableArrayList(images);
			}
		};
		this.currentFrame = this.frames.valueAt(layer.animation.currentFrameIndex);
	}

	public void invalidateAll() {
		this.frames.invalidate();
	}

	public ImageView createView() {
		ImageView view = new ImageView();
		view.imageProperty().bind(this.currentFrame);
		return view;
	}

	public CompletableFuture<Void> draw(FutureState state, int frameIndex, WritableImage fxImage) {
		HDRImage hdr = this.layer.getFrame(frameIndex).asyncCopy();
		int toWidth    = (int)(fxImage.getWidth());
		int toHeight   = (int)(fxImage.getHeight());
		int fromWidth  = hdr.width;
		int fromHeight = hdr.height;
		return CompletableFuture.supplyAsync(
			() -> {
				byte[] packedPixels = new byte[toWidth * toHeight * 4];
				IntStream.range(0, toHeight).parallel().forEach((int toY) -> {
					if (!state.canWork) return;
					for (int toX = 0; toX < toWidth && state.canWork; toX++) {
						double fromMinX = ((double)(toX    )) * ((double)(fromWidth )) / ((double)(toWidth ));
						double fromMinY = ((double)(toY    )) * ((double)(fromHeight)) / ((double)(toHeight));
						double fromMaxX = ((double)(toX + 1)) * ((double)(fromWidth )) / ((double)(toWidth ));
						double fromMaxY = ((double)(toY + 1)) * ((double)(fromHeight)) / ((double)(toHeight));
						FloatVector sum = Util.INVISIBLACK;
						float samples = 0.0F;
						int iterMinX = (int)(Math.floor(fromMinX));
						int iterMinY = (int)(Math.floor(fromMinY));
						int iterMaxX = (int)(Math.ceil (fromMaxX));
						int iterMaxY = (int)(Math.ceil (fromMaxY));
						for (int fromY = iterMinY; fromY < iterMaxY; fromY++) {
							for (int fromX = iterMinX; fromX < iterMaxX; fromX++) {
								FloatVector color = hdr.getColor(fromX, fromY);
								color = color.max(0.0F).min(1.0F);
								color = color.mul(color, Util.RGB_MASK);
								double regionMinX = Math.max(fromX,     fromMinX);
								double regionMinY = Math.max(fromY,     fromMinY);
								double regionMaxX = Math.min(fromX + 1, fromMaxX);
								double regionMaxY = Math.min(fromY + 1, fromMaxY);
								float weight = (float)((regionMaxX - regionMinX) * (regionMaxY - regionMinY));
								sum = sum.add(color.mul(weight));
								samples += weight;
							}
						}
						sum = sum.div(samples).lanewise(VectorOperators.SQRT, Util.RGB_MASK);
						float clampedAlpha = Util.clampF(sum.lane(HDRImage.ALPHA_OFFSET));
						sum = sum.mul(clampedAlpha, Util.RGB_MASK);
						IntVector rounded = ((IntVector)(sum.mul(256.0F).convert(VectorOperators.F2I, 0))).max(0).min(255);
						int baseIndex = (toY * toWidth + toX) << 2;
						packedPixels[baseIndex    ] = (byte)(rounded.lane(HDRImage. BLUE_OFFSET));
						packedPixels[baseIndex | 1] = (byte)(rounded.lane(HDRImage.GREEN_OFFSET));
						packedPixels[baseIndex | 2] = (byte)(rounded.lane(HDRImage.  RED_OFFSET));
						packedPixels[baseIndex | 3] = (byte)(rounded.lane(HDRImage.ALPHA_OFFSET));
					}
				});
				return packedPixels;
			},
			ForkJoinPool.commonPool()
		)
		.handleAsync(
			(byte[] pixels, Throwable problem) -> {
				if (problem != null) {
					problem.printStackTrace();
				}
				else if (state.canWork) {
					PixelWriter writer = fxImage.getPixelWriter();
					if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
						throw new IllegalStateException("Pixel format changed");
					}
					writer.setPixels(0, 0, toWidth, toHeight, PixelFormat.getByteBgraPreInstance(), pixels, 0, toWidth << 2);
				}
				return null;
			},
			Platform::runLater
		);
	}

	public static class FutureState {

		public CompletableFuture<Void> working;
		public volatile boolean canWork = true;

		public static FutureState push(FutureState prev, Function<FutureState, CompletableFuture<Void>> future) {
			assert Platform.isFxApplicationThread();
			if (prev != null) prev.canWork = false;
			FutureState next = new FutureState();
			next.working = future.apply(next);
			return next;
		}
	}
}