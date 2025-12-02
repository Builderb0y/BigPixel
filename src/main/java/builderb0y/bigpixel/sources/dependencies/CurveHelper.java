package builderb0y.bigpixel.sources.dependencies;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.Util;

public class CurveHelper {

	public static final Color
		NORMAL     = new Color(0.25D, 0.75D,  0.25D, 1.0D),
		MASK       = new Color(1.0D,  0.875D, 0.25D, 1.0D),
		VIEW       = new Color(0.25D, 0.5D,   1.0D,  1.0D),
		VIEW_PARAM = new Color(0.5D,  0.25D,  1.0D,  1.0D),
		PARAM      = new Color(1.0D,  0.25D,  1.0D,  1.0D);

	public LayerNode
		ourLayer;
	public CubicCurve
		curve = new CubicCurve();
	public SimpleBooleanProperty
		otherBindingIsVarying = new SimpleBooleanProperty(this, "otherBindingIsVarying"),
		selfSourceIsSelected  = new SimpleBooleanProperty(this, "selfSourceIsSelected");
	public ObservableBooleanValue
		inScene = this.otherBindingIsVarying.and(this.selfSourceIsSelected);
	public SimpleObjectProperty<Direction>
		direction = new SimpleObjectProperty<>(this, "direction");

	public CurveHelper(LayerNode layer, Color color) {
		this.ourLayer = layer;
		super();
		this.curve.setStrokeWidth(8.0D);
		this.curve.setStroke(color);
		this.curve.setFill(null);
		Pane preview = layer.getPreviewNode();
		DoubleProperty layoutX = preview.layoutXProperty();
		DoubleProperty layoutY = preview.layoutYProperty();

		DoubleExpression endX = Bindings.createDoubleBinding(
			() -> switch (this.direction.get()) {
				case LEFT -> layoutX.get();
				case RIGHT -> layoutX.get() + LayerNode.PREVIEW_WIDTH;
				case UP, DOWN -> layoutX.get() + LayerNode.PREVIEW_WIDTH * 0.5D;
				case null -> layoutX.get() + LayerNode.PREVIEW_WIDTH * 0.5D;
			},
			layoutX, this.direction
		);
		DoubleExpression endY = Bindings.createDoubleBinding(
			() -> switch (this.direction.get()) {
				case null -> layoutY.get() + LayerNode.PREVIEW_HEIGHT * 0.5D;
				case LEFT, RIGHT -> layoutY.get() + LayerNode.PREVIEW_HEIGHT * 0.5D;
				case UP -> layoutY.get();
				case DOWN -> layoutY.get() + LayerNode.PREVIEW_HEIGHT;
			},
			layoutY, this.direction
		);
		DoubleBinding midX = this.curve.startXProperty().add(this.curve.endXProperty()).multiply(0.5D);
		this.curve.endXProperty().bind(endX);
		this.curve.endYProperty().bind(endY);
		this.curve.controlX1Property().bind(midX);
		this.curve.controlX2Property().bind(midX);
		this.curve.controlY1Property().bind(this.curve.startYProperty());
		this.curve.controlY2Property().bind(this.curve.endYProperty());
		ObservableList<Node> children = layer.graph.underlay.getChildren();
		this.inScene.addListener(Util.change((Boolean oldVisible, Boolean newVisible) -> {
			if      (newVisible && !oldVisible) children.add   (this.curve);
			else if (oldVisible && !newVisible) children.remove(this.curve);
		}));
	}

	public void setOtherEnd(SamplerProvider input) {
		this.curve.startXProperty().unbind();
		this.curve.startYProperty().unbind();
		switch (input) {
			case null -> {
				this.otherBindingIsVarying.set(false);
			}
			case UniformSamplerProvider _ -> {
				this.otherBindingIsVarying.set(false);
			}
			case VaryingSamplerProvider varying -> {
				LayerNode layer = varying.getBackingLayer();
				Pane pane = layer.getPreviewNode();
				if (layer.getGridX() < this.ourLayer.getGridX()) {
					this.direction.set(Direction.LEFT);
					this.curve.startXProperty().bind(pane.layoutXProperty().add(LayerNode.PREVIEW_WIDTH));
					this.curve.startYProperty().bind(pane.layoutYProperty().add(LayerNode.PREVIEW_HEIGHT * 0.5D));
				}
				else if (layer.getGridX() > this.ourLayer.getGridX()) {
					this.direction.set(Direction.RIGHT);
					this.curve.startXProperty().bind(pane.layoutXProperty());
					this.curve.startYProperty().bind(pane.layoutYProperty().add(LayerNode.PREVIEW_HEIGHT * 0.5D));
				}
				else if (layer.getGridY() < this.ourLayer.getGridY()) {
					this.direction.set(Direction.UP);
					this.curve.startXProperty().bind(pane.layoutXProperty().add(LayerNode.PREVIEW_WIDTH * 0.5D));
					this.curve.startYProperty().bind(pane.layoutYProperty().add(LayerNode.PREVIEW_HEIGHT));
				}
				else if (layer.getGridY() > this.ourLayer.getGridY()) {
					this.direction.set(Direction.DOWN);
					this.curve.startXProperty().bind(pane.layoutXProperty().add(LayerNode.PREVIEW_WIDTH * 0.5D));
					this.curve.startYProperty().bind(pane.layoutYProperty());
				}
				else {
					this.direction.set(null);
				}
				this.otherBindingIsVarying.set(layer != this.ourLayer);
			}
		}
	}

	public static enum Direction {
		LEFT,
		RIGHT,
		UP,
		DOWN;
	}
}