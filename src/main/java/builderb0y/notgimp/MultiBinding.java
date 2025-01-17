package builderb0y.notgimp;

import java.util.function.Supplier;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

public abstract class MultiBinding<R> extends ObjectBinding<R> {

	public final ObservableValue<?>[] values;

	public MultiBinding(ObservableValue<?>... values) {
		this.values = values;
		this.bind(this.values);
	}

	public static <R> MultiBinding<R> of(Supplier<R> supplier, ObservableValue<?>... values) {
		return new MultiBinding<>(values) {

			@Override
			public R computeValue() {
				return supplier.get();
			}
		};
	}

	@Override
	public void dispose() {
		this.unbind(this.values);
	}
}