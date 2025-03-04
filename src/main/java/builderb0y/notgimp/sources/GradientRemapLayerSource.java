package builderb0y.notgimp.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.CanvasHelper;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;

public class GradientRemapLayerSource extends EffectLayerSource {

	public SimpleObjectProperty<FloatVector>
		fromStartColor = new SimpleObjectProperty<>(this, "fromStartColor"),
		fromEndColor   = new SimpleObjectProperty<>(this, "fromEndColor"),
		toStartColor   = new SimpleObjectProperty<>(this, "toStartColor"),
		toEndColor     = new SimpleObjectProperty<>(this, "toEndColor");
	public CanvasHelper
		fromStart    = new CanvasHelper().checkerboard().popIn().fixedSize( 16.0D, 16.0D),
		fromGradient = new CanvasHelper().checkerboard().popIn().fixedSize(129.0D, 16.0D),
		fromEnd      = new CanvasHelper().checkerboard().popIn().fixedSize( 16.0D, 16.0D),
		toStart      = new CanvasHelper().checkerboard().popIn().fixedSize( 16.0D, 16.0D),
		toGradient   = new CanvasHelper().checkerboard().popIn().fixedSize(129.0D, 16.0D),
		toEnd        = new CanvasHelper().checkerboard().popIn().fixedSize( 16.0D, 16.0D);
	public CheckBox
		preserveAlpha = new CheckBox("Preserve Alpha");
	public GridPane
		rootPane = new GridPane();

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "gradient_remap")
			.with("from_start", jsonify(this.fromStartColor.get()))
			.with("from_end",   jsonify(this.  fromEndColor.get()))
			.with("to_start",   jsonify(this.  toStartColor.get()))
			.with("to_end",     jsonify(this.    toEndColor.get()))
			.with("preserve_alpha", this.preserveAlpha.isSelected())
		);
	}

	public static JsonArray jsonify(FloatVector vector) {
		return (
			new JsonArray()
			.with(vector.lane(0))
			.with(vector.lane(1))
			.with(vector.lane(2))
			.with(vector.lane(3))
		);
	}

	@Override
	public void load(JsonMap map) {
		unjsonify(map.getArray("from_start"), this.fromStartColor);
		unjsonify(map.getArray("from_end"  ), this.  fromEndColor);
		unjsonify(map.getArray("to_start"  ), this.  toStartColor);
		unjsonify(map.getArray("to_end"    ), this.    toEndColor);
		this.preserveAlpha.setSelected(map.getBoolean("preserve_alpha"));
	}

	public static void unjsonify(JsonArray array, SimpleObjectProperty<FloatVector> vector) {
		vector.set(
			FloatVector.fromArray(
				FloatVector.SPECIES_128,
				new float[] {
					array.getFloat(0),
					array.getFloat(1),
					array.getFloat(2),
					array.getFloat(3)
				},
				0
			)
		);
	}

	public GradientRemapLayerSource(LayerSources sources) {
		super(sources, "Gradient Remap");
		float[] array = new float[4];
		array[HDRImage.ALPHA_OFFSET] = 1.0F;
		FloatVector black = FloatVector.fromArray(FloatVector.SPECIES_128, array, 0);
		array[HDRImage.  RED_OFFSET] =
		array[HDRImage.GREEN_OFFSET] =
		array[HDRImage. BLUE_OFFSET] =
		1.0F;
		FloatVector white = FloatVector.fromArray(FloatVector.SPECIES_128, array, 0);
		this.fromStartColor.set(black);
		this.fromEndColor.set(white);
		this.toStartColor.set(black);
		this.toEndColor.set(white);
	}

	@Override
	public void doRedraw() throws RedrawException {

	}

	@Override
	public Node getRootNode() {
		return this.rootPane;
	}
}