package builderb0y.notgimp.scripting.parsing;

import java.lang.reflect.Parameter;

import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.scripting.tree.*;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.GroupShape;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class ScriptHandlers {

	public static @Nullable InsnTree cast(InsnTree actual, VectorType expected) {
		if (actual.type == expected) {
			return actual;
		}
		else if (actual instanceof ConstantInsnTree constant && constant.value instanceof Number && expected.shape == GroupShape.UNIT) {
			if (expected.componentType.isFloatingPoint() == actual.type.componentType.isFloatingPoint()) {
				return new ConstantInsnTree(expected, constant.value);
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	public static InsnTree[] multiCast(InsnTree[] arguments, VectorType... types) {
		int length = arguments.length;
		if (types.length != length) return null;
		InsnTree[] result = arguments;
		for (int index = 0; index < length; index++) {
			InsnTree replacement = cast(arguments[index], types[index]);
			if (replacement == null) return null;
			if (replacement == arguments[index]) continue;
			if (result == arguments) result = result.clone();
			result[index] = replacement;
		}
		return result;
	}

	@FunctionalInterface
	public static interface VariableHandler {

		public abstract InsnTree getVariable(ExpressionParser<?> parser, String name) throws ScriptParsingException;

		public static VariableHandler builtinParameter(String name, VectorType type) {
			return (ExpressionParser<?> parser, String name_) -> {
				for (Parameter parameter : parser.implMethod.getParameters()) {
					if (!parameter.isNamePresent()) {
						throw new ScriptParsingException(parser.implMethod + " does not have parameter names", parser.reader);
					}
					if (parameter.getName().equals(name)) {
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
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract InsnTree getMethod(ExpressionParser<?> parser, InsnTree receiver, String name, InsnTree[] params) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface StaticFunctionHandler {

		public abstract InsnTree getStaticFunction(ExpressionParser<?> parser, VectorType type, String name, InsnTree[] params) throws ScriptParsingException;

		public static StaticFunctionHandler invoker(MethodInfo method, VectorType returnType, VectorType... paramTypes) {
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
				InsnTree result = parser.nextExpression();
				if (result.type != type) {
					throw new ScriptParsingException(STR."Can't return \{result.type} from method expecting \{type}", parser.reader);
				}
				return new ReturnInsnTree(result);
			};
		}
	}
}