package builderb0y.notgimp;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Spinner;
import javafx.scene.input.ScrollEvent;

public class Util {

	public static <T> Spinner<T> setupSpinner(Spinner<T> spinner) {
		spinner.setOnScroll((ScrollEvent event) -> {
			if (event.getDeltaY() > 0.0D) spinner.increment();
			else if (event.getDeltaY() < 0.0D) spinner.decrement();
		});
		return spinner;
	}

	public static <T> ChangeListener<T> change(Consumer<T> consumer) {
		return (ObservableValue<? extends T> observable, T oldValue, T newValue) -> consumer.accept(newValue);
	}

	public static <T, U> ChangeListener<T> weakChange(U value, BiConsumer<U, T> consumer) {
		WeakReference<U> weakValue = new WeakReference<>(value);
		return new ChangeListener<>() {

			@Override
			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
				U instance = weakValue.get();
				if (instance != null) {
					consumer.accept(instance, newValue);
				}
				else {
					observable.removeListener(this);
				}
			}
		};
	}

	public static File changeExtension(File file, String extension) {
		String path = file.getPath();
		for (int index = path.length(); --index >= 0;) {
			char c = path.charAt(index);
			if (c == '.') {
				return new File(path.substring(0, index + 1) + extension);
			}
			else if (c == File.separatorChar) {
				break;
			}
		}
		return new File(path + '.' + extension);
	}
}