package builderb0y.notgimp.sources;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public class InvertLayerSource extends EffectLayerSource {

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
		ChangeListener<Boolean> redrawer = Util.change(() -> this.redraw(false));
		this.red  .selectedProperty().addListener(redrawer);
		this.green.selectedProperty().addListener(redrawer);
		this.blue .selectedProperty().addListener(redrawer);
		this.alpha.selectedProperty().addListener(redrawer);
	}

	@Override
	public Node getRootNode() {
		return this.channels;
	}

	@Override
	public void doRedraw(boolean fromAnimation) throws RedrawException {
		if (this.watching.size() != 1) {
			throw new RedrawException("Expected exactly one child layer");
		}
		boolean[] channels = new boolean[4];
		channels[HDRImage.  RED_OFFSET] = this.red  .isSelected();
		channels[HDRImage.GREEN_OFFSET] = this.green.isSelected();
		channels[HDRImage. BLUE_OFFSET] = this.blue .isSelected();
		channels[HDRImage.ALPHA_OFFSET] = this.alpha.isSelected();
		VectorMask<Float> mask = VectorMask.fromArray(FloatVector.SPECIES_128, channels, 0);
		HDRImage source = this.watching.iterator().next().image;
		HDRImage destination = this.sources.layer.image;
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			pixel.blend(pixel.broadcast(1.0F).sub(pixel), mask).intoArray(destination.pixels, index);
		}
		destination.markDirty(fromAnimation);
	}

	public void copyFrom(InvertLayerSource from) {
		this.red  .setSelected(from.red  .isSelected());
		this.green.setSelected(from.green.isSelected());
		this.blue .setSelected(from.blue .isSelected());
		this.alpha.setSelected(from.alpha.isSelected());
	}
}