package builderb0y.notgimp.sources;

import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.json.JsonString;
import builderb0y.notgimp.json.JsonValue;

public abstract class SourceParameter<T_Value, T_Holder> {

	public final T_Holder holder;
	public final String name;
	public final Class<T_Value> valueClass;

	public SourceParameter(T_Holder holder, String name, Class<T_Value> aClass) {
		this.holder = holder;
		this.name = name;
		this.valueClass = aClass;
	}

	public abstract Property<T_Value> value();

	public T_Value get() {
		return this.value().getValue();
	}

	public void set(T_Value value) {
		this.value().setValue(value);
	}

	public abstract void save(JsonMap map);

	public abstract void load(JsonMap map);

	public static SourceParameter<Boolean, CheckBox> checkbox(CheckBox checkBox, String name) {
		return new SourceParameter<>(checkBox, name, Boolean.class) {

			@Override
			public Property<Boolean> value() {
				return this.holder.selectedProperty();
			}

			@Override
			public void save(JsonMap map) {
				map.put(this.name, this.get());
			}

			@Override
			public void load(JsonMap map) {
				this.set(map.getBoolean(this.name));
			}
		};
	}

	public static SourceParameter<Integer, Spinner<Integer>> intSpinner(Spinner<Integer> spinner, String name) {
		return new SourceParameter<>(spinner, name, Integer.class) {

			@Override
			public Property<Integer> value() {
				return this.holder.getValueFactory().valueProperty();
			}

			@Override
			public void save(JsonMap map) {
				map.put(this.name, this.get());
			}

			@Override
			public void load(JsonMap map) {
				this.set(map.getInt(this.name));
			}
		};
	}

	public static SourceParameter<Float, Spinner<Float>> floatSpinner(Spinner<Float> spinner, String name) {
		return new SourceParameter<>(spinner, name, Float.class) {

			@Override
			public Property<Float> value() {
				return spinner.getValueFactory().valueProperty();
			}

			@Override
			public void save(JsonMap map) {
				map.put(this.name, this.get());
			}

			@Override
			public void load(JsonMap map) {
				this.set(map.getFloat(this.name));
			}
		};
	}

	public static SourceParameter<Double, Spinner<Double>> doubleSpinner(Spinner<Double> spinner, String name) {
		return new SourceParameter<>(spinner, name, Double.class) {

			@Override
			public Property<Double> value() {
				return this.holder.getValueFactory().valueProperty();
			}

			@Override
			public void save(JsonMap map) {
				map.put(this.name, this.get());
			}

			@Override
			public void load(JsonMap map) {
				this.set(map.getDouble(this.name));
			}
		};
	}

	public static <E extends Enum<E>> SourceParameter<E, ChoiceBox<E>> enumChoiceBox(ChoiceBox<E> box, Class<E> enumClass, String name) {
		return new SourceParameter<>(box, name, enumClass) {

			@Override
			public Property<E> value() {
				return this.holder.valueProperty();
			}

			@Override
			public void save(JsonMap map) {
				map.put(this.name, this.get().name());
			}

			@Override
			public void load(JsonMap map) {
				this.set(Enum.valueOf(this.valueClass, map.getString(this.name)));
			}
		};
	}

	public static SourceParameter<String, ChoiceBox<String>> stringChoiceBox(ChoiceBox<String> box, String name) {
		return new SourceParameter<>(box, name, String.class) {

			@Override
			public Property<String> value() {
				return box.valueProperty();
			}

			@Override
			public void save(JsonMap map) {
				String value = this.get();
				if (value != null) map.put(this.name, value);
			}

			@Override
			public void load(JsonMap map) {
				if (map.get(this.name) instanceof JsonString string) {
					this.set(string.value);
				}
			}
		};
	}

	public static SourceParameter<FloatVector, ColorBox> colorBox(ColorBox box, String name) {
		return new SourceParameter<>(box, name, FloatVector.class) {

			@Override
			public Property<FloatVector> value() {
				return box.color;
			}

			@Override
			public void save(JsonMap map) {
				FloatVector color = this.get();
				map.put(this.name, colorToJson(color));
			}

			@Override
			public void load(JsonMap map) {
				JsonArray array = map.getArray(this.name);
				this.set(colorFromJson(array));
			}
		};
	}

	public static JsonArray colorToJson(FloatVector color) {
		return (
			new JsonArray()
			.with(color.lane(0))
			.with(color.lane(1))
			.with(color.lane(2))
			.with(color.lane(3))
		);
	}

	public static FloatVector colorFromJson(JsonArray array) {
		return FloatVector.fromArray(
			FloatVector.SPECIES_128,
			new float[] {
				array.getFloat(0),
				array.getFloat(1),
				array.getFloat(2),
				array.getFloat(3)
			},
			0
		);
	}

	public static SourceParameter<String, TextArea> code(TextArea area, String name) {
		return new SourceParameter<>(area, name, String.class) {

			@Override
			public Property<String> value() {
				return this.holder.textProperty();
			}

			@Override
			public void save(JsonMap map) {
				JsonArray array = new JsonArray();
				this.get().lines().forEachOrdered(array::add);
				map.put(this.name, array);
			}

			@Override
			public void load(JsonMap map) {
				this.set(map.getArray(this.name).stream().map(JsonValue::asString).collect(Collectors.joining(System.lineSeparator())));
			}
		};
	}
}