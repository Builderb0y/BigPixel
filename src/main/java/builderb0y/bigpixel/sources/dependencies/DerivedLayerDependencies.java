package builderb0y.bigpixel.sources.dependencies;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Parent;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.DerivedLayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;

public class DerivedLayerDependencies extends LayerDependencies {

	public DerivedLayerSource source;
	public Map<String, LayerNode> potentialDependencies = new HashMap<>();
	public Set<LayerNode> actualDependencies = Collections.emptySet();
	public List<CurveHelper> curves = new ArrayList<>();
	public SimpleBooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);
	public boolean directlyAnimated;

	@Override
	public JsonMap save() {
		return new JsonMap();
	}

	@Override
	public void load(JsonMap saveData) {

	}

	public DerivedLayerDependencies(DerivedLayerSource source) {
		this.source = source;
	}

	public void setActualDependencies(Set<LayerNode> dependencies, boolean directlyAnimated) {
		this.actualDependencies = dependencies;
		boolean animated = directlyAnimated;
		int index = 0;
		for (LayerNode dependency : dependencies) {
			if (!animated) animated = dependency.sources.currentSource().getDependencies().animatedProperty().get();
			CurveHelper curve;
			if (this.curves.size() > index) {
				curve = this.curves.get(index);
			}
			else {
				curve = new CurveHelper(this.source.sources.layer, CurveHelper.NORMAL);
				this.curves.add(curve);
				curve.selfSourceIsSelected.set(true);
				curve.otherBindingIsVarying.set(true);
			}
			curve.setOtherEnd(dependency);
			index++;
		}
		this.directlyAnimated = directlyAnimated;
		this.animated.set(animated);
		while (this.curves.size() > index) {
			this.curves.removeLast().otherBindingIsVarying.set(false);
		}
	}

	@Override
	public void retainAll(List<LayerNode> layers) {
		this.potentialDependencies.clear();
		for (LayerNode layer : layers) {
			this.potentialDependencies.put(layer.getDisplayName(), layer);
		}
		this.source.recompile();
	}

	@Override
	public Stream<SamplerProvider> getAll() {
		return this.actualDependencies.stream().map(Function.identity());
	}

	@Override
	public boolean dependsOn(LayerNode layer) {
		return this.actualDependencies.contains(layer);
	}

	@Override
	public ObservableBooleanValue animatedProperty() {
		return this.animated;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return this.curves.stream();
	}

	@Override
	public Parent getConfigPane() {
		return null;
	}
}