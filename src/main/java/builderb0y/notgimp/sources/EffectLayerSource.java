package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.List;

import javafx.scene.control.TreeItem;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;

public abstract class EffectLayerSource extends LayerSource {

	public EffectLayerSource(LayerSources sources, String saveName, String displayName) {
		super(sources, saveName, displayName);
	}

	@Override
	public void init(boolean fromSave) {
		this.invalidateStructure();
	}

	@Override
	public void invalidateStructure() {
		this.requestRedraw();
	}

	public void checkSameSize(List<TreeItem<Layer>> layers) throws RedrawException {
		if (layers.isEmpty()) {
			throw new RedrawException("Expected at least one child layer");
		}
		HDRImage first = layers.get(0).getValue().image;
		for (int index = 1, size = layers.size(); index < size; index++) {
			HDRImage compare = layers.get(index).getValue().image;
			if (compare.width != first.width || compare.height != first.height) {
				throw new RedrawException("All children must have the same resolution");
			}
		}
		HDRImage destination = this.sources.layer.image;
		if (first.width != destination.width || first.height != destination.height) {
			destination.resize(first.width, first.height, false);
		}
	}

	public List<TreeItem<Layer>> getWatchedItems() {
		return this.sources.layer.item.getChildren();
	}

	@Override
	public Collection<Layer> getDependencies() {
		return this.getWatchedItems().stream().map(TreeItem<Layer>::getValue).toList();
	}

	@Override
	public boolean isAnimated() {
		return false;
	}
}