package builderb0y.bigpixel.sources.dependencies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.OrganizedSelection.Value;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;

public abstract class AbstractNamedDependencies extends LayerDependencies {

	public OrganizedSelection.Value<?> owner;
	public Map<String, InputBinding> allBindings;
	public ColorBoxGroup colorBoxGroup;
	public AnimatedBinding animated;

	public AbstractNamedDependencies(Value<?> owner) {
		this.owner = owner;
		this.allBindings = new HashMap<>();
		ColorHelper colorHelper = owner.getLayer().graph.openImage.mainWindow.colorPicker.currentColor;
		this.colorBoxGroup = new ColorBoxGroup(colorHelper, this.getConfigPane());
		this.animated = new AnimatedBinding();
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

	public void onBindingAdded(InputBinding binding) {
		this.animated.callBind(binding.animated);
	}

	@Override
	public void retainAll(List<LayerNode> layers) {
		for (InputBinding binding : this.allBindings.values()) {
			binding.retainAll(layers);
		}
	}

	@Override
	public Stream<SamplerProvider> getAll() {
		return this.allBindings.values().stream().map(InputBinding::getCurrent);
	}

	@Override
	public ObservableBooleanValue animatedProperty() {
		return this.animated;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return this.allBindings.values().stream().map((InputBinding binding) -> binding.curve);
	}

	public class AnimatedBinding extends BooleanBinding {

		public void callBind(Observable observable) {
			this.bind(observable);
		}

		@Override
		public boolean computeValue() {
			for (InputBinding binding : AbstractNamedDependencies.this.allBindings.values()) {
				if (binding.animated.get()) return true;
			}
			return false;
		}
	}
}