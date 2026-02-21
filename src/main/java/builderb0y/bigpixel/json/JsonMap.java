package builderb0y.bigpixel.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class JsonMap extends LinkedHashMap<@NotNull String, @NotNull JsonValue> implements JsonValue {

	public boolean inline;

	public JsonMap() {}

	public JsonMap(int initialCapacity) {
		super(initialCapacity);
	}

	public JsonMap(Map<String, ? extends JsonValue> map) {
		super(map);
	}

	public JsonMap inline(boolean inline) {
		this.inline = inline;
		return this;
	}

	@Override
	public JsonMap asMap() {
		return this;
	}

	public JsonValue require(String key) {
		JsonValue value = this.get(key);
		if (value != null) return value;
		else throw new JsonStructureException("No such value for key " + key + " in map " + this);
	}

	public boolean getBoolean(String key) {
		return this.require(key).asBoolean();
	}

	public JsonNumber getNumber(String key) {
		return this.require(key).asNumber();
	}

	public int getInt(String key) {
		return this.getNumber(key).intValue();
	}

	public long getLong(String key) {
		return this.getNumber(key).longValue();
	}

	public float getFloat(String key) {
		return this.getNumber(key).floatValue();
	}

	public double getDouble(String key) {
		return this.getNumber(key).doubleValue();
	}

	public String getString(String key) {
		return this.require(key).asString();
	}

	public JsonArray getArray(String key) {
		return this.require(key).asArray();
	}

	public JsonMap getMap(String key) {
		return this.require(key).asMap();
	}

	public JsonValue evict(String key) {
		JsonValue value = this.remove(key);
		if (value != null) return value;
		else throw new JsonStructureException("No such value for key " + key + " in map " + this);
	}

	public boolean removeBoolean(String key) {
		return this.evict(key).asBoolean();
	}

	public JsonNumber removeNumber(String key) {
		return this.evict(key).asNumber();
	}

	public int removeInt(String key) {
		return this.removeNumber(key).intValue();
	}

	public long removeLong(String key) {
		return this.removeNumber(key).longValue();
	}

	public float removeFloat(String key) {
		return this.removeNumber(key).floatValue();
	}

	public double removeDouble(String key) {
		return this.removeNumber(key).doubleValue();
	}

	public String removeString(String key) {
		return this.evict(key).asString();
	}

	public JsonArray removeArray(String key) {
		return this.evict(key).asArray();
	}

	public JsonMap removeMap(String key) {
		return this.evict(key).asMap();
	}

	public void add(String key, JsonValue value) {
		JsonValue old = this.putIfAbsent(key, value);
		if (old != null) {
			throw new JsonException("Duplicate key: " + key + " (replacing " + old + " with " + value + ')');
		}
	}

	public void add(String key, boolean value) {
		this.add(key, JsonBoolean.valueOf(value));
	}

	public void add(String key, int value) {
		this.add(key, new JsonFixedPoint(value));
	}

	public void add(String key, long value) {
		this.add(key, new JsonFixedPoint(value));
	}

	public void add(String key, float value) {
		this.add(key, new JsonFloatingPoint(value));
	}

	public void add(String key, double value) {
		this.add(key, new JsonFloatingPoint(value));
	}

	public void add(String key, String value) {
		this.add(key, new JsonString(value));
	}

	public JsonMap with(String key, boolean value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, int value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, long value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, float value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, double value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, String value) {
		this.add(key, value);
		return this;
	}

	public JsonMap with(String key, JsonValue value) {
		this.add(key, value);
		return this;
	}

	public JsonValue put(String key, boolean value) {
		return this.put(key, JsonBoolean.valueOf(value));
	}

	public JsonValue put(String key, int value) {
		return this.put(key, new JsonFixedPoint(value));
	}

	public JsonValue put(String key, long value) {
		return this.put(key, new JsonFixedPoint(value));
	}

	public JsonValue put(String key, float value) {
		return this.put(key, new JsonFloatingPoint(value));
	}

	public JsonValue put(String key, double value) {
		return this.put(key, new JsonFloatingPoint(value));
	}

	public JsonValue put(String key, String value) {
		return this.put(key, new JsonString(value));
	}

	public JsonMap rename(String from, String to) {
		this.put(to, this.evict(from));
		return this;
	}

	@Override
	public String toString() {
		if (this.isEmpty()) return "{}";
		StringBuilder builder = new StringBuilder(this.size() << 6).append("{ ");
		Iterator<Map.Entry<String, JsonValue>> iterator = this.entrySet().iterator();
		while (true) {
			Map.Entry<String, JsonValue> entry = iterator.next();
			builder.append(JsonString.escape(entry.getKey())).append(": ").append(entry.getValue());
			if (!iterator.hasNext()) return builder.append(" }").toString();
			else builder.append(", ");
		}
	}

	@Override
	public void write(Appendable appendable, int indentation) throws IOException {
		if (this.isEmpty()) {
			appendable.append("{}");
			return;
		}
		Iterator<Map.Entry<String, JsonValue>> iterator = this.entrySet().iterator();
		if (this.inline || indentation < 0) {
			appendable.append("{ ");
			Map.Entry<String, JsonValue> entry = iterator.next();
			entry.getValue().write(appendable.append(JsonString.escape(entry.getKey())).append(": "), -1);
			while (iterator.hasNext()) {
				entry = iterator.next();
				entry.getValue().write(appendable.append(", ").append(JsonString.escape(entry.getKey())).append(": "), -1);
			}
			appendable.append(" }");
		}
		else {
			appendable.append('{');
			String indentString = "\t".repeat(indentation + 1);
			while (true) {
				Map.Entry<String, JsonValue> entry = iterator.next();
				appendable.append('\n').append(indentString).append(JsonString.escape(entry.getKey())).append(": ");
				entry.getValue().write(appendable, indentation + 1);
				if (!iterator.hasNext()) {
					appendable.append('\n').append(indentString, 0, indentation).append("}");
					break;
				}
				appendable.append(',');
			}
		}
	}
}