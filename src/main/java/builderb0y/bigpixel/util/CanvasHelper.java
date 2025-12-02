package builderb0y.bigpixel.util;

import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;

public class CanvasHelper extends BaseCanvasHelper {

	public ObjectBinding<byte[]> pixels = Bindings.createObjectBinding(
		() -> new byte[((int)(this.display.getWidth())) * ((int)(this.display.getHeight())) * 4],
		this.display.widthProperty(),
		this.display.heightProperty()
	);
	public ObjectBinding<WritableImage> image = Bindings.createObjectBinding(
		() -> {
			int width = (int)(this.display.getWidth());
			int height = (int)(this.display.getHeight());
			if (width > 0 && height > 0) {
				return new WritableImage(width, height);
			}
			else {
				return null;
			}
		},
		this.display.widthProperty(),
		this.display.heightProperty()
	);

	public WritableImage getImage() {
		return this.image.get();
	}

	public void blit() {
		GraphicsContext context = this.display.getGraphicsContext2D();
		context.clearRect(0.0D, 0.0D, this.display.getWidth(), this.display.getHeight());
		context.drawImage(this.getImage(), 0.0D, 0.0D);
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
		return (CanvasHelper)(super.fixedSize(width, height));
	}

	@Override
	public CanvasHelper resizeable() {
		return (CanvasHelper)(super.resizeable());
	}
}