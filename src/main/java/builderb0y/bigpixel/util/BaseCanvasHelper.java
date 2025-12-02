package builderb0y.bigpixel.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;

public class BaseCanvasHelper extends BorderHelper<Canvas> {

	public BaseCanvasHelper() {
		super(new Canvas());
	}

	public void redraw() {}

	@Override
	public BaseCanvasHelper checkerboard() {
		return (BaseCanvasHelper)(super.checkerboard());
	}

	@Override
	public BaseCanvasHelper pop(ObservableValue<Boolean> inward) {
		return (BaseCanvasHelper)(super.pop(inward));
	}

	@Override
	public BaseCanvasHelper popIn() {
		return (BaseCanvasHelper)(super.popIn());
	}

	@Override
	public BaseCanvasHelper popOut() {
		return (BaseCanvasHelper)(super.popOut());
	}

	@Override
	public BaseCanvasHelper fixedSize(double width, double height) {
		this.display.setWidth(width);
		this.display.setHeight(height);
		super.fixedSize(width, height);
		this.bindRedrawListener();
		return this;
	}

	public BaseCanvasHelper resizeable() {
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.display.widthProperty().bind(this.innerPane.widthProperty());
		this.display.heightProperty().bind(this.innerPane.heightProperty());
		this.bindRedrawListener();
		return this;
	}

	public void bindRedrawListener() {
		ChangeListener<Object> listener = Util.change(() -> {
			if (this.display.getWidth() > 0.0D && this.display.getHeight() > 0.0D) {
				this.redraw();
			}
		});
		this.display.widthProperty().addListener(listener);
		this.display.heightProperty().addListener(listener);
	}

	public void blit(WritableImage image) {
		GraphicsContext context = this.display.getGraphicsContext2D();
		context.clearRect(0.0D, 0.0D, this.display.getWidth(), this.display.getHeight());
		context.drawImage(image, 0.0D, 0.0D);
	}
}