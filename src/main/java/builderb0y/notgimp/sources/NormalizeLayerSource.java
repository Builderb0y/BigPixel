package builderb0y.notgimp.sources;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.scripting.types.UtilityOperations;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class NormalizeLayerSource extends EffectLayerSource {

	public CheckBox
		perChannel = new CheckBox("Per channel"),
		red = new CheckBox("Red"),
		green = new CheckBox("Green"),
		blue = new CheckBox("Blue"),
		alpha = new CheckBox("Alpha");
	public VBox rootPane = new VBox(
		this.perChannel,
		this.red,
		this.green,
		this.blue,
		this.alpha
	);

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "normalize")
			.with("per_channel", this.perChannel.isSelected())
		);
	}

	@Override
	public void load(JsonMap map) {
		this.perChannel.setSelected(map.getBoolean("per_channel"));
	}

	public NormalizeLayerSource(LayerSources sources) {
		super(sources, "Normalize");
		this.red.setSelected(true);
		this.green.setSelected(true);
		this.blue.setSelected(true);
		this.alpha.setSelected(false);
		ChangeListener<Boolean> listener = Util.change(() -> this.redraw(false));
		this.perChannel.selectedProperty().addListener(listener);
		this.red.selectedProperty().addListener(listener);
		this.green.selectedProperty().addListener(listener);
		this.blue.selectedProperty().addListener(listener);
		this.alpha.selectedProperty().addListener(listener);
	}

	public void copyFrom(NormalizeLayerSource that) {
		this.perChannel.setSelected(that.perChannel.isSelected());
		this.red.setSelected(that.red.isSelected());
		this.green.setSelected(that.green.isSelected());
		this.blue.setSelected(that.blue.isSelected());
		this.alpha.setSelected(that.alpha.isSelected());
	}

	@Override
	public void doRedraw(boolean fromAnimation) throws RedrawException {
		if (this.watching.size() != 1) {
			throw new RedrawException("Expected exactly one child layer");
		}
		boolean[] channels = new boolean[4];
		channels[HDRImage.  RED_OFFSET] = this.red  .isSelected();
		channels[HDRImage.GREEN_OFFSET] = this.green.isSelected();
		channels[HDRImage. BLUE_OFFSET] = this.blue .isSelected();
		channels[HDRImage.ALPHA_OFFSET] = this.alpha.isSelected();
		VectorMask<Float> mask = VectorMask.fromArray(FloatVector.SPECIES_128, channels, 0);
		if (mask.not().allTrue()) { //WHY IS THERE NO allFalse() AAAAAAAAAAAAAAAAAAAAAAAAAAA
			return;
		}
		HDRImage source = this.watching.iterator().next().image;
		HDRImage destination = this.sources.layer.image;
		boolean perChannel = this.perChannel.isSelected();
		FloatVector min = FloatVector.broadcast(FloatVector.SPECIES_128, Float.POSITIVE_INFINITY);
		FloatVector max = FloatVector.broadcast(FloatVector.SPECIES_128, Float.NEGATIVE_INFINITY);
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			min = min.min(pixel);
			max = max.max(pixel);
		}
		if (!perChannel) {
			min = FloatVector.broadcast(FloatVector.SPECIES_128, min.reduceLanes(VectorOperators.MIN, mask));
			max = FloatVector.broadcast(FloatVector.SPECIES_128, max.reduceLanes(VectorOperators.MAX, mask));
		}
		for (int index = 0, length = source.pixels.length; index < length; index += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			FloatVector newPixel = VectorOperations.unmix_float4_float4_float4(min, max, pixel);
			newPixel.intoArray(destination.pixels, index, mask);
		}
		destination.markDirty(fromAnimation);
	}

	@Override
	public Node getRootNode() {
		return this.rootPane;
	}
}