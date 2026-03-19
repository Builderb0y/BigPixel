package builderb0y.bigpixel;

@SuppressWarnings({ "deprecation", "DeprecatedIsStillUsed", "unused", "ImplicitNumericConversion", "MethodParameterNamingConvention", "LocalVariableNamingConvention" })
public abstract class CommonReader<X extends Throwable> {

	public final String source;
	public int cursor, line, column;

	public CommonReader(String source) {
		this.source = source;
		this.line = 1;
		this.column = 1;
	}

	/** replaces "\r" and "\r\n" with "\n". */
	public static String canonicalizeLineEndings(String input) {
		if (input.indexOf('\0') >= 0) throw new IllegalArgumentException("NUL character in input");
		int start = input.indexOf('\r');
		if (start < 0) return input;

		int length = input.length();
		StringBuilder builder = new StringBuilder(length).append(input, 0, start);
		for (int index = start; index < length;) {
			char c = input.charAt(index);
			if (c == '\r') {
				index++;
				if (index < length && input.charAt(index) == '\n') {
					index++;
				}
				builder.append('\n');
			}
			else {
				builder.append(c);
				index++;
			}
		}
		return builder.toString();
	}

	@Deprecated
	public char getChar(int index) {
		return index < this.source.length() ? this.source.charAt(index) : 0;
	}

	@Deprecated
	public boolean canRead() {
		return this.cursor < this.source.length();
	}

	public boolean canReadAfterWhitespace() throws X {
		this.skipWhitespace();
		return this.canRead();
	}

	public void onCharRead(char c) {
		this.cursor++;
		if (c == '\n') {
			this.line++;
			this.column = 1;
		}
		else {
			this.column++;
		}
	}

	public void onCharsRead(String s) {
		for (int index = 0, length = s.length(); index < length; index++) {
			this.onCharRead(s.charAt(index));
		}
	}

	@Deprecated
	public char read() {
		if (this.canRead()) {
			char c = this.source.charAt(this.cursor);
			this.onCharRead(c);
			return c;
		}
		return 0;
	}

	public char readAfterWhitespace() throws X {
		this.skipWhitespace();
		return this.read();
	}

	@Deprecated
	public char peek() {
		return this.canRead() ? this.source.charAt(this.cursor) : 0;
	}

	public char peekAfterWhitespace() throws X {
		this.skipWhitespace();
		return this.peek();
	}

	@Deprecated
	public boolean skip() {
		return this.read() != 0;
	}

	@Deprecated
	public int skip(int count) {
		count = Math.min(count, this.source.length() - this.cursor);
		for (int i = 0; i < count; i++) {
			this.onCharRead(this.source.charAt(this.cursor));
		}
		return count;
	}

	@Deprecated
	public String read(int count) {
		int startIndex = this.cursor;
		int endIndex = Math.min(startIndex + count, this.source.length());
		String read = this.source.substring(startIndex, endIndex);
		this.onCharsRead(read);
		return read;
	}

	@Deprecated
	public boolean has(char expected) {
		if (!this.canRead()) return false;
		char got = this.source.charAt(this.cursor);
		if (got != expected) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(char expected) throws X {
		this.skipWhitespace();
		return this.has(expected);
	}

	@Deprecated
	public boolean has(String expected) {
		if (this.source.regionMatches(this.cursor, expected, 0, expected.length())) {
			this.onCharsRead(expected);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(String expected) throws X {
		this.skipWhitespace();
		return this.has(expected);
	}

	@Deprecated
	public boolean has(CharPredicate predicate) {
		if (!this.canRead()) return false;
		char got = this.source.charAt(this.cursor);
		if (!predicate.test(got)) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(CharPredicate predicate) throws X {
		this.skipWhitespace();
		return this.has(predicate);
	}

	public void skipWhile(CharPredicate predicate) {
		for (char c; this.canRead() && predicate.test(c = this.source.charAt(this.cursor));) {
			this.onCharRead(c);
		}
	}

	@Deprecated
	public String readWhile(CharPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.source.substring(start, this.cursor);
	}

	@Deprecated
	public void expect(char expected) throws X {
		if (!this.has(expected)) {
			throw this.newException("Expected '" + expected + '\'');
		}
	}

	public void expectAfterWhitespace(char expected) throws X {
		this.skipWhitespace();
		this.expect(expected);
	}

	@Deprecated
	public void expect(String expected) throws X {
		if (!this.has(expected)) {
			throw this.newException("Expected '" + expected + '\'');
		}
	}

	public void expectAfterWhitespace(String expected) throws X {
		this.skipWhitespace();
		this.expect(expected);
	}

	public abstract void skipWhitespace() throws X;

	public String getSource() {
		return this.source;
	}

	public String getSourceForError() {
		//grab the last 10 lines leading up to the error.
		int start = this.cursor;
		for (int loop = 0; loop < 10; loop++) {
			start = this.source.lastIndexOf('\n', start - 1);
			if (start < 0) return this.source.substring(0, this.cursor);
		}
		return this.source.substring(start + 1, this.cursor);
	}

	public void unread() {
		int newCursor = this.cursor - 1;
		char c = this.source.charAt(newCursor);
		if (c == '\n') throw new IllegalStateException("Cannot unread newline");
		this.cursor = newCursor;
		this.column--;
	}

	public abstract X newException(String message);

	public CursorPos getCursor() {
		return new CursorPos(this.cursor, this.line, this.column);
	}

	public CursorPos getCursorAfterWhitespace() throws X {
		this.skipWhitespace();
		return this.getCursor();
	}

	public void setCursor(CursorPos position) {
		this.cursor = position.cursor;
		this.line   = position.line  ;
		this.column = position.column;
	}

	public static record CursorPos(int cursor, int line, int column) {}

	@FunctionalInterface
	public static interface CharPredicate {

		public abstract boolean test(char c);
	}
}