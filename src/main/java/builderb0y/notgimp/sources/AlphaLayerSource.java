package builderb0y.notgimp.sources;

import java.util.Iterator;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;

public class AlphaLayerSource extends EffectLayerSource {

	public AlphaLayerSource(LayerSources sources) {
		super(sources, "alpha", "Alpha Blend");
	}

	@Override
	public void doRedraw() throws RedrawException {
		List<TreeItem<Layer>> watching = this.getWatchedItems();
		this.checkSameSize(watching);
		HDRImage destination = this.sources.layer.image;
		Iterator<TreeItem<Layer>> iterator = watching.reversed().iterator();
		HDRImage first = iterator.next().getValue().image;
		System.arraycopy(first.pixels, 0, destination.pixels, 0, destination.pixels.length);
		while (iterator.hasNext()) {
			HDRImage source = iterator.next().getValue().image;
			for (int base = 0; base < source.pixels.length; base += 4) {
				FloatVector oldValue = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newValue = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, base);
				float oldAlpha = oldValue.lane(HDRImage.ALPHA_OFFSET);
				float newAlpha = newValue.lane(HDRImage.ALPHA_OFFSET);
				float finalAlpha = 1.0F - (1.0F - oldAlpha) * (1.0F - newAlpha);
				FloatVector result = oldValue.mul(oldAlpha - oldAlpha * newAlpha /* oldAlpha * (1 - newAlpha) */).add(newValue.mul(newAlpha));
				if (finalAlpha != 0.0F) result = result.div(finalAlpha);
				result = result.withLane(HDRImage.ALPHA_OFFSET, finalAlpha);
				result.intoArray(destination.pixels, base);
			}
		}
	}

	@Override
	public Node getRootNode() {
		return null;
	}
}