package builderb0y.notgimp.sources;

import java.util.Collection;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public class ClampLayerSource extends EffectLayerSource {

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
			.with("type", "clamp")
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

	public ClampLayerSource(LayerSources sources) {
		super(sources, "Clamp");
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
	}

	public void copyFrom(ClampLayerSource from) {
		this.red  .setSelected(from.red  .isSelected());
		this.green.setSelected(from.green.isSelected());
		this.blue .setSelected(from.blue .isSelected());
		this.alpha.setSelected(from.alpha.isSelected());
	}

	@Override
	public void doRedraw() throws RedrawException {
		Collection<TreeItem<Layer>> watching = this.getWatchedItems();
		if (watching.size() != 1) {
			throw new RedrawException("Expected exactly one child layer");
		}
		boolean[] channels = new boolean[4];
		channels[HDRImage.  RED_OFFSET] = this.red  .isSelected();
		channels[HDRImage.GREEN_OFFSET] = this.green.isSelected();
		channels[HDRImage. BLUE_OFFSET] = this.blue .isSelected();
		channels[HDRImage.ALPHA_OFFSET] = this.alpha.isSelected();
		VectorMask<Float> mask = VectorMask.fromArray(FloatVector.SPECIES_128, channels, 0);
		HDRImage source = watching.iterator().next().getValue().image;
		HDRImage destination = this.sources.layer.image;
		FloatVector
			zero = FloatVector.zero(FloatVector.SPECIES_128),
			one = FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F);
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			pixel.lanewise(VectorOperators.MAX, zero, mask).lanewise(VectorOperators.MIN, one, mask).intoArray(destination.pixels, index);
		}
	}

	@Override
	public Node getRootNode() {
		return this.channels;
	}
}