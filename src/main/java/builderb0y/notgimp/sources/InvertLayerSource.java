package builderb0y.notgimp.sources;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.notgimp.HDRImage;

public class InvertLayerSource extends SingleInputEffectLayerSource {

	public CheckBox
		red   = this.addCheckbox("red", "Red", true),
		green = this.addCheckbox("green", "Green", true),
		blue  = this.addCheckbox("blue", "Blue", true),
		alpha = this.addCheckbox("alpha", "Alpha", false);
	public VBox
		channels = new VBox(this.red, this.green, this.blue, this.alpha);

	public InvertLayerSource(LayerSources sources) {
		super(sources, "invert", "Invert");
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
}