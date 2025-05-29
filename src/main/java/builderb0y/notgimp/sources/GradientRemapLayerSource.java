package builderb0y.notgimp.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.*;
import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.scripting.types.UtilityOperations;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class GradientRemapLayerSource extends SingleInputEffectLayerSource {

	public SimpleObjectProperty<ColorBox>
		activeBox = new SimpleObjectProperty<>(this, "activeBox");
	public GradientRow
		from = new GradientRow(this),
		to   = new GradientRow(this);
	public CheckBox
		perChannel    = new CheckBox("Per Channel"),
		preserveAlpha = new CheckBox("Preserve Alpha");
	public HBox
		toggles = new HBox(this.perChannel, this.preserveAlpha);
	public GridPane
		gridPane = new GridPane();

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "gradient_remap")
			.with("from_start", jsonify(this.from.start.color.get()))
			.with("from_end",   jsonify(this.from.end  .color.get()))
			.with("to_start",   jsonify(this.to  .start.color.get()))
			.with("to_end",     jsonify(this.to  .end  .color.get()))
			.with("per_channel",    this.perChannel   .isSelected())
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
		unjsonify(map.getArray("from_start"), this.from.start.color);
		unjsonify(map.getArray("from_end"  ), this.from.end  .color);
		unjsonify(map.getArray("to_start"  ), this.to  .start.color);
		unjsonify(map.getArray("to_end"    ), this.to  .end  .color);
		this.perChannel.setSelected(map.getBoolean("per_channel"));
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
		this.from.start.color.set(black);
		this.from.end  .color.set(white);
		this.to  .start.color.set(black);
		this.to  .end  .color.set(white);
	}

	@Override
	public void init(boolean fromSave) {
		super.init(fromSave);
		ColorHelper colorHelper = this.sources.layer.openImage.mainWindow.colorPicker.currentColor;
		this.activeBox.addListener(Util.change((ColorBox oldValue, ColorBox newValue) -> {
			if (oldValue != null) {
				oldValue.color.unbind();
			}
			if (newValue != null) {
				FloatVector current = newValue.color.get();
				colorHelper.setRGBA(
					current.lane(HDRImage.  RED_OFFSET),
					current.lane(HDRImage.GREEN_OFFSET),
					current.lane(HDRImage. BLUE_OFFSET),
					current.lane(HDRImage.ALPHA_OFFSET)
				);
				colorHelper.markDirty();
				newValue.color.bind(colorHelper.rgba);
			}
		}));
		this.from.init();
		this.to.init();
		this.from.addTo(this.gridPane, 0, "From: ");
		this.to.addTo(this.gridPane, 1, "To: ");
		this.gridPane.add(this.toggles, 0, 2, 4, 1);
		this.rootNode.setCenter(this.gridPane);
		ChangeListener<FloatVector> listener = Util.change(this.sources.layer::requestRedraw);
		this.from.start.color.addListener(listener);
		this.from.end  .color.addListener(listener);
		this.to  .start.color.addListener(listener);
		this.to  .end  .color.addListener(listener);
		ChangeListener<Boolean> theSameListener = Util.change(this.sources.layer::requestRedraw);
		this.perChannel.selectedProperty().addListener(theSameListener);
		this.preserveAlpha.selectedProperty().addListener(theSameListener);
	}

	public void copyFrom(GradientRemapLayerSource that) {
		this.from.copyFrom(that.from);
		this.to.copyFrom(that.to);
		this.perChannel.setSelected(that.perChannel.isSelected());
		this.preserveAlpha.setSelected(that.preserveAlpha.isSelected());
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		HDRImage destination = this.sources.layer.image;
		FloatVector
			fromStart = this.from.start.color.get(),
			fromEnd   = this.from.end  .color.get(),
			toStart   = this.to  .start.color.get(),
			toEnd     = this.to  .end  .color.get();
		if (this.perChannel.isSelected()) {
			VectorMask<Float> usedChannels = fromStart.compare(VectorOperators.NE, fromEnd);
			if (this.preserveAlpha.isSelected()) {
				//why is there no withLane() for masks?
				boolean[] array = usedChannels.toArray();
				array[HDRImage.ALPHA_OFFSET] = false;
				usedChannels = VectorMask.fromArray(FloatVector.SPECIES_128, array, 0);
			}
			for (int index = 0; index < source.pixels.length; index += 4) {
				FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
				FloatVector fraction = VectorOperations.unmix_float4_float4_float4(fromStart, fromEnd, color);
				FloatVector remixed = VectorOperations.mix_float4_float4_float4(toStart, toEnd, fraction);
				color.blend(remixed, usedChannels).intoArray(destination.pixels, index);
			}
		}
		else {
			if (this.preserveAlpha.isSelected()) {
				for (int index = 0; index < source.pixels.length; index += 4) {
					FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
					float fraction = UtilityOperations.projectLineFrac_float3_float3_float3(fromStart, fromEnd, color);
					color = color.blend(VectorOperations.mix_float3_float3_float(toStart, toEnd, fraction), RGB_MASK);
					color.intoArray(destination.pixels, index);
				}
			}
			else {
				for (int index = 0; index < source.pixels.length; index += 4) {
					FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
					float fraction = UtilityOperations.projectLineFrac_float4_float4_float4(fromStart, fromEnd, color);
					color = VectorOperations.mix_float4_float4_float(toStart, toEnd, fraction);
					color.intoArray(destination.pixels, index);
				}
			}
		}
	}

	public static class ColorBox {

		public SimpleObjectProperty<FloatVector> color;
		public CanvasHelper canvas;

		public ColorBox(GradientRemapLayerSource source) {
			this.color = new SimpleObjectProperty<>(this, "color");
			this.canvas = new CanvasHelper().checkerboard().fixedSize(16.0D, 16.0D).popOut();
			this.canvas.canvas.setOnMouseClicked((MouseEvent _) -> {
				source.activeBox.set(source.activeBox.getValue() == this ? null : this);
			});
			//this is supposed to be made easy with CanvasHelper.pop(),
			//but nooooooooooooooooooooooooooooooooooooooooooooooooooo,
			//javaFX decided to make that a pain in the ass instead.
			source.activeBox.addListener(Util.change((ColorBox box) -> this.canvas.setPop(box == this)));
		}

		public void init() {
			this.color.addListener(Util.change(this::redraw));
			this.redraw();
		}

		public void redraw() {
			FloatVector color = this.color.get();
			float alpha = color.lane(HDRImage.ALPHA_OFFSET);
			FloatVector preMultiplied = color.mul(alpha);
			PixelWriter writer = this.canvas.canvas.getGraphicsContext2D().getPixelWriter();
			byte[] pixels = new byte[16 * 4];
			for (int x = 0; x < 16; x++) {
				int baseIndex = x << 2;
				pixels[baseIndex    ] = Util.clampB(preMultiplied.lane(HDRImage. BLUE_OFFSET));
				pixels[baseIndex | 1] = Util.clampB(preMultiplied.lane(HDRImage.GREEN_OFFSET));
				pixels[baseIndex | 2] = Util.clampB(preMultiplied.lane(HDRImage.  RED_OFFSET));
				pixels[baseIndex | 3] = Util.clampB(alpha);
			}
			writer.setPixels(0, 0, 16, 16, PixelFormat.getByteBgraPreInstance(), pixels, 0, 0);
		}
	}

	public static class GradientRow extends Gradient {

		public ColorBox start, end;

		public GradientRow(GradientRemapLayerSource source) {
			this.checkerboard().popOut().fixedSize(129.0D, 16.0D);
			this.start = new ColorBox(source);
			this.end = new ColorBox(source);
		}

		public void init() {
			this.start.init();
			this.end.init();
			ChangeListener<FloatVector> listener = Util.change(this::redraw);
			this.start.color.addListener(listener);
			this.end.color.addListener(listener);
			this.redraw();
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			return VectorOperations.mix_float4_float4_float(this.start.color.get(), this.end.color.get(), fraction);
		}

		public void addTo(GridPane gridPane, int y, String name) {
			gridPane.add(new Label(name), 0, y);
			gridPane.add(this.start.canvas.getRootPane(), 1, y);
			gridPane.add(this.getRootPane(), 2, y);
			gridPane.add(this.end.canvas.getRootPane(), 3, y);
		}

		public void copyFrom(GradientRow that) {
			this.start.color.set(that.start.color.get());
			this.end.color.set(that.end.color.get());
		}
	}
}