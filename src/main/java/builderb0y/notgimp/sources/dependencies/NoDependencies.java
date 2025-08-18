package builderb0y.notgimp.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.scene.Parent;

import builderb0y.notgimp.LayerNode;
import builderb0y.notgimp.json.JsonMap;

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
	public boolean dependsOn(LayerNode layer) {
		return false;
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> layers) {
		return false;
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