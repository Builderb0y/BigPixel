package builderb0y.bigpixel.scripting.tree;

import java.lang.classfile.Label;
import java.lang.classfile.instruction.SwitchCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.scripting.types.VectorType;

public class SwitchInsnTree extends InsnTree {

	public static record Case(int[] matchedValues, InsnTree tree) {}

	public final InsnTree value;
	public final List<Case> cases;
	public final @Nullable InsnTree defaultCase;

	public SwitchInsnTree(
		VectorType[] types,
		InsnTree value,
		List<Case> cases,
		@Nullable InsnTree defaultCase
	) {
		super(types);
		this.cases = cases;
		this.value = value;
		this.defaultCase = defaultCase;
	}

	public static InsnTree create(InsnTree value, List<Case> cases, @Nullable InsnTree defaultCase) {
		int casesCount = cases.size();
		if (casesCount == 0) throw new IllegalArgumentException("No cases");
		VectorType[] types = null;
		for (int index = 0; index <= casesCount; index++) {
			InsnTree tree;
			if (index < cases.size()) {
				tree = cases.get(index).tree;
			}
			else if (defaultCase != null) {
				tree = defaultCase;
			}
			else {
				break;
			}
			if (tree.jumpsUnconditionally()) continue;
			VectorType[] newTypes = tree.types();
			if (types == null) types = newTypes;
			else if (!Arrays.equals(types, newTypes)) throw new IllegalArgumentException("All cases must evaluate to the same type");
		}
		if (types == null) types = new VectorType[0];
		return new SwitchInsnTree(types, value, cases, defaultCase);
	}

	@Override
	public void emitBytecode(Context context) {
		List<Label> labels = new ArrayList<>(this.cases.size());
		Label end = context.codeBuilder.newLabel();
		int minKey = Integer.MAX_VALUE;
		int maxKey = Integer.MIN_VALUE;
		List<SwitchCase> compiledCases = new ArrayList<>(this.cases.size() << 1);
		for (Case case_ : this.cases) {
			Label label = context.codeBuilder.newLabel();
			labels.add(label);
			for (int matchedValue : case_.matchedValues) {
				minKey = Math.min(minKey, matchedValue);
				maxKey = Math.max(maxKey, matchedValue);
				compiledCases.add(SwitchCase.of(matchedValue, label));
			}
		}
		Label defaultLabel = this.defaultCase != null ? context.codeBuilder.newLabel() : end;
		int range = maxKey - minKey + 1;
		int occupancy = this.cases.size();
		this.value.emitBytecode(context);
		if (occupancy < range >> 2) { //sparse.
			context.codeBuilder.lookupswitch(defaultLabel, compiledCases);
		}
		else { //dense.
			context.codeBuilder.tableswitch(minKey, maxKey, defaultLabel, compiledCases);
		}
		List<Case> cases = this.cases;
		for (int index = 0, size = cases.size(); index < size; index++) {
			Case case_ = cases.get(index);
			context.codeBuilder.labelBinding(labels.get(index));
			case_.tree.emitBytecode(context);
			context.codeBuilder.goto_(end);
		}
		if (this.defaultCase != null) {
			context.codeBuilder.labelBinding(defaultLabel);
			this.defaultCase.emitBytecode(context);
			//we can fallthrough here, since we're at the end anyway.
		}
		context.codeBuilder.labelBinding(end);
	}

	@Override
	public boolean canBeStatement() {
		return (
			this.cases.stream().map(Case::tree).allMatch(InsnTree::canBeStatement) &&
			(this.defaultCase == null || this.defaultCase.canBeStatement())
		);
	}

	@Override
	public @Nullable InsnTree cast(VectorType... types) {
		if (Arrays.equals(this.types, types)) return this;
		if (this.defaultCase == null) return null;
		List<Case> newCases = new ArrayList<>(this.cases.size());
		for (Case case_ : this.cases) {
			InsnTree tree = case_.tree.cast(types);
			if (tree == null) return null;
			newCases.add(new Case(case_.matchedValues, tree));
		}
		InsnTree defaultCase = this.defaultCase.cast(types);
		if (defaultCase == null) return null;
		return new SwitchInsnTree(types, this.value, newCases, defaultCase);
	}

	@Override
	public InsnTree castToVoid() {
		if (this.types.length == 0) return this;
		List<Case> newCases = new ArrayList<>(this.cases.size());
		for (Case case_ : this.cases) {
			newCases.add(new Case(case_.matchedValues, case_.tree.castToVoid()));
		}
		InsnTree defaultCase = this.defaultCase != null ? this.defaultCase.castToVoid() : null;
		return new SwitchInsnTree(new VectorType[0], this.value, newCases, defaultCase);
	}
}