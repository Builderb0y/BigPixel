package builderb0y.notgimp.scripting.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings({ "deprecation", "DeprecatedIsStillUsed", "unused", "ImplicitNumericConversion", "MethodParameterNamingConvention", "LocalVariableNamingConvention" })
public class ExpressionReader {

	public String input;
	public int cursor, line, column;

	public ExpressionReader(String input) {
		this.input = canonicalizeLineEndings(input);
		this.line = 1;
		this.column = 1;
	}

	/** replaces "\r" and "\r\n" with "\n". */
	public static String canonicalizeLineEndings(String input) {
		int length = input.length();
		StringBuilder builder = new StringBuilder(length);
		for (int index = 0; index < length; ) {
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
		return index < this.input.length() ? this.input.charAt(index) : 0;
	}

	@Deprecated
	public boolean canRead() {
		return this.cursor < this.input.length();
	}

	public boolean canReadAfterWhitespace() throws ScriptParsingException {
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
	public char read() throws ScriptParsingException {
		if (this.canRead()) {
			char c = this.input.charAt(this.cursor);
			this.onCharRead(c);
			if (c == 0) {
				throw new ScriptParsingException("Encountered NUL character in input", this);
			}
			return c;
		}
		return 0;
	}

	public char readAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.read();
	}

	@Deprecated
	public char peek() {
		return this.canRead() ? this.input.charAt(this.cursor) : 0;
	}

	public char peekAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peek();
	}

	@Deprecated
	public boolean skip() throws ScriptParsingException {
		return this.read() != 0;
	}

	@Deprecated
	public int skip(int count) {
		count = Math.min(count, this.input.length() - this.cursor);
		for (int i = 0; i < count; i++) {
			this.onCharRead(this.input.charAt(this.cursor));
		}
		return count;
	}

	@Deprecated
	public String read(int count) {
		int startIndex = this.cursor;
		int endIndex = Math.min(startIndex + count, this.input.length());
		String read = this.input.substring(startIndex, endIndex);
		this.onCharsRead(read);
		return read;
	}

	@Deprecated
	public boolean has(char expected) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (got != expected) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	@Deprecated
	public boolean has(String expected) {
		if (this.input.regionMatches(this.cursor, expected, 0, expected.length())) {
			this.onCharsRead(expected);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	@Deprecated
	public boolean has(CharPredicate predicate) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (!predicate.test(got)) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(CharPredicate predicate) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(predicate);
	}

	@Deprecated
	public void skipWhile(CharPredicate predicate) {
		char c;
		while (this.canRead() && predicate.test(c = this.input.charAt(this.cursor))) {
			this.onCharRead(c);
		}
	}

	@Deprecated
	public String readWhile(CharPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.input.substring(start, this.cursor);
	}

	public void skipWhitespace() throws ScriptParsingException {
		while (true) {
			this.skipWhile(Character::isWhitespace);
			if (this.has('"')) {
				int end;
				if (this.has('(')) {
					int depth = 1;
					while (depth > 0) {
						char read = this.read();
						if (read == '(') depth++; else
						if (read == ')') depth--; else
						if (read ==  0 ) throw new ScriptParsingException("Mismatched parentheses in comment", this);
					}
				}
				/*
				else if (this.has(';')) {
					end = this.input.indexOf(";;", this.cursor);
					if (end < 0) throw new ScriptParsingException("Un-terminated multi-line comment", this);
					this.skip(end + 2 - this.cursor);
				}
				*/
				else {
					end = this.input.indexOf('\n', this.cursor);
					if (end < 0) end = this.input.length();
					this.skip(end + 1 - this.cursor);
				}
			}
			else {
				break;
			}
		}
	}

	@Deprecated
	public void expect(char expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	@Deprecated
	public void expect(String expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	@Deprecated
	public String peekOperator() {
		CursorPos old = this.getCursor();
		String operator = this.readOperator();
		this.setCursor(old);
		return operator;
	}

	public String peekOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekOperator();
	}

	@Deprecated
	public String readOperator() {
		return this.readWhile(ExpressionReader::isOperatorSymbol);
	}

	public String readOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readOperator();
	}

	public static boolean isOperatorSymbol(char c) {
		return switch (c) {
			case '!', '#', '$', '%', '&', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '\\', '^', '|', '~' -> true;
			default -> false;
		};
	}

	@Deprecated
	public boolean hasOperator(String operator) {
		CursorPos revert = this.getCursor();
		if (this.has(operator) && !isOperatorSymbol(this.peek())) {
			return true;
		}
		else {
			this.setCursor(revert);
			return false;
		}
	}

	public boolean hasOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasOperator(operator);
	}

	@Deprecated
	public void expectOperator(String operator) throws ScriptParsingException {
		if (!this.hasOperator(operator)) throw new ScriptParsingException("Expected '" + operator + '\'', this);
	}

	public void expectOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectOperator(operator);
	}

	public static boolean isLetterOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
	}

	public static boolean isLetterNumberOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_');
	}

	@Deprecated
	public @Nullable String readIdentifier() throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			int startIndex = this.cursor;
			do {
				this.onCharRead(c);
				c = this.peek();
			}
			while (isLetterNumberOrUnderscore(c));
			return this.input.substring(startIndex, this.cursor);
		}
		else if (c == '`') {
			this.onCharRead('`');
			CursorPos start = this.getCursor();
			while (true) {
				c = this.peek();
				if (c == 0 || c == '\n') {
					this.setCursor(start);
					throw new ScriptParsingException("Un-terminated escaped identifier", this);
				}
				if (c == '`') {
					this.onCharRead('`');
					break;
				}
				this.onCharRead(c);
			}
			return this.input.substring(start.cursor, this.cursor - 1);
		}
		else {
			return null;
		}
	}

	public @Nullable String readIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readIdentifier();
	}

	@Deprecated
	public @Nullable String peekIdentifier() throws ScriptParsingException {
		CursorPos revert = this.getCursor();
		String identifier = this.readIdentifier();
		this.setCursor(revert);
		return identifier;
	}

	public String peekIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekIdentifier();
	}

	@Deprecated
	public boolean hasIdentifier(String identifier) throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			if (
				this.input.regionMatches(this.cursor, identifier, 0, identifier.length()) &&
				!isLetterNumberOrUnderscore(this.getChar(this.cursor + identifier.length()))
			) {
				this.onCharsRead(identifier);
				return true;
			}
		}
		else if (c == '`') {
			if (
				this.input.regionMatches(this.cursor + 1, identifier, 0, identifier.length()) &&
				this.getChar(this.cursor + identifier.length() + 2) == '`'
			) {
				this.onCharRead('`');
				this.onCharsRead(identifier);
				this.onCharRead('`');
				return true;
			}
		}
		return false;
	}

	public boolean hasIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasIdentifier(identifier);
	}

	@Deprecated
	public String expectIdentifier() throws ScriptParsingException {
		String identifier = this.readIdentifier();
		if (identifier != null) return identifier;
		else throw new ScriptParsingException("Expected identifier", this);
	}

	public String expectIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.expectIdentifier();
	}

	@Deprecated
	public void expectIdentifier(String identifier) throws ScriptParsingException {
		if (!this.hasIdentifier(identifier)) {
			throw new ScriptParsingException("Expected '" + identifier + '\'', this);
		}
	}

	public void expectIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectIdentifier(identifier);
	}

	@Deprecated
	public boolean hasNumber() throws ScriptParsingException {
		char c = this.peek();
		return c >= '0' && c <= '9';
	}

	public boolean hasNumberAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasNumber();
	}

	public Number readNumber0(int radix) throws ScriptParsingException {
		BigInteger bigRadix = BigInteger.valueOf(radix);
		BigInteger result = BigInteger.ZERO;
		while (true) {
			char c = this.peek();
			int charValue = Character.digit(c, radix);
			if (charValue >= 0) {
				this.onCharRead(c);
				result = result.multiply(bigRadix).add(BigInteger.valueOf(charValue));
			}
			else if (c == 'x') {
				this.onCharRead('x');
				int newRadix = result.intValueExact();
				if (newRadix >= 2 && newRadix <= 16) {
					return this.readNumber0(newRadix);
				}
				else {
					throw new ScriptParsingException("Radix must be between 2 and 16", this);
				}
			}
			else if (c == '.') {
				this.onCharRead('.');
				BigDecimal decimalResult = new BigDecimal(result);
				BigDecimal smallRadix = BigDecimal.ONE;
				BigDecimal rcpRadix = BigDecimal.ONE.divide(new BigDecimal(bigRadix), MathContext.DECIMAL128);
				while (true) {
					c = this.peek();
					charValue = Character.digit(c, radix);
					if (charValue >= 0) {
						this.onCharRead(c);
						smallRadix = smallRadix.multiply(rcpRadix);
						decimalResult = decimalResult.add(smallRadix.multiply(BigDecimal.valueOf(charValue)));
					}
					else if (c == '.' || c == 'x') {
						throw new ScriptParsingException("Duplicate character: " + c, this);
					}
					else {
						return decimalResult;
					}
				}
			}
			else {
				return result;
			}
		}
	}

	public Number readNumber() throws ScriptParsingException {
		return this.readNumber0(10);
	}

	public String getSource() {
		return this.input;
	}

	public String getSourceForError() {
		//grab the last 10 lines leading up to the error.
		int start = this.cursor;
		for (int loop = 0; loop < 10; loop++) {
			start = this.input.lastIndexOf('\n', start - 1);
			if (start < 0) return this.input.substring(0, this.cursor);
		}
		return this.input.substring(start + 1, this.cursor);
	}

	public void unread() {
		int newCursor = this.cursor - 1;
		char c = this.input.charAt(newCursor);
		if (c == '\n') throw new IllegalStateException("Cannot unread newline");
		this.cursor = newCursor;
		this.column--;
	}

	public CursorPos getCursor() {
		return new CursorPos(this.cursor, this.line, this.column);
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