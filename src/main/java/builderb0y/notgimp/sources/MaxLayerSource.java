package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonMap;

public class MaxLayerSource extends EffectLayerSource {

	@Override
	public JsonMap save() {
		return new JsonMap().with("type", "max");
	}

	@Override
	public void load(JsonMap map) {

	}

	public MaxLayerSource(LayerSources sources) {
		super(sources, "Max");
	}

	public void copyFrom(MaxLayerSource that) {}

	@Override
	public void doRedraw() throws RedrawException {
		List<TreeItem<Layer>> watching = this.getWatchedItems();
		this.checkSameSize(watching);
		HDRImage destination = this.sources.layer.image;
		Iterator<TreeItem<Layer>> iterator = watching.iterator();
		HDRImage first = iterator.next().getValue().image;
		System.arraycopy(first.pixels, 0, destination.pixels, 0, destination.pixels.length);
		while (iterator.hasNext()) {
			HDRImage next = iterator.next().getValue().image;
			for (int base = 0; base < destination.pixels.length; base += 4) {
				FloatVector oldColor = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, base);
				FloatVector newColor = FloatVector.fromArray(FloatVector.SPECIES_128, next.pixels, base);
				newColor.max(oldColor).intoArray(destination.pixels, base);
			}
		}
	}

	@Override
	public Node getRootNode() {
		return null;
	}
}