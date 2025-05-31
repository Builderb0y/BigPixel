package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class ScreenLayerSource extends EffectLayerSource {

	public CheckBox alphaWeighting = this.addCheckbox("alpha_weighting", "Alpha Weighting", false);

	public ScreenLayerSource(LayerSources sources) {
		super(sources, "screen", "Screen");
	}

	public void copyFrom(ScreenLayerSource that) {
		this.alphaWeighting.setSelected(that.alphaWeighting.isSelected());
	}

	@Override
	public void doRedraw() throws RedrawException {
		List<TreeItem<Layer>> watching = this.getWatchedItems();
		this.checkSameSize(watching);
		HDRImage destination = this.sources.layer.image;
		Arrays.fill(destination.pixels, 1.0F);
		boolean alphaWeighting = this.alphaWeighting.isSelected();
		for (TreeItem<Layer> layer : watching) {
			HDRImage image = layer.getValue().image;
			for (int base = 0; base < image.pixels.length; base += 4) {
				FloatVector oldColor = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newColor = FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F).sub(FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base));
				if (alphaWeighting) {
					VectorOperations.mix_float4_float4_float(FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F), newColor, newColor.lane(HDRImage.ALPHA_OFFSET)).mul(oldColor).intoArray(destination.pixels, base);
				}
				else {
					newColor.mul(oldColor).intoArray(destination.pixels, base);
				}
			}
		}
		for (int base = 0; base < destination.pixels.length; base += 4) {
			FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F).sub(FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base)).intoArray(destination.pixels, base);
		}
		if (alphaWeighting) {
			for (int base = HDRImage.ALPHA_OFFSET; base < destination.pixels.length; base += 4) {
				destination.pixels[base] = 1.0F;
			}
		}
	}

	@Override
	public Node getRootNode() {
		return this.alphaWeighting;
	}
}