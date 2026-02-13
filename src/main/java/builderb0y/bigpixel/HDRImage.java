package builderb0y.bigpixel;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Base64;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.util.Util;

//serializable for the sole purpose of allowing HDRImage's to be copy-pasted with a Clipboard.
public class HDRImage implements Serializable, Cloneable {

	@Serial
	private static final long serialVersionUID = 0xBE5393B55731DD65L;
	public static final DataFormat HDR_DATA_FORMAT = new DataFormat("application/bigpixel.hdrimage");

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
		map.add("pixels", Base64.getEncoder().encodeToString(History.compress(this.pixels)));
		return map;
	}

	public HDRImage(JsonMap saveData) {
		this(saveData.getInt("width"), saveData.getInt("height"));
		this.doCopyFrom(saveData);
	}

	@Deprecated //for serialization only.
	public HDRImage() {
		this.pixels = new float[0];
	}

	public HDRImage(int width, int height) {
		this.width = width;
		this.height = height;
		this.pixels = new float[width * height * 4];
	}

	public HDRImage(int width, int height, float[] pixels) {
		if (pixels.length != width * height * 4) {
			throw new IllegalArgumentException("Pixels array is wrong length");
		}
		this.width  = width;
		this.height = height;
		this.pixels = pixels;
	}

	public HDRImage(Image from) {
		this((int)(from.getWidth()), (int)(from.getHeight()));
		this.doCopyFrom(from);
	}

	public HDRImage(HDRImage from) {
		this(from.width, from.height);
		this.doCopyFrom(from);
	}

	public static @Nullable HDRImage fromClipboard(Clipboard clipboard) {
		if (clipboard.hasContent(HDR_DATA_FORMAT)) {
			Object content = clipboard.getContent(HDR_DATA_FORMAT);
			if (content instanceof HDRImage image) {
				return image;
			}
			else {
				System.err.println("Clipboard's HDR_DATA_FORMAT contained " + content);
			}
		}
		if (clipboard.hasContent(DataFormat.IMAGE)) {
			return new HDRImage(clipboard.getImage());
		}
		return null;
	}

	public void copyFrom(JsonMap saveData) {
		this.checkSize(saveData.getInt("width"), saveData.getInt("height"), false);
		this.doCopyFrom(saveData);
	}

	public void doCopyFrom(JsonMap saveData) {
		try {
			String base64Pixels = saveData.getString("pixels");
			byte[] compressedPixels = Base64.getDecoder().decode(base64Pixels);
			this.decompressPixels(compressedPixels);
		}
		catch (IOException exception) {
			throw new SaveException(exception);
		}
	}

	public void copyFrom(HDRImage from) {
		this.checkSize(from.width, from.height, false);
		this.doCopyFrom(from);
	}

	public void doCopyFrom(HDRImage from) {
		System.arraycopy(from.pixels, 0, this.pixels, 0, from.pixels.length);
	}

	public void copyFrom(Image image) {
		this.checkSize((int)(image.getWidth()), (int)(image.getHeight()), false);
		this.doCopyFrom(image);
	}

	public void doCopyFrom(Image image) {
		int width = this.width;
		int height = this.height;
		PixelReader reader = image.getPixelReader();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color color = reader.getColor(x, y);
				this.setRgba(x, y, (float)(color.getRed()), (float)(color.getGreen()), (float)(color.getBlue()), (float)(color.getOpacity()));
			}
		}
	}

	public void checkSize(int width, int height, boolean copy) {
		if (this.width != width || this.height != height) {
			this.resize(width, height, copy);
		}
	}

	public void resize(int width, int height, boolean copy) {
		float[] oldPixels = this.pixels;
		float[] newPixels = new float[width * height * 4];
		if (copy) {
			int minWidth = Math.min(this.width, width);
			int minHeight = Math.min(this.height, height);
			for (int y = 0; y < minHeight; y++) {
				System.arraycopy(oldPixels, y * this.width * 4, newPixels, y * width * 4, minWidth * 4);
			}
		}
		this.width = width;
		this.height = height;
		this.pixels = newPixels;
	}

	public void decompressPixels(byte[] compressed) throws IOException {
		GZIPInputStream decompressor = new GZIPInputStream(new ByteArrayInputStream(compressed));
		byte[] decompressed = new byte[this.pixels.length * Float.BYTES];
		readFully(decompressor, decompressed);
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(decompressed));
		for (int index = 0, length = this.pixels.length; index < length; index++) {
			this.pixels[index] = in.readFloat();
		}
	}

	//why does stream.read(destination) not read ALL available input bytes?
	public static void readFully(InputStream stream, byte[] destination) throws IOException {
		int start = 0;
		while (start < destination.length) {
			int read = stream.read(destination, start, destination.length - start);
			if (read <= 0) throw new IOException("Could not read enough bytes! Successfully read " + start + " / " + destination.length + " + bytes");
			start += read;
		}
		if (start > destination.length /* ??? */ || stream.available() > 0 /* more likely. */) throw new IOException("Trailing data");
	}

	public int baseIndex(int x, int y) {
		return (y * this.width + x) << 2;
	}

	public int getPackedArgb(int x, int y) {
		int baseIndex = this.baseIndex(x, y);
		float red   = this.pixels[baseIndex |   RED_OFFSET];
		float green = this.pixels[baseIndex | GREEN_OFFSET];
		float blue  = this.pixels[baseIndex |  BLUE_OFFSET];
		float alpha = this.pixels[baseIndex | ALPHA_OFFSET];
		return packRgbaToArgb(red, green, blue, alpha);
	}

	public FloatVector getColor(int x, int y) {
		return FloatVector.fromArray(FloatVector.SPECIES_128, this.pixels, this.baseIndex(x, y));
	}

	public void setRgba(int x, int y, float red, float green, float blue, float alpha) {
		int baseIndex = this.baseIndex(x, y);
		this.pixels[baseIndex |   RED_OFFSET] = red;
		this.pixels[baseIndex | GREEN_OFFSET] = green;
		this.pixels[baseIndex |  BLUE_OFFSET] = blue;
		this.pixels[baseIndex | ALPHA_OFFSET] = alpha;
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

	public HDRImage asyncCopy() {
		try {
			return (HDRImage)(this.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw new AssertionError(exception);
		}
	}

	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.width = in.readInt();
		this.height = in.readInt();
		int channelCount = this.width * this.height * 4;
		if (this.pixels == null || this.pixels.length != channelCount) {
			this.pixels = new float[channelCount];
		}
		for (int index = 0; index < channelCount; index++) {
			this.pixels[index] = in.readFloat();
		}
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(this.width);
		out.writeInt(this.height);
		for (float channel : this.pixels) {
			out.writeFloat(channel);
		}
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

	public BufferedImage toAwtImage() {
		BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
		this.toAwtImage(image, 0);
		return image;
	}

	public void toAwtImage(BufferedImage image, int frame) {
		int width = this.width, height = this.height;
		WritableRaster raster = image.getRaster();
		int[] pixel = new int[1];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixel[0] = this.getPackedArgb(x, y);
				raster.setDataElements(x, y + frame * height, pixel);
			}
		}
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