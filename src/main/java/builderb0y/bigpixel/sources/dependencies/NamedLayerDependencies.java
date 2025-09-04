package builderb0y.bigpixel.sources.dependencies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class NamedLayerDependencies extends LayerDependencies {

	public LayerSource source;
	public Map<String, InputBinding> allBindings;
	public ColorBoxGroup colorBoxGroup;
	public GridPane gridPane;

	@Override
	public JsonMap save() {
		JsonMap map = new JsonMap();
		for (Map.Entry<String, InputBinding> entry : this.allBindings.entrySet()) {
			map.put(entry.getKey(), entry.getValue().save());
		}
		return map;
	}

	@Override
	public void load(JsonMap saveData) {
		for (Map.Entry<String, InputBinding> entry : this.allBindings.entrySet()) {
			entry.getValue().load(saveData.getMap(entry.getKey()));
		}
	}

	public NamedLayerDependencies(LayerSource source) {
		this.source = source;
		this.gridPane = new GridPane();
		this.allBindings = new HashMap<>();
		ColorHelper colorHelper = source.sources.layer.graph.openImage.mainWindow.colorPicker.currentColor;
		this.colorBoxGroup = new ColorBoxGroup(colorHelper, this.gridPane);
	}

	public UnmovableInputBinding addBinding(String saveName, UnmovableInputBinding binding) {
		int size = this.allBindings.size();
		if (this.allBindings.putIfAbsent(saveName, binding) != null) {
			throw new IllegalArgumentException("Duplicate input binding: " + saveName);
		}
		binding.addRow(this.gridPane, size);
		return binding;
	}

	public UnmovableInputBinding addBinding(String saveName, String displayName) {
		return this.addBinding(saveName, new UnmovableInputBinding(this.source, displayName, this.colorBoxGroup));
	}

	public void addExtraNodeRow(Node node) {
		int width  = this.gridPane.getColumnCount();
		int height = this.gridPane.getRowCount();
		this.gridPane.add(node, 0, height, width, 1);
	}

	@Override
	public void retainAll(List<LayerNode> layers) {
		for (InputBinding binding : this.allBindings.values()) {
			binding.retainAll(layers);
		}
	}

	@Override
	public boolean dependsOn(LayerNode layer) {
		for (InputBinding binding : this.allBindings.values()) {
			if (binding.getSelectedLayer() == layer) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> layers) {
		for (InputBinding binding : this.allBindings.values()) {
			LayerNode layer = binding.getSelectedLayer();
			if (layer != null && layers.test(layer)) return true;
		}
		return false;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return this.allBindings.values().stream().map((InputBinding binding) -> binding.curve);
	}

	@Override
	public Parent getConfigPane() {
		return this.gridPane;
	}
}