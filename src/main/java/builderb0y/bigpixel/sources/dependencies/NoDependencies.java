package builderb0y.bigpixel.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Parent;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.util.Constant.ConstantBoolean;

public class NoDependencies extends LayerDependencies {

	public static final NoDependencies INSTANCE = new NoDependencies();

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
		return Stream.empty();
	}

	@Override
	public Stream<LayerNode> getLayers() {
		return Stream.empty();
	}

	@Override
	public boolean dependsOn(LayerNode layer) {
		return false;
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> filter) {
		return false;
	}

	@Override
	public ObservableBooleanValue animatedProperty() {
		return ConstantBoolean.FALSE;
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