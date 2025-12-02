package builderb0y.bigpixel.util;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import org.jetbrains.annotations.Nullable;

public class BaseCanvasHelper extends BorderHelper<Canvas> {

	public @Nullable Consumer<BaseCanvasHelper> redrawer;

	public BaseCanvasHelper() {
		super(new Canvas());
	}

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
	@Deprecated
	public BorderHelper<Canvas> fixedSize(double width, double height) {
		this.display.setWidth(width);
		this.display.setHeight(height);
		return super.fixedSize(width, height);
	}

	public BaseCanvasHelper fixedSize(double width, double height, Consumer<BaseCanvasHelper> redrawer) {
		this.redrawer = redrawer;
		this.fixedSize(width, height);
		this.bindRedrawListener();
		return this;
	}

	public BaseCanvasHelper resizeable(Consumer<BaseCanvasHelper> redrawer) {
		this.redrawer = redrawer;
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.display.widthProperty().bind(this.innerPane.widthProperty());
		this.display.heightProperty().bind(this.innerPane.heightProperty());
		this.bindRedrawListener();
		return this;
	}

	public void bindRedrawListener() {
		ChangeListener<Object> listener = Util.change(() -> {
			if (this.redrawer != null && this.display.getWidth() > 0.0D && this.display.getHeight() > 0.0D) {
				this.redrawer.accept(this);
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