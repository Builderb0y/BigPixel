package builderb0y.bigpixel.sources.dependencies;

import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class MainDependencies extends NamedLayerDependencies {

	public UnmovableInputBinding main = this.addBinding("main", "Input: ", CurveHelper.NORMAL);

	public MainDependencies(LayerSource source) {
		super(source);
	}
}