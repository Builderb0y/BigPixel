package builderb0y.bigpixel.scripting.util;

import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.scripting.tree.CodeEmitter;

public class VectorOpCompiler extends BinaryCombiner<CodeEmitter> {

	public static final VectorOpCompiler INSTANCE = new VectorOpCompiler();

	@Override
	public @Nullable CodeEmitter invoker(Method method) {
		Class<?> owner = method.getDeclaringClass();
		String name = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		ClassDesc ownerDesc = Util.desc(owner);
		Opcode opcode = Modifier.isStatic(method.getModifiers()) ? Opcode.INVOKESTATIC : owner.isInterface() ? Opcode.INVOKEINTERFACE : Opcode.INVOKEVIRTUAL;
		MethodTypeDesc paramDesc = MethodTypeDesc.of(Util.desc(method.getReturnType()), Arrays.stream(paramTypes).map(Class::descriptorString).map(ClassDesc::ofDescriptor).toArray(ClassDesc[]::new));
		boolean isInterface = owner.isInterface();
		return (CodeEmitter.Context context) -> context.codeBuilder.invoke(
			opcode,
			ownerDesc,
			name,
			paramDesc,
			isInterface
		);
	}
}