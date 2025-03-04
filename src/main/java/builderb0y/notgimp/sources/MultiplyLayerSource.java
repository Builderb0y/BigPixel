package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class MultiplyLayerSource extends EffectLayerSource {

	public CheckBox alphaWeighting = new CheckBox("Alpha Weighting");

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "mul")
			.with("alpha_weighting", this.alphaWeighting.isSelected())
		);
	}

	@Override
	public void load(JsonMap map) {
		this.alphaWeighting.setSelected(map.getBoolean("alpha_weighting"));
	}

	public MultiplyLayerSource(LayerSources sources) {
		super(sources, "Multiply");
		this.alphaWeighting.selectedProperty().addListener(Util.change(this::requestRedraw));
	}

	public void copyFrom(MultiplyLayerSource that) {
		this.alphaWeighting.setSelected(that.alphaWeighting.isSelected());
	}

	@Override
	public void doRedraw() throws RedrawException {
		Collection<TreeItem<Layer>> watching = this.getWatchedItems();
		if (watching.isEmpty()) {
			throw new RedrawException("Expected at least one child layer");
		}
		HDRImage destination = this.sources.layer.image;
		Arrays.fill(destination.pixels, 1.0F);
		boolean alphaWeighting = this.alphaWeighting.isSelected();
		for (TreeItem<Layer> layer : watching) {
			HDRImage image = layer.getValue().image;
			for (int base = 0; base < image.pixels.length; base += 4) {
				FloatVector oldColor = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newColor = FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base);
				if (alphaWeighting) {
					VectorOperations.mix_float4_float4_float(FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F), newColor, newColor.lane(HDRImage.ALPHA_OFFSET)).mul(oldColor).intoArray(destination.pixels, base);
				}
				else {
					newColor.mul(oldColor).intoArray(destination.pixels, base);
				}
			}
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