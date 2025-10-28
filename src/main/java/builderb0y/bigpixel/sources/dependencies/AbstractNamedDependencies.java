package builderb0y.bigpixel.sources.dependencies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.OrganizedSelection.Value;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;

public abstract class AbstractNamedDependencies extends LayerDependencies {

	public OrganizedSelection.Value<?> owner;
	public Map<String, InputBinding> allBindings;
	public ColorBoxGroup colorBoxGroup;

	public AbstractNamedDependencies(Value<?> owner) {
		this.owner = owner;
		this.allBindings = new HashMap<>();
		ColorHelper colorHelper = owner.getLayer().graph.openImage.mainWindow.colorPicker.currentColor;
		this.colorBoxGroup = new ColorBoxGroup(colorHelper, this.getConfigPane());
	}

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
}