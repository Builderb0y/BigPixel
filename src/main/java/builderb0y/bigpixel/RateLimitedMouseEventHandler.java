package builderb0y.bigpixel;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

public class RateLimitedMouseEventHandler implements EventHandler<MouseEvent> {

	public final EventHandler<MouseEvent> delegate;
	public long prevInvocationTime;

	public RateLimitedMouseEventHandler(EventHandler<MouseEvent> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(MouseEvent event) {
		if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
			this.prevInvocationTime = System.currentTimeMillis();
			this.delegate.handle(event);
		}
		else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
			long newTime = System.currentTimeMillis();
			if (newTime - this.prevInvocationTime >= 20) {
				this.prevInvocationTime = newTime;
				this.delegate.handle(event);
			}
		}
		else {
			this.delegate.handle(event);
		}
	}
}