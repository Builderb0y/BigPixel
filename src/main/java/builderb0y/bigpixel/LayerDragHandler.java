package builderb0y.bigpixel;

import javafx.animation.Transition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.util.Util;

public class LayerDragHandler {

	public LayerNode
		layer;
	public SimpleIntegerProperty
		gridX = new SimpleIntegerProperty(),
		gridY = new SimpleIntegerProperty();
	public MoveAnimation
		animation = new MoveAnimation();
	public SimpleObjectProperty<Point2D>
		mouseLocation = new SimpleObjectProperty<>();

	public LayerDragHandler(LayerNode layer) {
		this.layer = layer;
	}

	public void init() {
		Pane node = this.layer.getPreviewNode();
		node.relocate(
			this.gridX.get() * LayerNode.GRID_WIDTH,
			this.gridY.get() * LayerNode.GRID_HEIGHT
		);
		node.setOnMousePressed(this::mouseDown);
		node.setOnMouseDragged(this::mouseDragged);
		node.setOnMouseReleased(this::mouseUp);
	}

	public void updateRectangle() {
		this.layer.graph.dragRectangle.relocate(
			Math.floor((this.layer.getPreviewNode().getLayoutX() + LayerNode.PREVIEW_WIDTH  * 0.5D) / LayerNode.GRID_WIDTH ) * LayerNode.GRID_WIDTH,
			Math.floor((this.layer.getPreviewNode().getLayoutY() + LayerNode.PREVIEW_HEIGHT * 0.5D) / LayerNode.GRID_HEIGHT) * LayerNode.GRID_HEIGHT
		);
	}

	public void mouseDown(MouseEvent event) {
		//prevent pane from resizing while layer is being dragged.
		Pane mainGrid = this.layer.graph.mainGrid;
		mainGrid.setMinSize(
			mainGrid.getWidth() + LayerNode.GRID_WIDTH,
			mainGrid.getHeight() + LayerNode.GRID_HEIGHT
		);
		this.mouseLocation.set(new Point2D(event.getSceneX(), event.getSceneY()));
		this.layer.getPreviewNode().layoutXProperty().unbind();
		this.layer.getPreviewNode().layoutYProperty().unbind();
		this.layer.graph.dragRectangle.setVisible(true);
		this.updateRectangle();
	}

	public void mouseDragged(MouseEvent event) {
		Point2D oldPoint = this.mouseLocation.get();
		Point2D newPoint = new Point2D(event.getSceneX(), event.getSceneY());
		this.layer.getPreviewNode().relocate(
			this.layer.getPreviewNode().getLayoutX() + newPoint.getX() - oldPoint.getX(),
			this.layer.getPreviewNode().getLayoutY() + newPoint.getY() - oldPoint.getY()
		);
		this.mouseLocation.set(newPoint);
		this.updateRectangle();
	}

	public void mouseUp(MouseEvent event) {
		int gridX = (int)(Math.floor((this.layer.getPreviewNode().getLayoutX() + LayerNode.PREVIEW_WIDTH  * 0.5D) / LayerNode.GRID_WIDTH ));
		int gridY = (int)(Math.floor((this.layer.getPreviewNode().getLayoutY() + LayerNode.PREVIEW_HEIGHT * 0.5D) / LayerNode.GRID_HEIGHT));
		this.layer.graph.moveNodeTo(this.layer, gridX, gridY);
		this.mouseLocation.set(null);
		this.layer.graph.dragRectangle.setVisible(false);
		Pane mainGrid = this.layer.graph.mainGrid;
		mainGrid.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
	}

	public void updateLayerNodePos(int gridX, int gridY, boolean animate) {
		double newX = gridX * LayerNode.GRID_WIDTH;
		double newY = gridY * LayerNode.GRID_HEIGHT;
		if (animate) {
			this.animation.run(newX, newY);
		}
		else {
			this.animation.stop();
			this.layer.getPreviewNode().relocate(newX, newY);
		}
	}

	public void setGridPos(int gridX, int gridY, boolean animate) {
		this.gridX.set(gridX);
		this.gridY.set(gridY);
		this.updateLayerNodePos(gridX, gridY, animate);
	}

	public void setGridX(int gridX, boolean animate) {
		this.gridX.set(gridX);
		this.updateLayerNodePos(gridX, this.gridY.get(), animate);
	}

	public void setGridY(int gridY, boolean animate) {
		this.gridY.set(gridY);
		this.updateLayerNodePos(this.gridX.get(), gridY, animate);
	}

	public class MoveAnimation extends Transition {

		public double fromX, fromY, toX, toY;

		public MoveAnimation() {
			this.setInterpolator(Util.SMOOTH_INTERPOLATOR);
			this.setCycleDuration(Duration.seconds(0.25D));
			this.setOnFinished((ActionEvent _) -> LayerDragHandler.this.layer.getPreviewNode().relocate(this.toX, this.toY));
		}

		public void run(double toX, double toY) {
			Pane pane = LayerDragHandler.this.layer.getPreviewNode();
			this.fromX = pane.getLayoutX();
			this.fromY = pane.getLayoutY();
			this.toX = toX;
			this.toY = toY;
			this.playFromStart();
		}

		@Override
		public void interpolate(double v) {
			Pane pane = LayerDragHandler.this.layer.getPreviewNode();
			pane.relocate(
				(this.toX - this.fromX) * v + this.fromX,
				(this.toY - this.fromY) * v + this.fromY
			);
		}
	}
}