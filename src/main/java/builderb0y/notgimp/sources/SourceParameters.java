package builderb0y.notgimp.sources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.NotNull;

import builderb0y.notgimp.Util;

public class SourceParameters implements Iterable<SourceParameter<?, ?>> {

	public List<SourceParameter<?, ?>> parameters;
	public ChangeListener<Object> parameterListener;

	public SourceParameters(LayerSource source) {
		this.parameters = new ArrayList<>();
		this.parameterListener = Util.change(source::requestRedraw);
	}

	@Override
	public @NotNull Iterator<SourceParameter<?, ?>> iterator() {
		return this.parameters.iterator();
	}

	public <V, T> T addParameter(SourceParameter<V, T> parameter) {
		parameter.value().addListener(this.parameterListener);
		this.parameters.add(parameter);
		return parameter.holder;
	}

	public CheckBox addCheckbox(String saveName, String displayName, boolean defaultSelected) {
		CheckBox checkBox = new CheckBox(displayName);
		checkBox.setSelected(defaultSelected);
		this.addParameter(SourceParameter.checkbox(checkBox, saveName));
		return checkBox;
	}

	public Spinner<Integer> addIntSpinner(String saveName, int min, int max, int start, int step, double width) {
		Spinner<Integer> spinner = Util.setupSpinner(new Spinner<>(min, max, start, step), width);
		this.addParameter(SourceParameter.intSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, double min, double max, double start, double step, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(min, max, start, step), width);
		this.addParameter(SourceParameter.doubleSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Integer> addIntSpinner(String saveName, SpinnerValueFactory<Integer> valueFactory, double width) {
		Spinner<Integer> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(SourceParameter.intSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Float> addFloatSpinner(String saveName, SpinnerValueFactory<Float> valueFactory, double width) {
		Spinner<Float> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(SourceParameter.floatSpinner(spinner, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, SpinnerValueFactory<Double> valueFactory, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		this.addParameter(SourceParameter.doubleSpinner(spinner, saveName));
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
		this.addParameter(SourceParameter.enumChoiceBox(box, enumClass, saveName));
		return box;
	}

	public ChoiceBox<String> addStringChoiceBox(String saveName) {
		return this.addParameter(SourceParameter.stringChoiceBox(new ChoiceBox<>(), saveName));
	}

	public ColorBox addColorBox(String saveName, FloatVector initialColor) {
		ColorBox box = new ColorBox(initialColor);
		this.addParameter(SourceParameter.colorBox(box, saveName));
		return box;
	}

	public ColorBox addColorBox(String saveName, float red, float green, float blue, float alpha) {
		return this.addColorBox(saveName, Util.rgba(red, green, blue, alpha));
	}

	public TextArea addCode(String saveName) {
		TextArea textArea = new TextArea();
		textArea.setFont(Font.font("monospace"));
		this.addParameter(SourceParameter.code(textArea, saveName));
		return textArea;
	}

	public void copyFrom(SourceParameters that) {
		if (this.parameters.size() != that.parameters.size()) {
			throw new IllegalStateException("Parameter lengths differ: attempting to copy " + that.parameters.size() + " parameters to " + this.parameters.size() + " (on " + this.getClass() + ')');
		}
		for (int index = 0; index < this.parameters.size(); index++) {
			SourceParameter<?, ?> parameter = this.parameters.get(index);
			Object sourceValue = that.parameters.get(index).value().getValue();
			_set(parameter, sourceValue);
		}
	}

	public static <T> void _set(SourceParameter<T, ?> parameter, Object value) {
		parameter.value().setValue(parameter.valueClass.cast(value));
	}
}