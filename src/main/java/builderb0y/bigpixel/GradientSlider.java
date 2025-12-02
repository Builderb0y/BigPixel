package builderb0y.bigpixel;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.util.RateLimitedMouseEventHandler;

public abstract class GradientSlider extends Gradient {

	public final SimpleIntegerProperty clickedPosition = new SimpleIntegerProperty();

	public GradientSlider() {
		EventHandler<MouseEvent> mouseHandler = new RateLimitedMouseEventHandler(
			(MouseEvent event) -> {
				this.clickedPosition.set(this.castPosition(event.getX()));
				this.redraw();
			}
		);
		this.display.setOnMousePressed(mouseHandler);
		this.display.setOnMouseDragged(mouseHandler);
		this.display.setOnMouseReleased(mouseHandler);
	}

	public int castPosition(double pos) {
		return Math.clamp((int)(pos), 0, (int)(this.getImage().getWidth()) - 1);
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