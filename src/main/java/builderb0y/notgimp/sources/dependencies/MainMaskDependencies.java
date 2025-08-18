package builderb0y.notgimp.sources.dependencies;

import builderb0y.notgimp.sources.LayerSource;
import builderb0y.notgimp.sources.dependencies.inputs.UnmovableInputBinding;

public class MainMaskDependencies extends NamedLayerDependencies {

	public UnmovableInputBinding
		main = this.addBinding("main", "Input: "),
		mask = this.addBinding("mask", "Mask: ");

	public MainMaskDependencies(LayerSource source) {
		super(source);
	}
}