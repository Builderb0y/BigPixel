package builderb0y.notgimp.json;

public class JsonBoolean implements JsonValue {

	public static final JsonBoolean
		TRUE = new JsonBoolean(true),
		FALSE = new JsonBoolean(false);

	public final boolean value;

	public JsonBoolean(boolean value) {
		this.value = value;
	}

	public static JsonBoolean valueOf(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public boolean asBoolean() {
		return this.value;
	}

	@Override
	public String toString() {
		return Boolean.toString(this.value);
	}
}