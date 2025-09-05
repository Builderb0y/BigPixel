package builderb0y.bigpixel;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;

public class CanvasHelper extends BorderHelper<Canvas> {

	public byte[] pixels = new byte[0];

	public CanvasHelper() {
		super(new Canvas());
	}

	@Override
	public CanvasHelper checkerboard() {
		return (CanvasHelper)(super.checkerboard());
	}

	@Override
	public CanvasHelper pop(ObservableValue<Boolean> inward) {
		return (CanvasHelper)(super.pop(inward));
	}

	@Override
	public CanvasHelper popIn() {
		return (CanvasHelper)(super.popIn());
	}

	@Override
	public CanvasHelper popOut() {
		return (CanvasHelper)(super.popOut());
	}

	@Override
	public CanvasHelper fixedSize(double width, double height) {
		this.display.setWidth(width);
		this.display.setHeight(height);
		return (CanvasHelper)(super.fixedSize(width, height));
	}

	public CanvasHelper resizeable(Consumer<CanvasHelper> redrawer) {
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.display.widthProperty().bind(this.innerPane.widthProperty());
		this.display.heightProperty().bind(this.innerPane.heightProperty());
		ChangeListener<Number> listener = Util.change(() -> {
			int expectedLength = ((int)(this.display.getWidth())) * ((int)(this.display.getHeight())) << 2;
			if (this.pixels.length != expectedLength) {
				this.pixels = new byte[expectedLength];
			}
			if (expectedLength != 0) {
				redrawer.accept(this);
			}
		});
		this.display.widthProperty().addListener(listener);
		this.display.heightProperty().addListener(listener);
		return this;
	}
}