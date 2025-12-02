package builderb0y.bigpixel;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.util.Util;

public class Thumbnail extends AnimationView {

	public final HDRAnimation hdrAnimation;
	public final SimpleIntegerProperty targetSize;
	public final IntegerBinding width, height;
	public final ObjectBinding<WritableImage> frame;

	public Thumbnail(AnimationSource animationSource, HDRAnimation hdrAnimation, int targetSize) {
		super(animationSource);
		this.hdrAnimation = hdrAnimation;
		this.targetSize = new SimpleIntegerProperty(this, "targetSize", targetSize);
		this.width = Bindings.createIntegerBinding(
			() -> {
				int width = hdrAnimation.width();
				int height = hdrAnimation.height();
				return (
					width >= height
					? this.targetSize.get()
					: Math.ceilDiv(width * this.targetSize.get(), height)
				);
			},
			hdrAnimation.width,
			hdrAnimation.height,
			this.targetSize
		);
		this.height = Bindings.createIntegerBinding(
			() -> {
				int width = hdrAnimation.width();
				int height = hdrAnimation.height();
				return (
					height >= width
					? this.targetSize.get()
					: Math.ceilDiv(height * this.targetSize.get(), width)
				);
			},
			hdrAnimation.width,
			hdrAnimation.height,
			this.targetSize
		);
		this.frame = Bindings.createObjectBinding(
			() -> this.getImage(this.hdrAnimation.getFrameIndex()),
			this.hdrAnimation.currentFrameIndex,
			this.width,
			this.height
		);
	}

	@Override
	public CachedImage createCachedImage(int frame) {
		CachedImage cached = super.createCachedImage(frame);
		this.hdrAnimation.getFrame(frame).addListener(cached);
		return cached;
	}

	public WritableImage getImage() {
		return this.frame.get();
	}

	public WritableImage getImage(int frame) {
		record EmptyParams() implements DrawParams {

			public static final EmptyParams INSTANCE = new EmptyParams();
		}
		return this.getImage(new DrawKey(this.width.get(), this.height.get(), frame, EmptyParams.INSTANCE));
	}

	public ImageView createView() {
		ImageView view = new ImageView();
		view.imageProperty().bind(this.frame);
		return view;
	}

	@Override
	public void draw(DrawKey key, WritableImage fxImage) {
		HDRImage hdr = this.hdrAnimation.getFrame(key.frame());
		int toWidth    = (int)(fxImage.getWidth());
		int toHeight   = (int)(fxImage.getHeight());
		int fromWidth  = hdr.width;
		int fromHeight = hdr.height;
		byte[] packedPixels = new byte[toWidth * toHeight * 4];
		for (int toY = 0; toY < toHeight; toY++) {
			for (int toX = 0; toX < toWidth; toX++) {
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
		}
		PixelWriter writer = fxImage.getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		writer.setPixels(0, 0, toWidth, toHeight, PixelFormat.getByteBgraPreInstance(), packedPixels, 0, toWidth << 2);
	}
}