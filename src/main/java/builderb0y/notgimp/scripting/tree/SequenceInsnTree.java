package builderb0y.notgimp.scripting.tree;

public class SequenceInsnTree extends InsnTree {

	public final InsnTree[] statements;

	public SequenceInsnTree(InsnTree... statements) {
		super(statements[statements.length - 1].types());
		this.statements = flatten(statements);
	}

	/** I expect this method to be a bit of a hot spot, so optimize the hell out of it! */
	public static InsnTree[] flatten(InsnTree[] statements) {
		//count elements so we only need to allocate an array once.
		int flattenedLength = 0;
		for (int index = 0, length = statements.length; index < length; index++) {
			InsnTree statement = statements[index];
			if (statement == null) {
				throw new NullPointerException("Null statement at index " + index);
			}
			flattenedLength += statement instanceof SequenceInsnTree sequence ? sequence.statements.length : 1;
		}
		//now flatten all the elements.
		InsnTree[] result = new InsnTree[flattenedLength];
		int writeIndex = 0;
		for (int readIndex = 0, length = statements.length; readIndex < length; readIndex++) {
			InsnTree statement = statements[readIndex];
			if (statement instanceof SequenceInsnTree sequence) {
				System.arraycopy(sequence.statements, 0, result, writeIndex, sequence.statements.length);
				writeIndex += sequence.statements.length;
			}
			else {
				result[writeIndex++] = statement;
			}
			//when appending a sequence, only the last statement in that
			//sequence needs to be popped. all the other statements
			//in the sequence are guaranteed to already be statements.
			//
			//when appending a single element, that
			//element always needs to be popped.
			//
			//in either case, after an append operation is complete,
			//the last element in result needs to be popped.
			//except, the last element in result should not be popped
			//when result is completely full and contains an element
			//at every index. hence the length check below.
			if (writeIndex != flattenedLength) {
				result[writeIndex - 1] = new PopInsnTree(result[writeIndex - 1]);
			}
		}
		return result;
	}

	@Override
	public void emitBytecode(Context context) {
		for (InsnTree statement : this.statements) {
			statement.emitBytecode(context);
		}
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.statements[this.statements.length - 1].jumpsUnconditionally();
	}

	@Override
	public boolean canBeStatement() {
		return this.statements[this.statements.length - 1].canBeStatement();
	}
}