package builderb0y.bigpixel.sources.dependencies;

import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class MainMaskDependencies extends NamedLayerDependencies {

	public UnmovableInputBinding
		main = this.addBinding("main", "Input: "),
		mask = this.addBinding("mask", "Mask: ");

	public MainMaskDependencies(LayerSource source) {
		super(source);
	}
}