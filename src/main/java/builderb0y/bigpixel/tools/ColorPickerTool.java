package builderb0y.bigpixel.tools;

import javafx.scene.input.MouseButton;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OpenImage;
import builderb0y.bigpixel.tools.ColorPickerTool.ColorPickerCallback;
import builderb0y.bigpixel.tools.Tool.ToolType;

public class ColorPickerTool extends SourcelessTool<ColorPickerCallback> {

	public static final ToolType TYPE = new ToolType("color_picker", Assets.Tools.COLOR_PICKER, 1.0D, 23.0D);

	public final OpenImage openImage;

	public ColorPickerTool(OpenImage openImage) {
		super(TYPE);
		this.openImage = openImage;
	}

	@Override
	public void mouseDown(int x, int y, LayerNode layer, MouseButton button) {
		if (this.work == null) {
			this.escape();
			return;
		}
		HDRImage image = layer.getFrame();
		if (x >= 0 && x < image.width && y >= 0 && y < image.height) {
			int baseIndex = image.baseIndex(x, y);
			this.work.onColorPicked(
				image.pixels[baseIndex | HDRImage.  RED_OFFSET],
				image.pixels[baseIndex | HDRImage.GREEN_OFFSET],
				image.pixels[baseIndex | HDRImage. BLUE_OFFSET],
				image.pixels[baseIndex | HDRImage.ALPHA_OFFSET]
			);
		}
	}

	@Override
	public void mouseDragged(int x, int y, LayerNode layer, MouseButton button) {
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