package builderb0y.notgimp.scripting.util;

import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.tree.CodeEmitter;

@SuppressWarnings("preview")
public class VectorOpCompiler extends BinaryCombiner<CodeEmitter> {

	public static final VectorOpCompiler INSTANCE = new VectorOpCompiler();

	@Override
	public CodeEmitter invoker(Class<?> owner, String name, Class<?>... paramTypes) {
		try {
			Method method = owner.getMethod(name, paramTypes);
			ClassDesc ownerDesc = Util.desc(owner);
			Opcode opcode = Modifier.isStatic(method.getModifiers()) ? Opcode.INVOKESTATIC : owner.isInterface() ? Opcode.INVOKEINTERFACE : Opcode.INVOKEVIRTUAL;
			MethodTypeDesc paramDesc = MethodTypeDesc.of(Util.desc(method.getReturnType()), Arrays.stream(paramTypes).map(Class::descriptorString).map(ClassDesc::ofDescriptor).toArray(ClassDesc[]::new));
			boolean isInterface = owner.isInterface();
			return (CodeEmitter.Context context) -> context.codeBuilder.invokeInstruction(
				opcode,
				ownerDesc,
				name,
				paramDesc,
				isInterface
			);
		}
		catch (NoSuchMethodException exception) {
			return null;
		}
	}
}