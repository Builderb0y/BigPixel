package builderb0y.bigpixel.scripting.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.CommonReader;

@SuppressWarnings({ "deprecation", "DeprecatedIsStillUsed", "unused", "ImplicitNumericConversion", "MethodParameterNamingConvention", "LocalVariableNamingConvention" })
public class ExpressionReader extends CommonReader<ScriptParsingException> {

	public ExpressionReader(String input) {
		super(input);
	}

	@Override
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
					end = this.source.indexOf(";;", this.cursor);
					if (end < 0) throw new ScriptParsingException("Un-terminated multi-line comment", this);
					this.skip(end + 2 - this.cursor);
				}
				*/
				else {
					end = this.source.indexOf('\n', this.cursor);
					if (end < 0) end = this.source.length();
					this.skip(end + 1 - this.cursor);
				}
			}
			else {
				break;
			}
		}
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
			return this.source.substring(startIndex, this.cursor);
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
			return this.source.substring(start.cursor(), this.cursor - 1);
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
	public boolean hasIdentifier(String identifier) {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			if (
				this.source.regionMatches(this.cursor, identifier, 0, identifier.length()) &&
				!isLetterNumberOrUnderscore(this.getChar(this.cursor + identifier.length()))
			) {
				this.onCharsRead(identifier);
				return true;
			}
		}
		else if (c == '`') {
			if (
				this.source.regionMatches(this.cursor + 1, identifier, 0, identifier.length()) &&
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

	@Override
	public ScriptParsingException newException(String message) {
		return new ScriptParsingException(message, this);
	}
}