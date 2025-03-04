package builderb0y.notgimp.sources;

import java.util.Collection;

import javafx.scene.control.TreeItem;

import builderb0y.notgimp.Layer;

public abstract class EffectLayerSource extends LayerSource {

	public EffectLayerSource(LayerSources sources, String displayName) {
		super(sources, displayName);
	}

	public void init(boolean fromSave) {
		this.invalidateStructure();
	}

	@Override
	public void invalidateStructure() {
		this.requestRedraw();
	}

	public Collection<TreeItem<Layer>> getWatchedItems() {
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