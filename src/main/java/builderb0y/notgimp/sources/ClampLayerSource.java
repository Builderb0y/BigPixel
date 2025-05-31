package builderb0y.notgimp.sources;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;

public class ClampLayerSource extends SingleInputEffectLayerSource {

	public VBox channels = new VBox();
	public CheckBox
		red   = this.addCheckbox("red",   "Red", true),
		green = this.addCheckbox("green", "Green", true),
		blue  = this.addCheckbox("blue",  "Blue", true),
		alpha = this.addCheckbox("alpha", "Alpha", true);

	public ClampLayerSource(LayerSources sources) {
		super(sources, "clamp", "Clamp");
		this.channels.getChildren().addAll(this.red, this.green, this.blue, this.alpha);
		this.rootNode.setCenter(this.channels);
	}

	@Override
	public void doRedraw() throws RedrawException {
		Layer input = this.getSingleInput(true);
		boolean[] channels = new boolean[4];
		channels[HDRImage.  RED_OFFSET] = this.red  .isSelected();
		channels[HDRImage.GREEN_OFFSET] = this.green.isSelected();
		channels[HDRImage. BLUE_OFFSET] = this.blue .isSelected();
		channels[HDRImage.ALPHA_OFFSET] = this.alpha.isSelected();
		VectorMask<Float> mask = VectorMask.fromArray(FloatVector.SPECIES_128, channels, 0);
		HDRImage source = input.image;
		HDRImage destination = this.sources.layer.image;
		FloatVector
			zero = FloatVector.zero(FloatVector.SPECIES_128),
			one = FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F);
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			pixel.lanewise(VectorOperators.MAX, zero, mask).lanewise(VectorOperators.MIN, one, mask).intoArray(destination.pixels, index);
		}
	}
}