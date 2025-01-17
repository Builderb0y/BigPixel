package builderb0y.notgimp.tools;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.ColorSelector;
import builderb0y.notgimp.Layer;

public class ColorPickerTool extends Tool<Tool.Work> {

	public static final ToolType TYPE = new ToolType("color_picker", 1.0D, 23.0D, Tools::colorPickerTool);

	public ColorPickerTool() {
		super(TYPE);
	}

	@Override
	public void mouseDown(Layer layer, int x, int y, MouseButton button) {
		ColorHelper color = layer.openImage.colorPicker.currentColor;
		color.setRed  (ColorSelector.clamp(layer.image.getRed  (x, y)));
		color.setGreen(ColorSelector.clamp(layer.image.getGreen(x, y)));
		color.setBlue (ColorSelector.clamp(layer.image.getBlue (x, y)));
		color.setAlpha(ColorSelector.clamp(layer.image.getAlpha(x, y)));
		color.markDirty();
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		//no-op.
	}

	@Override
	public void colorChanged() {
		//no-op.
	}

	@Override
	public void enter(Layer layer) {
		//no-op.
	}

	@Override
	public void escape(Layer layer) {
		layer.openImage.mainWindow.currentTool.set(null);
	}

	@Override
	public Node getConfiguration() {
		return null;
	}

	@Override
	public void updateLabelText() {
		this.labelText.set("Color picker tool active");
	}
}