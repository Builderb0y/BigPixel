package builderb0y.notgimp;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;

public class RateLimiter implements Runnable {

	public static final Timer TIMER = new Timer(true);

	public final long millisecondDelay;
	public final Runnable action;
	public boolean canRun = true, runQueued;

	public RateLimiter(long millisecondDelay, Runnable action) {
		this.millisecondDelay = millisecondDelay;
		this.action = action;
	}

	@Override
	public void run() {
		if (!this.canRun) {
			this.runQueued = true;
			return;
		}
		this.action.run();
		this.canRun = false;
		TIMER.schedule(
			new TimerTask() {

				@Override
				public void run() {
					Platform.runLater(() -> {
						RateLimiter.this.canRun = true;
						if (RateLimiter.this.runQueued) {
							RateLimiter.this.runQueued = false;
							RateLimiter.this.action.run();
						}
					});
				}
			},
			this.millisecondDelay
		);
	}
}