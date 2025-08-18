package builderb0y.bigpixel.scripting.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

public class ConstantFolder extends BinaryCombiner<MethodHandle> {

	public static final ConstantFolder INSTANCE = new ConstantFolder();

	@Override
	public @Nullable MethodHandle invoker(Method method) {
		try {
			return MethodHandles.lookup().unreflect(method);
		}
		catch (IllegalAccessException _) {
			return null;
		}
	}
}