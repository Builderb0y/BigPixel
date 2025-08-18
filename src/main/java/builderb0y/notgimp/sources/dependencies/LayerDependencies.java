package builderb0y.notgimp.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.scene.Parent;

import builderb0y.notgimp.LayerNode;
import builderb0y.notgimp.json.JsonMap;

public abstract class LayerDependencies {

	public abstract JsonMap save();

	public abstract void load(JsonMap saveData);

	public abstract void retainAll(List<LayerNode> layers);

	public abstract boolean dependsOn(LayerNode layer);

	public abstract boolean containsAny(Predicate<LayerNode> layers);

	public abstract Stream<CurveHelper> getCurves();

	public boolean isAnimated() {
		return false;
	}

	public abstract Parent getConfigPane();
}