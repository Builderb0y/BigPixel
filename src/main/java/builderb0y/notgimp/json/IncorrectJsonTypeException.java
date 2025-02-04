package builderb0y.notgimp.json;

public class IncorrectJsonTypeException extends JsonStructureException {

	public IncorrectJsonTypeException() {}

	public IncorrectJsonTypeException(String message) {
		super(message);
	}

	public IncorrectJsonTypeException(Throwable cause) {
		super(cause);
	}

	public IncorrectJsonTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}