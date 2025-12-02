package builderb0y.bigpixel.scripting.parsing;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.*;
import builderb0y.bigpixel.scripting.tree.SwitchInsnTree.MultiCase;
import builderb0y.bigpixel.scripting.tree.condition.ConditionTree;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;

public class ScriptHandlers {

	public static InsnTree @Nullable [] multiCast(InsnTree[] arguments, VectorType... types) {
		int length = arguments.length;
		InsnTree[] result = arguments;
		int fromIndex = 0;
		for (int argIndex = 0; argIndex < length; argIndex++) {
			VectorType[] to = Arrays.copyOfRange(types, fromIndex, fromIndex + arguments[argIndex].types().length);
			InsnTree replacement = arguments[argIndex].cast(to);
			if (replacement == null) {
				return null;
			}
			fromIndex += arguments[argIndex].types().length;
			if (replacement == arguments[argIndex]) continue;
			if (result == arguments) result = result.clone();
			result[argIndex] = replacement;
		}
		if (fromIndex != types.length) {
			return null;
		}
		return result;
	}

	public static class UsageTracker implements Runnable {

		public boolean used;

		@Override
		public void run() {
			this.used = true;
		}
	}

	@FunctionalInterface
	public static interface VariableHandler {

		public abstract InsnTree getVariable(ExpressionParser<?> parser, String name) throws ScriptParsingException;

		public static VariableHandler builtinParameter(String name, VectorType type) {
			return builtinParameter(name, type, null);
		}

		public static VariableHandler builtinParameter(String name, VectorType type, Runnable onUsed) {
			return (ExpressionParser<?> parser, String name_) -> {
				for (Parameter parameter : parser.implMethod.getParameters()) {
					if (!parameter.isNamePresent()) {
						throw new ScriptParsingException(parser.implMethod + " does not have parameter names", parser.reader);
					}
					if (parameter.getName().equals(name)) {
						if (onUsed != null) onUsed.run();
						return new LoadInsnTree(type, name);
					}
				}
				throw new ScriptParsingException(parser.implMethod + " does not have a parameter named " + name, parser.reader);
			};
		}

		public static VariableHandler userVar(VectorType type) {
			return (ExpressionParser<?> parser, String name) -> {
				return new LoadInsnTree(type, name);
			};
		}

		public static VariableHandler constant(VectorType type, Object value) {
			ConstantInsnTree tree = new ConstantInsnTree(type, value);
			return (ExpressionParser<?> parser, String name) -> tree;
		}
	}

	@FunctionalInterface
	public static interface FieldHandler {

		public abstract InsnTree getField(ExpressionParser<?> parser, InsnTree receiver, String name) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface IndexHandler {

		public abstract InsnTree getIndex(ExpressionParser<?> parser, InsnTree receiver, InsnTree[] params) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface FunctionHandler {

		public abstract InsnTree getFunction(ExpressionParser<?> parser, String name, InsnTree[] params) throws ScriptParsingException;

		public static FunctionHandler invoker(MethodInfo method) {
			VectorType returnType = method.vectorReturnType();
			VectorType[] paramTypes = method.vectorParamTypes();
			return (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
				InsnTree[] castArgs = multiCast(params, paramTypes);
				if (castArgs == null) return null;
				return new InvokeInsnTree(returnType, castArgs, method);
			};
		}
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract InsnTree getMethod(ExpressionParser<?> parser, InsnTree receiver, String name, InsnTree[] params) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface StaticFunctionHandler {

		public abstract InsnTree getStaticFunction(ExpressionParser<?> parser, VectorType type, String name, InsnTree[] params) throws ScriptParsingException;

		public static StaticFunctionHandler invoker(MethodInfo method) {
			VectorType returnType = method.vectorReturnType();
			VectorType[] paramTypes = method.vectorParamTypes();
			return (ExpressionParser<?> parser, VectorType type, String name, InsnTree[] params) -> {
				InsnTree[] castArgs = multiCast(params, paramTypes);
				if (castArgs == null) return null;
				return new InvokeInsnTree(returnType, castArgs, method);
			};
		}
	}

	@FunctionalInterface
	public static interface KeywordHandler {

		public abstract InsnTree getKeyword(ExpressionParser<?> parser, String name, boolean statement) throws ScriptParsingException;

		public static KeywordHandler returner(VectorType type) {
			return (ExpressionParser<?> parser, String name, boolean statement) -> {
				if (!statement) return null;
				if (type == VectorType.VOID) {
					parser.reader.expectOperatorAfterWhitespace(";");
					return new ReturnInsnTree(NoopInsnTree.INSTANCE);
				}
				else {
					InsnTree result = parser.nextExpression();
					parser.reader.expectOperatorAfterWhitespace(";");
					if (result.type() != type) {
						throw new ScriptParsingException("Can't return " + result.type() + " from method expecting " + type, parser.reader);
					}
					return new ReturnInsnTree(result);
				}
			};
		}

		public static KeywordHandler makeIf() {
			return (ExpressionParser<?> parser, String name, boolean statement) -> {
				if (!statement) return null;
				parser.reader.expectAfterWhitespace('(');
				InsnTree condition = parser.nextExpression();
				parser.reader.expectAfterWhitespace(')');
				ConditionTree conditionTree = condition.toCondition();
				if (name.equals("unless")) conditionTree = conditionTree.not();
				InsnTree body = parser.nextStatement(false);
				if (parser.reader.hasIdentifierAfterWhitespace("else")) {
					InsnTree falseBranch = parser.nextStatement(false);
					return new IfElseInsnTree(conditionTree, body, falseBranch, true);
				}
				else {
					return new IfInsnTree(conditionTree, body);
				}
			};
		}

		public static KeywordHandler switcher() {
			return (ExpressionParser<?> parser, String name, boolean statement) -> {
				parser.reader.expectAfterWhitespace('(');
				InsnTree value = parser.nextExpression();
				parser.reader.expectAfterWhitespace(')');
				parser.reader.expectAfterWhitespace('{');
				HashSet<Integer> allCases = new HashSet<>();
				List<MultiCase> cases = new ArrayList<>();
				InsnTree defaultCase = null;
				while (true) {
					if (parser.reader.hasIdentifierAfterWhitespace("case")) {
						parser.reader.expectAfterWhitespace('(');
						int[] matchedValues = new int[1];
						int valueCount = 0;
						if (parser.nextExpression() instanceof ConstantInsnTree first && first.type() == VectorType.INT) {
							if (!allCases.add(first.get())) {
								throw new ScriptParsingException("Duplicate case", parser.reader);
							}
							matchedValues[valueCount++] = first.get();
						}
						else {
							throw new ScriptParsingException("Case value must be constant int", parser.reader);
						}
						while (parser.reader.hasOperatorAfterWhitespace(",")) {
							if (parser.nextExpression() instanceof ConstantInsnTree next && next.type() == VectorType.INT) {
								if (valueCount == matchedValues.length) {
									matchedValues = Arrays.copyOf(matchedValues, valueCount << 1);
								}
								if (!allCases.add(first.get())) {
									throw new ScriptParsingException("Duplicate case", parser.reader);
								}
								matchedValues[valueCount++] = next.get();
							}
							else {
								throw new ScriptParsingException("Case value must be constant int", parser.reader);
							}
						}
						parser.reader.expectAfterWhitespace(')');
						InsnTree tree = parser.nextStatement(false);
						if (valueCount != matchedValues.length) {
							matchedValues = Arrays.copyOf(matchedValues, valueCount);
						}
						cases.add(new MultiCase(matchedValues, tree));
					}
					else if (parser.reader.hasIdentifierAfterWhitespace("default")) {
						if (defaultCase != null) {
							throw new ScriptParsingException("Duplicate default case", parser.reader);
						}
						defaultCase = parser.nextStatement(false);
					}
					else {
						break;
					}
				}
				parser.reader.expectAfterWhitespace('}');
				return SwitchInsnTree.create(value, cases, defaultCase);
			};
		}
	}
}