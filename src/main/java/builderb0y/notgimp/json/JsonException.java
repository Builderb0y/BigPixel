package builderb0y.notgimp.json;

public class JsonException extends RuntimeException {

	public JsonException() {}

	public JsonException(String message) {
		super(message);
	}

	public JsonException(Throwable cause) {
		super(cause);
	}

	public JsonException(String message, Throwable cause) {
		super(message, cause);
	}
}