package builderb0y.notgimp;

import java.util.TimerTask;

import javafx.application.Platform;

public abstract class RateLimiter implements Runnable {

	public final long millisecondDelay;
	public final Runnable action;

	public RateLimiter(long millisecondDelay, Runnable action) {
		this.millisecondDelay = millisecondDelay;
		this.action = action;
	}

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
			NotGimp.TIMER.schedule(
				new TimerTask() {

					@Override
					public void run() {
						Platform.runLater(() -> {
							PeriodicRateLimiter.this.canRun = true;
							if (PeriodicRateLimiter.this.runQueued) {
								PeriodicRateLimiter.this.runQueued = false;
								PeriodicRateLimiter.this.action.run();
							}
						});
					}
				},
				this.millisecondDelay
			);
		}
	}

	public static class NonPeriodicRateLimiter extends RateLimiter {

		public TimerTask waitingFor;

		public NonPeriodicRateLimiter(long millisecondDelay, Runnable action) {
			super(millisecondDelay, action);
		}

		@Override
		public void run() {
			if (this.waitingFor != null) {
				this.waitingFor.cancel();
			}
			this.waitingFor = new TimerTask() {

				@Override
				public void run() {
					Platform.runLater(() -> {
						NonPeriodicRateLimiter.this.waitingFor = null;
						NonPeriodicRateLimiter.this.action.run();
					});
				}
			};
			NotGimp.TIMER.schedule(this.waitingFor, this.millisecondDelay);
		}
	}
}