package builderb0y.notgimp.scripting.tree;

public class ArityException extends RuntimeException {

	public ArityException() {}

	public ArityException(String message) {
		super(message);
	}

	public ArityException(Throwable cause) {
		super(cause);
	}

	public ArityException(String message, Throwable cause) {
		super(message, cause);
	}
}