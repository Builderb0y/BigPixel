package builderb0y.bigpixel.json;

import builderb0y.bigpixel.CommonReader;

public class JsonReader extends CommonReader<JsonSyntaxException> {

	public JsonReader(String source) {
		super(source);
	}

	@Deprecated
	public String readString() {
		this.expect('"');
		int start = this.cursor;
		StringBuilder builder = null;
		while (true) {
			char c = this.read();
			switch (c) {
				case '"' -> {
					return builder != null ? builder.toString() : this.source.substring(start, this.cursor - 1);
				}
				case '\\' -> {
					c = this.read();
					switch (c) {
						case 'n' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\n');
						}
						case 'r' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\r');
						}
						case 't' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\t');
						}
						case 'f' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\f');
						}
						case 'b' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\b');
						}
						case '"' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('"');
						}
						case '\\' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append('\\');
						}
						case 'u' -> {
							if (builder == null) builder = new StringBuilder(this.cursor - start + 16).append(this.source, start, this.cursor - 2);
							builder.append((char)(
								(this.hex() << 12) |
								(this.hex() <<  8) |
								(this.hex() <<  4) |
								(this.hex()      )
							));
						}
						default -> {
							throw new JsonSyntaxException("Unknown escaped char", this);
						}
					}
				}
				default -> {
					if (c < ' ') {
						throw new JsonSyntaxException("Control characters are not allowed in json strings", this);
					}
					if (builder != null) builder.append(c);
				}
			}
		}
	}

	@Deprecated
	public int hex() {
		char c = this.read();
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return c - 'a';
		if (c >= 'A' && c <= 'F') return c - 'A';
		throw new JsonSyntaxException("Unknown hex character", this);
	}

	public String readStringAfterWhitespace() {
		this.skipWhitespace();
		return this.readString();
	}

	@Deprecated
	public JsonNumber readNumber() {
		try {
			int start = this.cursor;
			this.has('-');
			this.skipWhile(JsonReader::isDigit);
			if (this.has('.')) {
				this.skipWhile(JsonReader::isDigit);
				if (this.has('e')) {
					this.has('-');
					this.skipWhile(JsonReader::isDigit);
				}
				return new JsonFloatingPoint(Double.parseDouble(this.source.substring(start, this.cursor)));
			}
			else {
				return new JsonFixedPoint(Long.parseLong(this.source.substring(start, this.cursor)));
			}
		}
		catch (NumberFormatException exception) {
			throw new JsonSyntaxException(exception, this);
		}
	}

	public static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	public JsonNumber readNumberAfterWhitespace() {
		this.skipWhitespace();
		return this.readNumber();
	}

	@Deprecated
	public JsonValue readValue() {
		return switch (this.peek()) {
			case '"' -> new JsonString(this.readString());
			case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> this.readNumber();
			case 't', 'f' -> {
				if (this.has("true")) yield JsonBoolean.TRUE;
				if (this.has("false")) yield JsonBoolean.FALSE;
				throw new JsonSyntaxException("Not true or false", this);
			}
			case '[' -> {
				this.onCharRead('[');
				JsonArray array = new JsonArray();
				if (this.hasAfterWhitespace(']')) {
					yield array;
				}
				while (true) {
					array.add(this.readValueAfterWhitespace());
					if (this.hasAfterWhitespace(']')) break;
					else if (this.hasAfterWhitespace(',')) continue;
					else throw new JsonSyntaxException("Expected ']' or ','", this);
				}
				yield array;
			}
			case '{' -> {
				this.onCharRead('{');
				JsonMap map = new JsonMap();
				if (this.hasAfterWhitespace('}')) yield map;
				while (true) {
					String key = this.readStringAfterWhitespace();
					this.expectAfterWhitespace(':');
					JsonValue value = this.readValueAfterWhitespace();
					map.add(key, value);
					if (this.hasAfterWhitespace('}')) break;
					else if (this.hasAfterWhitespace(',')) continue;
					else throw new JsonSyntaxException("Expected '}' or ','", this);
				}
				yield map;
			}
			default -> {
				throw new JsonSyntaxException("Syntax error", this);
			}
		};
	}

	public JsonValue readValueAfterWhitespace() {
		this.skipWhitespace();
		return this.readValue();
	}

	@Override
	public void skipWhitespace() {
		this.skipWhile(Character::isWhitespace);
	}

	@Override
	public JsonSyntaxException newException(String message) {
		return new JsonSyntaxException(message, this);
	}
}