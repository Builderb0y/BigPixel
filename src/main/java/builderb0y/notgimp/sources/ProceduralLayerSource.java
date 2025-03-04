package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.json.JsonValue;

public class ProceduralLayerSource extends LayerSource {

	public TextArea textArea = new TextArea();

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("type", "procedural");
		JsonArray source = new JsonArray();
		this.textArea.getText().lines().forEachOrdered(source::add);
		map.add("code", source);
		return map;
	}

	@Override
	public void load(JsonMap map) {
		this.textArea.setText(map.getArray("code").stream().map(JsonValue::asString).collect(Collectors.joining(System.lineSeparator())));
	}

	public ProceduralLayerSource(LayerSources sources) {
		super(sources, "Procedural");
	}

	public void init(boolean fromSave) {}

	public void copyFrom(ProceduralLayerSource that) {
		this.textArea.setText(that.textArea.getText());
	}

	@Override
	public Collection<Layer> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public boolean isAnimated() {
		return false;
	}

	@Override
	public void onChanged(Change<? extends String, ? extends Layer> change) {
		//no-op.
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