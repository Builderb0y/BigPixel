package builderb0y.notgimp.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

public class JsonArray extends ArrayList<@NotNull JsonValue> implements JsonValue {

	public JsonArray() {}

	public JsonArray(int initialCapacity) {
		super(initialCapacity);
	}

	public JsonArray(@NotNull Collection<? extends JsonValue> collection) {
		super(collection);
	}

	public JsonArray(JsonValue... elements) {
		super(Arrays.asList(elements));
	}

	@Override
	public JsonArray asArray() {
		return this;
	}

	public boolean getBoolean(int index) {
		return this.get(index).asBoolean();
	}

	public JsonNumber getNumber(int index) {
		return this.get(index).asNumber();
	}

	public int getInt(int index) {
		return this.getNumber(index).intValue();
	}

	public long getLong(int index) {
		return this.getNumber(index).longValue();
	}

	public float getFloat(int index) {
		return this.getNumber(index).floatValue();
	}

	public double getDouble(int index) {
		return this.getNumber(index).doubleValue();
	}

	public String getString(int index) {
		return this.get(index).asString();
	}

	public JsonArray getArray(int index) {
		return this.get(index).asArray();
	}

	public JsonMap getMap(int index) {
		return this.get(index).asMap();
	}

	public JsonValue setBoolean(int index, boolean value) {
		return this.set(index, JsonBoolean.valueOf(value));
	}

	public JsonValue setInt(int index, int value) {
		return this.set(index, new JsonFixedPoint(value));
	}

	public JsonValue setLong(int index, long value) {
		return this.set(index, new JsonFixedPoint(value));
	}

	public JsonValue setFloat(int index, float value) {
		return this.set(index, new JsonFloatingPoint(value));
	}

	public JsonValue setDouble(int index, double value) {
		return this.set(index, new JsonFloatingPoint(value));
	}

	public JsonValue setString(int index, String value) {
		return this.set(index, new JsonString(value));
	}

	public JsonValue setArray(int index, JsonArray array) {
		return this.set(index, array);
	}

	public JsonValue setMap(int index, JsonMap map) {
		return this.set(index, map);
	}

	public void add(int index, boolean value) {
		this.add(index, JsonBoolean.valueOf(value));
	}

	public void add(int index, int value) {
		this.add(index, new JsonFixedPoint(value));
	}

	public void add(int index, long value) {
		this.add(index, new JsonFixedPoint(value));
	}

	public void add(int index, float value) {
		this.add(index, new JsonFloatingPoint(value));
	}

	public void add(int index, double value) {
		this.add(index, new JsonFloatingPoint(value));
	}

	public void add(int index, String value) {
		this.add(index, new JsonString(value));
	}

	public void add(boolean value) {
		this.add(JsonBoolean.valueOf(value));
	}

	public void add(int value) {
		this.add(new JsonFixedPoint(value));
	}

	public void add(long value) {
		this.add(new JsonFixedPoint(value));
	}

	public void add(float value) {
		this.add(new JsonFloatingPoint(value));
	}

	public void add(double value) {
		this.add(new JsonFloatingPoint(value));
	}

	public void add(String value) {
		this.add(new JsonString(value));
	}

	@Override
	public void write(Appendable appendable, int indentation) throws IOException {
		if (this.isEmpty()) {
			appendable.append("[]");
			return;
		}
		appendable.append('[');
		String indentString = "\t".repeat(indentation + 1);
		for (int index = 0, size = this.size(); index < size;) {
			appendable.append('\n').append(indentString);
			this.get(index).write(appendable, indentation + 1);
			if (++index >= size) {
				appendable.append('\n').append(indentString, 0, indentation).append(']');
				return;
			}
			appendable.append(',');
		}
	}
}