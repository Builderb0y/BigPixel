package builderb0y.bigpixel.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface Result<T, X extends Throwable> {

	public abstract @Nullable T result();

	public abstract @Nullable X exception();

	public abstract @NotNull T orElseThrow() throws X;

	public static <T, X extends Throwable> Success<T, X> success(@NotNull T result) {
		return new Success<>(result);
	}

	public static <T, X extends Throwable> Failure<T, X> failure(@NotNull X exception) {
		return new Failure<>(exception);
	}

	public static record Success<T, X extends Throwable>(@NotNull T result) implements Result<T, X> {

		@Override
		public @Nullable X exception() {
			return null;
		}

		@Override
		public @NotNull T orElseThrow() throws RuntimeException {
			return this.result;
		}
	}

	public static record Failure<T, X extends Throwable>(@NotNull X exception) implements Result<T, X> {

		@Override
		public @Nullable T result() {
			return null;
		}

		@Override
		public @NotNull T orElseThrow() throws X {
			throw this.exception;
		}
	}
}