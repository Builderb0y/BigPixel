package builderb0y.notgimp.scripting.parsing;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.components.ClassPrinter;
import java.lang.classfile.components.ClassPrinter.Verbosity;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jdk.incubator.vector.VectorOperators;
import org.jetbrains.annotations.NotNull;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.notgimp.scripting.tree.*;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.ComponentType;
import builderb0y.notgimp.scripting.types.VectorType.GroupShape;
import builderb0y.notgimp.scripting.types.generators.OpsGenerator;
import builderb0y.notgimp.scripting.util.LocalVariable;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class ExpressionParser<I> {

	public static final AtomicInteger UNIQUIFIER = new AtomicInteger();

	public final ExpressionReader reader;
	public final Method implMethod;
	public Scope scope;
	public Map<String, Integer> usedLayers;

	public ExpressionParser(ExpressionReader reader, Method implMethod) {
		this.reader = reader;
		this.implMethod = implMethod;
		this.scope = this.new Scope();
		this.usedLayers = new HashMap<>();
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
		/*
		for (Method method : VectorApi.class.getDeclaredMethods()) {
			Name annotation = method.getAnnotation(Name.class);
			String name = annotation != null ? annotation.value() : method.getName();
			MethodInfo model = new MethodInfo(method);
			VectorType returnType = VectorType.get(method.getAnnotatedReturnType());
			VectorType[] paramTypes = Arrays.stream(method.getAnnotatedParameterTypes()).map(VectorType::get).toArray(VectorType[]::new);
			this.scope.environment.addFunction(name, (ExpressionParser<?> parser, String name_, InsnTree[] params) -> {
				InsnTree[] castParameters = ScriptHandlers.multiCast(params, paramTypes);
				if (castParameters == null) return null;
				return new InvokeInsnTree(returnType, castParameters, model);
			});
		}
		*/
		for (VectorType type : VectorType.VALUES) {
			if (type.isNotBig()) {
				this.scope.environment.addFunction(type.name, (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
					int length = params.length;
					VectorType[] types = new VectorType[length];
					for (int index = 0; index < length; index++) {
						VectorType paramType = params[index].type;
						if (paramType.isBig()) paramType = VectorType.get(type.componentType, GroupShape.UNIT);
						types[index] = paramType;
					}
					InsnTree[] castArgs = ScriptHandlers.multiCast(params, types);
					if (castArgs == null) return null;
					String fullName = name + "_from_" + Arrays.stream(types).map((VectorType paramType) -> paramType.name).collect(Collectors.joining("_"));
					try {
						MethodInfo model = new MethodInfo(VectorOperations.class, fullName);
						return new InvokeInsnTree(type, castArgs, model);
					}
					catch (IllegalArgumentException exception) {
						return null;
					}
				});
				if (type.shape != GroupShape.UNIT) {
					this.scope.environment.addIndex(type, (ExpressionParser<?> parser, InsnTree receiver, InsnTree[] params) -> {
						InsnTree[] castArguments = ScriptHandlers.multiCast(params, VectorType.INT);
						if (castArguments == null) return null;
						return new InvokeInsnTree(
							VectorType.get(receiver.type.componentType, GroupShape.UNIT),
							receiver,
							castArguments,
							new MethodInfo(
								receiver.type.holderClass(),
								receiver.type.componentType == ComponentType.BOOLEAN ? "laneIsSet" : "lane",
								int.class
							)
						);
					});
				}
				/*
				switch (type.shape) {
					case UNIT -> {}
					case VEC2, VEC4, VEC8 -> {
						MethodInfo model = new MethodInfo(type.holderClass(), type.componentType == ComponentType.BOOLEAN ? "laneIsSet" : "lane", int.class);
						this.scope.environment.addIndex(type, (ExpressionParser<?> parser, InsnTree receiver, InsnTree[] params) -> {
							InsnTree[] cast = ScriptHandlers.multiCast(params, VectorType.INT);
							if (cast == null) return null;
							return new InvokeInsnTree(VectorType.get(type.componentType, GroupShape.UNIT), receiver, cast, model);
						});
					}
					case MAT2, MAT4, MAT8 -> {
						VectorType unit = VectorType.get(type.componentType, GroupShape.UNIT);
						VectorType vector = VectorType.get(type.componentType, GroupShape.VALUES[type.shape.ordinal() - 3]);
						this.scope.environment.addStaticFunction(type, "columns",  StaticFunctionHandler.invoker(new MethodInfo(type.holderClass(), "columns" ), type, Util.fill(new VectorType[type.shape.columns], vector)));
						this.scope.environment.addStaticFunction(type, "rows",     StaticFunctionHandler.invoker(new MethodInfo(type.holderClass(), "rows"    ), type, Util.fill(new VectorType[type.shape.columns], vector)));
						this.scope.environment.addStaticFunction(type, "diagonal", StaticFunctionHandler.invoker(new MethodInfo(type.holderClass(), "diagonal"), type, Util.fill(new VectorType[1], vector)));
						this.scope.environment.addStaticFunction(type, "scalar",   StaticFunctionHandler.invoker(new MethodInfo(type.holderClass(), "scalar"  ), type, Util.fill(new VectorType[1], unit)));
						this.scope.environment.addStaticFunction(type, "fill",     StaticFunctionHandler.invoker(new MethodInfo(type.holderClass(), "fill"    ), type, Util.fill(new VectorType[1], unit)));
					}
				}
				*/
			}
		}
		for (VectorOperators.Unary operator : OpsGenerator.UNARIES) {
			this.scope.environment.addFunction(operator.name().toLowerCase(Locale.ROOT), (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				if (params.length != 1) return null;
				return UnaryInsnTree.create(params[0], operator);
			});
		}
		for (VectorOperators.Binary operator : OpsGenerator.BINARIES) {
			this.scope.environment.addFunction(operator.name().toLowerCase(Locale.ROOT), (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				if (params.length != 2) return null;
				return BinaryInsnTree.create(params[0], params[1], operator);
			});
		}
		for (String name : new String[] { "dot", "lengthSquared", "length", "normalize", "mix" }) {
			this.scope.environment.addFunction(name, (ExpressionParser<?> parser, String name_, InsnTree[] params) -> {
				String fullName = name_ + '_' + Arrays.stream(params).map((InsnTree tree) -> tree.type.name).collect(Collectors.joining("_"));
				try {
					MethodInfo model = new MethodInfo(VectorOperations.class, fullName);
					return new InvokeInsnTree(model.vectorReturnType(), params, model);
				}
				catch (IllegalArgumentException exception) {
					return null;
				}
			});
		}
		return this;
	}

	public ExpressionParser<I> addLayers(Map<String, Layer> layers) {
		MethodInfo one = new MethodInfo(Layer.class, "getPixelWrapped", 1);
		MethodInfo two = new MethodInfo(Layer.class, "getPixelWrapped", 2);
		for (Map.Entry<String, Layer> entry : layers.entrySet()) {
			this.scope.environment.addFunction(entry.getKey(), (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				int index = parser.usedLayers.computeIfAbsent(name, (String _) -> parser.usedLayers.size());
				return switch (params.length) {
					case 1 -> {
						InsnTree[] castParams = ScriptHandlers.multiCast(params, VectorType.INT2);
						if (castParams == null) yield null;
						yield new SampleInsnTree(castParams, "layer" + index, one);
					}
					case 2 -> {
						InsnTree[] castParams = ScriptHandlers.multiCast(params, VectorType.INT, VectorType.INT);
						if (castParams == null) yield null;
						yield new SampleInsnTree(castParams, "layer" + index, two);
					}
					default -> {
						yield null;
					}
				};
			});
		}
		return this;
	}

	public ExpressionParser<I> configureEnvironment(Consumer<ScriptEnvironment> configurator) {
		configurator.accept(this.scope.environment);
		return this;
	}

	public I parse(Map<String, Layer> layers) throws ScriptParsingException {
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
		ClassDesc generatedClassDesc = ClassDesc.ofInternalName(ExpressionParser.class.getName().replace('.', '/') + "$Generated$" + UNIQUIFIER.getAndIncrement());
		byte[] bytes = ClassFile.of().build(generatedClassDesc, (ClassBuilder clazz) -> {
			clazz
			.withVersion(ClassFile.latestMajorVersion(), ClassFile.latestMinorVersion())
			.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL, AccessFlag.SYNTHETIC)
			.withSuperclass(ConstantDescs.CD_Object)
			.withInterfaceSymbols(Util.desc(this.implMethod.getDeclaringClass()));

			for (Map.Entry<String, Integer> entry : this.usedLayers.entrySet()) {
				clazz.withField("layer" + entry.getValue(), Util.desc(Layer.class), Modifier.PUBLIC | Modifier.FINAL);
			}

			clazz
			.withMethod(
				ConstantDescs.INIT_NAME,
				MethodTypeDesc.of(ConstantDescs.CD_void, Util.desc(Map.class)),
				Modifier.PUBLIC,
				(MethodBuilder constructor) -> constructor.withCode((CodeBuilder code) -> {
					code
					.aload(code.receiverSlot())
					.invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, false);

					for (Map.Entry<String, Integer> entry : this.usedLayers.entrySet()) {
						code
						.aload(code.receiverSlot())
						.aload(code.parameterSlot(0))
						.ldc(entry.getKey())
						.invokeinterface(Util.desc(Map.class), "get", MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
						.checkcast(Util.desc(Layer.class))
						.putfield(generatedClassDesc, "layer" + entry.getValue(), Util.desc(Layer.class));
					}

					code.return_();
				})
			)
			.withMethod(
				this.implMethod.getName(),
				MethodTypeDesc.of(
					Util.desc(this.implMethod.getReturnType()),
					Arrays.stream(this.implMethod.getParameterTypes()).map(Util::desc).toArray(ClassDesc[]::new)
				),
				Modifier.PUBLIC,
				(MethodBuilder method) -> method.withCode((CodeBuilder code) -> {
					Map<String, LocalVariable> variables = new HashMap<>();
					Parameter[] parameters = this.implMethod.getParameters();
					for (int index = 0, length = parameters.length; index < length; index++) {
						Parameter parameter = parameters[index];
						if (!parameter.isNamePresent()) throw new IllegalStateException(this.implMethod + " is missing parameter names");
						variables.put(parameter.getName(), new LocalVariable(code.parameterSlot(index), VectorType.get(parameter.getAnnotatedType())));
					}
					tree.emitBytecode(new CodeEmitter.Context(generatedClassDesc, clazz, method, code, variables));
				})
			);
		});
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false);
			return (I)(lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class, Map.class)).invoke(layers));
		}
		catch (Throwable throwable) {
			StringBuilder message = new StringBuilder("Error defining class!\n");
			ClassPrinter.toJson(ClassFile.of().parse(bytes), Verbosity.TRACE_ALL, message::append);
			throw new ScriptParsingException(message.toString(), throwable, null);
		}
	}

	public @NotNull InsnTree nextScript() throws ScriptParsingException {
		if (this.reader.peekAfterWhitespace() == '}') return NoopInsnTree.INSTANCE;
		List<InsnTree> statements = new ArrayList<>();
		do {
			InsnTree statement = this.nextStatement();
			statements.add(statement);
		}
		while (this.reader.canReadAfterWhitespace() && this.reader.peekAfterWhitespace() != '}');
		return new SequenceInsnTree(statements.toArray(new InsnTree[statements.size()]));
	}

	public @NotNull InsnTree nextStatement() throws ScriptParsingException {
		CursorPos revert = this.reader.getCursor();
		String maybeKeyword = this.reader.readIdentifierAfterWhitespace();
		if (maybeKeyword != null) {
			InsnTree result = this.nextIdentifier(maybeKeyword, true);
			if (result != null) {
				this.reader.expectOperatorAfterWhitespace(";");
				return result;
			}
		}
		this.reader.setCursor(revert);
		InsnTree result = this.nextExpression();
		this.reader.expectOperatorAfterWhitespace(";");
		if (!result.canBeStatement()) {
			throw new ScriptParsingException("Not a statement", this.reader);
		}
		return result;
	}

	public @NotNull InsnTree nextExpression() throws ScriptParsingException {
		return this.nextSum();
	}

	public @NotNull InsnTree nextSum() throws ScriptParsingException {
		InsnTree left = this.nextProduct();
		while (true) {
			String read = this.reader.peekOperatorAfterWhitespace();
			switch (read) {
				case "+" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.add(left, this.nextProduct());
				}
				case "-" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.sub(left, this.nextProduct());
				}
				case "&" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.and(left, this.nextProduct());
				}
				case "|" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.or(left, this.nextProduct());
				}
				case "#" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.xor(left, this.nextProduct());
				}
				default -> {
					return left;
				}
			}
		}
	}

	public @NotNull InsnTree nextProduct() throws ScriptParsingException {
		InsnTree left = this.nextPrefix();
		while (true) {
			String read = this.reader.peekOperatorAfterWhitespace();
			switch (read) {
				case "*" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.mul(left, this.nextPrefix());
				}
				case "/" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.div(left, this.nextPrefix());
				}
				case "<<" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.shl(left, this.nextPrefix());
				}
				case ">>" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.shr(left, this.nextPrefix());
				}
				case ">>>" -> {
					this.reader.onCharsRead(read);
					left = BinaryInsnTree.ushr(left, this.nextPrefix());
				}
				default -> {
					return left;
				}
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
				term = this.scope.environment.getField(this, term, this.reader.expectIdentifierAfterWhitespace());
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
				VectorType type = switch (suffix) {
					case 'i', 'I' -> { this.reader.onCharRead(suffix); yield number instanceof BigDecimal ? VectorType.FLOAT  : VectorType.INT;  }
					case 'l', 'L' -> { this.reader.onCharRead(suffix); yield number instanceof BigDecimal ? VectorType.DOUBLE : VectorType.LONG; }
					default -> number instanceof BigDecimal ? VectorType.BIGDEC : VectorType.BIGINT;
				};
				yield new ConstantInsnTree(type, number);
			}
			case
				'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
				'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
				'_', '`'
			-> {
				yield this.nextIdentifier(this.reader.readIdentifier(), false);
			}
			default -> {
				this.reader.onCharRead(first);
				throw new ScriptParsingException("Unexpected character: " + first, this.reader);
			}
		};
	}

	public @NotNull InsnTree nextIdentifier(String name, boolean statement) throws ScriptParsingException {
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
				String declarationName = this.reader.expectIdentifierAfterWhitespace();
				if (this.scope.environment.variables.containsKey(declarationName)) {
					throw new ScriptParsingException("Duplicate variable: " + declarationName, this.reader);
				}
				this.reader.expectOperatorAfterWhitespace("=");
				InsnTree initializer = this.nextExpression();
				if (initializer.type == type) {
					this.scope.environment.addVariable(declarationName, VariableHandler.userVar(type));
					return new VariableDeclarationInsnTree(declarationName, initializer);
				}
				else {
					throw new ScriptParsingException(STR."Attempt to initialize \{type} with \{initializer.type}", this.reader);
				}
			}
			else {
				throw new ScriptParsingException("Syntax error", this.reader);
			}
		}
		else {
			result = this.scope.environment.getVariable(this, name);
			if (result != null) return result;
			else throw new ScriptParsingException("No such variable with name " + name, this.reader);
		}
	}

	public InsnTree[] nextExpressionList() throws ScriptParsingException {
		if (this.reader.hasAfterWhitespace(')')) return new InsnTree[0];
		List<InsnTree> expressions = new ArrayList<>();
		while (true) {
			expressions.add(Objects.requireNonNull(this.nextExpression()));
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