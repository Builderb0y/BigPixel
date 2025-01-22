package builderb0y.notgimp.scripting.tree;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.util.LocalVariable;

public interface CodeEmitter {

	public abstract void emitBytecode(Context context);

	public default CodeEmitter andThen(CodeEmitter that) {
		Objects.requireNonNull(that);
		return (Context context) -> { this.emitBytecode(context); that.emitBytecode(context); };
	}

	public static class Context {

		public final ClassDesc self;
		public final ClassBuilder clazz;
		public final MethodBuilder method;
		public final CodeBuilder codeBuilder;
		public final Map<String, LocalVariable> variablesByName;

		public Context(ClassDesc self, ClassBuilder clazz, MethodBuilder method, CodeBuilder codeBuilder, Map<String, LocalVariable> variables) {
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
			int index = this.codeBuilder.allocateLocal(TypeKind.from(type.holderClass()));
			LocalVariable variable = new LocalVariable(index, type);
			this.variablesByName.put(name, variable);
			return variable;
		}

		public LocalVariable getVariable(String name) {
			LocalVariable variable = this.variablesByName.get(name);
			if (variable != null) return variable;
			else throw new IllegalStateException("No such variable with name: " + name);
		}

		public Context fork(CodeBuilder codeBuilder) {
			return new Context(this.self, this.clazz, this.method, codeBuilder, new HashMap<>(this.variablesByName));
		}
	}
}