package builderb0y.notgimp;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.json.JsonMap;

public class HDRImage {

	public static final int
		RED_OFFSET   = 0,
		GREEN_OFFSET = 1,
		BLUE_OFFSET  = 2,
		ALPHA_OFFSET = 3;

	public int width, height;
	public float[] pixels;

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("width", this.width);
		map.add("height", this.height);
		try {
			map.add("pixels", Base64.getEncoder().encodeToString(this.compressPixels()));
		}
		catch (IOException exception) {
			throw new SaveException(exception);
		}
		return map;
	}

	public HDRImage(JsonMap saveData) {
		this(saveData.getInt("width"), saveData.getInt("height"));
		try {
			String base64Pixels = saveData.getString("pixels");
			byte[] compressedPixels = Base64.getDecoder().decode(base64Pixels);
			this.decompressPixels(compressedPixels);
		}
		catch (IOException exception) {
			throw new SaveException(exception);
		}
	}

	public HDRImage(int width, int height) {
		this.width = width;
		this.height = height;
		this.pixels = new float[width * height * 4];
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
	}

	public byte[] compressPixels() throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream(this.pixels.length * Float.BYTES);
		GZIPOutputStream compressor = new GZIPOutputStream(baos);
		DataOutputStream dos = new DataOutputStream(compressor);
		for (float pixel : this.pixels) {
			dos.writeFloat(pixel);
		}
		dos.close();
		return baos.toByteArray();
	}

	public void decompressPixels(byte[] pixels) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(pixels);
		GZIPInputStream decompressor = new GZIPInputStream(bais);
		DataInputStream in = new DataInputStream(decompressor);
		for (int index = 0, length = this.pixels.length; index < length; index++) {
			this.pixels[index] = in.readFloat();
		}
		if (in.available() != 0) throw new IOException("Trailing data");
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

	public FloatVector getPixel(int x, int y) {
		return FloatVector.fromArray(FloatVector.SPECIES_128, this.pixels, this.baseIndex(x, y));
	}

	public void setPixel(int x, int y, FloatVector vector) {
		vector.intoArray(this.pixels, this.baseIndex(x, y));
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

	public void setColor(int x, int y, FloatVector color) {
		color.intoArray(this.pixels, this.baseIndex(x, y));
	}

	public static int packRgbaToArgb(float red, float green, float blue, float alpha) {
		return (Util.clampI(alpha) << 24) | (Util.clampI(red) << 16) | (Util.clampI(green) << 8) | Util.clampI(blue);
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
				float clampedAlpha = Util.clampF(alpha);
				pixels[baseIndex    ] = Util.clampB(blue  * clampedAlpha);
				pixels[baseIndex | 1] = Util.clampB(green * clampedAlpha);
				pixels[baseIndex | 2] = Util.clampB(red   * clampedAlpha);
				pixels[baseIndex | 3] = Util.clampB(               alpha);
			}
		}
		image.getPixelWriter().setPixels(0, 0, this.width, this.height, PixelFormat.getByteBgraPreInstance(), pixels, 0, this.width << 2);
		return image;
	}

	public BufferedImage toAwtImage(AnimationSource animation) {
		int frames = animation.frames.get();
		BufferedImage image = new BufferedImage(this.width, this.height * frames, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = image.getRaster();
		int[] pixel = new int[1];
		int oldFrame = animation.frame.get();
		for (int frame = 0; frame < frames; frame++) {
			animation.frame.set(frame);
			animation.openImage.redrawAll(false);
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.height; x++) {
					pixel[0] = this.getPackedArgb(x, y);
					raster.setDataElements(x, y + frame * this.height, pixel);
				}
			}
		}
		animation.frame.set(oldFrame);
		animation.openImage.redrawAll(false);
		return image;
	}

	public static byte[] toPngByteArray(BufferedImage image, SaveProgress progress) {
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
}