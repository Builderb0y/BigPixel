package builderb0y.bigpixel.sources;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.util.TriangleHelper;
import builderb0y.bigpixel.util.Util;

public class ColorBox implements UniformSamplerProvider {

	public static final FloatVector GRAYSCALE_DOT = Util.rgba(0.25F, 0.5F, 0.25F, 0.0F);

	public SimpleObjectProperty<FloatVector> color;
	public ObservableValue<FloatVector> displayColor;
	public SimpleBooleanProperty disabled;
	public TriangleHelper box;

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
		this.box = new TriangleHelper().fixedSize(24.0D, 24.0D);
		this.box.color.bind(this.displayColor);
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

	public Pane getDisplayPane() {
		return this.box.getRootPane();
	}

	@Override
	public String toString() {
		return "Constant color"; //for InputBinding.
	}
}