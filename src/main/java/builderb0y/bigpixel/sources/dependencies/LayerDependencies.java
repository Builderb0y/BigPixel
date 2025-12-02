package builderb0y.bigpixel.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Parent;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;

public abstract class LayerDependencies {

	public abstract JsonMap save();

	public abstract void load(JsonMap saveData);

	public abstract void retainAll(List<LayerNode> layers);

	public abstract Stream<SamplerProvider> getAll();

	public Stream<LayerNode> getLayers() {
		return (
			this
			.getAll()
			.filter(VaryingSamplerProvider.class::isInstance)
			.map((SamplerProvider supplier) -> (
				((VaryingSamplerProvider)(supplier)).getBackingLayer()
			))
		);
	}

	public boolean dependsOn(LayerNode layer) {
		return this.getLayers().anyMatch(layer::equals);
	}

	public boolean containsAny(Predicate<LayerNode> filter) {
		return this.getLayers().anyMatch(filter);
	}

	public abstract ObservableBooleanValue animatedProperty();

	public abstract Stream<CurveHelper> getCurves();

	public abstract Parent getConfigPane();
}