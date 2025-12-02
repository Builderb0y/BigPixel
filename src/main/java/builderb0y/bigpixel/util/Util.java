package builderb0y.bigpixel.util;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.*;
import javafx.scene.control.Spinner;
import javafx.scene.input.ScrollEvent;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.bigpixel.HDRImage;

public class Util {

	public static final FloatVector
		INVISIBLACK = FloatVector.broadcast(FloatVector.SPECIES_128, 0.0F),
		WHITE       = FloatVector.broadcast(FloatVector.SPECIES_128, 1.0F),
		BLACK       = INVISIBLACK.withLane(HDRImage.ALPHA_OFFSET, 1.0F);
	public static final VectorMask<Float> RGB_MASK;

	static {
		boolean[] mask = new boolean[4];
		mask[HDRImage.  RED_OFFSET] =
		mask[HDRImage.GREEN_OFFSET] =
		mask[HDRImage. BLUE_OFFSET] =
		true;
		RGB_MASK = VectorMask.fromValues(FloatVector.SPECIES_128, mask);
	}

	public static <T> Spinner<T> setupSpinner(Spinner<T> spinner, double width) {
		spinner.setEditable(true);
		spinner.setMinWidth(width);
		spinner.setMaxWidth(width);
		spinner.setOnScroll((ScrollEvent event) -> {
			if (spinner.isEditable()) {
				if (event.getDeltaY() > 0.0D) spinner.increment();
				else if (event.getDeltaY() < 0.0D) spinner.decrement();
				event.consume();
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

	@SuppressWarnings("unchecked")
	public static <X extends Throwable> RuntimeException rethrow(Throwable throwable) throws X {
		throw (X)(throwable);
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

	public static <T> T make(T object, Consumer<T> action) {
		action.accept(object);
		return object;
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

	public static double mix(double a, double b, double f) {
		return (b - a) * f + a;
	}

	public static float unmix(float a, float b, float f) {
		return (f - a) / (b - a);
	}

	public static double unmix(double a, double b, double f) {
		return (f - a) / (b - a);
	}

	public static int square(int x) {
		return x * x;
	}

	public static float square(float x) {
		return x * x;
	}

	public static double square(double x) {
		return x * x;
	}

	public static FloatVector rgba(float red, float green, float blue, float alpha) {
		float[] array = new float[4];
		array[HDRImage.  RED_OFFSET] = red;
		array[HDRImage.GREEN_OFFSET] = green;
		array[HDRImage. BLUE_OFFSET] = blue;
		array[HDRImage.ALPHA_OFFSET] = alpha;
		return FloatVector.fromArray(FloatVector.SPECIES_128, array, 0);
	}

	public static void invokeAndWait(Runnable task) {
		CompletableFuture.runAsync(task, Platform::runLater).join();
	}

	public static <T> T getAndWait(Supplier<T> supplier) {
		return CompletableFuture.supplyAsync(supplier, Platform::runLater).join();
	}

	public static int     or(Number  number, int     nullDefault) { return number != null ? number.    intValue() : nullDefault; }
	public static long    or(Number  number, long    nullDefault) { return number != null ? number.   longValue() : nullDefault; }
	public static float   or(Number  number, float   nullDefault) { return number != null ? number.  floatValue() : nullDefault; }
	public static double  or(Number  number, double  nullDefault) { return number != null ? number. doubleValue() : nullDefault; }
	public static boolean or(Boolean bool,   boolean nullDefault) { return bool   != null ? bool  .booleanValue() : nullDefault; }

	public static ObservableIntegerValue toInt(ObservableValue<? extends Number> value, int nullDefault) {
		return (
			value instanceof ObservableIntegerValue correct ? correct :
			value instanceof ObservableNumberValue number ? Bindings.createIntegerBinding(number::intValue, number) :
			Bindings.createIntegerBinding(() -> or(value.getValue(), nullDefault), value)
		);
	}

	public static ObservableLongValue toLong(ObservableValue<? extends Number> value, long nullDefault) {
		return (
			value instanceof ObservableLongValue correct ? correct :
			value instanceof ObservableNumberValue number ? Bindings.createLongBinding(number::longValue, number) :
			Bindings.createLongBinding(() -> or(value.getValue(), nullDefault), value)
		);
	}

	public static ObservableFloatValue toFloat(ObservableValue<? extends Number> value, float nullDefault) {
		return (
			value instanceof ObservableFloatValue correct ? correct :
			value instanceof ObservableNumberValue number ? Bindings.createFloatBinding(number::floatValue, number) :
			Bindings.createFloatBinding(() -> or(value.getValue(), nullDefault), value)
		);
	}

	public static ObservableDoubleValue toDouble(ObservableValue<? extends Number> value, double nullDefault) {
		return (
			value instanceof ObservableDoubleValue correct ? correct :
			value instanceof ObservableNumberValue number ? Bindings.createDoubleBinding(number::doubleValue, number) :
			Bindings.createDoubleBinding(() -> or(value.getValue(), nullDefault), value)
		);
	}

	public static ObservableBooleanValue toBoolean(ObservableValue<Boolean> value, boolean nullDefault) {
		return (
			value instanceof ObservableBooleanValue correct ? correct :
			Bindings.createBooleanBinding(() -> or(value.getValue(), nullDefault), value)
		);
	}
}