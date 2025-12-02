package builderb0y.bigpixel.scripting.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import builderb0y.bigpixel.scripting.tree.CodeEmitter;

public class VectorOpCompiler extends BinaryCombiner<CodeEmitter> {

	public static final VectorOpCompiler INSTANCE = new VectorOpCompiler();

	@Override
	public @Nullable CodeEmitter invoker(Method method) {
		Class<?> owner = method.getDeclaringClass();
		String name = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		String ownerDesc = Type.getInternalName(owner);
		int opcode = Modifier.isStatic(method.getModifiers()) ? Opcodes.INVOKESTATIC : owner.isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL;
		String paramDesc = Type.getMethodDescriptor(Type.getType(method.getReturnType()), Arrays.stream(paramTypes).map(Type::getType).toArray(Type[]::new));
		boolean isInterface = owner.isInterface();
		return (CodeEmitter.Context context) -> context.codeBuilder.visitMethodInsn(
			opcode,
			ownerDesc,
			name,
			paramDesc,
			isInterface
		);
	}
}