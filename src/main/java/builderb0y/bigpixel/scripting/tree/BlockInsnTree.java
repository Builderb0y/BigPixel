package builderb0y.bigpixel.scripting.tree;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.types.VectorType;

public class BlockInsnTree extends InsnTree {

	public final InsnTree wrapped;

	public BlockInsnTree(InsnTree wrapped) {
		if (!wrapped.canBeStatement()) {
			throw new IllegalArgumentException("Not a statement: " + wrapped);
		}
		super(wrapped.types);
		this.wrapped = wrapped;
	}

	@Override
	public void emitBytecode(Context context) {
		this.wrapped.emitBytecode(context.fork());
	}

	@Override
	public @Nullable InsnTree cast(VectorType... types) {
		InsnTree newWrapped = this.wrapped.cast(types);
		return newWrapped != null ? new BlockInsnTree(newWrapped) : null;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.wrapped.jumpsUnconditionally();
	}
}