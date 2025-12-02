package builderb0y.bigpixel.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Parent;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;

public class SingletonDependencies extends LayerDependencies {

	public final LayerNode owner;

	public SingletonDependencies(LayerNode owner) {
		this.owner = owner;
	}

	@Override
	public JsonMap save() {
		return new JsonMap();
	}

	@Override
	public void load(JsonMap saveData) {

	}

	@Override
	public void retainAll(List<LayerNode> layers) {

	}

	@Override
	public Stream<SamplerProvider> getAll() {
		return Stream.of(this.owner);
	}

	@Override
	public Stream<LayerNode> getLayers() {
		return Stream.of(this.owner);
	}

	@Override
	public boolean dependsOn(LayerNode layer) {
		return this.owner == layer;
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> filter) {
		return filter.test(this.owner);
	}

	@Override
	public ObservableBooleanValue animatedProperty() {
		return this.owner.animation.animated;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return Stream.empty();
	}

	@Override
	public Parent getConfigPane() {
		return null;
	}
}