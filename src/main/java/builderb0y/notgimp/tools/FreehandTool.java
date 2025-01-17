package builderb0y.notgimp.tools;

import java.util.HashSet;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.Layer;

public class FreehandTool extends Tool<FreehandTool.Work> {

	public static final ToolType TYPE = new ToolType("freehand", 9.0D, 9.0D, Tools::freehandTool);

	public FreehandTool() {
		super(TYPE);
	}

	@Override
	public void mouseDown(Layer layer, int x, int y, MouseButton button) {
		if (this.work == null) {
			layer.beginUsingTool();
			this.work = new Work(layer);
		}
		this.handleMouse(x, y, button);
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		this.handleMouse(x, y, button);
	}

	public void handleMouse(int x, int y, MouseButton button) {
		Layer layer = this.work.layer;
		if (x >= 0 && x < layer.image.width && y >= 0 && y < layer.image.height) {
			if (button == MouseButton.PRIMARY) {
				this.work.points.add(new Point(x, y));
				layer.image.setColor(x, y, layer.openImage.colorPicker.currentColor);
				layer.image.markDirty();
				this.updateLabelText();
			}
			else if (button == MouseButton.SECONDARY) {
				this.work.points.remove(new Point(x, y));
				int index = layer.image.baseIndex(x, y);
				System.arraycopy(layer.toollessImage.pixels, index, layer.image.pixels, index, 4);
				layer.image.markDirty();
				this.updateLabelText();
			}
		}
	}

	@Override
	public void colorChanged() {
		if (this.work != null) this.redraw();
	}

	public void redraw() {
		Work work = this.work;
		work.layer.beforeToolChanged();
		ColorHelper color = work.layer.openImage.colorPicker.currentColor;
		for (Point point : work.points) {
			work.layer.image.setColor(point.x, point.y, color);
		}
		work.layer.image.markDirty();
	}

	@Override
	public void enter(Layer layer) {
		super.enter(layer);
	}

	@Override
	public Node getConfiguration() {
		return null;
	}

	@Override
	public void updateLabelText() {
		if (this.work != null) {
			this.labelText.set("Drawing " + this.work.points.size() + " pixel(s) freehand");
		}
		else {
			this.labelText.set("Freehand tool inactive");
		}
	}

	public static class Work extends Tool.Work {

		public Set<Point> points = new HashSet<>();

		public Work(Layer layer) {
			super(layer);
		}
	}

	public static record Point(int x, int y) {}
}