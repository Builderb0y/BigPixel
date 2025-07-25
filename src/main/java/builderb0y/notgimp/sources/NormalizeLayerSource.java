package builderb0y.notgimp.sources;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class NormalizeLayerSource extends SingleInputEffectLayerSource {

	public CheckBox
		perChannel = this.addCheckbox("per_channel", "Per channel", true),
		red        = this.addCheckbox("red",   "Red", true),
		green      = this.addCheckbox("green", "Green", true),
		blue       = this.addCheckbox("blue",  "Blue", true),
		alpha      = this.addCheckbox("alpha", "Alpha", true);
	public VBox channels = new VBox(
		this.perChannel,
		this.red,
		this.green,
		this.blue,
		this.alpha
	);

	public NormalizeLayerSource(LayerSources sources) {
		super(sources, "normalize", "Normalize");
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
		if (!mask.anyTrue()) {
			throw new RedrawException("No channels selected");
		}
		HDRImage destination = this.sources.layer.image;
		boolean perChannel = this.perChannel.isSelected();
		FloatVector min = FloatVector.broadcast(FloatVector.SPECIES_128, Float.POSITIVE_INFINITY);
		FloatVector max = FloatVector.broadcast(FloatVector.SPECIES_128, Float.NEGATIVE_INFINITY);
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			if (pixel.lane(HDRImage.ALPHA_OFFSET) != 0.0F) {
				min = min.min(pixel);
				max = max.max(pixel);
			}
		}
		if (!perChannel) {
			min = FloatVector.broadcast(FloatVector.SPECIES_128, min.reduceLanes(VectorOperators.MIN, mask));
			max = FloatVector.broadcast(FloatVector.SPECIES_128, max.reduceLanes(VectorOperators.MAX, mask));
		}
		mask = mask.andNot(min.eq(max));
		if (mask.anyTrue()) {
			for (int index = 0, length = source.pixels.length; index < length; index += 4) {
				FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
				if (pixel.lane(HDRImage.ALPHA_OFFSET) != 0.0F) {
					FloatVector newPixel = VectorOperations.unmix_float4_float4_float4(min, max, pixel);
					pixel = pixel.blend(newPixel, mask);
				}
				pixel.intoArray(destination.pixels, index);
			}
		}
		else {
			System.arraycopy(source.pixels, 0, destination.pixels, 0, source.pixels.length);
		}
	}
}