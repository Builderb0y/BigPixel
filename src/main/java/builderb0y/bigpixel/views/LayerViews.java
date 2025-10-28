package builderb0y.bigpixel.views;

import javafx.scene.canvas.Canvas;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.Util;

public class LayerViews extends OrganizedSelection<LayerView, LayerView.Type, LayerView.Category> {

	public LayerViews(LayerNode layer) {
		super(layer, LayerView.Type.class, LayerView.Category.class);
	}

	public void init() {
		this.selectedValue.addListener(Util.change(this.layer.graph.openImage.imageDisplay::redrawLater));
	}

	@Override
	public LayerView createValue(LayerView.Type type) {
		LayerView value = super.createValue(type);
		value.setPossibleDependencies(this.layer.graph.layerList);
		Canvas canvas = this.layer.graph.openImage.imageDisplay.display.display;
		if (canvas.getWidth() > 0.0D && canvas.getHeight() > 0.0D) {
			value.beforeRedraw(canvas, this.layer);
			value.center();
		}
		return value;
	}
}