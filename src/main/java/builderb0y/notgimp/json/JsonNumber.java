package builderb0y.notgimp.json;

public class JsonNumber extends Number implements JsonValue {

	@Override
	public JsonNumber asNumber() {
		return this;
	}

	@Override
	public int intValue() {
		throw new IncorrectJsonTypeException("Not an int: " + this);
	}

	@Override
	public long longValue() {
		throw new IncorrectJsonTypeException("Not a long: " + this);
	}

	@Override
	public float floatValue() {
		throw new IncorrectJsonTypeException("Not a float: " + this);
	}

	@Override
	public double doubleValue() {
		throw new IncorrectJsonTypeException("Not a double: " + this);
	}
}