package builderb0y.bigpixel.sources;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.Gradient;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;
import builderb0y.bigpixel.util.CanvasHelper;
import builderb0y.bigpixel.util.Util;

public class CliffCurveLayerSource extends PerPixelLayerSource {

	public static class Dependencies extends MainMaskDependencies {

		public UnmovableInputBinding
			strength = this.addBinding("strength", "Strength: ", CurveHelper.PARAM),
			midpoint = this.addBinding("midpoint", "Midpoint: ", CurveHelper.PARAM);

		public Dependencies(LayerSource source) {
			super(source);
			FloatVector half = FloatVector.broadcast(FloatVector.SPECIES_128, 0.5F);
			this.strength.colorBox.color.set(half);
			this.midpoint.colorBox.color.set(half);
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
		super(LayerSourceType.CLIFF_CURVE, sources);
		this.dependencies().midpoint.bindDisabled(this.dual.selectedProperty().not());
		this.dependencies().addExtraNodeRow(new HBox(this.linear, this.dual));
		this.dependencies().addExtraNodeRow(this.gradient.getRootPane());
	}

	public CliffCurver getCurver(boolean requireUniform, int frame) {
		SamplerProvider strength = this.dependencies().strength.getCurrent();
		if (requireUniform && !(strength instanceof UniformSamplerProvider)) return null;
		boolean linear = this.linear.isSelected();
		if (this.dual.isSelected()) {
			SamplerProvider midpoint = this.dependencies().midpoint.getCurrent();
			if (requireUniform && !(midpoint instanceof UniformSamplerProvider)) return null;
			return new DualCliffCurver(strength.createSamplerForFrame(frame), midpoint.createSamplerForFrame(frame), linear);
		}
		else {
			return new SingleCliffCurver(strength.createSamplerForFrame(frame), linear);
		}
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		this.gradient.redraw(this.gradient);
		super.doRedraw(frame);
	}

	@Override
	public PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException {
		return this.getCurver(false, frame);
	}

	public static abstract class CliffCurver extends PerPixelApplicator {

		public final boolean linear;
		public final Sampler strength;

		public CliffCurver(Sampler strength, boolean linear) {
			this.strength = strength;
			this.linear = linear;
		}
	}

	public static class SingleCliffCurver extends CliffCurver {

		public SingleCliffCurver(Sampler strength, boolean linear) {
			super(strength, linear);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector input) {
			input = input.min(1.0F).max(0.0F);
			if (this.linear) input = input.mul(input);
			FloatVector strength = this.strength.getColor(x, y);
			strength = strength.div(strength.sub(1.0F));
			strength = strength.mul(strength);
			input = input.mul(strength).div(strength.sub(1.0F).mul(input).add(1.0F));
			if (this.linear) input = input.sqrt();
			return input;
		}
	}

	public static class DualCliffCurver extends CliffCurver {

		public final Sampler midpoint;

		public DualCliffCurver(Sampler strength, Sampler midpoint, boolean linear) {
			super(strength, linear);
			this.midpoint = midpoint;
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector input) {
			input = input.min(1.0F).max(0.0F);
			FloatVector strength = this.strength.getColor(x, y);
			FloatVector midpoint = this.midpoint.getColor(x, y);
			strength = strength.div(strength.sub(1.0F));
			strength = strength.mul(strength);
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
		public void redraw(CanvasHelper canvas) {
			this.curver = CliffCurveLayerSource.this.getCurver(true, 0);
			this.getRootPane().setVisible(this.curver != null);
			if (this.curver != null) super.redraw(canvas);
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			return this.curver.apply(0, 0, FloatVector.broadcast(FloatVector.SPECIES_128, fraction).withLane(HDRImage.ALPHA_OFFSET, 1.0F));
		}
	}
}