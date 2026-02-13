package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;

public abstract class PerPixelLayerSource extends MainMaskLayerSource {

	public PerPixelLayerSource(LayerSourceType type, LayerSources sources) {
		super(type, sources);
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
		PerPixelApplicator applicator = this.getApplicator(main, mask, frame);
		switch (mask) {
			case UniformSampler uniform -> {
				FloatVector maskColor = uniform.getColor();
				VectorMask<Float> oneLanes = maskColor.compare(VectorOperators.GE, 1.0F);
				VectorMask<Float> zeroLanes = maskColor.compare(VectorOperators.LE, 0.0F);
				if (oneLanes.allTrue()) {
					//mask behaves as passthrough to computed color.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							destination.setColor(x, y, applicator.apply(x, y, main.getColor(x, y)));
						}
					}
				}
				else if (zeroLanes.allTrue()) {
					//mask blocks computed color, allowing main color to be visible.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							destination.setColor(x, y, main.getColor(x, y));
						}
					}
				}
				else if (oneLanes.or(zeroLanes).allTrue()) {
					//mask is either completely passthrough or completely blocking for each component.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							FloatVector color = main.getColor(x, y);
							destination.setColor(x, y, color.blend(applicator.apply(x, y, color), oneLanes));
						}
					}
				}
				else {
					//mask contains at least one component which needs to be interpolated.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							FloatVector color = main.getColor(x, y);
							destination.setColor(x, y, carefulMix(color, applicator.apply(x, y, color), maskColor, zeroLanes, oneLanes));
						}
					}
				}
			}
			case VaryingSampler _ -> {
				for (int y = 0; y < destination.height; y++) {
					for (int x = 0; x < destination.width; x++) {
						FloatVector maskColor = mask.getColor(x, y);
						VectorMask<Float> zeroLanes = maskColor.compare(VectorOperators.LE, 0.0F);
						VectorMask<Float>  oneLanes = maskColor.compare(VectorOperators.GE, 1.0F);
						if (oneLanes.allTrue()) {
							destination.setColor(x, y, applicator.apply(x, y, main.getColor(x, y)));
						}
						else if (zeroLanes.allTrue()) {
							destination.setColor(x, y, main.getColor(x, y));
						}
						else if (oneLanes.or(zeroLanes).allTrue()) {
							FloatVector mainColor = main.getColor(x, y);
							destination.setColor(x, y, mainColor.blend(applicator.apply(x, y, mainColor), oneLanes));
						}
						else {
							FloatVector mainColor = main.getColor(x, y);
							destination.setColor(x, y, carefulMix(mainColor, applicator.apply(x, y, mainColor), maskColor, zeroLanes, oneLanes));
						}
					}
				}
			}
		}
		this.clampImage(destination);
	}

	public abstract PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException;

	public static abstract class PerPixelApplicator {

		public abstract FloatVector apply(int x, int y, FloatVector original);
	}

	public static class NoopApplicator extends PerPixelApplicator {

		public static final NoopApplicator INSTANCE = new NoopApplicator();

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return original;
		}
	}
}