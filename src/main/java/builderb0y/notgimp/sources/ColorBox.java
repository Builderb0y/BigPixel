package builderb0y.notgimp.sources;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.RectangleHelper;
import builderb0y.notgimp.Util;

public class ColorBox {

	public SimpleObjectProperty<FloatVector> color;
	public RectangleHelper box;

	public ColorBox(FloatVector color) {
		this.color = new SimpleObjectProperty<>(this, "color", color);
		this.box = new RectangleHelper().fixedSize(16.0D, 16.0D);
		this.box.display.fillProperty().bind(
			this.color.map(
				(FloatVector vector) -> new Color(
					vector.lane(HDRImage.  RED_OFFSET),
					vector.lane(HDRImage.GREEN_OFFSET),
					vector.lane(HDRImage. BLUE_OFFSET),
					vector.lane(HDRImage.ALPHA_OFFSET)
				)
			)
		);
	}

	public ColorBox selectWhen(Property<ColorBox> property) {
		//this is supposed to be made easy with CanvasHelper.pop(),
		//but nooooooooooooooooooooooooooooooooooooooooooooooooooo,
		//javaFX decided to make that a pain in the ass instead.
		property.addListener(Util.change((ColorBox box) -> this.box.setPop(box == this)));
		this.box.setPop(property.getValue() == this);
		return this;
	}
}