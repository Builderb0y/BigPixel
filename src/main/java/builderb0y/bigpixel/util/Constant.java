package builderb0y.bigpixel.util;

import javafx.beans.InvalidationListener;
import javafx.beans.value.*;

public abstract class Constant<T> implements ObservableValue<T> {

	@Override
	public void addListener(ChangeListener<? super T> listener) {

	}

	@Override
	public void removeListener(ChangeListener<? super T> listener) {

	}

	@Override
	public void addListener(InvalidationListener listener) {

	}

	@Override
	public void removeListener(InvalidationListener listener) {

	}

	public static class ConstantBoolean extends Constant<Boolean> implements ObservableBooleanValue {

		public static final ConstantBoolean
			TRUE  = new ConstantBoolean(true),
			FALSE = new ConstantBoolean(false);

		public final boolean value;

		public ConstantBoolean(boolean value) {
			this.value = value;
		}

		public static ConstantBoolean valueOf(boolean value) {
			return value ? TRUE : FALSE;
		}

		@Override
		public boolean get() {
			return this.value;
		}

		@Override
		public Boolean getValue() {
			return this.value;
		}
	}

	public static class ConstantNumber extends Constant<Number> implements ObservableNumberValue {

		public final Number value;

		public ConstantNumber(Number value) {
			this.value = value;
		}

		@Override
		public int intValue() {
			return this.value.intValue();
		}

		@Override
		public long longValue() {
			return this.value.longValue();
		}

		@Override
		public float floatValue() {
			return this.value.floatValue();
		}

		@Override
		public double doubleValue() {
			return this.value.doubleValue();
		}

		@Override
		public Number getValue() {
			return this.value;
		}
	}

	public static class ConstantInt extends ConstantNumber implements ObservableIntegerValue {

		public ConstantInt(Integer value) {
			super(value);
		}

		@Override
		public int get() {
			return this.intValue();
		}
	}

	public static class ConstantLong extends ConstantNumber implements ObservableLongValue {

		public ConstantLong(Long value) {
			super(value);
		}

		@Override
		public long get() {
			return this.longValue();
		}
	}

	public static class ConstantFloat extends ConstantNumber implements ObservableFloatValue {

		public ConstantFloat(Float value) {
			super(value);
		}

		@Override
		public float get() {
			return this.floatValue();
		}
	}

	public static class ConstantDouble extends ConstantNumber implements ObservableDoubleValue {

		public ConstantDouble(Double value) {
			super(value);
		}

		@Override
		public double get() {
			return this.doubleValue();
		}
	}

	public static class ConstantObject<T> extends Constant<T> implements ObservableObjectValue<T> {

		public final T value;

		public ConstantObject(T value) {
			this.value = value;
		}

		@Override
		public T get() {
			return this.value;
		}

		@Override
		public T getValue() {
			return this.value;
		}
	}
}