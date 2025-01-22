package builderb0y.notgimp.scripting.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class ConstantFolder extends BinaryCombiner<MethodHandle> {

	public static final ConstantFolder INSTANCE = new ConstantFolder();

	@Override
	public MethodHandle invoker(Class<?> owner, String name, Class<?>... paramTypes) {
		try {
			Method method = owner.getMethod(name, paramTypes);
			return MethodHandles.lookup().unreflect(method);
		}
		catch (ReflectiveOperationException exception) {
			return null;
		}
	}
}