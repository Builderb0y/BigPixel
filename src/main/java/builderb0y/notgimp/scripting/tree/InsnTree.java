package builderb0y.notgimp.scripting.tree;

import java.util.Objects;

import builderb0y.notgimp.scripting.types.VectorType;

public abstract class InsnTree implements CodeEmitter {

	public final VectorType type;

	public InsnTree(VectorType type) {
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public abstract void emitBytecode(Context context);

	public boolean canBeStatement() {
		return false;
	}

	public boolean jumpsUnconditionally() {
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " of type " + this.type;
	}
}