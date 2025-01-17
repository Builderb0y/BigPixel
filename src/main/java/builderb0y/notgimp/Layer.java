package builderb0y.notgimp;

import java.util.stream.IntStream;

import javafx.beans.Observable;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import static builderb0y.notgimp.HDRImage.*;

public class Layer {

	public OpenImage openImage;
	public String name;
	public HDRImage image, toollessImage;
	public WritableImage thumbnail;
	public ImageView thumbnailView;
	public LayerSources sources;
	public History history;

	public Layer(OpenImage openImage, String name, HDRImage image) {
		this.openImage = openImage;
		this.setName(name);
		this.image = image;
		this.toollessImage = new HDRImage(image.width, image.height);
		this.thumbnail = new WritableImage(image.width, image.height);
		this.thumbnailView = new ImageView(this.thumbnail);
		this.sources = new LayerSources(this);
		this.history = new History(this);
		this.postConstruct();
	}

	public Layer(Layer from) {
		this.openImage = from.openImage;
		this.setName(from.name);
		this.image = new HDRImage(from.image);
		this.toollessImage = new HDRImage(from.image.width, from.image.height);
		this.thumbnail = new WritableImage(from.thumbnail.getPixelReader(), from.image.width, from.image.height);
		this.thumbnailView = new ImageView(this.thumbnail);
		this.sources = new LayerSources(this, from.sources);
		this.history = new History(this);
		this.postConstruct();
	}

	public void beginUsingTool() {
		System.arraycopy(this.image.pixels, 0, this.toollessImage.pixels, 0, this.image.pixels.length);
	}

	public void beforeToolChanged() {
		System.arraycopy(this.toollessImage.pixels, 0, this.image.pixels, 0, this.toollessImage.pixels.length);
	}

	public void finishUsingTool() {
		System.arraycopy(this.image.pixels, 0, this.toollessImage.pixels, 0, this.image.pixels.length);
	}

	public void cancelToolAction() {
		System.arraycopy(this.toollessImage.pixels, 0, this.image.pixels, 0, this.toollessImage.pixels.length);
		this.image.markDirty();
	}

	public void setName(String name) {
		name = name.trim();
		this.name = null; //ensure name does not match this layer.
		if (this.openImage.findLayer(name) != null) {
			for (int index = 1; true; index++) {
				String nextName = name + " (" + index + ')';
				if (this.openImage.findLayer(nextName) == null) {
					name = nextName;
					break;
				}
			}
		}
		this.name = name;
	}

	public void postConstruct() {
		if (this.image.width >= this.image.height) {
			this.thumbnailView.setFitWidth(32.0D);
		}
		else {
			this.thumbnailView.setFitHeight(32.0D);
		}
		this.thumbnailView.setPreserveRatio(true);
		this.redrawThumbnail();
		this.image.value.addListener((Observable observable) -> this.redrawThumbnail());
		this.sources.init();
	}

	public Layer(OpenImage openImage, String name, int width, int height) {
		this(openImage, name, new HDRImage(width, height));
	}

	public void redrawThumbnail() {
		PixelWriter writer = this.thumbnail.getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		int width = this.image.width;
		int height = this.image.height;
		byte[] pixels = new byte[width * height * 4];
		IntStream.range(0, height).parallel().forEach((int y) -> {
			for (int x = 0; x < width; x++) {
				int baseIndex = this.image.baseIndex(x, y);
				float red     = this.image.pixels[baseIndex |   RED_OFFSET];
				float green   = this.image.pixels[baseIndex | GREEN_OFFSET];
				float blue    = this.image.pixels[baseIndex |  BLUE_OFFSET];
				float alpha   = this.image.pixels[baseIndex | ALPHA_OFFSET];
				pixels[baseIndex    ] = (byte)(clamp(blue));
				pixels[baseIndex | 1] = (byte)(clamp(green));
				pixels[baseIndex | 2] = (byte)(clamp(red));
				pixels[baseIndex | 3] = (byte)(clamp(alpha));
			}
		});
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
	}

	@Override
	public String toString() {
		return this.name;
	}
}