package builderb0y.bigpixel.sources;

import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.Gradient;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.scripting.types.UtilityOperations;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;
import builderb0y.bigpixel.util.Util;

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
		super(LayerSourceType.GRADIENT_REMAP, sources);
		this.dependencies.addExtraNodeRow(this.fromGradient.getRootPane());
		this.dependencies.addExtraNodeRow(this.toGradient.getRootPane());
		this.dependencies.addExtraNodeRow(this.toggles);
	}

	@Override
	public PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException {
		if (this.perChannel.isSelected()) {
			if (this.preserveAlpha.isSelected()) {
				return new PerChannelRGBApplicator(this, frame);
			}
			else {
				return new PerChannelAlphaApplicator(this, frame);
			}
		}
		else {
			if (this.preserveAlpha.isSelected()) {
				return new UniformRGBApplicator(this, frame);
			}
			else {
				return new UniformAlphaApplicator(this, frame);
			}
		}
	}

	public static abstract class Applicator extends PerPixelApplicator {

		public final Sampler fromStart, fromEnd, toStart, toEnd;

		public Applicator(GradientRemapLayerSource source, int frame) {
			Dependencies dependencies = source.dependencies();
			this.fromStart = dependencies.fromStart.getCurrent().createSamplerForFrame(frame);
			this.fromEnd   = dependencies.fromEnd  .getCurrent().createSamplerForFrame(frame);
			this.toStart   = dependencies.toStart  .getCurrent().createSamplerForFrame(frame);
			this.toEnd     = dependencies.toEnd    .getCurrent().createSamplerForFrame(frame);
		}
	}

	public static class UniformAlphaApplicator extends Applicator {

		public UniformAlphaApplicator(GradientRemapLayerSource source, int frame) {
			super(source, frame);
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

		public UniformRGBApplicator(GradientRemapLayerSource source, int frame) {
			super(source, frame);
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

		public PerChannelAlphaApplicator(GradientRemapLayerSource source, int frame) {
			super(source, frame);
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

		public PerChannelRGBApplicator(GradientRemapLayerSource source, int frame) {
			super(source, frame);
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
		public ObservableValue<FloatVector> startColor, endColor;

		public GradientRow(GradientRemapLayerSource source, boolean to) {
			this.fixedSize(129.0D, 16.0D).checkerboard().popOut();
			this.start = to ? source.dependencies().toStart : source.dependencies().fromStart;
			this.end   = to ? source.dependencies().toEnd   : source.dependencies().fromEnd;
			Function<SamplerProvider, ObservableValue<FloatVector>> mapper = (SamplerProvider provider) -> provider instanceof UniformSamplerProvider uniform ? uniform.colorProperty() : null;
			this.startColor = this.start.selection.valueProperty().flatMap(mapper);
			this.endColor   = this.end.selection.valueProperty().flatMap(mapper);
			InvalidationListener redrawer = (Observable _) -> this.redraw();
			this.startColor.addListener(redrawer);
			this.endColor.addListener(redrawer);
			this.redraw();
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			FloatVector start = this.startColor.getValue();
			FloatVector end = this.endColor.getValue();
			if (start != null && end != null) {
				return VectorOperations.mix_float4_float4_float(start, end, fraction);
			}
			else {
				return Util.INVISIBLACK;
			}
		}
	}
}