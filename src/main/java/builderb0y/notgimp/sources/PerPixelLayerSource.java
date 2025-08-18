package builderb0y.notgimp.sources;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public abstract class PerPixelLayerSource extends MainMaskLayerSource {

	public PerPixelLayerSource(Type type, LayerSources sources) {
		super(type, sources);
	}

	@Override
	public void doRedraw(LayerSourceInput main, LayerSourceInput mask, HDRImage destination) throws RedrawException {
		PerPixelApplicator applicator = this.getApplicator(main, mask);
		switch (mask) {
			case UniformLayerSourceInput uniform -> {
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
			case VaryingLayerSourceInput _ -> {
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
							destination.setColor(x, y, main.getColor(x, y).blend(destination.getColor(x, y), oneLanes));
						}
						else {
							destination.setColor(x, y, carefulMix(main.getColor(x, y), destination.getColor(x, y), maskColor, zeroLanes, oneLanes));
						}
					}
				}
			}
		}
	}

	public abstract PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException;

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