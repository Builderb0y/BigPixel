package builderb0y.notgimp;

public class SaveException extends RuntimeException {

	public SaveException() {}

	public SaveException(String message) {
		super(message);
	}

	public SaveException(Throwable cause) {
		super(cause);
	}

	public SaveException(String message, Throwable cause) {
		super(message, cause);
	}
}