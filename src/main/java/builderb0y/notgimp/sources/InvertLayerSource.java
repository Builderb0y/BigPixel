package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public class InvertLayerSource extends SingleInputEffectLayerSource {

	public VBox channels = new VBox();
	public CheckBox
		red   = new CheckBox("Red"),
		green = new CheckBox("Green"),
		blue  = new CheckBox("Blue"),
		alpha = new CheckBox("Alpha");

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "invert")
			.with("red",   this.red  .isSelected())
			.with("green", this.green.isSelected())
			.with("blue",  this.blue .isSelected())
			.with("alpha", this.alpha.isSelected())
		);
	}

	@Override
	public void load(JsonMap map) {
		this.red  .setSelected(map.getBoolean("red"  ));
		this.green.setSelected(map.getBoolean("green"));
		this.blue .setSelected(map.getBoolean("blue" ));
		this.alpha.setSelected(map.getBoolean("alpha"));
	}

	public InvertLayerSource(LayerSources sources) {
		super(sources, "Invert");
		this.red.setSelected(true);
		this.green.setSelected(true);
		this.blue.setSelected(true);
		this.alpha.setSelected(false);
		this.channels.getChildren().addAll(this.red, this.green, this.blue, this.alpha);
		ChangeListener<Boolean> redrawer = Util.change(this::requestRedraw);
		this.red  .selectedProperty().addListener(redrawer);
		this.green.selectedProperty().addListener(redrawer);
		this.blue .selectedProperty().addListener(redrawer);
		this.alpha.selectedProperty().addListener(redrawer);
		this.rootNode.setCenter(this.channels);
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		boolean[] channels = new boolean[4];
		channels[HDRImage.  RED_OFFSET] = this.red  .isSelected();
		channels[HDRImage.GREEN_OFFSET] = this.green.isSelected();
		channels[HDRImage. BLUE_OFFSET] = this.blue .isSelected();
		channels[HDRImage.ALPHA_OFFSET] = this.alpha.isSelected();
		VectorMask<Float> mask = VectorMask.fromArray(FloatVector.SPECIES_128, channels, 0);
		HDRImage destination = this.sources.layer.image;
		FloatVector one = FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F);
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			pixel.blend(one.sub(pixel), mask).intoArray(destination.pixels, index);
		}
	}

	public void copyFrom(InvertLayerSource from) {
		this.red  .setSelected(from.red  .isSelected());
		this.green.setSelected(from.green.isSelected());
		this.blue .setSelected(from.blue .isSelected());
		this.alpha.setSelected(from.alpha.isSelected());
	}
}