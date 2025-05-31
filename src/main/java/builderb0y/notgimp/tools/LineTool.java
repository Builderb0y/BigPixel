package builderb0y.notgimp.tools;

import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.input.MouseButton;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.ManualLayerSource;

public class LineTool extends Tool<LineTool.Work> {

	public static final ToolType TYPE = new ToolType("line", 9.0D, 9.0D);

	public Spinner<Double> radius;

	public LineTool(ManualLayerSource source) {
		super(TYPE, source);
		this.radius = Util.setupSpinner(new Spinner<>(0.0D, Double.MAX_VALUE, 0.0D, 0.5D), 80);
		this.radius.valueProperty().addListener(
			Util.change(() -> {
				if (this.work != null) {
					this.requestRedraw();
				}
			})
		);
	}

	public static int square(int a, int b) { return a * a + b * b; }
	public static double square(double n) { return n * n; }

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		Work work = this.work;
		if (work != null) {
			work.prevX = x;
			work.prevY = y;
			int dist1 = square(x - work.x1, y - work.y1);
			int dist2 = square(x - work.x2, y - work.y2);
			double maxDist = Math.max(square(this.radius.getValue()), 0.25D);
			if (dist1 < maxDist) {
				if (dist2 < maxDist) {
					work.moving2 = dist2 <= dist1;
				}
				else {
					work.moving2 = false;
				}
			}
			else {
				if (dist2 < maxDist) {
					work.moving2 = true;
				}
				else {
					this.enter();
					this.work = new Work(x, y);
				}
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
		if (work.moving2) {
			work.x2 += x - work.prevX;
			work.y2 += y - work.prevY;
		}
		else {
			work.x1 += x - work.prevX;
			work.y1 += y - work.prevY;
		}
		work.prevX = x;
		work.prevY = y;
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
		double radius = this.radius.getValue();
		int width = layer.image.width;
		int height = layer.image.height;
		ColorHelper color = layer.openImage.mainWindow.colorPicker.currentColor;
		if (radius == 0.0D) {
			if (work.x1 == work.x2 && work.y1 == work.y2) {
				if (work.x1 >= 0 && work.x1 < width && work.y1 >= 0 && work.y1 < height) {
					layer.image.setColor(work.x1, work.y1, color);
				}
			}
			else {
				int steps = Math.max(Math.abs(work.x2 - work.x1), Math.abs(work.y2 - work.y1));
				double
					x1 = work.x1 + 0.5D,
					y1 = work.y1 + 0.5D,
					x2 = work.x2 + 0.5D,
					y2 = work.y2 + 0.5D;
				for (int step = 0; step <= steps; step++) {
					double frac = ((double)(step)) / ((double)(steps));
					int
						x = (int)(Math.floor((x2 - x1) * frac + x1)),
						y = (int)(Math.floor((y2 - y1) * frac + y1));
					if (x >= 0 && x < width && y >= 0 && y < height) {
						layer.image.setColor(x, y, color);
					}
				}
			}
		}
		else {
			if (work.x1 == work.x2 && work.y1 == work.y2) {
				int
					iRadius = (int)(radius),
					x1 = Math.max(work.x1 - iRadius, 0),
					y1 = Math.max(work.y1 - iRadius, 0),
					x2 = Math.min(work.x1 + iRadius, width - 1),
					y2 = Math.min(work.y1 + iRadius, height - 1);
				for (int y = y1; y <= y2; y++) {
					for (int x = x1; x <= x2; x++) {
						double offsetX = x - work.x1;
						double offsetY = y - work.y1;
						if (offsetX * offsetX + offsetY * offsetY < radius * radius) {
							layer.image.setColor(x, y, color);
						}
					}
				}
			}
			else {
				double normX = work.x2 - work.x1;
				double normY = work.y2 - work.y1;
				double length = Math.sqrt(normX * normX + normY * normY);
				{
					double scalar = 1.0D / length;
					normX *= scalar;
					normY *= scalar;
				}
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						double
							offsetX = x - work.x1,
							offsetY = y - work.y1,
							dot = Math.clamp(offsetX * normX + offsetY * normY, 0.0D, length),
							closestX = normX * dot,
							closestY = normY * dot,
							projectionX = offsetX - closestX,
							projectionY = offsetY - closestY;
						if (projectionX * projectionX + projectionY * projectionY < radius * radius) {
							layer.image.setColor(x, y, color);
						}
					}
				}
			}
		}
		this.updateLabelText();
	}

	@Override
	public Node getConfiguration() {
		return this.radius;
	}

	@Override
	public void updateLabelText() {
		if (this.work != null) {
			this.labelText.set(
				"Drawing line from (" +
				this.work.x1 +
				", " +
				this.work.y1 +
				") to (" +
				this.work.x2 +
				", " +
				this.work.y2 +
				')'
			);
		}
		else {
			this.labelText.set("Line tool inactive");
		}
	}

	public static class Work {

		public int x1, y1, x2, y2;
		public int prevX, prevY;
		public boolean moving2;

		public Work() {}

		public Work(int x, int y) {
			this.x1 = this.x2 = this.prevX = x;
			this.y1 = this.y2 = this.prevY = y;
			this.moving2 = true;
		}
	}
}