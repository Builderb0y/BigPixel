package builderb0y.bigpixel.scripting.util;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;
import builderb0y.bigpixel.scripting.types.VectorType;

public record MethodInfo(int access, Class<?> owner, String name, AnnotatedType annotatedReturnType, AnnotatedType... annotatedParamTypes) implements CodeEmitter {

	public MethodInfo(Method method) {
		this(method.getModifiers(), method.getDeclaringClass(), method.getName(), method.getAnnotatedReturnType(), method.getAnnotatedParameterTypes());
	}

	public MethodInfo(Class<?> owner, String name, Class<?>... paramTypes) {
		Method method;
		try {
			method = owner.getDeclaredMethod(name, paramTypes);
		}
		catch (NoSuchMethodException exception) {
			throw new IllegalArgumentException(exception);
		}
		this(method);
	}

	public MethodInfo(Class<?> owner, String name) {
		Method method = null;
		for (Method declared : owner.getDeclaredMethods()) {
			if (declared.getName().equals(name)) {
				if (method == null) method = declared;
				else throw new IllegalArgumentException("Multiple methods in " + owner + " with name " + name);
			}
		}
		if (method == null) throw new IllegalArgumentException("No methods in " + owner + " with name " + name);
		this(method);
	}

	public MethodInfo(Class<?> owner, String name, int paramCount) {
		Method method = null;
		for (Method declared : owner.getDeclaredMethods()) {
			if (declared.getName().equals(name) && declared.getParameterCount() == paramCount) {
				if (method == null) method = declared;
				else throw new IllegalArgumentException("Multiple methods in " + owner + " with name " + name + " and " + paramCount + " parameter(s).");
			}
		}
		if (method == null) throw new IllegalArgumentException("No methods in " + owner + " with name " + name + " and " + paramCount + " parameter(s).");
		this(method);
	}

	public Class<?> returnType() {
		return (Class<?>)(this.annotatedReturnType.getType());
	}

	public Class<?>[] paramTypes() {
		return Arrays.stream(this.annotatedParamTypes).map(AnnotatedType::getType).map(Class.class::cast).toArray(Class<?>[]::new);
	}

	public VectorType vectorReturnType() {
		return VectorType.get(this.annotatedReturnType);
	}

	public VectorType[] vectorParamTypes() {
		return Arrays.stream(this.annotatedParamTypes).map(VectorType::get).toArray(VectorType[]::new);
	}

	@Override
	public void emitBytecode(Context context) {
		context.codeBuilder.visitMethodInsn(
			Modifier.isStatic(this.access)
			? Opcodes.INVOKESTATIC
			: this.owner.isInterface()
			? Opcodes.INVOKEINTERFACE
			: Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(this.owner()),
			this.name,
			Type.getMethodDescriptor(
				this.returnTypeDesc(),
				this.paramTypesDesc()
			),
			this.owner.isInterface()
		);
	}

	public Type ownerDesc() {
		return Type.getType(this.owner);
	}

	public Type returnTypeDesc() {
		return Type.getType(this.returnType());
	}

	public Type[] paramTypesDesc() {
		Class<?>[] paramTypes = this.paramTypes();
		int length = paramTypes.length;
		Type[] descs = new Type[length];
		for (int index = 0; index < length; index++) {
			descs[index] = Type.getType(paramTypes[index]);
		}
		return descs;
	}
}