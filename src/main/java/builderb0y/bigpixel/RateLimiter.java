package builderb0y.bigpixel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

public abstract class RateLimiter implements Runnable {

	public final long millisecondDelay;
	public final Runnable action;

	public RateLimiter(long millisecondDelay, Runnable action) {
		this.millisecondDelay = millisecondDelay;
		this.action = action;
	}

	/**
	if this action is run faster than its delay,
	it will run once each delay.
	*/
	public static class PeriodicRateLimiter extends RateLimiter {

		public boolean canRun = true, runQueued;

		public PeriodicRateLimiter(long millisecondDelay, Runnable action) {
			super(millisecondDelay, action);
		}

		@Override
		public void run() {
			if (!this.canRun) {
				this.runQueued = true;
				return;
			}
			this.action.run();
			this.canRun = false;
			BigPixel.SCHEDULER.schedule(
				() -> Platform.runLater(() -> {
					this.canRun = true;
					if (this.runQueued) {
						this.runQueued = false;
						this.action.run();
					}
				}),
				this.millisecondDelay,
				TimeUnit.MILLISECONDS
			);
		}
	}

	/**
	if this action is run faster than its delay,
	it will wait for the runs to finish before actually running.
	*/
	public static class NonPeriodicRateLimiter extends RateLimiter {

		public ScheduledFuture<?> waitingFor;

		public NonPeriodicRateLimiter(long millisecondDelay, Runnable action) {
			super(millisecondDelay, action);
		}

		@Override
		public void run() {
			if (this.waitingFor != null) {
				this.waitingFor.cancel(false);
			}
			this.waitingFor = BigPixel.SCHEDULER.schedule(
				() -> Platform.runLater(() -> {
					this.waitingFor = null;
					this.action.run();
				}),
				this.millisecondDelay,
				TimeUnit.MILLISECONDS
			);
		}
	}
}