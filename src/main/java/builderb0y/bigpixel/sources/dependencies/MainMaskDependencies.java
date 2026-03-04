package builderb0y.bigpixel.sources.dependencies;

import javafx.scene.control.Button;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class MainMaskDependencies extends NamedLayerDependencies {

	public UnmovableInputBinding
		main = this.addBinding("main", "Input: ", CurveHelper.NORMAL),
		mask = this.addBinding("mask", "Mask: ",  CurveHelper.MASK);

	public MainMaskDependencies(LayerSource source) {
		super(source);
		LayerNode layer = source.sources.layer;
		int index = layer.graph.indexOfPosition(layer.getGridX() - 1, layer.getGridY());
		if (index >= 0) this.main.selection.setValue(layer.graph.layerList.get(index));
	}

	public void addBoundsHandlingButton(Button button) {
		this.gridPane.add(button, 3, 0);
	}
}