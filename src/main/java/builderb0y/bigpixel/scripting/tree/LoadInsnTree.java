package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.scripting.types.VectorType;

import static org.objectweb.asm.Opcodes.*;

public class LoadInsnTree extends InsnTree {

	public final String name;

	public LoadInsnTree(VectorType type, String name) {
		super(type);
		this.name = name;
	}

	@Override
	public void emitBytecode(Context context) {
		int index = context.getVariable(this.name).index();
		context.codeBuilder.visitVarInsn(
			switch (this.type()) {
				case INT, BOOLEAN -> ILOAD;
				case LONG -> LLOAD;
				case FLOAT -> FLOAD;
				case DOUBLE -> DLOAD;
				default -> ALOAD;
			},
			index
		);
	}

	@Override
	public Assigner assigner() {
		return (InsnTree value) -> {
			if (this.type() == value.type()) {
				return new ReassignInsnTree(this.name, value);
			}
			else {
				throw new IllegalArgumentException("Can't store " + value.type() + " in variable of type " + this.type());
			}
		};
	}
}