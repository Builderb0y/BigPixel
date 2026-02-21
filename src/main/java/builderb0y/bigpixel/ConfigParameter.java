package builderb0y.bigpixel;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.JsonConverter.*;
import builderb0y.bigpixel.json.JsonMap;

public class ConfigParameter<T_Value> {

	public final ParameterMultiStorage<T_Value> storage;
	public final String saveName;
	public final Class<T_Value> valueClass;
	public final JsonConverter<T_Value> converter;

	public ConfigParameter(ParameterMultiStorage<T_Value> storage, String saveName, Class<T_Value> aClass, JsonConverter<T_Value> converter) {
		this.storage = storage;
		this.saveName = saveName;
		this.valueClass = aClass;
		this.converter = converter;
	}

	public T_Value get() {
		return this.storage.property.getValue();
	}

	public void set(T_Value value) {
		this.storage.property.setValue(value);
	}

	public void save(JsonMap map) {
		map.put(this.saveName, this.storage.save(this.converter));
	}

	public void load(JsonMap map) {
		this.storage.load(map.getMap(this.saveName), this.converter);
	}

	public static ConfigParameter<Boolean> createBoolean(ParameterMultiStorage<Boolean> storage, String saveName) {
		return new ConfigParameter<>(storage, saveName, Boolean.class, BooleanJsonConverter.INSTANCE);
	}

	public static ConfigParameter<Integer> createInt(ParameterMultiStorage<Integer> storage, String saveName) {
		return new ConfigParameter<>(storage, saveName, Integer.class, IntJsonConverter.INSTANCE);
	}

	public static ConfigParameter<Long> createLong(ParameterMultiStorage<Long> storage, String saveName) {
		return new ConfigParameter<>(storage, saveName, Long.class, LongJsonConverter.INSTANCE);
	}

	public static ConfigParameter<Float> createFloat(ParameterMultiStorage<Float> storage, String name) {
		return new ConfigParameter<>(storage, name, Float.class, FloatJsonConverter.INSTANCE);
	}

	public static ConfigParameter<Double> createDouble(ParameterMultiStorage<Double> storage, String name) {
		return new ConfigParameter<>(storage, name, Double.class, DoubleJsonConverter.INSTANCE);
	}

	public static ConfigParameter<String> createString(ParameterMultiStorage<String> storage, String name) {
		return new ConfigParameter<>(storage, name, String.class, StringJsonConverter.INSTANCE);
	}

	public static ConfigParameter<String> createMultiLineString(ParameterMultiStorage<String> storage, String name) {
		return new ConfigParameter<>(storage, name, String.class, MultiLineStringJsonConverter.INSTANCE);
	}

	public static ConfigParameter<FloatVector> createColor(ParameterMultiStorage<FloatVector> storage, String name) {
		return new ConfigParameter<>(storage, name, FloatVector.class, ColorJsonConverter.INSTANCE);
	}

	public static <E extends Enum<E>> ConfigParameter<E> createEnum(ParameterMultiStorage<E> storage, Class<E> enumClass, String name) {
		return new ConfigParameter<>(storage, name, enumClass, new EnumJsonConverter<>(enumClass));
	}
}