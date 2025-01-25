package builderb0y.notgimp;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import javafx.beans.value.ObservableValueBase;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class HDRImage {

	public static final int
		RED_OFFSET   = 0,
		GREEN_OFFSET = 1,
		BLUE_OFFSET  = 2,
		ALPHA_OFFSET = 3;

	public int width, height;
	public float[] pixels;
	public HDRImageValue value;

	public HDRImage(int width, int height) {
		this.width = width;
		this.height = height;
		this.pixels = new float[width * height * 4];
		this.value = this.new HDRImageValue();
	}

	public HDRImage(Image image) {
		this((int)(image.getWidth()), (int)(image.getHeight()));
		PixelReader reader = image.getPixelReader();
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				Color color = reader.getColor(x, y);
				this.setRgba(x, y, (float)(color.getRed()), (float)(color.getGreen()), (float)(color.getBlue()), (float)(color.getOpacity()));
			}
		}
	}

	public HDRImage(HDRImage from) {
		this.width = from.width;
		this.height = from.height;
		this.pixels = from.pixels.clone();
		this.value = this.new HDRImageValue();
	}

	public void markDirty() {
		this.value.fireValueChangedEvent();
	}

	public int baseIndex(int x, int y) {
		return (y * this.width + x) << 2;
	}

	public int redIndex  (int x, int y) { return this.baseIndex(x, y) |   RED_OFFSET; }
	public int greenIndex(int x, int y) { return this.baseIndex(x, y) | GREEN_OFFSET; }
	public int blueIndex (int x, int y) { return this.baseIndex(x, y) |  BLUE_OFFSET; }
	public int alphaIndex(int x, int y) { return this.baseIndex(x, y) | ALPHA_OFFSET; }

	public float getRed  (int x, int y) { return this.pixels[this.  redIndex(x, y)]; }
	public float getGreen(int x, int y) { return this.pixels[this.greenIndex(x, y)]; }
	public float getBlue (int x, int y) { return this.pixels[this. blueIndex(x, y)]; }
	public float getAlpha(int x, int y) { return this.pixels[this.alphaIndex(x, y)]; }

	public void setRed  (int x, int y, float red  ) { this.pixels[this.  redIndex(x, y)] = red  ; }
	public void setGreen(int x, int y, float green) { this.pixels[this.greenIndex(x, y)] = green; }
	public void setBlue (int x, int y, float blue ) { this.pixels[this. blueIndex(x, y)] = blue ; }
	public void setAlpha(int x, int y, float alpha) { this.pixels[this.alphaIndex(x, y)] = alpha; }

	public int getPackedArgb(int x, int y) {
		int baseIndex = this.baseIndex(x, y);
		float red   = this.pixels[baseIndex |   RED_OFFSET];
		float green = this.pixels[baseIndex | GREEN_OFFSET];
		float blue  = this.pixels[baseIndex |  BLUE_OFFSET];
		float alpha = this.pixels[baseIndex | ALPHA_OFFSET];
		return packRgbaToArgb(red, green, blue, alpha);
	}

	public void setRgba(int x, int y, float red, float green, float blue, float alpha) {
		int baseIndex = this.baseIndex(x, y);
		this.pixels[baseIndex |   RED_OFFSET] = red;
		this.pixels[baseIndex | GREEN_OFFSET] = green;
		this.pixels[baseIndex |  BLUE_OFFSET] = blue;
		this.pixels[baseIndex | ALPHA_OFFSET] = alpha;
	}

	public void setGrayscale(int x, int y, float brightness, float alpha) {
		this.setRgba(x, y, brightness, brightness, brightness, alpha);
	}

	public void setColor(int x, int y, Color color) {
		this.setRgba(x, y, (float)(color.getRed()), (float)(color.getGreen()), (float)(color.getBlue()), (float)(color.getOpacity()));
	}

	public void setColor(int x, int y, ColorHelper color) {
		this.setRgba(x, y, color.red.get(), color.green.get(), color.blue.get(), color.alpha.get());
	}

	public static int clamp(float value) {
		int clamped = (int)(value * 256.0F);
		if (clamped <=   0) return   0;
		if (clamped >= 255) return 255;
		return clamped;
	}

	public static int packRgbaToArgb(float red, float green, float blue, float alpha) {
		return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
	}

	public Image toJfxImage() {
		WritableImage image = new WritableImage(this.width, this.height);
		byte[] pixels = new byte[this.width * this.height * 4];
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				int baseIndex = this.baseIndex(x, y);
				float red   = this.pixels[baseIndex |   RED_OFFSET];
				float green = this.pixels[baseIndex | GREEN_OFFSET];
				float blue  = this.pixels[baseIndex |  BLUE_OFFSET];
				float alpha = this.pixels[baseIndex | ALPHA_OFFSET];
				pixels[baseIndex    ] = (byte)(clamp(blue  * alpha));
				pixels[baseIndex | 1] = (byte)(clamp(green * alpha));
				pixels[baseIndex | 2] = (byte)(clamp(red   * alpha));
				pixels[baseIndex | 3] = (byte)(clamp(        alpha));
			}
		}
		image.getPixelWriter().setPixels(0, 0, this.width, this.height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
		return image;
	}

	public BufferedImage toAwt(SaveProgress progress) {
		BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = image.getRaster();
		int[] pixel = new int[1];
		for (int y = 0; y < this.height; y++) {
			if (progress.isCanceled()) return null;
			for (int x = 0; x < this.height; x++) {
				pixel[0] = this.getPackedArgb(x, y);
				raster.setDataElements(x, y, pixel);
			}
		}
		return image;
	}

	public byte[] toPngByteArray(SaveProgress progress) {
		BufferedImage image = this.toAwt(progress);
		if (image == null) return null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
			Iterator<ImageWriter> iterator = ImageIO.getImageWriters(type, "png");
			ImageWriter writer = iterator.next();
			progress.setWriter(writer);
			ImageOutputStream imageStream = ImageIO.createImageOutputStream(stream);
			writer.setOutput(imageStream);
			writer.write(image);
			writer.dispose();
			imageStream.flush();
			return progress.isCanceled() ? null : stream.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static class SaveProgress {

		public boolean canceled;
		public ImageWriter writer;

		public synchronized void cancel() {
			this.canceled = true;
			if (this.writer != null) this.writer.abort();
		}

		public synchronized void setWriter(ImageWriter writer) {
			this.writer = writer;
			if (this.canceled) writer.abort();
		}

		public synchronized boolean isCanceled() {
			return this.canceled;
		}
	}

	public class HDRImageValue extends ObservableValueBase<HDRImage> {

		@Override
		public HDRImage getValue() {
			return HDRImage.this;
		}

		@Override
		public void fireValueChangedEvent() {
			super.fireValueChangedEvent();
		}
	}
}