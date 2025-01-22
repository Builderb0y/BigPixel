package builderb0y.notgimp.scripting.tree;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.MethodInfo;

public class InvokeInsnTree extends InsnTree {

	public final InsnTree[] arguments;
	public final MethodInfo method;

	public InvokeInsnTree(VectorType type, InsnTree[] arguments, MethodInfo method) {
		super(type);
		this.arguments = arguments;
		this.method = method;
		int length = method.paramTypes().length;
		if (Modifier.isStatic(method.access())) {
			for (int index = 0; index < length; index++) {
				if (!method.paramTypes()[index].isAssignableFrom(arguments[index].type.holderClass())) {
					throw new IllegalArgumentException(STR."Expected \{Arrays.stream(method.paramTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ", "[", "]"))}, got \{Arrays.stream(arguments).map(tree -> tree.type.toString()).collect(Collectors.joining(", ", "[", "]"))}");
				}
			}
		}
		else {
			if (!method.owner().isAssignableFrom(arguments[0].type.holderClass())) {
				throw new IllegalArgumentException(STR."Expected receiver to be of type \{method.owner().getSimpleName()} but it was \{arguments[0].type.holderClass().getSimpleName()}");
			}
			for (int index = 0; index < length; index++) {
				if (!method.paramTypes()[index].isAssignableFrom(arguments[index + 1].type.holderClass())) {
					throw new IllegalArgumentException(STR."Expected \{Arrays.stream(method.paramTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ", "[", "]"))}, got \{Arrays.stream(arguments, 1, arguments.length).map(tree -> tree.type.toString()).collect(Collectors.joining(", ", "[", "]"))}");
				}
			}
		}
	}

	public InvokeInsnTree(VectorType type, InsnTree receiver, InsnTree[] arguments, MethodInfo method) {
		InsnTree[] newArguments = new InsnTree[arguments.length + 1];
		newArguments[0] = receiver;
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		this(type, newArguments, method);
	}

	public InvokeInsnTree(VectorType type, List<InsnTree> arguments, MethodInfo method) {
		this(type, arguments.toArray(new InsnTree[arguments.size()]), method);
	}

	@Override
	public void emitBytecode(Context context) {
		for (InsnTree argument : this.arguments) {
			argument.emitBytecode(context);
		}
		this.method.emitBytecode(context);
	}
}