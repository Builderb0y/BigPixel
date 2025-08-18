package builderb0y.notgimp;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class LayerDragHandler {

	public LayerNode
		layer;
	public SimpleIntegerProperty
		gridX = new SimpleIntegerProperty(),
		gridY = new SimpleIntegerProperty();
	public IntegerBinding
		snapX = this.gridX.multiply(LayerNode.GRID_WIDTH),
		snapY = this.gridY.multiply(LayerNode.GRID_HEIGHT);
	public SimpleObjectProperty<Point2D>
		mouseLocation = new SimpleObjectProperty<>();

	public LayerDragHandler(LayerNode layer) {
		this.layer = layer;
	}

	public void init() {
		Pane node = this.layer.getPreviewNode();
		node.layoutXProperty().bind(this.snapX);
		node.layoutYProperty().bind(this.snapY);
		node.setOnMousePressed(this::mouseDown);
		node.setOnMouseDragged(this::mouseDragged);
		node.setOnMouseReleased(this::mouseUp);
	}

	public void updateRectangle() {
		this.layer.graph.dragRectangle.relocate(
			Math.floor((this.layer.getPreviewNode().getLayoutX() + LayerNode.PREVIEW_WIDTH * 0.5D) / LayerNode.GRID_WIDTH) * LayerNode.GRID_WIDTH,
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
		int gridX = (((int)(this.layer.getPreviewNode().getLayoutX())) + 128) >> 8;
		int gridY = (((int)(this.layer.getPreviewNode().getLayoutY())) +  32) >> 6;
		this.layer.graph.moveNodeTo(this.layer, gridX, gridY);
		this.mouseLocation.set(null);
		this.layer.getPreviewNode().layoutXProperty().bind(this.snapX);
		this.layer.getPreviewNode().layoutYProperty().bind(this.snapY);
		this.layer.graph.dragRectangle.setVisible(false);
		Pane mainGrid = this.layer.graph.mainGrid;
		mainGrid.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
	}
}