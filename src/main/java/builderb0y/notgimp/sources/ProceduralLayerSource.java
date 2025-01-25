package builderb0y.notgimp.sources;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

public class ProceduralLayerSource extends LayerSource {

	public TextArea textArea = new TextArea();

	public ProceduralLayerSource(LayerSources sources) {
		super(sources);
	}

	public void init() {}

	public void copyFrom(ProceduralLayerSource that) {
		this.textArea.setText(that.textArea.getText());
	}

	@Override
	public Node getRootNode() {
		return this.textArea;
	}

	@Override
	public void redraw() {

	}
}