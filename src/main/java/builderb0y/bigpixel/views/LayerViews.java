package builderb0y.bigpixel.views;

import javafx.scene.canvas.Canvas;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView.LayerViewCategory;
import builderb0y.bigpixel.views.LayerView.LayerViewType;

public class LayerViews extends OrganizedSelection<LayerView, LayerViewType, LayerViewCategory> {

	public LayerViews(LayerNode layer) {
		super(layer, LayerViewType.class, LayerViewCategory.class);
	}

	public void init() {

	}

	@Override
	public LayerView createValue(LayerViewType type) {
		LayerView value = super.createValue(type);
		value.setPossibleDependencies(this.layer.graph.layerList);
		Canvas canvas = this.layer.graph.openImage.imageDisplay.canvasHolder.display;
		int width  = (int)(canvas.getWidth());
		int height = (int)(canvas.getHeight());
		if (width > 0 && height > 0) {
			value.beforeRedraw(this.layer.imageWidth(), this.layer.imageHeight(), width, height);
			value.center();
		}
		return value;
	}

	public LayerView currentView() {
		return this.selectedValue.get();
	}
}