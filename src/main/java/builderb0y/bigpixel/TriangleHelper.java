package builderb0y.bigpixel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import jdk.incubator.vector.FloatVector;

public class TriangleHelper extends BorderHelper<Pane> {

	public Polygon topLeft, bottomRight;
	public SimpleObjectProperty<FloatVector> color;

	public TriangleHelper() {
		super(new Pane(this.topLeft = new Polygon(), this.bottomRight = new Polygon()));
		this.color = new SimpleObjectProperty<>(this, "color");
		ChangeListener<Object> listener = Util.change(() -> {
			double width = this.display.getWidth();
			double height = this.display.getHeight();
			this.topLeft.getPoints().setAll(0.0D, 0.0D, width, 0.0D, 0.0D, height);
			this.bottomRight.getPoints().setAll(width, height, 0.0D, height, width, 0.0D);
		});
		this.display.widthProperty().addListener(listener);
		this.display.heightProperty().addListener(listener);
		this.color.addListener(Util.change((FloatVector color) -> {
			this.topLeft.setFill(new Color(
				color.lane(HDRImage.RED_OFFSET),
				color.lane(HDRImage.GREEN_OFFSET),
				color.lane(HDRImage.BLUE_OFFSET),
				1.0D
			));
			this.bottomRight.setFill(new Color(
				color.lane(HDRImage.ALPHA_OFFSET),
				color.lane(HDRImage.ALPHA_OFFSET),
				color.lane(HDRImage.ALPHA_OFFSET),
				1.0D
			));
		}));
	}

	@Override
	@Deprecated
	public TriangleHelper checkerboard() {
		return (TriangleHelper)(super.checkerboard());
	}

	@Override
	public TriangleHelper popOut() {
		return (TriangleHelper)(super.popOut());
	}

	@Override
	public TriangleHelper popIn() {
		return (TriangleHelper)(super.popIn());
	}

	@Override
	public TriangleHelper pop(ObservableValue<Boolean> inward) {
		return (TriangleHelper)(super.pop(inward));
	}

	@Override
	public TriangleHelper fixedSize(double width, double height) {
		return (TriangleHelper)(super.fixedSize(width, height));
	}
}