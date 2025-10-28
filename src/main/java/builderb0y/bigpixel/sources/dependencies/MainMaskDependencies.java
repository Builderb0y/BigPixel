package builderb0y.bigpixel.sources.dependencies;

import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class MainMaskDependencies extends NamedLayerDependencies {

	public UnmovableInputBinding
		main = this.addBinding("main", "Input: ", CurveHelper.NORMAL),
		mask = this.addBinding("mask", "Mask: ",  CurveHelper.MASK);

	public MainMaskDependencies(LayerSource source) {
		super(source);
	}
}