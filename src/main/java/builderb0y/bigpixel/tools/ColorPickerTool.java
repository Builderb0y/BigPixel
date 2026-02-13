package builderb0y.bigpixel.tools;

import javafx.scene.input.MouseButton;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.OpenImage;
import builderb0y.bigpixel.tools.ColorPickerTool.ColorPickerCallback;
import builderb0y.bigpixel.tools.Tool.ToolType;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;

public class ColorPickerTool extends SourcelessTool<ColorPickerCallback> {

	public static final ToolType TYPE = new ToolType("color_picker", Assets.Tools.COLOR_PICKER, 1.0D, 23.0D);

	public final OpenImage openImage;

	public ColorPickerTool(OpenImage openImage) {
		super(TYPE);
		this.openImage = openImage;
	}

	@Override
	public void mouseDown(ProjectionResult position, MouseButton button) {
		if (this.work == null) {
			this.escape();
			return;
		}
		this.work.onColorPicked(
			position.rawR(),
			position.rawG(),
			position.rawB(),
			position.rawA()
		);
	}

	@Override
	public void mouseDragged(ProjectionResult position, MouseButton button) {
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