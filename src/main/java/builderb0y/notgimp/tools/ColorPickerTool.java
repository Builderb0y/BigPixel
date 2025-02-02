package builderb0y.notgimp.tools;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.*;
import builderb0y.notgimp.sources.ManualLayerSource;

public class ColorPickerTool extends Tool<Void> {

	public static final ToolType TYPE = new ToolType("color_picker", 1.0D, 23.0D);

	public ColorPickerTool(ManualLayerSource source) {
		super(TYPE, source);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		Layer layer = this.layer();
		ColorHelper color = layer.openImage.mainWindow.colorPicker.currentColor;
		int baseIndex = layer.image.baseIndex(x, y);
		color.setRed  (Util.clampF(layer.image.pixels[baseIndex | HDRImage.  RED_OFFSET]));
		color.setGreen(Util.clampF(layer.image.pixels[baseIndex | HDRImage.GREEN_OFFSET]));
		color.setBlue (Util.clampF(layer.image.pixels[baseIndex | HDRImage. BLUE_OFFSET]));
		color.setAlpha(Util.clampF(layer.image.pixels[baseIndex | HDRImage.ALPHA_OFFSET]));
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
	public void enter() {
		//no-op.
	}

	@Override
	public void escape() {
		this.source.currentTool.set(null);
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