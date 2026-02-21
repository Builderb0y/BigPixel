package builderb0y.bigpixel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.util.Util;

public class ConfigParameters implements Iterable<ConfigParameter<?>> {

	public ParameterSetTop top;
	public List<ConfigParameter<?>> parameters;
	public ChangeListener<Object> parameterListener;

	public ConfigParameters(ParameterSetTop top, ChangeListener<Object> parameterListener) {
		this.top = top;
		this.parameters = new ArrayList<>();
		this.parameterListener = parameterListener;
	}

	public void save(JsonMap map) {
		for (ConfigParameter<?> parameter : this) {
			parameter.save(map);
		}
	}

	public void load(JsonMap map) {
		for (ConfigParameter<?> parameter : this) {
			parameter.load(map);
		}
	}

	@Override
	public @NotNull Iterator<ConfigParameter<?>> iterator() {
		return this.parameters.iterator();
	}

	public <V> void addParameter(ConfigParameter<V> parameter) {
		if (this.parameterListener != null) {
			parameter.storage.property.addListener(this.parameterListener);
		}
		this.parameters.add(parameter);
	}

	public static <T> void setupContextMenu(ParameterSetTop top, Control control, ParameterMultiStorage<T> storage) {
		ContextMenu menu = new ContextMenu();
		Control mainPart = control instanceof Spinner<?> spinner ? spinner.getEditor() : control;
		mainPart.setContextMenu(menu);
		mainPart.setOnContextMenuRequested((ContextMenuEvent event) -> {
			ToggleGroup group = new ToggleGroup();
			menu.getItems().setAll(
				top
				.getSortedMoveableComponents()
				.stream()
				.map((ParameterSetMiddle middle) -> {
					RadioMenuItem item = new RadioMenuItem(middle.name.getText());
					item.setToggleGroup(group);
					if (middle == storage.middle.get()) {
						group.selectToggle(item);
					}
					item.setOnAction((ActionEvent _) -> {
						storage.middle.set(middle);
					});
					return item;
				})
				.toArray(RadioMenuItem[]::new)
			);
			menu.show(mainPart, event.getScreenX(), event.getScreenY());
		});
	}

	public <T> void setupContextMenu(Control control, ParameterMultiStorage<T> storage) {
		setupContextMenu(this.top, control, storage);
	}

	public CheckBox addCheckbox(String saveName, String displayName, boolean defaultSelected) {
		CheckBox checkBox = new CheckBox(displayName);
		checkBox.setSelected(defaultSelected);
		ParameterMultiStorage<Boolean> storage = new ParameterMultiStorage<>(checkBox.selectedProperty(), this.top);
		this.setupContextMenu(checkBox, storage);
		this.addParameter(ConfigParameter.createBoolean(storage, saveName));
		return checkBox;
	}

	public Spinner<Integer> addIntSpinner(String saveName, int min, int max, int start, int step, double width) {
		return this.addIntSpinner(saveName, new IntegerSpinnerValueFactory(min, max, start, step), width);
	}

	public Spinner<Integer> addIntSpinner(String saveName, SpinnerValueFactory<Integer> valueFactory, double width) {
		Spinner<Integer> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		ParameterMultiStorage<Integer> storage = new ParameterMultiStorage<>(spinner.getValueFactory().valueProperty(), this.top);
		this.setupContextMenu(spinner, storage);
		this.addParameter(ConfigParameter.createInt(storage, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, double min, double max, double start, double step, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(min, max, start, step), width);
		ParameterMultiStorage<Double> storage = new ParameterMultiStorage<>(spinner.getValueFactory().valueProperty(), this.top);
		this.setupContextMenu(spinner, storage);
		this.addParameter(ConfigParameter.createDouble(storage, saveName));
		return spinner;
	}

	public Spinner<Float> addFloatSpinner(String saveName, SpinnerValueFactory<Float> valueFactory, double width) {
		Spinner<Float> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		ParameterMultiStorage<Float> storage = new ParameterMultiStorage<>(spinner.getValueFactory().valueProperty(), this.top);
		this.setupContextMenu(spinner, storage);
		this.addParameter(ConfigParameter.createFloat(storage, saveName));
		return spinner;
	}

	public Spinner<Double> addDoubleSpinner(String saveName, SpinnerValueFactory<Double> valueFactory, double width) {
		Spinner<Double> spinner = Util.setupSpinner(new Spinner<>(valueFactory), width);
		ParameterMultiStorage<Double> storage = new ParameterMultiStorage<>(spinner.getValueFactory().valueProperty(), this.top);
		this.setupContextMenu(spinner, storage);
		this.addParameter(ConfigParameter.createDouble(storage, saveName));
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
		ParameterMultiStorage<E> storage = new ParameterMultiStorage<>(box.valueProperty(), this.top);
		this.setupContextMenu(box, storage);
		this.addParameter(ConfigParameter.createEnum(storage, enumClass, saveName));
		return box;
	}

	public TextArea addCode(String saveName) {
		TextArea textArea = Util.setupCodeArea(new TextArea());
		ParameterMultiStorage<String> storage = new ParameterMultiStorage<>(textArea.textProperty(), this.top);
		this.setupContextMenu(textArea, storage);
		this.addParameter(ConfigParameter.createMultiLineString(storage, saveName));
		return textArea;
	}

	public void copyFrom(ConfigParameters that) {
		if (this.parameters.size() != that.parameters.size()) {
			throw new IllegalStateException("Parameter lengths differ: attempting to copy " + that.parameters.size() + " parameters to " + this.parameters.size() + " (on " + this.getClass() + ')');
		}
		for (int index = 0; index < this.parameters.size(); index++) {
			ConfigParameter<?> parameter = this.parameters.get(index);
			Object sourceValue = that.parameters.get(index).storage.property.getValue();
			_set(parameter, sourceValue);
		}
	}

	public static <T> void _set(ConfigParameter<T> parameter, Object value) {
		parameter.storage.property.setValue(parameter.valueClass.cast(value));
	}
}