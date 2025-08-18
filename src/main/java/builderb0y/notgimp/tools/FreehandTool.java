package builderb0y.notgimp.tools;

import java.util.HashSet;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.Assets;
import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.LayerNode;
import builderb0y.notgimp.sources.ManualLayerSource;

public class FreehandTool extends Tool<FreehandTool.Work> {

	public static final ToolType TYPE = new ToolType("freehand", Assets.Tools.FREEHAND, 9.0D, 9.0D);

	public FreehandTool(ManualLayerSource source) {
		super(TYPE, source);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		if (this.work == null) {
			this.source.beginUsingTool();
			this.work = new Work();
		}
		this.handleMouse(x, y, button);
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		this.handleMouse(x, y, button);
	}

	public void handleMouse(int x, int y, MouseButton button) {
		LayerNode layer = this.layer();
		if (x >= 0 && x < layer.image.width && y >= 0 && y < layer.image.height) {
			if (button == MouseButton.PRIMARY) {
				this.work.points.add(new Point(x, y));
				layer.requestRedraw();
				this.updateLabelText();
			}
			else if (button == MouseButton.SECONDARY) {
				this.work.points.remove(new Point(x, y));
				layer.requestRedraw();
				this.updateLabelText();
			}
		}
	}

	@Override
	public void colorChanged() {
		if (this.work != null) this.requestRedraw();
	}

	@Override
	public void redraw() {
		Work work = this.work;
		if (work == null) return;
		LayerNode layer = this.layer();
		ColorHelper color = layer.graph.openImage.mainWindow.colorPicker.currentColor;
		for (Point point : work.points) {
			layer.image.setColor(point.x, point.y, color);
		}
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

	public static class Work {

		public Set<Point> points = new HashSet<>();
	}

	public static record Point(int x, int y) {}
}