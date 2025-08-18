package builderb0y.bigpixel.sources.dependencies;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class CurveHelper {

	public CubicCurve curve = new CubicCurve();
	public SimpleBooleanProperty
		otherBindingIsVarying = new SimpleBooleanProperty(this, "otherBindingIsVarying"),
		selfSourceIsSelected  = new SimpleBooleanProperty(this, "selfSourceIsSelected");
	public ObservableBooleanValue
		inScene = this.otherBindingIsVarying.and(this.selfSourceIsSelected);

	public CurveHelper(LayerNode layer) {
		this.curve.setStrokeWidth(8.0D);
		this.curve.setStroke(new Color(0.25D, 0.75D, 0.25D, 1.0D));
		this.curve.setFill(null);
		Pane preview = layer.getPreviewNode();
		DoubleExpression endX = preview.layoutXProperty();
		DoubleExpression endY = preview.layoutYProperty().add(LayerNode.PREVIEW_HEIGHT * 0.5D);
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

	public void setOtherEnd(LayerSourceInput input) {
		this.curve.startXProperty().unbind();
		this.curve.startYProperty().unbind();
		switch (input) {
			case null -> {
				this.otherBindingIsVarying.set(false);
			}
			case UniformLayerSourceInput _ -> {
				this.otherBindingIsVarying.set(false);
			}
			case VaryingLayerSourceInput varying -> {
				Pane otherLayer = varying.getBackingLayer().getPreviewNode();
				this.curve.startXProperty().bind(otherLayer.layoutXProperty().add(LayerNode.PREVIEW_WIDTH));
				this.curve.startYProperty().bind(otherLayer.layoutYProperty().add(LayerNode.PREVIEW_HEIGHT * 0.5D));
				this.otherBindingIsVarying.set(true);
			}
		}
	}
}