package builderb0y.notgimp.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public abstract class LayerSource {

	public static final VectorMask<Float> RGB_MASK;

	static {
		boolean[] mask = new boolean[4];
		mask[HDRImage.  RED_OFFSET] =
		mask[HDRImage.GREEN_OFFSET] =
		mask[HDRImage. BLUE_OFFSET] =
		true;
		RGB_MASK = VectorMask.fromValues(FloatVector.SPECIES_128, mask);
	}

	public LayerSources sources;
	public String saveName, displayName;
	public List<SourceParameter<?, ?>> parameters;
	public ChangeListener<Object> parameterListener;

	public LayerSource(LayerSources sources, String saveName, String displayName) {
		this.sources = sources;
		this.saveName = saveName;
		this.displayName = displayName;
		this.parameters = new ArrayList<>();
		this.parameterListener = Util.change(this::requestRedraw);
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

	public JsonMap save() {
		JsonMap map = new JsonMap().with("type",this.saveName);
		for (SourceParameter<?, ?> parameter : this.parameters) {
			parameter.save(map);
		}
		return map;
	}

	public void load(JsonMap map) {
		for (SourceParameter<?, ?> parameter : this.parameters) {
			parameter.load(map);
		}
	}

	public abstract void init(boolean fromSave);

	public void copyFrom(LayerSource source) {
		if (this.getClass() != source.getClass()) {
			throw new IllegalArgumentException("Class mismatch: attempting to copy " + source.getClass() + " to " + this.getClass());
		}
		if (this.parameters.size() != source.parameters.size()) {
			throw new IllegalStateException("Parameter lengths differ: attempting to copy " + source.parameters.size() + " parameters to " + this.parameters.size() + " (on " + this.getClass() + ')');
		}
		for (int index = 0; index < this.parameters.size(); index++) {
			SourceParameter<?, ?> parameter = this.parameters.get(index);
			Object sourceValue = source.parameters.get(index).value().getValue();
			_set(parameter, sourceValue);
		}
	}

	public static <T> void _set(SourceParameter<T, ?> parameter, Object value) {
		parameter.value().setValue(parameter.valueClass.cast(value));
	}

	public void onSelected() {
		this.invalidateStructure();
	}

	public void onDeselected() {
		this.sources.layer.redrawException.set(null);
	}

	public abstract void invalidateStructure();

	public abstract Collection<Layer> getDependencies();

	public abstract boolean isAnimated();

	public abstract Node getRootNode();

	public void requestRedraw() {
		this.sources.layer.requestRedraw();
	}

	public void redraw() {
		try {
			this.doRedraw();
			this.sources.layer.redrawException.set(null);
		}
		catch (Throwable exception) {
			while (exception.getCause() != null) {
				exception = exception.getCause();
			}
			this.sources.layer.redrawException.set(exception);
		}
	}

	public abstract void doRedraw() throws RedrawException;

	@Override
	public String toString() {
		return this.displayName;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}
}