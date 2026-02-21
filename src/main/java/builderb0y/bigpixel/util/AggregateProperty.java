package builderb0y.bigpixel.util;

import com.sun.javafx.binding.ExpressionHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public abstract class AggregateProperty<T> extends ObjectProperty<T> {

	public ExpressionHelper<T> helper;
	public boolean setting;

	@Override
	public void set(T value) {
		boolean oldSetting = this.setting;
		this.setting = true;
		try {
			this.doSet(value);
		}
		finally {
			this.setting = oldSetting;
		}
		this.fireValueChangedEvent();
	}

	public abstract void doSet(T value);

	@Override
	public void bind(ObservableValue<? extends T> observable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unbind() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBound() {
		return false;
	}

	public void fireValueChangedEvent() {
		if (!this.setting) {
			ExpressionHelper.fireValueChangedEvent(this.helper);
		}
	}

	@Override
	public void addListener(InvalidationListener listener) {
		this.helper = ExpressionHelper.addListener(this.helper, this, listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		this.helper = ExpressionHelper.removeListener(this.helper, listener);
	}

	@Override
	public void addListener(ChangeListener<? super T> listener) {
		this.helper = ExpressionHelper.addListener(this.helper, this, listener);
	}

	@Override
	public void removeListener(ChangeListener<? super T> listener) {
		this.helper = ExpressionHelper.removeListener(this.helper, listener);
	}
}