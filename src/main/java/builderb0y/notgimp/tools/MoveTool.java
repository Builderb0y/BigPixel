package builderb0y.notgimp.tools;

import java.util.Arrays;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.*;
import builderb0y.notgimp.sources.AlphaLayerSource;
import builderb0y.notgimp.sources.ManualLayerSource;

public class MoveTool extends Tool<MoveTool.Work> {

	public static final ToolType TYPE = new ToolType("move", Assets.Tools.MOVE, 4.0D, 4.0D);

	public CheckBox
		fill = new CheckBox("Fill");
	public Button
		again     = this.button(Assets.Tools.Move.AGAIN, this::again),
		rotate90  = this.symmetrifyButton(Assets.Tools.Move.ROTATE_CW,  Symmetry.ROTATE_CW),
		rotate180 = this.symmetrifyButton(Assets.Tools.Move.ROTATE_180, Symmetry.ROTATE_180),
		rotate270 = this.symmetrifyButton(Assets.Tools.Move.ROTATE_CCW, Symmetry.ROTATE_CCW),
		flipH     = this.symmetrifyButton(Assets.Tools.Move.FLIP_H,     Symmetry.FLIP_H),
		flipV     = this.symmetrifyButton(Assets.Tools.Move.FLIP_V,     Symmetry.FLIP_V),
		flipL     = this.symmetrifyButton(Assets.Tools.Move.FLIP_L,     Symmetry.FLIP_L),
		flipR     = this.symmetrifyButton(Assets.Tools.Move.FLIP_R,     Symmetry.FLIP_R),
		extract   = this.button(Assets.Tools.Move.EXTRACT, this::extract);
	public GridPane
		buttons = new GridPane();
	public BorderPane
		rootPane = new BorderPane();

	public MoveTool(ManualLayerSource source) {
		super(TYPE, source);
		this.fill.selectedProperty().addListener(
			Util.change(() -> {
				if (this.work != null) {
					this.requestRedraw();
					this.updateLabelText();
				}
			})
		);
		this.buttons.addRow(0, this.again, this.rotate90, this.rotate180, this.rotate270);
		this.buttons.addRow(1, this.flipH, this.flipV, this.flipL, this.flipR);
		this.buttons.addRow(2, this.extract);
		this.rootPane.setTop(this.fill);
		this.rootPane.setCenter(this.buttons);
	}

	public Button symmetrifyButton(Image icon, Symmetry symmetry) {
		return this.button(icon, () -> this.symmetrify(symmetry));
	}

	public Button button(Image icon, Runnable action) {
		Button button = new Button(null, new ImageView(icon));
		button.setOnAction((ActionEvent _) -> action.run());
		return button;
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
				this.work = new Work(this.source.toollessImage, x, y);
			}
			else {
				work.prevX = x;
				work.prevY = y;
				work.moving = BoundaryPosition.INSIDE;
			}
		}
		else {
			this.source.beginUsingTool();
			this.work = new Work(this.source.toollessImage, x, y);
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
		if (this.work != null && this.fill.isSelected()) {
			this.requestRedraw();
		}
	}

	@Override
	public void redraw() {
		Work work = this.work;
		if (work == null) return;
		HDRImage to = this.layer().image;
		this.maybeFill(work, to);
		this.transfer(work, work.source, to);
	}

	public void maybeFill(Work work, HDRImage fillImage) {
		int minX = Math.min(work.x1, work.x2);
		int minY = Math.min(work.y1, work.y2);
		int maxX = Math.max(work.x1, work.x2);
		int maxY = Math.max(work.y1, work.y2);
		if (this.fill.isSelected()) {
			ColorHelper color = this.layer().openImage.mainWindow.colorPicker.currentColor;
			for (int fromY = minY; fromY <= maxY; fromY++) {
				for (int fromX = minX; fromX <= maxX; fromX++) {
					if (fromX >= 0 && fromX < fillImage.width && fromY >= 0 && fromY < fillImage.height) {
						fillImage.setColor(fromX, fromY, color);
					}
				}
			}
		}
	}

	public void transfer(Work work, HDRImage fromImage, HDRImage copyImage) {
		int minX = Math.min(work.x1, work.x2);
		int minY = Math.min(work.y1, work.y2);
		int maxX = Math.max(work.x1, work.x2);
		int maxY = Math.max(work.y1, work.y2);
		int offsetX = work.offsetX;
		int offsetY = work.offsetY;
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
				if (toX >= 0 && toX < copyImage.width && toY >= 0 && toY < copyImage.height) {
					if (fromX >= 0 && fromX < fromImage.width && fromY >= 0 && fromY < fromImage.height) {
						int fromIndex = fromImage.baseIndex(fromX, fromY);
						int toIndex = copyImage.baseIndex(toX, toY);
						System.arraycopy(fromImage.pixels, fromIndex, copyImage.pixels, toIndex, 4);
					}
					else {
						int toIndex = copyImage.baseIndex(toX, toY);
						Arrays.fill(copyImage.pixels, toIndex, toIndex + 4, 0.0F);
					}
				}
			}
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

	public void again() {
		Work work = this.work;
		if (work != null) {
			super.enter();
			this.work = new Work(this.source.toollessImage);
			this.work.x1 = work.x1 + work.offsetX;
			this.work.x2 = work.x2 + work.offsetX;
			this.work.y1 = work.y1 + work.offsetY;
			this.work.y2 = work.y2 + work.offsetY;
			this.requestRedraw();
		}
	}

	public void extract() {
		Work work = this.work;
		if (work == null) return;
		this.work = null;
		Layer layer = this.layer();
		System.arraycopy(this.source.toollessImage.pixels, 0, layer.image.pixels, 0, layer.image.pixels.length);
		TreeItem<Layer> item = layer.item;
		OpenImage image = layer.openImage;
		Layer newLayer;
		if (item.getParent() != null && item.getParent().getValue().sources.getCurrentSource() instanceof AlphaLayerSource) {
			newLayer = image.createLayerAbove(layer.item, "extract");
		}
		else {
			newLayer = image.createParentLayer(layer.item, "merge");
			newLayer.sources.choiceBox.getSelectionModel().select(newLayer.sources.alphaSource);
			newLayer = image.createLayerAbove(layer.item, "extract");
		}
		this.transfer(work, layer.image, newLayer.sources.manualSource.toollessImage);
		this.maybeFill(work, layer.image);
		image.layerTree.getSelectionModel().select(newLayer.item);
		newLayer.requestRedraw();
		layer.requestRedraw();
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
		return this.rootPane;
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

		public HDRImage source;
		public int x1, x2, y1, y2;
		public int prevX, prevY, offsetX, offsetY;
		public BoundaryPosition moving;
		public Symmetry symmetry = Symmetry.IDENTITY;

		public Work(HDRImage source) {
			this.source = source;
		}

		public Work(HDRImage source, int x, int y) {
			this.source = source;
			this.x1 = this.x2 = x;
			this.y1 = this.y2 = y;
			this.moving = BoundaryPosition.CORNER_X2_Y2;
		}
	}
}