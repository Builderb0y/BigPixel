package builderb0y.bigpixel.sources;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.Gradient;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.scripting.types.UtilityOperations;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class GradientRemapLayerSource extends PerPixelLayerSource {

	public static class Dependencies extends MainMaskDependencies {

		public UnmovableInputBinding
			fromStart = this.addBinding("from_start", "From Start: ", CurveHelper.PARAM),
			fromEnd   = this.addBinding("from_end",   "From End: ",   CurveHelper.PARAM),
			toStart   = this.addBinding("to_start",   "To Start: ",   CurveHelper.PARAM),
			toEnd     = this.addBinding("to_end",     "To End: ",     CurveHelper.PARAM);

		public Dependencies(LayerSource source) {
			super(source);
			this.fromStart.colorBox.color.set(Util.BLACK);
			this.  toStart.colorBox.color.set(Util.BLACK);
		}
	}

	@Override
	public MainMaskDependencies createDependencies() {
		return new Dependencies(this);
	}

	public Dependencies dependencies() {
		return (Dependencies)(this.dependencies);
	}

	public GradientRow
		fromGradient = new GradientRow(this, false),
		toGradient = new GradientRow(this, true);
	public CheckBox
		perChannel    = this.parameters.addCheckbox("per_channel", "Per Channel", false),
		preserveAlpha = this.parameters.addCheckbox("preserve_alpha", "Preserve Alpha", false);
	public HBox
		toggles = new HBox(this.perChannel, this.preserveAlpha);

	public GradientRemapLayerSource(LayerSources sources) {
		super(Type.GRADIENT_REMAP, sources);
		this.dependencies.addExtraNodeRow(this.fromGradient.getRootPane());
		this.dependencies.addExtraNodeRow(this.toGradient.getRootPane());
		this.dependencies.addExtraNodeRow(this.toggles);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
		if (this.perChannel.isSelected()) {
			if (this.preserveAlpha.isSelected()) {
				return new PerChannelRGBApplicator(this);
			}
			else {
				return new PerChannelAlphaApplicator(this);
			}
		}
		else {
			if (this.preserveAlpha.isSelected()) {
				return new UniformRGBApplicator(this);
			}
			else {
				return new UniformAlphaApplicator(this);
			}
		}
	}

	public static abstract class Applicator extends PerPixelApplicator {

		public final LayerSourceInput fromStart, fromEnd, toStart, toEnd;

		public Applicator(GradientRemapLayerSource source) {
			Dependencies dependencies = source.dependencies();
			this.fromStart = dependencies.fromStart.getCurrent();
			this.fromEnd   = dependencies.fromEnd  .getCurrent();
			this.toStart   = dependencies.toStart  .getCurrent();
			this.toEnd     = dependencies.toEnd    .getCurrent();
		}
	}

	public static class UniformAlphaApplicator extends Applicator {

		public UniformAlphaApplicator(GradientRemapLayerSource source) {
			super(source);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return VectorOperations.mix_float4_float4_float(
				this.toStart.getColor(x, y),
				this.toEnd.getColor(x, y),
				UtilityOperations.projectLineFrac_float4_float4_float4(
					this.fromStart.getColor(x, y),
					this.fromEnd.getColor(x, y),
					original
				)
			);
		}
	}

	public static class UniformRGBApplicator extends Applicator {

		public UniformRGBApplicator(GradientRemapLayerSource source) {
			super(source);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return VectorOperations.mix_float3_float3_float(
				this.toStart.getColor(x, y),
				this.toEnd.getColor(x, y),
				UtilityOperations.projectLineFrac_float3_float3_float3(
					this.fromStart.getColor(x, y),
					this.fromEnd.getColor(x, y),
					original
				)
			)
			.withLane(HDRImage.ALPHA_OFFSET, original.lane(HDRImage.ALPHA_OFFSET));
		}
	}

	public static class PerChannelAlphaApplicator extends Applicator {

		public PerChannelAlphaApplicator(GradientRemapLayerSource source) {
			super(source);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return VectorOperations.mix_float4_float4_float4(
				this.toStart.getColor(x, y),
				this.toEnd.getColor(x, y),
				VectorOperations.unmix_float4_float4_float4(
					this.fromStart.getColor(x, y),
					this.fromEnd.getColor(x, y),
					original
				)
			);
		}
	}

	public static class PerChannelRGBApplicator extends Applicator {

		public PerChannelRGBApplicator(GradientRemapLayerSource source) {
			super(source);
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return VectorOperations.mix_float3_float3_float3(
				this.toStart.getColor(x, y),
				this.toEnd.getColor(x, y),
				VectorOperations.unmix_float3_float3_float3(
					this.fromStart.getColor(x, y),
					this.fromEnd.getColor(x, y),
					original
				)
			)
			.withLane(HDRImage.ALPHA_OFFSET, original.lane(HDRImage.ALPHA_OFFSET));
		}
	}

	public static class GradientRow extends Gradient {

		public UnmovableInputBinding start, end;
		public FloatVector startColor, endColor;

		public GradientRow(GradientRemapLayerSource source, boolean to) {
			this.checkerboard().popOut().fixedSize(129.0D, 16.0D);
			this.start = to ? source.dependencies().toStart : source.dependencies().fromStart;
			this.end   = to ? source.dependencies().toEnd   : source.dependencies().fromEnd;
		}

		@Override
		public void redraw() {
			this.startColor = this.start.colorBox.getColor();
			this.endColor = this.end.colorBox.getColor();
			super.redraw();
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			return VectorOperations.mix_float4_float4_float(this.startColor, this.endColor, fraction);
		}
	}
}