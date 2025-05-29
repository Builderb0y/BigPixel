package builderb0y.notgimp.sources;

import java.util.Collection;

import javafx.scene.Node;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonMap;

public abstract class LayerSource {

	public static final VectorMask<Float> RGB_MASK;

	static {
		boolean[] mask = new boolean[4];
		mask[HDRImage.  RED_OFFSET] =
		mask[HDRImage.GREEN_OFFSET] =
		mask[HDRImage. BLUE_OFFSET] =
		true;
		RGB_MASK = VectorMask.fromValues(FloatVector.SPECIES_128, mask);
	}

	public LayerSources sources;
	public String displayName;

	public LayerSource(LayerSources sources, String displayName) {
		this.sources = sources;
		this.displayName = displayName;
	}

	public abstract JsonMap save();

	public abstract void load(JsonMap map);

	public void onSelected() {
		this.invalidateStructure();
	}

	public void onDeselected() {
		this.sources.layer.redrawException.set(null);
	}

	public abstract void invalidateStructure();

	public abstract Collection<Layer> getDependencies();

	public abstract boolean isAnimated();

	public abstract Node getRootNode();

	public void requestRedraw() {
		this.sources.layer.requestRedraw();
	}

	public void redraw() {
		try {
			this.doRedraw();
			this.sources.layer.redrawException.set(null);
		}
		catch (Exception exception) {
			this.sources.layer.redrawException.set(exception);
		}
	}

	public abstract void doRedraw() throws RedrawException;

	@Override
	public String toString() {
		return this.displayName;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}
}