package builderb0y.bigpixel.scripting.tree;

import static org.objectweb.asm.Opcodes.*;

public class ReturnInsnTree extends InsnTree {

	public final InsnTree value;

	public ReturnInsnTree(InsnTree value) {
		value.type();
		super();
		this.value = value;
	}

	@Override
	public void emitBytecode(Context context) {
		this.value.emitBytecode(context);
		context.codeBuilder.visitInsn(
			switch (this.value.type()) {
				case INT, BOOLEAN -> IRETURN;
				case LONG -> LRETURN;
				case FLOAT -> FRETURN;
				case DOUBLE -> DRETURN;
				case VOID -> RETURN;
				default -> ARETURN;
			}
		);
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return true;
	}
}