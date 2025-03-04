package builderb0y.notgimp;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import jdk.incubator.vector.FloatVector;

public abstract class GradientSlider extends Gradient {

	public final SimpleIntegerProperty clickedPosition = new SimpleIntegerProperty();

	public GradientSlider() {
		EventHandler<MouseEvent> mouseHandler = new RateLimitedMouseEventHandler(
			(MouseEvent event) -> {
				this.clickedPosition.set(Math.clamp((int)(event.getX()), 0, (int)(this.canvas.getWidth()) - 1));
				this.redraw();
			}
		);
		this.canvas.setOnMousePressed(mouseHandler);
		this.canvas.setOnMouseDragged(mouseHandler);
		this.canvas.setOnMouseReleased(mouseHandler);
	}

	@Override
	public FloatVector computeColor(int pixelPos, float fraction) {
		FloatVector color = this.computeColor0(pixelPos, fraction);
		if (pixelPos == this.clickedPosition.get()) {
			color = color.broadcast(1.0F).sub(color).withLane(HDRImage.ALPHA_OFFSET, 1.0F);
		}
		return color;
	}

	public abstract FloatVector computeColor0(int pixelPos, float fraction);
}