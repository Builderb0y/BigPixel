package builderb0y.bigpixel.scripting.tree;

import builderb0y.bigpixel.scripting.types.VectorType;

import static org.objectweb.asm.Opcodes.*;

public class ReassignInsnTree extends InsnTree {

	public final String name;
	public final InsnTree value;

	public ReassignInsnTree(String name, InsnTree value) {
		if (value.type() == VectorType.VOID) {
			throw new IllegalArgumentException("Can't assign void to variable");
		}
		super();
		this.name = name;
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		int index = context.getVariable(this.name).index();
		this.value.emitBytecode(context);
		context.codeBuilder.visitVarInsn(
			switch (this.value.type()) {
				case BOOLEAN, INT -> ISTORE;
				case LONG -> LSTORE;
				case FLOAT -> FSTORE;
				case DOUBLE -> DSTORE;
				default -> ASTORE;
			},
			index
		);
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}