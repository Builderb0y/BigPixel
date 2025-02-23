package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javafx.scene.control.TreeItem;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.HDRImage.HdrImageWatcher;
import builderb0y.notgimp.Layer;

public abstract class EffectLayerSource extends LayerSource {

	public HdrImageWatcher imageWatcher = (HDRImage image, boolean fromAnimation) -> {
		//if this change was triggered by an animation,
		//redraw() will be called automatically later.
		if (!fromAnimation) this.redraw(false);
	};
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
			child.getValue().image.addWatcher(this.imageWatcher);
			this.watching.add(child.getValue());
		}
		this.redraw(false);
	}

	@Override
	public void onChanged(Change<? extends String, ? extends Layer> change) {
		TreeItem<Layer> parent;
		if (change.wasRemoved() && (parent = change.getValueRemoved().item.getParent()) != null && parent.getValue() == this.sources.layer) {
			change.getValueRemoved().image.removeWatcher(this.imageWatcher);
			this.watching.remove(change.getValueRemoved());
		}
		if (change.wasAdded() && (parent = change.getValueAdded().item.getParent()) != null && parent.getValue() == this.sources.layer) {
			change.getValueAdded().image.addWatcher(this.imageWatcher);
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