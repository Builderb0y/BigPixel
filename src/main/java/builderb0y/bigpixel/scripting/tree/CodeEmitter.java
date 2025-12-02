package builderb0y.bigpixel.scripting.tree;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.LocalVariable;

public interface CodeEmitter {

	public abstract void emitBytecode(Context context);

	public static class Context {

		public final Type self;
		public final ClassNode clazz;
		public final MethodNode method;
		public final GeneratorAdapter codeBuilder;
		public final Map<String, LocalVariable> variablesByName;

		public Context(Type self, ClassNode clazz, MethodNode method, GeneratorAdapter codeBuilder, Map<String, LocalVariable> variables) {
			this.self = self;
			this.clazz = clazz;
			this.method = method;
			this.codeBuilder = codeBuilder;
			this.variablesByName = variables;
		}

		public LocalVariable allocateLocal(String name, VectorType type) {
			if (this.variablesByName.containsKey(name)) {
				throw new IllegalArgumentException("Duplicate variable with name: " + name);
			}
			int index = this.codeBuilder.newLocal(Type.getType(type.holderClass()));
			LocalVariable variable = new LocalVariable(index, type);
			this.variablesByName.put(name, variable);
			return variable;
		}

		public LocalVariable getVariable(String name) {
			LocalVariable variable = this.variablesByName.get(name);
			if (variable != null) return variable;
			else throw new IllegalStateException("No such variable with name: " + name);
		}

		public Context fork() {
			return new Context(this.self, this.clazz, this.method, this.codeBuilder, new HashMap<>(this.variablesByName));
		}
	}
}