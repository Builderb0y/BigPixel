package builderb0y.notgimp.sources;

import java.util.Collection;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public class CliffCurveLayerSource extends EffectLayerSource {

	public CheckBox
		splitRgb = new CheckBox(),
		dual     = new CheckBox(),
		linear   = new CheckBox();
	public Spinner<Float>
		rgbCoefficient   = Util.setupSpinner(new Spinner<>(new CoefficientModel()), 96.0D),
		redCoefficient   = Util.setupSpinner(new Spinner<>(new CoefficientModel()), 96.0D),
		greenCoefficient = Util.setupSpinner(new Spinner<>(new CoefficientModel()), 96.0D),
		blueCoefficient  = Util.setupSpinner(new Spinner<>(new CoefficientModel()), 96.0D),
		alphaCoefficient = Util.setupSpinner(new Spinner<>(new CoefficientModel()), 96.0D),
		rgbMid           = Util.setupSpinner(new Spinner<>(new MidModel()), 96.0D),
		redMid           = Util.setupSpinner(new Spinner<>(new MidModel()), 96.0D),
		greenMid         = Util.setupSpinner(new Spinner<>(new MidModel()), 96.0D),
		blueMid          = Util.setupSpinner(new Spinner<>(new MidModel()), 96.0D),
		alphaMid         = Util.setupSpinner(new Spinner<>(new MidModel()), 96.0D);
	public GridPane
		gridPane = new GridPane();

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "cliff")
			.with("split_rgb",         this.splitRgb        .isSelected())
			.with("dual",              this.dual            .isSelected())
			.with("linear",            this.linear          .isSelected())
			.with("rgb_coefficient",   this.rgbCoefficient  .getValue())
			.with("red_coefficient",   this.redCoefficient  .getValue())
			.with("green_coefficient", this.greenCoefficient.getValue())
			.with("blue_coefficient",  this.blueCoefficient .getValue())
			.with("alpha_coefficient", this.alphaCoefficient.getValue())
			.with("rgb_mid",           this.rgbMid          .getValue())
			.with("red_mid",           this.redMid          .getValue())
			.with("green_mid",         this.greenMid        .getValue())
			.with("blue_mid",          this.blueMid         .getValue())
			.with("alpha_mid",         this.alphaMid        .getValue())
		);
	}

	@Override
	public void load(JsonMap map) {
		this.splitRgb        .setSelected               (map.getBoolean("split_rgb"        ));
		this.dual            .setSelected               (map.getBoolean("dual"             ));
		this.linear          .setSelected               (map.getBoolean("linear"           ));
		this.rgbCoefficient  .getValueFactory().setValue(map.getFloat  ("rgb_coefficient"  ));
		this.redCoefficient  .getValueFactory().setValue(map.getFloat  ("red_coefficient"  ));
		this.greenCoefficient.getValueFactory().setValue(map.getFloat  ("green_coefficient"));
		this.blueCoefficient .getValueFactory().setValue(map.getFloat  ("blue_coefficient" ));
		this.alphaCoefficient.getValueFactory().setValue(map.getFloat  ("alpha_coefficient"));
		this.rgbMid          .getValueFactory().setValue(map.getFloat  ("rgb_mid"          ));
		this.redMid          .getValueFactory().setValue(map.getFloat  ("red_mid"          ));
		this.greenMid        .getValueFactory().setValue(map.getFloat  ("green_mid"        ));
		this.blueMid         .getValueFactory().setValue(map.getFloat  ("blue_mid"         ));
		this.alphaMid        .getValueFactory().setValue(map.getFloat  ("alpha_mid"        ));
	}

	public CliffCurveLayerSource(LayerSources sources) {
		super(sources, "Cliff Curve");
		this.splitRgb.selectedProperty().addListener(Util.change(this::layout));
		BooleanBinding disableMid = this.dual.selectedProperty().not();
		this.rgbMid  .disableProperty().bind(disableMid);
		this.redMid  .disableProperty().bind(disableMid);
		this.greenMid.disableProperty().bind(disableMid);
		this.blueMid .disableProperty().bind(disableMid);
		this.alphaMid.disableProperty().bind(disableMid);
		this.linear.setSelected(true);
		this.layout();
		ChangeListener<Object> listener = Util.change(this::requestRedraw);
		this.splitRgb        .selectedProperty().addListener(listener);
		this.dual            .selectedProperty().addListener(listener);
		this.linear          .selectedProperty().addListener(listener);
		this.rgbCoefficient  .getValueFactory().valueProperty().addListener(listener);
		this.redCoefficient  .getValueFactory().valueProperty().addListener(listener);
		this.greenCoefficient.getValueFactory().valueProperty().addListener(listener);
		this.blueCoefficient .getValueFactory().valueProperty().addListener(listener);
		this.alphaCoefficient.getValueFactory().valueProperty().addListener(listener);
		this.rgbMid          .getValueFactory().valueProperty().addListener(listener);
		this.redMid          .getValueFactory().valueProperty().addListener(listener);
		this.greenMid        .getValueFactory().valueProperty().addListener(listener);
		this.blueMid         .getValueFactory().valueProperty().addListener(listener);
		this.alphaMid        .getValueFactory().valueProperty().addListener(listener);
	}

	public void copyFrom(CliffCurveLayerSource that) {
		this.splitRgb        .setSelected(that.splitRgb.isSelected());
		this.dual            .setSelected(that.dual    .isSelected());
		this.linear          .setSelected(that.linear  .isSelected());
		this.rgbCoefficient  .getValueFactory().setValue(that.rgbCoefficient  .getValueFactory().getValue());
		this.redCoefficient  .getValueFactory().setValue(that.redCoefficient  .getValueFactory().getValue());
		this.greenCoefficient.getValueFactory().setValue(that.greenCoefficient.getValueFactory().getValue());
		this.blueCoefficient .getValueFactory().setValue(that.blueCoefficient .getValueFactory().getValue());
		this.alphaCoefficient.getValueFactory().setValue(that.alphaCoefficient.getValueFactory().getValue());
		this.rgbMid          .getValueFactory().setValue(that.rgbMid          .getValueFactory().getValue());
		this.redMid          .getValueFactory().setValue(that.redMid          .getValueFactory().getValue());
		this.greenMid        .getValueFactory().setValue(that.greenMid        .getValueFactory().getValue());
		this.blueMid         .getValueFactory().setValue(that.blueMid         .getValueFactory().getValue());
		this.alphaMid        .getValueFactory().setValue(that.alphaMid        .getValueFactory().getValue());
	}

	public void layout() {
		this.gridPane.getChildren().clear();
		int row = 0;
		this.gridPane.addRow(row++, new Label("Split RGB: "), this.splitRgb);
		if (this.splitRgb.isSelected()) {
			this.gridPane.addRow(row++, new Label(  "Red: "), this.  redCoefficient);
			this.gridPane.addRow(row++, new Label("Green: "), this.greenCoefficient);
			this.gridPane.addRow(row++, new Label( "Blue: "), this. blueCoefficient);
		}
		else {
			this.gridPane.addRow(row++, new Label("RGB: "), this.rgbCoefficient);
		}
		this.gridPane.addRow(row++, new Label("Alpha: "), this.alphaCoefficient);
		this.gridPane.addRow(row++, new Label("Dual: "), this.dual);
		if (this.splitRgb.isSelected()) {
			this.gridPane.addRow(row++, new Label(  "Red mid: "), this.  redMid);
			this.gridPane.addRow(row++, new Label("Green mid: "), this.greenMid);
			this.gridPane.addRow(row++, new Label( "Blue mid: "), this. blueMid);
		}
		else {
			this.gridPane.addRow(row++, new Label("RGB mid: "), this.rgbMid);
		}
		this.gridPane.addRow(row++, new Label("Alpha mid: "), this.alphaMid);
		this.gridPane.addRow(row, new Label("Linear: "), this.linear);
	}

	public FloatVector getCoefficients() {
		float[] floats = new float[4];
		if (this.splitRgb.isSelected()) {
			floats[HDRImage.  RED_OFFSET] = this.  redCoefficient.getValue();
			floats[HDRImage.GREEN_OFFSET] = this.greenCoefficient.getValue();
			floats[HDRImage. BLUE_OFFSET] = this. blueCoefficient.getValue();
		}
		else {
			floats[HDRImage.  RED_OFFSET] =
			floats[HDRImage.GREEN_OFFSET] =
			floats[HDRImage. BLUE_OFFSET] =
			this.rgbCoefficient.getValue();
		}
		floats[HDRImage.ALPHA_OFFSET] = this.alphaCoefficient.getValue();
		return FloatVector.fromArray(FloatVector.SPECIES_128, floats, 0);
	}

	public FloatVector getMids() {
		float[] floats = new float[4];
		if (this.splitRgb.isSelected()) {
			floats[HDRImage.  RED_OFFSET] = this.  redMid.getValue();
			floats[HDRImage.GREEN_OFFSET] = this.greenMid.getValue();
			floats[HDRImage. BLUE_OFFSET] = this. blueMid.getValue();
		}
		else {
			floats[HDRImage.  RED_OFFSET] =
			floats[HDRImage.GREEN_OFFSET] =
			floats[HDRImage. BLUE_OFFSET] =
			this.rgbMid.getValue();
		}
		floats[HDRImage.ALPHA_OFFSET] = this.alphaMid.getValue();
		return FloatVector.fromArray(FloatVector.SPECIES_128, floats, 0);
	}

	@Override
	public void doRedraw() throws RedrawException {
		Collection<TreeItem<Layer>> watching = this.getWatchedItems();
		if (watching.size() != 1) {
			throw new RedrawException("Expected exactly 1 child layer");
		}
		HDRImage source = watching.iterator().next().getValue().image;
		HDRImage destination = this.sources.layer.image;
		FloatVector coefficients = this.getCoefficients();
		boolean linear = this.linear.isSelected();
		if (this.dual.isSelected()) {
			FloatVector mids = this.getMids();
			FloatVector rcpCoefficients = coefficients.broadcast(1.0F).div(coefficients);
			for (int index = 0, length = source.pixels.length; index < length; index += 4) {
				FloatVector value = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
				value = value.min(1.0F).max(0.0F);
				if (linear) value = value.mul(value);
				VectorMask<Float> low = value.lt(mids);
				VectorMask<Float> high = value.compare(VectorOperators.GT, mids);
				if (low.allTrue()) {
					//fast path: only need to compute curve for low end.
					value = value.mul(coefficients).div(coefficients.sub(1.0F).mul(value).add(1.0F)).mul(mids);
				}
				else if (high.allTrue()) {
					//medium path: need to compute curve for high end only.
					FloatVector invMids = mids.broadcast(1.0F).sub(mids);
					value = value.sub(mids).div(invMids);
					value = value.mul(rcpCoefficients).div(rcpCoefficients.sub(1.0F).mul(value).add(1.0F)).mul(invMids).add(mids);
				}
				else {
					//slow path: need to compute curve for high and low ends.
					FloatVector lowValue = value.div(mids);
					lowValue = lowValue.mul(coefficients).div(coefficients.sub(1.0F).mul(lowValue).add(1.0F)).mul(mids);
					FloatVector invMids = mids.broadcast(1.0F).sub(mids);
					FloatVector highValue = value.sub(mids).div(invMids);
					highValue = highValue.mul(rcpCoefficients).div(rcpCoefficients.sub(1.0F).mul(highValue).add(1.0F)).mul(invMids).add(mids);
					value = value.blend(lowValue, low).blend(highValue, high);
				}
				if (linear) value = value.sqrt();
				value.intoArray(destination.pixels, index);
			}
		}
		else {
			for (int index = 0, length = source.pixels.length; index < length; index += 4) {
				FloatVector value = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
				value = value.min(1.0F).max(0.0F);
				if (linear) value = value.mul(value);
				value = value.mul(coefficients).div(coefficients.sub(1.0F).mul(value).add(1.0F));
				if (linear) value = value.sqrt();
				value.intoArray(destination.pixels, index);
			}
		}
	}

	@Override
	public Node getRootNode() {
		return this.gridPane;
	}

	public static class CoefficientModel extends SpinnerValueFactory<Float> {

		{
			this.setValue(1.0F);
			this.setConverter(new StringConverter<>() {

				@Override
				public String toString(Float object) {
					return object.toString();
				}

				@Override
				public Float fromString(String string) {
					return Float.valueOf(string.trim());
				}
			});
		}

		@Override
		public void increment(int steps) {
			this.setValue(Float.intBitsToFloat(Float.floatToRawIntBits(this.getValue()) + (steps << 21)));
		}

		@Override
		public void decrement(int steps) {
			this.setValue(Float.intBitsToFloat(Float.floatToRawIntBits(this.getValue()) - (steps << 21)));
		}
	}

	public static class MidModel extends SpinnerValueFactory<Float> {

		{
			this.setValue(0.5F);
			this.setConverter(new StringConverter<>() {

				@Override
				public String toString(Float object) {
					return object.toString();
				}

				@Override
				public Float fromString(String string) {
					if (string.isBlank()) return 0.5F;
					float value = Float.parseFloat(string.trim());
					return Math.clamp(value, 0.0F, 1.0F);
				}
			});
		}

		@Override
		public void increment(int steps) {
			this.setValue(Math.min(this.getValue() + 0.0625F, 1.0F));
		}

		@Override
		public void decrement(int steps) {
			this.setValue(Math.max(this.getValue() - 0.0625F, 0.0F));
		}
	}
}