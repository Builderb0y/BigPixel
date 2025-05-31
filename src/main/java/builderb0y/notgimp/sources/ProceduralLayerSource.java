package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Collections;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

import builderb0y.notgimp.Layer;

public class ProceduralLayerSource extends LayerSource {

	public TextArea textArea = this.addCode("code");

	public ProceduralLayerSource(LayerSources sources) {
		super(sources, "procedural", "Procedural");
	}

	@Override
	public void init(boolean fromSave) {}

	@Override
	public Collection<Layer> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public boolean isAnimated() {
		return false;
	}

	@Override
	public void invalidateStructure() {

	}

	@Override
	public Node getRootNode() {
		return this.textArea;
	}

	@Override
	public void doRedraw() throws RedrawException {

	}
}