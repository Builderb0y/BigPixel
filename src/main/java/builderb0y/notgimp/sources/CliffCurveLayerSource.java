package builderb0y.notgimp.sources;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.Gradient;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.dependencies.MainMaskDependencies;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.notgimp.sources.dependencies.inputs.UnmovableInputBinding;

public class CliffCurveLayerSource extends PerPixelLayerSource {

	public static class Dependencies extends MainMaskDependencies {

		public UnmovableInputBinding
			strength = this.addBinding("strength", "Strength: ", (FloatVector color) -> {
				color = color.div(color.sub(1.0F));
				return color.mul(color);
			}),
			midpoint = this.addBinding("midpoint", "Midpoint: ");

		public Dependencies(LayerSource source) {
			super(source);
		}
	}

	@Override
	public MainMaskDependencies createDependencies() {
		return new Dependencies(this);
	}

	public Dependencies dependencies() {
		return (Dependencies)(this.dependencies);
	}

	public CheckBox
		dual     = this.parameters.addCheckbox("dual",   "Dual", false),
		linear   = this.parameters.addCheckbox("linear", "Linear", true);
	public CliffGradient
		gradient = (CliffGradient)(new CliffGradient().fixedSize(128.0D, 16.0D).popIn());

	public CliffCurveLayerSource(LayerSources sources) {
		super(Type.CLIFF_CURVE, sources);
		this.dependencies().midpoint.bindDisabled(this.dual.selectedProperty().not());
		this.dependencies().addExtraNodeRow(this.gradient.getRootPane());
	}

	public CliffCurver getCurver(boolean requireUniform) {
		LayerSourceInput strength = this.dependencies().strength.getCurrent();
		if (requireUniform && !(strength instanceof UniformLayerSourceInput)) return null;
		boolean linear = this.linear.isSelected();
		if (this.dual.isSelected()) {
			LayerSourceInput midpoint = this.dependencies().midpoint.getCurrent();
			if (requireUniform && !(midpoint instanceof UniformLayerSourceInput)) return null;
			return new DualCliffCurver(strength, midpoint, linear);
		}
		else {
			return new SingleCliffCurver(strength, linear);
		}
	}

	@Override
	public void doRedraw() throws RedrawException {
		this.gradient.redraw();
		super.doRedraw();
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
		return this.getCurver(false);
	}

	public static abstract class CliffCurver extends PerPixelApplicator {

		public final boolean linear;
		public final LayerSourceInput strength;

		public CliffCurver(LayerSourceInput strength, boolean linear) {
			this.strength = strength;
			this.linear = linear;
		}
	}

	public static class SingleCliffCurver extends CliffCurver {

		public SingleCliffCurver(LayerSourceInput strength, boolean linear) {
			super(strength, linear);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector input) {
			input = input.min(1.0F).max(0.0F);
			if (this.linear) input = input.mul(input);
			FloatVector strength = this.strength.getColor(x, y);
			input = input.mul(strength).div(strength.sub(1.0F).mul(input).add(1.0F));
			if (this.linear) input = input.sqrt();
			return input;
		}
	}

	public static class DualCliffCurver extends CliffCurver {

		public final LayerSourceInput midpoint;

		public DualCliffCurver(LayerSourceInput strength, LayerSourceInput midpoint, boolean linear) {
			super(strength, linear);
			this.midpoint = midpoint;
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector input) {
			FloatVector strength = this.strength.getColor(x, y);
			FloatVector midpoint = this.midpoint.getColor(x, y);
			input = input.min(1.0F).max(0.0F);
			if (this.linear) input = input.mul(input);
			VectorMask<Float> low = input.compare(VectorOperators.LE, midpoint);
			VectorMask<Float> high = input.compare(VectorOperators.GE, midpoint);
			if (low.allTrue()) {
				//fast path: only need to compute curve for low end.
				input = input.mul(strength).div(strength.sub(1.0F).mul(input).add(1.0F)).mul(midpoint);
			}
			else if (high.allTrue()) {
				//medium path: need to compute curve for high end only.
				FloatVector rcpStrength = Util.WHITE.div(strength);
				FloatVector invMidpoint = Util.WHITE.sub(midpoint);
				input = input.sub(midpoint).div(invMidpoint);
				input = input.mul(rcpStrength).div(rcpStrength.sub(1.0F).mul(input).add(1.0F)).mul(invMidpoint).add(midpoint);
			}
			else {
				//slow path: need to compute curve for high and low ends.
				FloatVector lowValue = input.div(midpoint);
				lowValue = lowValue.mul(strength).div(strength.sub(1.0F).mul(lowValue).add(1.0F)).mul(midpoint);
				FloatVector rcpStrength = Util.WHITE.div(strength);
				FloatVector invMidpoint = Util.WHITE.sub(midpoint);
				FloatVector highValue = input.sub(midpoint).div(invMidpoint);
				highValue = highValue.mul(rcpStrength).div(rcpStrength.sub(1.0F).mul(highValue).add(1.0F)).mul(invMidpoint).add(midpoint);
				input = input.blend(lowValue, low).blend(highValue, high);
			}
			if (this.linear) input = input.sqrt();
			return input;
		}
	}

	public class CliffGradient extends Gradient {

		public CliffCurver curver;

		@Override
		public void redraw() {
			this.curver = CliffCurveLayerSource.this.getCurver(true);
			this.getRootPane().setVisible(this.curver != null);
			if (this.curver != null) super.redraw();
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			return this.curver.apply(0, 0, FloatVector.broadcast(FloatVector.SPECIES_128, fraction).withLane(HDRImage.ALPHA_OFFSET, 1.0F));
		}
	}
}