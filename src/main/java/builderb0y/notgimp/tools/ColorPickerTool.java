package builderb0y.notgimp.tools;

import javafx.scene.input.MouseButton;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.OpenImage;
import builderb0y.notgimp.tools.ColorPickerTool.ColorPickerCallback;
import builderb0y.notgimp.tools.Tool.ToolType;

public class ColorPickerTool extends SourcelessTool<ColorPickerCallback> {

	public static final ToolType TYPE = new ToolType("color_picker", 1.0D, 23.0D);

	public final OpenImage openImage;

	public ColorPickerTool(OpenImage openImage) {
		super(TYPE);
		this.openImage = openImage;
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		if (this.work == null) {
			this.escape();
			return;
		}
		Layer layer = this.openImage.getVisibleLayer();
		int baseIndex = layer.image.baseIndex(x, y);
		this.work.onColorPicked(
			layer.image.pixels[baseIndex | HDRImage.  RED_OFFSET],
			layer.image.pixels[baseIndex | HDRImage.GREEN_OFFSET],
			layer.image.pixels[baseIndex | HDRImage. BLUE_OFFSET],
			layer.image.pixels[baseIndex | HDRImage.ALPHA_OFFSET]
		);
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
		this.openImage.stopPickingColor();
	}

	@FunctionalInterface
	public static interface ColorPickerCallback {

		public abstract void onColorPicked(float red, float green, float blue, float alpha);
	}
}