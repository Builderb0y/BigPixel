package builderb0y.notgimp.json;

import java.io.IOException;

public interface JsonValue {

	public default boolean asBoolean() {
		throw new IncorrectJsonTypeException("Not a boolean: " + this);
	}

	public default JsonNumber asNumber() {
		throw new IncorrectJsonTypeException("Not a number: " + this);
	}

	public default String asString() {
		throw new IncorrectJsonTypeException("Not a string: " + this);
	}

	public default JsonArray asArray() {
		throw new IncorrectJsonTypeException("Not an array: " + this);
	}

	public default JsonMap asMap() {
		throw new IncorrectJsonTypeException("Not a map: " + this);
	}

	public default void write(Appendable appendable, int indentation) throws IOException {
		appendable.append(this.toString());
	}

	/*
	public static void main(String[] args) throws Throwable {
		String expected = Files.readString(new File("test images/sparkles.json").toPath());
		JsonValue decoded = new JsonReader(expected).readValueAfterWhitespace();
		Appendable appendable = new StringBuilder(expected.length());
		decoded.write(appendable, 0);
		String actual = appendable.toString();
		System.out.println(actual);
		if (!expected.equals(actual)) {
			throw new AssertionError("Not equal!");
		}
		System.out.println("Equal!");
	}
	*/
}