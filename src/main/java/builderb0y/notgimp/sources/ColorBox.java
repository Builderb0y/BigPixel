package builderb0y.notgimp.sources;

import java.util.function.UnaryOperator;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.RectangleHelper;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;

public class ColorBox implements UniformLayerSourceInput {

	public static final FloatVector GRAYSCALE_DOT = Util.rgba(0.25F, 0.5F, 0.25F, 0.0F);

	public SimpleObjectProperty<FloatVector> color;
	public ObservableValue<FloatVector> displayColor;
	public SimpleBooleanProperty disabled;
	public RectangleHelper box;

	public ColorBox(FloatVector color) {
		this.color = new SimpleObjectProperty<>(this, "color", color);
		this.disabled = new SimpleBooleanProperty(this, "disabled", false);
		this.displayColor = new ObjectBinding<>() {

			{
				this.bind(ColorBox.this.color, ColorBox.this.disabled);
			}

			@Override
			public FloatVector computeValue() {
				FloatVector color = ColorBox.this.color.get();
				if (ColorBox.this.disabled.get()) {
					color = color.broadcast(color.mul(GRAYSCALE_DOT).reduceLanes(VectorOperators.ADD)).withLane(HDRImage.ALPHA_OFFSET, color.lane(HDRImage.ALPHA_OFFSET));
				}
				return color;
			}
		};
		this.box = new RectangleHelper().fixedSize(16.0D, 16.0D);
		this.box.display.fillProperty().bind(
			this.displayColor.map(
				(FloatVector vector) -> new Color(
					vector.lane(HDRImage.  RED_OFFSET),
					vector.lane(HDRImage.GREEN_OFFSET),
					vector.lane(HDRImage. BLUE_OFFSET),
					vector.lane(HDRImage.ALPHA_OFFSET)
				)
			)
		);
	}

	public ColorBox selectWhen(Property<ColorBox> property) {
		//this is supposed to be made easy with CanvasHelper.pop(),
		//but nooooooooooooooooooooooooooooooooooooooooooooooooooo,
		//javaFX decided to make that a pain in the ass instead.
		property.addListener(Util.change((ColorBox box) -> this.box.setPop(box == this)));
		this.box.setPop(property.getValue() == this);
		return this;
	}

	@Override
	public FloatVector getColor() {
		return this.color.get();
	}

	@Override
	public UniformLayerSourceInput mapColors(UnaryOperator<FloatVector> operator) {
		ObservableValue<FloatVector> mappedColor = this.color.map(operator);
		return new UniformLayerSourceInput() {

			@Override
			public FloatVector getColor() {
				return mappedColor.getValue();
			}

			@Override
			public UniformLayerSourceInput mapColors(UnaryOperator<FloatVector> operator2) {
				return ColorBox.this.mapColors((FloatVector color) -> operator2.apply(operator.apply(color)));
			}
		};
	}

	public Pane getDisplayPane() {
		return this.box.getRootPane();
	}

	@Override
	public String toString() {
		return "Constant color"; //for InputBinding.
	}
}