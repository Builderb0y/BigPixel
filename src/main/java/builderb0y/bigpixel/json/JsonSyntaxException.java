package builderb0y.bigpixel.json;

import org.jetbrains.annotations.Nullable;

public class JsonSyntaxException extends JsonException {

	public JsonSyntaxException(String message, JsonReader reader) {
		super(appendContext(message, reader));
	}

	public JsonSyntaxException(String message, Throwable cause, JsonReader input) {
		super(appendContext(message, input), cause);
	}

	public JsonSyntaxException(Throwable cause, JsonReader input) {
		super(appendContext(cause.getMessage(), input), cause);
	}

	public static @Nullable String appendContext(@Nullable String message, @Nullable JsonReader input) {
		if (message != null) {
			if (input != null) {
				return message + " at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
			}
			else {
				return message;
			}
		}
		else {
			if (input != null) {
				return "Unknown error at line " + input.line + ", column " + input.column + ":\n" + input.getSourceForError() + " <--- HERE";
			}
			else {
				return null;
			}
		}
	}
}