package builderb0y.notgimp.tools;

import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.Assets;
import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.ManualLayerSource;

public class RectangleTool extends Tool<RectangleTool.Work> {

	public static final ToolType TYPE = new ToolType("rectangle", Assets.Tools.RECTANGLE, 9.0D, 9.0D);

	public Spinner<Integer> thickness;

	public RectangleTool(ManualLayerSource source) {
		super(TYPE, source);
		this.thickness = Util.setupSpinner(new Spinner<>(0, Integer.MAX_VALUE, 1), 64);
		this.thickness.valueProperty().addListener(
			Util.change(() -> {
				if (this.work != null) {
					this.requestRedraw();
				}
			})
		);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		Work work = this.work;
		if (work != null) {
			BoundaryPosition moving = BoundaryPosition.get(
				x, y,
				work.x1, work.y1,
				work.x2, work.y2
			);
			if (moving == BoundaryPosition.OUTSIDE) {
				this.enter();
				this.work = new Work(x, y);
			}
			else {
				work.moving = moving;
				work.prevX = x;
				work.prevY = y;
			}
		}
		else {
			this.source.beginUsingTool();
			this.work = new Work(x, y);
		}
		this.requestRedraw();
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		Work work = this.work;
		switch (work.moving) {
			case CORNER_X1_Y1 -> { work.x1 = x; work.y1 = y; }
			case CORNER_X1_Y2 -> { work.x1 = x; work.y2 = y; }
			case CORNER_X2_Y1 -> { work.x2 = x; work.y1 = y; }
			case CORNER_X2_Y2 -> { work.x2 = x; work.y2 = y; }
			case EDGE_X1      -> { work.x1 = x; }
			case EDGE_Y1      -> { work.y1 = y; }
			case EDGE_X2      -> { work.x2 = x; }
			case EDGE_Y2      -> { work.y2 = y; }
			case INSIDE       -> {
				work.x1 += x - work.prevX;
				work.y1 += y - work.prevY;
				work.x2 += x - work.prevX;
				work.y2 += y - work.prevY;
				work.prevX = x;
				work.prevY = y;
			}
			case OUTSIDE -> throw new IllegalStateException(work.moving.toString());
		}
		this.requestRedraw();
	}

	@Override
	public void colorChanged() {
		if (this.work != null) this.requestRedraw();
	}

	@Override
	public void redraw() {
		Work work = this.work;
		if (work == null) return;
		Layer layer = this.layer();
		int
			thickness = this.thickness.getValue(),
			width = layer.image.width,
			height = layer.image.height,
			rectMinX = work.minX(),
			rectMinY = work.minY(),
			rectMaxX = work.maxX(),
			rectMaxY = work.maxY(),
			innerMinX = rectMinX + thickness,
			innerMinY = rectMinY + thickness,
			innerMaxX = rectMaxX - thickness,
			innerMaxY = rectMaxY - thickness,
			drawMinX = Math.max(rectMinX, 0),
			drawMinY = Math.max(rectMinY, 0),
			drawMaxX = Math.min(rectMaxX, width - 1),
			drawMaxY = Math.min(rectMaxY, height - 1);
		ColorHelper color = layer.openImage.mainWindow.colorPicker.currentColor;
		if (thickness == 0 || Math.min(rectMaxX - rectMinX + 1, rectMaxY - rectMinY + 1) < thickness << 1) {
			for (int y = drawMinY; y <= drawMaxY; y++) {
				for (int x = drawMinX; x <= drawMaxX; x++) {
					layer.image.setColor(x, y, color);
				}
			}
		}
		else {
			for (int y = drawMinY; y <= drawMaxY; y++) {
				for (int x = drawMinX; x <= drawMaxX; x++) {
					if (x < innerMinX || x > innerMaxX || y < innerMinY || y > innerMaxY) {
						layer.image.setColor(x, y, color);
					}
				}
			}
		}
		this.updateLabelText();
	}

	@Override
	public Node getConfiguration() {
		return this.thickness;
	}

	@Override
	public void updateLabelText() {
		Work work = this.work;
		if (work != null) {
			int x2 = Math.max(work.x1, work.x2);
			int x1 = Math.min(work.x1, work.x2);
			int y2 = Math.max(work.y1, work.y2);
			int y1 = Math.min(work.y1, work.y2);
			this.labelText.set(
				"Drawing " +
				(x2 - x1 + 1) +
				'x' +
				(y2 - y1 + 1) +
				" rectangle from (" +
				x1 +
				", " +
				y1 +
				") to (" +
				x2 +
				", " +
				y2 +
				')'
			);
		}
		else {
			this.labelText.set("Rectangle tool inactive");
		}
	}

	public static class Work {

		public int x1, x2, y1, y2;
		public int prevX, prevY;
		public BoundaryPosition moving;

		public Work() {}

		public Work(int x, int y) {
			this.x1 = this.x2 = x;
			this.y1 = this.y2 = y;
			this.moving = BoundaryPosition.CORNER_X2_Y2;
		}

		public int minX() { return Math.min(this.x1, this.x2); }
		public int minY() { return Math.min(this.y1, this.y2); }
		public int maxX() { return Math.max(this.x1, this.x2); }
		public int maxY() { return Math.max(this.y1, this.y2); }
	}
}