package builderb0y.bigpixel.scripting.tree;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.tree.condition.ConditionTree;
import builderb0y.bigpixel.scripting.tree.condition.InsnConditionTree;
import builderb0y.bigpixel.scripting.types.VectorType;

public abstract class InsnTree implements CodeEmitter {

	public final VectorType[] types;

	public InsnTree(VectorType... types) {
		this.types = types;
	}

	public static int countActualTypes(InsnTree... trees) {
		int count = 0;
		for (InsnTree tree : trees) {
			count += tree.types().length;
		}
		return count;
	}

	public static VectorType[] flattenTypes(InsnTree... trees) {
		int count = countActualTypes(trees);
		VectorType[] types = new VectorType[count];
		int index = 0;
		for (InsnTree tree : trees) {
			System.arraycopy(tree.types(), 0, types, index, tree.types().length);
			index += tree.types().length;
		}
		assert index == count;
		return types;
	}

	public VectorType[] types() {
		return this.types;
	}

	public VectorType type() {
		return switch (this.types.length) {
			case 0 -> VectorType.VOID;
			case 1 -> this.types[0];
			default -> throw new ArityException(Integer.toString(this.types.length));
		};
	}

	public ConditionTree toCondition() {
		if (this.type() == VectorType.BOOLEAN) {
			return new InsnConditionTree(this);
		}
		else {
			throw new IllegalArgumentException("Not a boolean: " + this);
		}
	}

	public Assigner assigner() {
		return null;
	}

	@FunctionalInterface
	public static interface Assigner {

		public abstract InsnTree assign(InsnTree value);
	}

	@Override
	public abstract void emitBytecode(Context context);

	public @Nullable InsnTree cast(VectorType... types) {
		return Arrays.equals(this.types, types) ? this : null;
	}

	public InsnTree castToVoid() {
		if (this.types.length == 0) return this;
		else return new PopInsnTree(this);
	}

	public boolean canBeStatement() {
		return false;
	}

	public boolean jumpsUnconditionally() {
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " of types " + Arrays.toString(this.types);
	}
}