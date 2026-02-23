package builderb0y.bigpixel.util;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;

import builderb0y.bigpixel.BigPixel;

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

		//-1: not running.
		// 0: currently running or waiting.
		//+1: waiting, and will need to run again after the wait is over.
		public int queued = -1;
		public final Runnable resetter;

		public PeriodicRateLimiter(long millisecondDelay, Runnable action) {
			super(millisecondDelay, action);
			Runnable resetter = () -> {
				if (--this.queued == 0) {
					this.action.run();
				}
			};
			this.resetter = () -> Platform.runLater(resetter);
		}

		@Override
		public void run() {
			if (this.queued > 0) return;
			if (++this.queued == 0) this.action.run();
			BigPixel.SCHEDULER.schedule(
				this.resetter,
				this.millisecondDelay,
				TimeUnit.MILLISECONDS
			);
		}
	}

	public static class AsyncPeriodicRateLimiter extends RateLimiter {

		//-1: not running.
		// 0: currently running or waiting.
		//+1: waiting, and will need to run again after the wait is over.
		public final AtomicInteger queued = new AtomicInteger(-1);
		public final Runnable resetter;

		public AsyncPeriodicRateLimiter(long millisecondDelay, Runnable action) {
			super(millisecondDelay, action);
			this.resetter = () -> {
				if (this.queued.decrementAndGet() == 0) {
					Platform.runLater(this.action);
				}
			};
		}

		@Override
		public void run() {
			int oldValue, newValue;
			do {
				oldValue = this.queued.get();
				if (oldValue > 0) return;
				newValue = oldValue + 1;
			}
			while (!this.queued.compareAndSet(oldValue, newValue));
			if (newValue == 0) {
				Platform.runLater(this.action);
			}
			BigPixel.SCHEDULER.schedule(
				this.resetter,
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