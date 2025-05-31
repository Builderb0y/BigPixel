package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;

public class AverageLayerSource extends EffectLayerSource {

	public CheckBox alphaWeighting = this.addCheckbox("alpha_weighting", "Alpha Weighting", false);

	public AverageLayerSource(LayerSources sources) {
		super(sources, "avg", "Average");
	}

	@Override
	public void doRedraw() throws RedrawException {
		List<TreeItem<Layer>> watching = this.getWatchedItems();
		this.checkSameSize(watching);
		HDRImage destination = this.sources.layer.image;
		Arrays.fill(destination.pixels, 0.0F);
		boolean alphaWeighting = this.alphaWeighting.isSelected();
		for (TreeItem<Layer> item : watching) {
			HDRImage image = item.getValue().image;
			for (int base = 0; base < image.pixels.length; base += 4) {
				FloatVector oldColor = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newColor = FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base);
				if (alphaWeighting) {
					newColor.mul(newColor.lane(HDRImage.ALPHA_OFFSET)).add(oldColor).intoArray(destination.pixels, base);
				}
				else {
					newColor.add(oldColor).intoArray(destination.pixels, base);
				}
			}
		}
		float rcpCount = 1.0F / watching.size();
		for (int base = 0; base < destination.pixels.length; base += 4) {
			FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base).mul(rcpCount).intoArray(destination.pixels, base);
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