package builderb0y.notgimp.sources.dependencies;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.scene.Parent;

import builderb0y.notgimp.LayerNode;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.sources.DerivedLayerSource;

public class DerivedLayerDependencies extends LayerDependencies {

	public DerivedLayerSource source;
	public Map<String, LayerNode> potentialDependencies = new HashMap<>();
	public Set<LayerNode> actualDependencies = Collections.emptySet();
	public List<CurveHelper> curves = new ArrayList<>();
	public boolean animated;

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

	public void setActualDependencies(Set<LayerNode> dependencies) {
		this.actualDependencies = dependencies;
		int index = 0;
		for (LayerNode dependency : dependencies) {
			CurveHelper curve;
			if (this.curves.size() > index) {
				curve = this.curves.get(index);
			}
			else {
				curve = new CurveHelper(this.source.sources.layer);
				this.curves.add(curve);
				curve.selfSourceIsSelected.set(true);
				curve.otherBindingIsVarying.set(true);
			}
			curve.setOtherEnd(dependency);
			index++;
		}
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
	public boolean dependsOn(LayerNode layer) {
		return this.actualDependencies.contains(layer);
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> layers) {
		for (LayerNode dependency : this.actualDependencies) {
			if (layers.test(dependency)) return true;
		}
		return false;
	}

	@Override
	public boolean isAnimated() {
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