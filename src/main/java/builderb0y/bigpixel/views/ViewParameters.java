package builderb0y.bigpixel.views;

import builderb0y.bigpixel.ConfigParameters;
import builderb0y.bigpixel.util.Util;

public class ViewParameters extends ConfigParameters {

	public ViewParameters(LayerView view) {
		this.parameterListener = Util.change(() -> view.views.layer.graph.openImage.imageDisplay.redrawLater());
	}
}