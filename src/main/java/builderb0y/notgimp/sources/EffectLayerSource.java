package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javafx.scene.control.TreeItem;

import builderb0y.notgimp.Layer;

public abstract class EffectLayerSource extends LayerSource {

	public Set<Layer> watching = new HashSet<>(4);

	public EffectLayerSource(LayerSources sources, String displayName) {
		super(sources, displayName);
	}

	public void init(boolean fromSave) {
		this.invalidateStructure();
	}

	@Override
	public void invalidateStructure() {
		this.watching.clear();
		for (TreeItem<Layer> child : this.sources.layer.item.getChildren()) {
			this.watching.add(child.getValue());
		}
		this.requestRedraw();
	}

	@Override
	public void onChanged(Change<? extends String, ? extends Layer> change) {
		TreeItem<Layer> parent;
		if (change.wasRemoved() && (parent = change.getValueRemoved().item.getParent()) != null && parent.getValue() == this.sources.layer) {
			this.watching.remove(change.getValueRemoved());
		}
		if (change.wasAdded() && (parent = change.getValueAdded().item.getParent()) != null && parent.getValue() == this.sources.layer) {
			this.watching.add(change.getValueAdded());
		}
	}

	@Override
	public Collection<Layer> getDependencies() {
		return this.watching;
	}

	@Override
	public boolean isAnimated() {
		return false;
	}
}