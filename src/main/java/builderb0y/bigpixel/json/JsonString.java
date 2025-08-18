package builderb0y.bigpixel.json;

public class JsonString implements JsonValue {

	public final String value;

	public JsonString(String value) {
		this.value = value;
	}

	@Override
	public String asString() {
		return this.value;
	}

	@Override
	public String toString() {
		return escape(this.value);
	}

	public static String escape(String string) {
		int length = string.length();
		StringBuilder builder = new StringBuilder(length + 16).append('"');
		for (int index = 0; index < length; index++) {
			char c = string.charAt(index);
			switch (c) {
				case '\n' -> builder.append("\\n");
				case '\r' -> builder.append("\\r");
				case '\t' -> builder.append("\\t");
				case '\f' -> builder.append("\\f");
				case '\b' -> builder.append("\\b");
				case '\\' -> builder.append("\\\\");
				case '"'  -> builder.append("\\\"");
				default -> {
					if (c >= ' ' && c <= '~') {
						builder.append(c);
					}
					else {
						builder.append("\\u");
						builder.append(hex((c >>> 12)     ));
						builder.append(hex((c >>>  8) & 15));
						builder.append(hex((c >>>  4) & 15));
						builder.append(hex((c       ) & 15));
					}
				}
			}
		}
		return builder.append('"').toString();
	}

	public static char hex(int i) {
		return (char)(i + (i >= 10 ? 'a' : '0'));
	}
}