package builderb0y.notgimp.sources;

import javafx.scene.Cursor;
import javafx.scene.Node;

public abstract class LayerSource {

	public LayerSources sources;

	public LayerSource(LayerSources sources) {
		this.sources = sources;
	}

	public boolean isSelected() {
		return this.sources.getCurrentSource() == this;
	}

	public abstract Node getRootNode();

	public abstract void redraw();

	public Cursor getCursor() {
		return null;
	}
}