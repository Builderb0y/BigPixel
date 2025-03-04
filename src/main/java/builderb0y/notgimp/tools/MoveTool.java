package builderb0y.notgimp.tools;

import java.util.Arrays;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.sources.ManualLayerSource;

public class MoveTool extends Tool<MoveTool.Work> {

	public static final ToolType TYPE = new ToolType("move", 12.0D, 12.0D);

	public CheckBox fill = new CheckBox("Fill");

	public MoveTool(ManualLayerSource source) {
		super(TYPE, source);
		this.fill.selectedProperty().addListener(
			(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
				if (this.work != null) {
					this.requestRedraw();
					this.updateLabelText();
				}
			}
		);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		Work work = this.work;
		if (work != null) {
			Selection selection = new Selection();
			this.transformWorkPos(selection);
			BoundaryPosition moving = BoundaryPosition.get(
				x, y,
				selection.minX, selection.minY,
				selection.maxX, selection.maxY
			);
			if (moving == BoundaryPosition.OUTSIDE) {
				this.enter();
				this.work = new Work(x, y);
			}
			else {
				work.prevX = x;
				work.prevY = y;
				work.moving = BoundaryPosition.INSIDE;
			}
		}
		else {
			this.source.beginUsingTool();
			this.work = new Work(x, y);
			this.requestRedraw();
			this.updateLabelText();
		}
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		Work work = this.work;
		switch (work.moving) {
			case CORNER_X1_Y1  -> { work.x1 = x; work.y1 = y; }
			case CORNER_X1_Y2  -> { work.x1 = x; work.y2 = y; }
			case CORNER_X2_Y1  -> { work.x2 = x; work.y1 = y; }
			case CORNER_X2_Y2  -> { work.x2 = x; work.y2 = y; }
			case EDGE_X1       -> { work.x1 = x; }
			case EDGE_Y1       -> { work.y1 = y; }
			case EDGE_X2       -> { work.x2 = x; }
			case EDGE_Y2       -> { work.y2 = y; }
			case INSIDE -> {
				work.offsetX += x - work.prevX;
				work.offsetY += y - work.prevY;
				work.prevX = x;
				work.prevY = y;
			}
			case OUTSIDE -> throw new IllegalStateException(work.moving.toString());
		}
		this.requestRedraw();
		this.updateLabelText();
	}

	@Override
	public void colorChanged() {
		if (this.work != null) {
			this.requestRedraw();
		}
	}

	@Override
	public void redraw() {
		Work work = this.work;
		if (work == null) return;
		HDRImage toImage = this.layer().image;
		HDRImage fromImage = this.source.toollessImage;
		int minX = Math.min(work.x1, work.x2);
		int minY = Math.min(work.y1, work.y2);
		int maxX = Math.max(work.x1, work.x2);
		int maxY = Math.max(work.y1, work.y2);
		int offsetX = work.offsetX;
		int offsetY = work.offsetY;
		if (this.fill.isSelected()) {
			ColorHelper color = this.layer().openImage.mainWindow.colorPicker.currentColor;
			for (int fromY = minY; fromY <= maxY; fromY++) {
				for (int fromX = minX; fromX <= maxX; fromX++) {
					if (fromX >= 0 && fromX < toImage.width && fromY >= 0 && fromY < toImage.height) {
						toImage.setColor(fromX, fromY, color);
					}
				}
			}
		}
		for (int fromY = minY; fromY <= maxY; fromY++) {
			for (int fromX = minX; fromX <= maxX; fromX++) {
				double
					centerX = (minX + maxX) * 0.5D,
					centerY = (minY + maxY) * 0.5D,
					relativeX = fromX - centerX,
					relativeY = fromY - centerY,
					symmetricX = work.symmetry.getX(relativeX, relativeY),
					symmetricY = work.symmetry.getY(relativeX, relativeY),
					transformedX = symmetricX + centerX + offsetX,
					transformedY = symmetricY + centerY + offsetY;
				int
					toX = (int)(Math.floor(transformedX)),
					toY = (int)(Math.floor(transformedY));
				if (toX >= 0 && toX < toImage.width && toY >= 0 && toY < toImage.height) {
					if (fromX >= 0 && fromX < fromImage.width && fromY >= 0 && fromY < fromImage.height) {
						int fromIndex = fromImage.baseIndex(fromX, fromY);
						int toIndex = toImage.baseIndex(toX, toY);
						System.arraycopy(fromImage.pixels, fromIndex, toImage.pixels, toIndex, 4);
					}
					else {
						int toIndex = toImage.baseIndex(toX, toY);
						Arrays.fill(toImage.pixels, toIndex, toIndex + 4, 0.0F);
					}
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyCode key) {
		switch (key) {
			case Q -> this.symmetrify(Symmetry.ROTATE_180);
			case E -> this.symmetrify(Symmetry.ROTATE_CCW);
			case R -> this.symmetrify(Symmetry.ROTATE_CW);
			case F, H -> this.symmetrify(Symmetry.FLIP_H);
			case V -> this.symmetrify(Symmetry.FLIP_V);
			case T -> this.symmetrify(Symmetry.FLIP_L);
			case Y -> this.symmetrify(Symmetry.FLIP_R);
			case C -> this.copyInPlace();
			default -> super.keyPressed(key);
		}
	}

	public void symmetrify(Symmetry symmetry) {
		if (this.work != null) {
			this.work.symmetry = this.work.symmetry.andThen(symmetry);
			this.requestRedraw();
			this.updateLabelText();
		}
	}

	@Override
	public void enter() {
		boolean hadWork = this.work != null;
		super.enter();
		if (hadWork) this.requestRedraw();
	}

	public void copyInPlace() {
		Work work = this.work;
		if (work != null) {
			super.enter();
			this.work = new Work();
			this.work.x1 = work.x1 + work.offsetX;
			this.work.x2 = work.x2 + work.offsetX;
			this.work.y1 = work.y1 + work.offsetY;
			this.work.y2 = work.y2 + work.offsetY;
			this.requestRedraw();
		}
	}

	public boolean transformWorkPos(Selection selection) {
		Work work = this.work;
		if (work == null) return false;
		double
			centerX = (work.x1 + work.x2) * 0.5D,
			centerY = (work.y1 + work.y2) * 0.5D,
			relativeX = work.x1 - centerX,
			relativeY = work.y1 - centerY,
			symmetricX = work.symmetry.getX(relativeX, relativeY),
			symmetricY = work.symmetry.getY(relativeX, relativeY),
			transformedX = symmetricX + centerX + work.offsetX,
			transformedY = symmetricY + centerY + work.offsetY;
		selection.minX = (int)(Math.floor(transformedX));
		selection.minY = (int)(Math.floor(transformedY));
		relativeX = work.x2 - centerX;
		relativeY = work.y2 - centerY;
		symmetricX = work.symmetry.getX(relativeX, relativeY);
		symmetricY = work.symmetry.getY(relativeX, relativeY);
		transformedX = symmetricX + centerX + work.offsetX;
		transformedY = symmetricY + centerY + work.offsetY;
		selection.maxX = (int)(Math.floor(transformedX));
		selection.maxY = (int)(Math.floor(transformedY));
		return true;
	}

	@Override
	public boolean getSelection(Selection selection) {
		if (!this.transformWorkPos(selection)) return false;
		if (selection.minX > selection.maxX) {
			int tmp = selection.minX;
			selection.minX = selection.maxX;
			selection.maxX = tmp;
		}
		if (selection.minY > selection.maxY) {
			int tmp = selection.minY;
			selection.minY = selection.maxY;
			selection.maxY = tmp;
		}
		return true;
	}

	@Override
	public @Nullable Node getConfiguration() {
		return this.fill;
	}

	@Override
	public void updateLabelText() {
		Work work = this.work;
		if (work != null) {
			if (work.moving == BoundaryPosition.CORNER_X2_Y2) {
				int x2 = Math.max(work.x1, work.x2);
				int x1 = Math.min(work.x1, work.x2);
				int y2 = Math.max(work.y1, work.y2);
				int y1 = Math.min(work.y1, work.y2);
				this.labelText.set(
					"Selecting " +
					(x2 - x1 + 1) +
					'x' +
					(y2 - y1 + 1) +
					" area from (" +
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
				StringBuilder builder = new StringBuilder(32);
				if (this.fill.isSelected()) {
					builder.append("Moving by (");
				}
				else {
					builder.append("Copying by (");
				}
				builder
				.append(work.offsetX)
				.append(", ")
				.append(work.offsetY)
				.append(')');

				if (work.symmetry != Symmetry.IDENTITY) {
					builder.append(work.symmetry);
				}
				this.labelText.set(builder.toString());
			}
		}
		else {
			this.labelText.set("Multi-tool inactive");
		}
	}

	public static class Work {

		public int x1, x2, y1, y2;
		public int prevX, prevY, offsetX, offsetY;
		public BoundaryPosition moving;
		public Symmetry symmetry = Symmetry.IDENTITY;

		public Work() {}

		public Work(int x, int y) {
			this.x1 = this.x2 = x;
			this.y1 = this.y2 = y;
			this.moving = BoundaryPosition.CORNER_X2_Y2;
		}
	}
}