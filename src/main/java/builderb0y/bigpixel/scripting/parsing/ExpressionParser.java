package builderb0y.bigpixel.scripting.parsing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigpixel.CommonReader.CursorPos;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.FunctionHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.bigpixel.scripting.tree.*;
import builderb0y.bigpixel.scripting.tree.InsnTree.Assigner;
import builderb0y.bigpixel.scripting.tree.condition.*;
import builderb0y.bigpixel.scripting.types.RngOperations;
import builderb0y.bigpixel.scripting.types.UtilityOperations;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.types.VectorType.ComponentType;
import builderb0y.bigpixel.scripting.types.VectorType.GroupShape;
import builderb0y.bigpixel.scripting.util.BinaryOperatorWrapper;
import builderb0y.bigpixel.scripting.util.LocalVariable;
import builderb0y.bigpixel.scripting.util.MethodInfo;
import builderb0y.bigpixel.scripting.util.UnaryOperatorWrapper;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.util.Util;

import static org.objectweb.asm.Opcodes.*;

public class ExpressionParser<I> {

	public static final AtomicInteger UNIQUIFIER = new AtomicInteger();

	public final ExpressionReader reader;
	public final Method implMethod;
	public Scope scope;

	public ExpressionParser(ExpressionReader reader, Method implMethod) {
		this.reader = reader;
		this.implMethod = implMethod;
		this.scope = this.new Scope();
	}

	public ExpressionParser(String source, Class<I> interfaceClass) {
		Method found = null;
		for (Method method : interfaceClass.getMethods()) {
			if (Modifier.isAbstract(method.getModifiers())) {
				if (found == null) found = method;
				else throw new IllegalArgumentException(interfaceClass + " must have exactly one abstract method.");
			}
		}
		this(new ExpressionReader(source), found);
	}

	public ExpressionParser<I> addBuiltins() {
		for (VectorType type : VectorType.VALUES) {
			this.scope.environment.addFunction(type.name, (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				VectorType[] types = InsnTree.flattenTypes(params);
				String fullName = name + "_from_" + Arrays.stream(types).map((VectorType paramType) -> paramType.name).collect(Collectors.joining("_"));
				try {
					MethodInfo model = new MethodInfo(VectorOperations.class, fullName);
					return new VectorConstructorInsnTree(type, params, model);
				}
				catch (IllegalArgumentException exception) {
					return null;
				}
			});
			if (type.shape != GroupShape.UNIT) {
				this.scope.environment.addIndex(type, (ExpressionParser<?> parser, InsnTree receiver, InsnTree[] params) -> {
					InsnTree[] castArguments = ScriptHandlers.multiCast(params, VectorType.INT);
					if (castArguments == null) return null;
					return new IndexInsnTree(
						VectorType.get(receiver.type().componentType, GroupShape.UNIT),
						receiver,
						castArguments,
						new MethodInfo(
							receiver.type().holderClass(),
							receiver.type().componentType == ComponentType.BOOLEAN ? "laneIsSet" : "lane",
							int.class
						)
					);
				});
			}
		}
		for (UnaryOperatorWrapper operator : UnaryOperatorWrapper.VALUES) {
			this.scope.environment.addFunction(operator.name().toLowerCase(Locale.ROOT), (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				if (params.length != 1) return null;
				return UnaryInsnTree.create(params[0], operator);
			});
		}
		for (BinaryOperatorWrapper operator : BinaryOperatorWrapper.VALUES) {
			this.scope.environment.addFunction(operator.name().toLowerCase(Locale.ROOT), (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				return switch (params.length) {
					case 1 -> BinaryInsnTree.createUnpacked(params[0], operator);
					case 2 -> BinaryInsnTree.create(params[0], params[1], operator);
					default -> null;
				};
			});
		}
		for (String name : new String[] { "dot", "lengthSquared", "length", "normalize", "mix", "unmix", "reverseBits" }) {
			this.scope.environment.addFunction(name, (ExpressionParser<?> parser, String name_, InsnTree[] params) -> {
				String fullName = name_ + '_' + Arrays.stream(params).map(InsnTree::types).flatMap(Arrays::stream).map((VectorType t) -> t.name).collect(Collectors.joining("_"));
				try {
					MethodInfo model = new MethodInfo(VectorOperations.class, fullName);
					return new InvokeInsnTree(model.vectorReturnType(), params, model);
				}
				catch (IllegalArgumentException exception) {
					return null;
				}
			});
		}
		this.scope.environment.addFunction("rng", (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
			for (InsnTree tree : params) {
				for (VectorType type : tree.types()) {
					if (type.shape != GroupShape.UNIT) {
						return null;
					}
				}
			}
			return new RandomInsnTree(params);
		});
		for (String sign : new String[] { "positive", "uniform", "bounded", "ranged" }) {
			for (VectorType outType : VectorType.VALUES) {
				if (outType.componentType != ComponentType.BOOLEAN && outType != VectorType.VOID) {
					MethodInfo method = new MethodInfo(RngOperations.class, "rng_to_" + sign + '_' + outType.name);
					this.scope.environment.addMethod(
						VectorType.LONG,
						"next" + Util.capitalize(sign) + Util.capitalize(outType.name),
						(ExpressionParser<?> parser, InsnTree receiver, String name, InsnTree[] params) -> {
							try {
								return new InvokeInsnTree(method.vectorReturnType(), receiver, params, method);
							}
							catch (IllegalArgumentException exception) {
								return null;
							}
						}
					);
				}
			}
		}
		for (Method method : UtilityOperations.class.getMethods()) {
			if (Modifier.isStatic(method.getModifiers())) { //exclude equals(), hashCode(), toString().
				int index = method.getName().indexOf('_');
				this.scope.environment.addFunction(
					index >= 0 ? method.getName().substring(0, index) : method.getName(),
					FunctionHandler.invoker(new MethodInfo(method))
				);
			}
		}
		this.scope.environment.addVariable("e", VariableHandler.constant(VectorType.DOUBLE, Math.E));
		this.scope.environment.addVariable("pi", VariableHandler.constant(VectorType.DOUBLE, Math.PI));
		this.scope.environment.addVariable("tau", VariableHandler.constant(VectorType.DOUBLE, Math.TAU));
		this.scope.environment.addVariable("true", VariableHandler.constant(VectorType.BOOLEAN, Boolean.TRUE));
		this.scope.environment.addVariable("false", VariableHandler.constant(VectorType.BOOLEAN, Boolean.FALSE));
		this.scope.environment.addVariable("goldenRatio", VariableHandler.constant(VectorType.DOUBLE, 1.618033988749895D));
		this.scope.environment.addVariable("goldenAngle", VariableHandler.constant(VectorType.DOUBLE, 2.399963229728653D));
		this.scope.environment.addKeyword("if", KeywordHandler.makeIf());
		this.scope.environment.addKeyword("unless", KeywordHandler.makeIf());
		this.scope.environment.addKeyword("switch", KeywordHandler.switcher());
		return this;
	}

	public static final MethodInfo getPixelWrappedOneArg  = new MethodInfo(Sampler.class, "getColor", 1);
	public static final MethodInfo getPixelWrappedTwoArgs = new MethodInfo(Sampler.class, "getColor", 2);

	public ExpressionParser<I> configureEnvironment(Consumer<ScriptEnvironment> configurator) {
		configurator.accept(this.scope.environment);
		return this;
	}

	public ClassNode parseBasic() throws ScriptParsingException {
		InsnTree tree;
		try {
			tree = this.nextScript();
		}
		catch (ScriptParsingException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new ScriptParsingException(exception, this.reader);
		}
		if (this.reader.canReadAfterWhitespace()) {
			this.reader.skip();
			throw new ScriptParsingException("Unexpected trailing character", this.reader);
		}
		if (!tree.jumpsUnconditionally()) {
			throw new ScriptParsingException("Missing return statement", this.reader);
		}
		ClassNode clazz = new ClassNode();
		clazz.visit(
			V24,
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			this.getClass().getName().replace('.', '/') + "$Generated_" + UNIQUIFIER.getAndIncrement(),
			null,
			Type.getInternalName(Object.class),
			new String[] { Type.getInternalName(this.implMethod.getDeclaringClass()) }
		);
		MethodNode method = new MethodNode(
			ACC_PUBLIC,
			this.implMethod.getName(),
			Type.getMethodDescriptor(this.implMethod),
			null,
			null
		);
		GeneratorAdapter codeBuilder = new GeneratorAdapter(
			method,
			ACC_PUBLIC,
			this.implMethod.getName(),
			Type.getMethodDescriptor(this.implMethod)
		);
		int parameterIndex = 1; //reserve slot for "this".
		Map<String, LocalVariable> parameters = new HashMap<>();
		for (Parameter parameter : this.implMethod.getParameters()) {
			if (!parameter.isNamePresent()) {
				throw new IllegalStateException("Parameter " + parameter + " is missing a name.");
			}
			parameters.put(parameter.getName(), new LocalVariable(parameterIndex, VectorType.get(parameter.getAnnotatedType())));
			parameterIndex += parameter.getType() == long.class || parameter.getType() == double.class ? 2 : 1;
		}
		clazz.methods.add(method);
		tree.emitBytecode(
			new CodeEmitter.Context(
				Type.getObjectType(clazz.name),
				clazz,
				method,
				codeBuilder,
				parameters
			)
		);
		return clazz;
	}

	public @NotNull InsnTree nextScript() throws ScriptParsingException {
		if (this.reader.peekAfterWhitespace() == '}') return NoopInsnTree.INSTANCE;
		List<InsnTree> statements = new ArrayList<>();
		do {
			InsnTree statement = this.nextStatement(true);
			if (!statements.isEmpty() && statements.getLast().jumpsUnconditionally()) {
				throw new ScriptParsingException("Unreachable statement", this.reader);
			}
			statements.add(statement);
		}
		while (this.reader.canReadAfterWhitespace() && this.reader.peekAfterWhitespace() != '}');
		return new SequenceInsnTree(statements.toArray(new InsnTree[statements.size()]));
	}

	public @NotNull InsnTree nextStatement(boolean allowDeclarations) throws ScriptParsingException {
		if (this.reader.hasAfterWhitespace('{')) {
			InsnTree result;
			try (Scope _ = this.pushScope()) {
				result = this.nextScript();
			}
			this.reader.expectAfterWhitespace('}');
			return new BlockInsnTree(result);
		}
		String maybeKeyword = this.reader.readIdentifierAfterWhitespace();
		if (maybeKeyword != null) {
			return this.nextIdentifier(maybeKeyword, true, allowDeclarations);
		}
		InsnTree result = this.nextExpression();
		this.reader.expectOperatorAfterWhitespace(";");
		if (!result.canBeStatement()) {
			throw new ScriptParsingException("Not a statement", this.reader);
		}
		return result;
	}

	public @NotNull InsnTree nextExpression() throws ScriptParsingException {
		if (this.reader.hasOperatorAfterWhitespace("...")) {
			InsnTree result = this.nextTernary();
			if (result.type().shape == GroupShape.UNIT) return result;
			return SwizzleInsnTree.unpack(result);
		}
		else {
			return this.nextTernary();
		}
	}

	public @NotNull InsnTree nextTernary() throws ScriptParsingException {
		InsnTree result = this.nextBoolean();
		if (this.reader.hasOperatorAfterWhitespace("?")) {
			ConditionTree condition = result.toCondition();
			InsnTree ifTrue = this.nextExpression();
			this.reader.expectOperatorAfterWhitespace(":");
			InsnTree ifFalse = this.nextExpression();
			return new IfElseInsnTree(condition, ifTrue, ifFalse, false);
		}
		return result;
	}

	public @NotNull InsnTree nextBoolean() throws ScriptParsingException {
		InsnTree result = this.nextCompare();
		while (true) {
			CursorPos revert = this.reader.getCursor();
			String operator = this.reader.readOperatorAfterWhitespace();
			switch (operator) {
				case "&&" -> result = new AndConditionTree(result.toCondition(), this.nextCompare().toCondition()).toInsn();
				case "||" -> result = new  OrConditionTree(result.toCondition(), this.nextCompare().toCondition()).toInsn();
				default -> { this.reader.setCursor(revert); return result; }
			}
		}
	}

	public @NotNull InsnTree nextCompare() throws ScriptParsingException {
		InsnTree result = this.nextSum();
		CursorPos revert = this.reader.getCursor();
		String operator = this.reader.readOperatorAfterWhitespace();
		switch (operator) {
			case ">"  -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.GT).toInsn();
			case "<"  -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.LT).toInsn();
			case ">=" -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.GE).toInsn();
			case "<=" -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.LE).toInsn();
			case "==" -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.EQ).toInsn();
			case "!=" -> result = CompareConditionTree.create(result, this.nextSum(), CompareMode.NE).toInsn();
			default -> this.reader.setCursor(revert);
		}
		return result;
	}

	public @NotNull InsnTree nextSum() throws ScriptParsingException {
		InsnTree left = this.nextProduct();
		while (true) {
			CursorPos revert = this.reader.getCursorAfterWhitespace();
			String read = this.reader.readOperatorAfterWhitespace();
			switch (read) {
				case "+" -> left = BinaryInsnTree.add(left, this.nextProduct());
				case "-" -> left = BinaryInsnTree.sub(left, this.nextProduct());
				case "&" -> left = BinaryInsnTree.and(left, this.nextProduct());
				case "|" -> left = BinaryInsnTree.or (left, this.nextProduct());
				case "#" -> left = BinaryInsnTree.xor(left, this.nextProduct());
				default -> { this.reader.setCursor(revert); return left; }
			}
		}
	}

	public @NotNull InsnTree nextProduct() throws ScriptParsingException {
		InsnTree left = this.nextPrefix();
		while (true) {
			CursorPos revert = this.reader.getCursorAfterWhitespace();
			String read = this.reader.readOperatorAfterWhitespace();
			switch (read) {
				case "*"   -> left = BinaryInsnTree.mul (left, this.nextPrefix());
				case "/"   -> left = BinaryInsnTree.div (left, this.nextPrefix());
				case "%"   -> left = BinaryInsnTree.mod (left, this.nextPrefix());
				case "<<"  -> left = BinaryInsnTree.shl (left, this.nextPrefix());
				case ">>"  -> left = BinaryInsnTree.shr (left, this.nextPrefix());
				case ">>>" -> left = BinaryInsnTree.ushr(left, this.nextPrefix());
				default -> { this.reader.setCursor(revert); return left; }
			}
		}
	}

	public @NotNull InsnTree nextPrefix() throws ScriptParsingException {
		String read = this.reader.peekOperatorAfterWhitespace();
		return switch (read) {
			case "+" -> {
				this.reader.onCharsRead(read);
				yield this.nextPower();
			}
			case "-" -> {
				this.reader.onCharsRead(read);
				yield UnaryInsnTree.negate(this.nextPower());
			}
			case "~" -> {
				this.reader.onCharsRead(read);
				yield UnaryInsnTree.not(this.nextPower());
			}
			default -> {
				yield this.nextPower();
			}
		};
	}

	public @NotNull InsnTree nextPower() throws ScriptParsingException {
		InsnTree member = this.nextMember();
		if (this.reader.hasOperatorAfterWhitespace("^")) {
			member = BinaryInsnTree.pow(member, this.nextPower());
		}
		return member;
	}

	public @NotNull InsnTree nextMember() throws ScriptParsingException {
		InsnTree term = this.nextTerm();
		while (true) {
			if (this.reader.hasOperatorAfterWhitespace(".")) {
				String name = this.reader.expectIdentifierAfterWhitespace();
				if (this.reader.hasAfterWhitespace('(')) {
					InsnTree[] params = this.nextExpressionList();
					this.reader.expectAfterWhitespace(')');
					term = this.scope.environment.getMethod(this, term, name, params);
				}
				else {
					term = this.scope.environment.getField(this, term, name);
				}
			}
			else if (this.reader.hasAfterWhitespace('[')) {
				InsnTree[] params = this.nextExpressionList();
				this.reader.expectAfterWhitespace(']');
				term = this.scope.environment.getIndex(this, term, params);
			}
			else {
				return term;
			}
		}
	}

	public @NotNull InsnTree nextTerm() throws ScriptParsingException {
		char first = this.reader.peekAfterWhitespace();
		return switch (first) {
			case 0 -> {
				throw new ScriptParsingException("Unexpected end of input", this.reader);
			}
			case '(' -> {
				this.reader.onCharRead('(');
				InsnTree result = this.nextExpression();
				this.reader.expectAfterWhitespace(')');
				yield result;
			}
			case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
				Number number = this.reader.readNumber();
				char suffix = this.reader.peek();
				VectorType type;
				switch (suffix) {
					case 'l':
					case 'L':
						this.reader.onCharRead(suffix);
						if (number instanceof BigDecimal) {
							number = number.doubleValue();
							type = VectorType.DOUBLE;
						}
						else {
							number = number.longValue();
							type = VectorType.LONG;
						}
						break;
					case 'i':
					case 'I':
						this.reader.onCharRead(suffix);
					//fallthrough
					default:
						if (number instanceof BigDecimal) {
							number = number.floatValue();
							type = VectorType.FLOAT;
						}
						else {
							number = number.intValue();
							type = VectorType.INT;
						}
						break;
				}
				;
				yield new ConstantInsnTree(type, number);
			}
			case
				'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
				'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
				'_', '`'
			-> {
				yield this.nextIdentifier(this.reader.readIdentifier(), false, false);
			}
			default -> {
				this.reader.onCharRead(first);
				throw new ScriptParsingException("Unexpected character: " + first, this.reader);
			}
		};
	}

	public @NotNull InsnTree nextIdentifier(String name, boolean statement, boolean allowDeclarations) throws ScriptParsingException {
		InsnTree result = this.scope.environment.getKeyword(this, name, statement);
		if (result != null) return result;

		if (this.reader.hasAfterWhitespace('(')) {
			InsnTree[] arguments = this.nextExpressionList();
			this.reader.expectAfterWhitespace(')');
			result = this.scope.environment.getFunction(this, name, arguments);
			if (result != null) return result;
			else throw new ScriptParsingException("No such function or incorrect arguments", this.reader);
		}

		VectorType type = VectorType.forName(name);
		if (type != null) {
			if (this.reader.hasOperatorAfterWhitespace(".")) {
				String memberName = this.reader.expectIdentifierAfterWhitespace();
				this.reader.expectAfterWhitespace('(');
				InsnTree[] arguments = this.nextExpressionList();
				this.reader.expectAfterWhitespace(')');
				result = this.scope.environment.getStaticFunction(this, type, memberName, arguments);
				if (result != null) return result;
				else throw new ScriptParsingException("No such static function or incorrect arguments", this.reader);
			}
			else if (statement) {
				if (allowDeclarations) {
					String declarationName = this.reader.expectIdentifierAfterWhitespace();
					if (this.scope.environment.variables.containsKey(declarationName)) {
						throw new ScriptParsingException("Duplicate variable: " + declarationName, this.reader);
					}
					this.reader.expectOperatorAfterWhitespace("=");
					InsnTree initializer = this.nextExpression();
					this.reader.expectOperatorAfterWhitespace(";");
					if (initializer.type() == type) {
						this.scope.environment.addVariable(declarationName, VariableHandler.userVar(type));
						return new VariableDeclarationInsnTree(declarationName, initializer);
					}
					else {
						throw new ScriptParsingException("Attempt to initialize " + type + " with " + initializer.type(), this.reader);
					}
				}
				else {
					throw new ScriptParsingException("Declarations are not allowed here", this.reader);
				}
			}
			else {
				throw new ScriptParsingException("Syntax error", this.reader);
			}
		}
		else {
			result = this.scope.environment.getVariable(this, name);
			if (result != null) {
				if (this.reader.hasOperatorAfterWhitespace("=")) {
					Assigner assigner = result.assigner();
					if (assigner == null) {
						throw new ScriptParsingException("Not an lvalue: " + result, this.reader);
					}
					InsnTree value = this.nextExpression();
					this.reader.expectOperatorAfterWhitespace(";");
					result = assigner.assign(value);
				}
				return result;
			}
			else {
				throw new ScriptParsingException("No such variable with name " + name, this.reader);
			}
		}
	}

	public InsnTree[] nextExpressionList() throws ScriptParsingException {
		if (this.reader.peekAfterWhitespace() == ')') return new InsnTree[0];
		List<InsnTree> expressions = new ArrayList<>();
		while (true) {
			expressions.add(this.nextExpression());
			if (!this.reader.hasOperatorAfterWhitespace(",")) {
				return expressions.toArray(new InsnTree[expressions.size()]);
			}
		}
	}

	public Scope pushScope() {
		return this.scope = this.new Scope();
	}

	public class Scope implements AutoCloseable {

		public final Scope parent = ExpressionParser.this.scope;
		public final ScriptEnvironment environment;

		public Scope() {
			if (this.parent != null) {
				this.environment = new ScriptEnvironment(this.parent.environment);
			}
			else {
				this.environment = new ScriptEnvironment();
			}
		}

		@Override
		public void close() throws ScriptParsingException {
			if (ExpressionParser.this.scope == this) {
				ExpressionParser.this.scope = this.parent;
			}
			else {
				throw new ScriptParsingException("Mismatched scope", ExpressionParser.this.reader);
			}
		}
	}
}