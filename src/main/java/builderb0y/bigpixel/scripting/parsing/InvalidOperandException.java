package builderb0y.bigpixel.scripting.parsing;

public class InvalidOperandException extends IllegalArgumentException {

	public InvalidOperandException() {}

	public InvalidOperandException(String message) {
		super(message);
	}

	public InvalidOperandException(Throwable cause) {
		super(cause);
	}

	public InvalidOperandException(String message, Throwable cause) {
		super(message, cause);
	}
}