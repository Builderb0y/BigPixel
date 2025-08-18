package builderb0y.notgimp.sources;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput;

public class NormalizeLayerSource extends PerPixelLayerSource {

	public CheckBox perChannel = this.parameters.addCheckbox("per_channel", "Per channel", true);

	public NormalizeLayerSource(LayerSources sources) {
		super(Type.NORMALIZE, sources);
		this.dependencies.addExtraNodeRow(this.perChannel);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
		HDRImage destination = this.sources.layer.image;
		boolean perChannel = this.perChannel.isSelected();
		FloatVector min = FloatVector.broadcast(FloatVector.SPECIES_128, Float.POSITIVE_INFINITY);
		FloatVector max = FloatVector.broadcast(FloatVector.SPECIES_128, Float.NEGATIVE_INFINITY);
		for (int y = 0; y < destination.height; y++) {
			for (int x = 0; x < destination.width; x++) {
				FloatVector pixel = main.getColor(x, y);
				if (pixel.lane(HDRImage.ALPHA_OFFSET) > 0.0F) {
					FloatVector maskColor = mask.getColor(x, y);
					VectorMask<Float> maskLanes = maskColor.compare(VectorOperators.GT, 0.0F).andNot(pixel.test(VectorOperators.IS_NAN));
					min = min.lanewise(VectorOperators.MIN, pixel, maskLanes);
					max = max.lanewise(VectorOperators.MAX, pixel, maskLanes);
				}
			}
		}
		if (!perChannel) {
			min = FloatVector.broadcast(FloatVector.SPECIES_128, min.reduceLanes(VectorOperators.MIN));
			max = FloatVector.broadcast(FloatVector.SPECIES_128, max.reduceLanes(VectorOperators.MAX));
		}
		if (min.lt(max).anyTrue()) {
			return new Applicator(min, max);
		}
		else {
			return NoopApplicator.INSTANCE;
		}
	}

	public static class Applicator extends PerPixelApplicator {

		public final FloatVector min, max;

		public Applicator(FloatVector min, FloatVector max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return VectorOperations.unmix_float4_float4_float4(this.min, this.max, original).blend(original, this.min.lt(this.max));
		}
	}
}