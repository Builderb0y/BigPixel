package builderb0y.notgimp.sources;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;

public class CliffCurveLayerSource extends SingleInputEffectLayerSource {

	public CheckBox
		splitRgb = this.addCheckbox("split_rgb", null, false),
		dual     = this.addCheckbox("dual",      null, false),
		linear   = this.addCheckbox("linear",    null, true);
	public Spinner<Float>
		rgbCoefficient   = this.addFloatSpinner("rgb_coefficient",   new CoefficientModel(), 96.0D),
		redCoefficient   = this.addFloatSpinner("red_coefficient",   new CoefficientModel(), 96.0D),
		greenCoefficient = this.addFloatSpinner("green_coefficient", new CoefficientModel(), 96.0D),
		blueCoefficient  = this.addFloatSpinner("blue_coefficient",  new CoefficientModel(), 96.0D),
		alphaCoefficient = this.addFloatSpinner("alpha_coefficient", new CoefficientModel(), 96.0D),
		rgbMid           = this.addFloatSpinner("rgb_mid",           new MidModel(), 96.0D),
		redMid           = this.addFloatSpinner("red_mid",           new MidModel(), 96.0D),
		greenMid         = this.addFloatSpinner("green_mid",         new MidModel(), 96.0D),
		blueMid          = this.addFloatSpinner("blue_mid",          new MidModel(), 96.0D),
		alphaMid         = this.addFloatSpinner("alpha_mid",         new MidModel(), 96.0D);
	public GridPane
		gridPane = new GridPane();

	public CliffCurveLayerSource(LayerSources sources) {
		super(sources, "cliff", "Cliff Curve");
		this.splitRgb.selectedProperty().addListener(Util.change(this::layout));
		BooleanBinding disableMid = this.dual.selectedProperty().not();
		this.rgbMid  .disableProperty().bind(disableMid);
		this.redMid  .disableProperty().bind(disableMid);
		this.greenMid.disableProperty().bind(disableMid);
		this.blueMid .disableProperty().bind(disableMid);
		this.alphaMid.disableProperty().bind(disableMid);
		this.layout();
		this.rootNode.setCenter(this.gridPane);
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
		HDRImage source = this.getSingleInput(true).image;
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