package builderb0y.notgimp;

import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class RectangleHelper extends BorderHelper<Rectangle> {

	public RectangleHelper() {
		super(new Rectangle());
	}

	public RectangleHelper color(double red, double green, double blue, double alpha) {
		this.display.setFill(new Color(red, green, blue, alpha));
		return this;
	}

	public RectangleHelper paint(Paint paint) {
		this.display.setFill(paint);
		return this;
	}

	@Override
	public RectangleHelper checkerboard() {
		return (RectangleHelper)(super.checkerboard());
	}

	@Override
	public RectangleHelper pop(ObservableValue<Boolean> inward) {
		return (RectangleHelper)(super.pop(inward));
	}

	@Override
	public RectangleHelper popIn() {
		return (RectangleHelper)(super.popIn());
	}

	@Override
	public RectangleHelper popOut() {
		return (RectangleHelper)(super.popOut());
	}

	@Override
	public RectangleHelper fixedSize(double width, double height) {
		this.display.setWidth(width);
		this.display.setHeight(height);
		return (RectangleHelper)(super.fixedSize(width, height));
	}

	public RectangleHelper resizeable() {
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.display.widthProperty().bind(this.innerPane.widthProperty());
		this.display.heightProperty().bind(this.innerPane.heightProperty());
		return this;
	}
}