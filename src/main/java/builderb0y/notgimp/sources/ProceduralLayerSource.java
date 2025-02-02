package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;

public class ProceduralLayerSource extends LayerSource {

	public TextArea textArea = new TextArea();

	public JsonObject save() {
		JsonObject object = new JsonObject();
		JsonArray source = new JsonArray();
		this.textArea.getText().lines().forEachOrdered(source::add);
		object.add("code", source);
		return object;
	}

	public void load(JsonObject object) {
		this.textArea.setText(StreamSupport.stream(object.getAsJsonArray("code").spliterator(), false).map(JsonElement::getAsString).collect(Collectors.joining(System.lineSeparator())));
	}

	public ProceduralLayerSource(LayerSources sources) {
		super(sources);
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
	public void onInvalidated() {

	}

	@Override
	public Node getRootNode() {
		return this.textArea;
	}

	@Override
	public @Nullable Cursor getCursor() {
		return null;
	}

	@Override
	public void redraw(boolean markDirty) {

	}
}