package builderb0y.notgimp;

import java.io.File;
import java.lang.constant.ClassDesc;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Spinner;
import javafx.scene.input.ScrollEvent;

public class Util {

	public static <T> Spinner<T> setupSpinner(Spinner<T> spinner, double width) {
		spinner.setEditable(true);
		spinner.setPrefWidth(width);
		spinner.setOnScroll((ScrollEvent event) -> {
			if (spinner.isEditable()) {
				if (event.getDeltaY() > 0.0D) spinner.increment();
				else if (event.getDeltaY() < 0.0D) spinner.decrement();
			}
		});
		return spinner;
	}

	public static <T> ChangeListener<T> change(Runnable action) {
		return (ObservableValue<? extends T> observable, T oldValue, T newValue) -> action.run();
	}

	public static <T> ChangeListener<T> change(Consumer<T> consumer) {
		return (ObservableValue<? extends T> observable, T oldValue, T newValue) -> consumer.accept(newValue);
	}

	public static <T> ChangeListener<T> change(BiConsumer<T, T> consumer) {
		return (ObservableValue<? extends T> observable, T oldValue, T newValue) -> consumer.accept(oldValue, newValue);
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

	public static String getExtension(File file) {
		String path = file.getPath();
		for (int index = path.length(); --index >= 0;) {
			char c = path.charAt(index);
			if (c == '.') {
				return path.substring(index + 1);
			}
			else if (c == File.separatorChar) {
				break;
			}
		}
		return "";
	}

	public static <T> T[] fill(T[] array, T element) {
		Arrays.fill(array, element);
		return array;
	}

	public static ClassDesc desc(Class<?> clazz) {
		return ClassDesc.ofDescriptor(clazz.descriptorString());
	}

	public static String capitalize(String word) {
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	public static float clampF(float x) {
		return x > 0.0F ? x > 1.0F ? 1.0F : x : 0.0F;
	}

	public static int clampI(float value) {
		int clamped = (int)(value * 256.0F);
		if (clamped <=   0) return   0;
		if (clamped >= 255) return 255;
		return clamped;
	}

	public static byte clampB(float value) {
		return (byte)(clampI(value));
	}

	public static float mix(float a, float b, float f) {
		return (b - a) * f + a;
	}

	public static float unmix(float a, float b, float f) {
		return (f - a) / (b - a);
	}
}