package builderb0y.notgimp;

import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;

import builderb0y.notgimp.sources.LayerSources;

import static builderb0y.notgimp.HDRImage.*;

public class Layer {

	public OpenImage openImage;
	public SimpleStringProperty name = new SimpleStringProperty();
	public TreeItem<Layer> item = new TreeItem<>(this);
	public HDRImage image;
	public WritableImage thumbnail;
	public ImageView thumbnailView;
	public LayerSources sources;
	public History history;

	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("name", this.name.get());
		object.addProperty("width", this.image.width);
		object.addProperty("height", this.image.height);
		JsonArray jsonChildren = new JsonArray(this.item.getChildren().size());
		for (TreeItem<Layer> child : this.item.getChildren()) {
			jsonChildren.add(child.getValue().save());
		}
		object.add("children", jsonChildren);
		object.add("sources", this.sources.save());
		object.addProperty("expanded", this.item.isExpanded());
		return object;
	}

	public Layer(OpenImage openImage, JsonObject saveData) {
		this(
			openImage,
			saveData.get("name").getAsString(),
			saveData.get("width").getAsInt(),
			saveData.get("height").getAsInt()
		);
		for (JsonElement element : saveData.getAsJsonArray("children")) {
			this.item.getChildren().add(new Layer(openImage, element.getAsJsonObject()).item);
		}
		this.sources.load(saveData.get("sources").getAsJsonObject());
		this.item.setExpanded(true);
	}

	public Layer(OpenImage openImage, String name, int width, int height) {
		this(openImage, name, new HDRImage(width, height));
	}

	public Layer(OpenImage openImage, String name, HDRImage image) {
		this.openImage = openImage;
		this.setName(name);
		this.image = image;
		this.thumbnail = new WritableImage(image.width, image.height);
		this.thumbnailView = new ImageView(this.thumbnail);
		this.sources = new LayerSources(this);
		this.history = new History(this);
	}

	public Layer(Layer from) {
		this.openImage = from.openImage;
		this.setName(from.name.get());
		this.image = new HDRImage(from.image);
		this.thumbnail = new WritableImage(from.thumbnail.getPixelReader(), from.image.width, from.image.height);
		this.thumbnailView = new ImageView(this.thumbnail);
		this.sources = new LayerSources(this, from.sources);
		this.history = new History(this);
	}

	public void init(boolean fromSave) {
		if (this.image.width >= this.image.height) {
			this.thumbnailView.setFitWidth(32.0D);
		}
		else {
			this.thumbnailView.setFitHeight(32.0D);
		}
		this.thumbnailView.setPreserveRatio(true);
		this.redrawThumbnail();
		this.image.addWatcher((HDRImage image, boolean fromAnimation) -> this.redrawThumbnail());

		RadioButton showing = new RadioButton();
		showing.setGraphic(this.thumbnailView);
		showing.setText(" ");
		showing.setToggleGroup(this.openImage.showingLayer);
		showing.setUserData(this);
		showing.setSelected(true);
		this.item.setGraphic(showing);

		this.sources.init(fromSave);
		this.history.init(fromSave);

		if (fromSave) {
			for (TreeItem<Layer> child : this.item.getChildren()) {
				child.getValue().init(true);
			}
		}
	}

	public void setName(String name) {
		name = name.trim();
		this.openImage.layerMap.remove(this.name.get());
		Layer found = this.openImage.findLayer(name);
		if (found != null && found != this) {
			for (int index = 1; true; index++) {
				String nextName = name + " (" + index + ')';
				found = this.openImage.findLayer(nextName);
				if (found == null || found == this) {
					name = nextName;
					break;
				}
			}
		}
		this.name.set(name);
		this.openImage.layerMap.put(name, this);
	}

	public FloatVector getPixelWrapped(int x, int y) {
		return FloatVector.fromArray(
			FloatVector.SPECIES_128,
			this.image.pixels,
			this.image.baseIndex(
				Math.floorMod(x, this.image.width),
				this.image.height - Math.floorMod(y, this.image.height) - 1
			)
		);
	}

	public FloatVector getPixelWrapped(IntVector coord) {
		return this.getPixelWrapped(coord.lane(0), coord.lane(1));
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
				pixels[baseIndex    ] = Util.clampB(blue  * alpha);
				pixels[baseIndex | 1] = Util.clampB(green * alpha);
				pixels[baseIndex | 2] = Util.clampB(red   * alpha);
				pixels[baseIndex | 3] = Util.clampB(alpha);
			}
		});
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
	}

	@Override
	public String toString() {
		return this.name.get();
	}
}