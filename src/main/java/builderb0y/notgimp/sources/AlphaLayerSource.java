package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Iterator;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class AlphaLayerSource extends EffectLayerSource {

	@Override
	public JsonMap save() {
		return new JsonMap().with("type", "alpha");
	}

	@Override
	public void load(JsonMap map) {

	}

	public AlphaLayerSource(LayerSources sources) {
		super(sources, "Alpha Blend");
	}

	public void copyFrom(AlphaLayerSource from) {}

	@Override
	public void doRedraw() throws RedrawException {
		Collection<TreeItem<Layer>> watching = this.getWatchedItems();
		if (watching.isEmpty()) {
			throw new RedrawException("Expected at least one child layer");
		}
		HDRImage destination = this.sources.layer.image;
		Iterator<TreeItem<Layer>> iterator = watching.iterator();
		HDRImage first = iterator.next().getValue().image;
		System.arraycopy(first.pixels, 0, destination.pixels, 0, destination.pixels.length);
		while (iterator.hasNext()) {
			HDRImage source = iterator.next().getValue().image;
			for (int base = 0; base < source.pixels.length; base += 4) {
				FloatVector oldValue = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newValue = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, base);
				VectorOperations.mix_float4_float4_float(oldValue, newValue.withLane(HDRImage.ALPHA_OFFSET, 1.0F), newValue.lane(HDRImage.ALPHA_OFFSET)).intoArray(destination.pixels, base);
			}
		}
	}

	@Override
	public Node getRootNode() {
		return null;
	}
}