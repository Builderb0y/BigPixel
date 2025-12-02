package builderb0y.bigpixel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.NotNull;

import builderb0y.bigpixel.sources.ColorBox;
import builderb0y.bigpixel.sources.SourceParameters;
import builderb0y.bigpixel.util.Util;

public class ConfigParameters implements Iterable<ConfigParameter<?, ?>> {

	public List<ConfigParameter<?, ?>> parameters;
	public ChangeListener<Object> parameterListener;

	public ConfigParameters() {
		this.parameters = new ArrayList<>();
	}

	@Override
	public @NotNull Iterator<ConfigParameter<?, ?>> iterator() {
		return this.parameters.iterator();
	}

	public <V, T> T addParameter(ConfigParameter<V, T> parameter) {
		parameter.value().addListener(this.parameterListener);
		this.parameters.add(parameter);
		return parameter.holder;
	}

	public CheckBox addCheckbox(String saveName, String displayName, boolean defaultSelected) {
		CheckBox checkBox = new CheckBox(displayName);
		checkBox.setSelected(defaultSelected);
		this.addParameter(ConfigParameter.checkbox(checkBox, saveName));
		return checkBox;
	}

	public Spinner<Integer> addIntSpinner(String saveName, int min, int max, int start, int step, double width) {
		Spinner<Integer> spinner = Util.setupSpinner(new Spinner<>(min, max, start, step), width);
		this.addParameter(ConfigParameter.intSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, double min, double max, double start, double step, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(min, max, start, step), width);
		this.addParameter(ConfigParameter.doubleSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Integer> addIntSpinner(String saveName, SpinnerValueFactory<Integer> valueFactory, double width) {
		Spinner<Integer> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(ConfigParameter.intSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Float> addFloatSpinner(String saveName, SpinnerValueFactory<Float> valueFactory, double width) {
		Spinner<Float> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(ConfigParameter.floatSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, SpinnerValueFactory<Double> valueFactory, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(ConfigParameter.doubleSpinner(spinner, saveName));
		return spinner;
	}

	public <E extends Enum<E>> ChoiceBox<E> addEnumChoiceBox(String saveName, Class<E> enumClass) {
		return this.addEnumChoiceBox(saveName, enumClass, null);
	}

	public <E extends Enum<E>> ChoiceBox<E> addEnumChoiceBox(String saveName, Class<E> enumClass, E defaultValue) {
		ChoiceBox<E> box = new ChoiceBox<>();
		E[] values = enumClass.getEnumConstants();
		box.getItems().addAll(values);
		box.setValue(defaultValue != null ? defaultValue : values[0]);
		this.addParameter(ConfigParameter.enumChoiceBox(box, enumClass, saveName));
		return box;
	}

	public ChoiceBox<String> addStringChoiceBox(String saveName) {
		return this.addParameter(ConfigParameter.stringChoiceBox(new ChoiceBox<>(), saveName));
	}

	public ColorBox addColorBox(String saveName, FloatVector initialColor) {
		ColorBox box = new ColorBox(initialColor);
		this.addParameter(ConfigParameter.colorBox(box, saveName));
		return box;
	}

	public ColorBox addColorBox(String saveName, float red, float green, float blue, float alpha) {
		return this.addColorBox(saveName, Util.rgba(red, green, blue, alpha));
	}

	public TextArea addCode(String saveName) {
		TextArea textArea = new TextArea();
		textArea.setFont(Font.font("monospace"));
		this.addParameter(ConfigParameter.code(textArea, saveName));
		return textArea;
	}

	public void copyFrom(SourceParameters that) {
		if (this.parameters.size() != that.parameters.size()) {
			throw new IllegalStateException("Parameter lengths differ: attempting to copy " + that.parameters.size() + " parameters to " + this.parameters.size() + " (on " + this.getClass() + ')');
		}
		for (int index = 0; index < this.parameters.size(); index++) {
			ConfigParameter<?, ?> parameter = this.parameters.get(index);
			Object sourceValue = that.parameters.get(index).value().getValue();
			_set(parameter, sourceValue);
		}
	}

	public static <T> void _set(ConfigParameter<T, ?> parameter, Object value) {
		parameter.value().setValue(parameter.valueClass.cast(value));
	}
}