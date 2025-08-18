package builderb0y.notgimp.scripting.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.scripting.parsing.InvalidOperandException;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.GroupShape;

public abstract class BinaryCombiner<T> {

	public static record UnaryKey(VectorType type, UnaryOperatorWrapper operator) {}
	public static record BinaryKey(VectorType left, VectorType right, BinaryOperatorWrapper operator) {}
	public static record UnaryValue<T>(VectorType in, VectorType out, T value) {}
	public static record BinaryValue<T>(VectorType left, VectorType right, VectorType out, T value) {}

	public final Map<UnaryKey, UnaryValue<T>> unaryCombiners = new HashMap<>();
	public final Map<BinaryKey, BinaryValue<T>> binaryCombiners = new HashMap<>();

	public BinaryCombiner() {
		for (UnaryOperatorWrapper operator : UnaryOperatorWrapper.VALUES) {
			for (VectorType operand : VectorType.VALUES) {
				UnaryValue<T> combiner = this.createUnary(operand, operator);
				if (combiner != null) this.unaryCombiners.put(new UnaryKey(operand, operator), combiner);
			}
		}
		for (BinaryOperatorWrapper operator : BinaryOperatorWrapper.VALUES) {
			for (VectorType left : VectorType.VALUES) {
				for (VectorType right : VectorType.VALUES) {
					BinaryValue<T> combiner = this.createBinary(left, right, operator);
					if (combiner != null) this.binaryCombiners.put(new BinaryKey(left, right, operator), combiner);
				}
			}
		}
	}

	public UnaryValue<T> unary(VectorType type, UnaryOperatorWrapper operator) {
		UnaryValue<T> result = this.unaryCombiners.get(new UnaryKey(type, operator));
		if (result != null) return result;
		else throw new InvalidOperandException("Can't " + operator.name().toLowerCase(Locale.ROOT) + " " + type);
	}

	public BinaryValue<T> binary(VectorType left, VectorType right, BinaryOperatorWrapper operator) {
		BinaryValue<T> result = this.binaryCombiners.get(new BinaryKey(left, right, operator));
		if (result != null) return result;
		else throw new InvalidOperandException("Can't " + operator.name().toLowerCase(Locale.ROOT) + " " + left + " and " + right);
	}

	public @Nullable UnaryValue<T> createUnary(VectorType type, UnaryOperatorWrapper operator) {
		String name = operator.name().toLowerCase(Locale.ROOT) + '_' + type.name;
		Method method = method(VectorOperations.class, name, type.holderClass());
		if (method == null) return null;
		T handler = this.invoker(method);
		if (handler == null) return null;
		return new UnaryValue<>(type, VectorType.get(method.getAnnotatedReturnType()), handler);
	}

	public static VectorType unit(VectorType type) {
		return VectorType.get(type.componentType, GroupShape.UNIT);
	}

	public @Nullable BinaryValue<T> createBinary(VectorType left, VectorType right, BinaryOperatorWrapper operator) {
		String name = operator.name().toLowerCase(Locale.ROOT) + '_' + left.name + '_' + right.name;
		Method method = method(VectorOperations.class, name, left.holderClass(), right.holderClass());
		if (method == null) return null;
		T handler = this.invoker(method);
		if (handler == null) return null;
		return new BinaryValue<>(left, right, VectorType.get(method.getAnnotatedReturnType()), handler);
	}

	public static @Nullable Method method(Class<?> clazz, String name, Class<?>... paramTypes) {
		try {
			return clazz.getMethod(name, paramTypes);
		}
		catch (NoSuchMethodException _) {
			return null;
		}
	}

	public abstract @Nullable T invoker(Method method);
}