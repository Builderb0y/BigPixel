package builderb0y.notgimp.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.CanvasHelper;
import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;

public class ColorMatrixLayerSource extends SingleInputEffectLayerSource {

	public SimpleObjectProperty<MutableColorBox>
		activeBox = new SimpleObjectProperty<>();
	public ColorBox
		fromRed   = new ColorBox(rgba(1.0F, 0.0F, 0.0F, 1.0F)),
		fromGreen = new ColorBox(rgba(0.0F, 1.0F, 0.0F, 1.0F)),
		fromBlue  = new ColorBox(rgba(0.0F, 0.0F, 1.0F, 1.0F));
	public MutableColorBox
		toRed   = new MutableColorBox(this, rgba(1.0F, 0.0F, 0.0F, 1.0F)),
		toGreen = new MutableColorBox(this, rgba(0.0F, 1.0F, 0.0F, 1.0F)),
		toBlue  = new MutableColorBox(this, rgba(0.0F, 0.0F, 1.0F, 1.0F));
	public GridPane
		gridPane = new GridPane();

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "color_matrix")
			.with("to_red", GradientRemapLayerSource.jsonify(this.toRed.color.get()))
			.with("to_green", GradientRemapLayerSource.jsonify(this.toGreen.color.get()))
			.with("to_blue", GradientRemapLayerSource.jsonify(this.toBlue.color.get()))
		);
	}

	@Override
	public void load(JsonMap map) {
		GradientRemapLayerSource.unjsonify(map.getArray("to_red"),   this.toRed  .color);
		GradientRemapLayerSource.unjsonify(map.getArray("to_green"), this.toGreen.color);
		GradientRemapLayerSource.unjsonify(map.getArray("to_blue"),  this.toBlue .color);
	}

	public ColorMatrixLayerSource(LayerSources sources) {
		super(sources, "Color Matrix");
		this.gridPane.addRow(0, this.fromRed  .canvas.getRootPane(), new Label(" -> "), this.toRed  .canvas.getRootPane());
		this.gridPane.addRow(1, this.fromGreen.canvas.getRootPane(), new Label(" -> "), this.toGreen.canvas.getRootPane());
		this.gridPane.addRow(2, this.fromBlue .canvas.getRootPane(), new Label(" -> "), this.toBlue .canvas.getRootPane());
		this.rootNode.setCenter(this.gridPane);
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
		this.fromRed.init();
		this.fromGreen.init();
		this.fromBlue.init();
		this.toRed.init();
		this.toGreen.init();
		this.toBlue.init();
		ChangeListener<FloatVector> listener = Util.change(this.sources.layer::requestRedraw);
		this.toRed.color.addListener(listener);
		this.toGreen.color.addListener(listener);
		this.toBlue.color.addListener(listener);
	}

	public void copyFrom(ColorMatrixLayerSource that) {
		this.toRed.color.set(that.toRed.color.get());
		this.toGreen.color.set(that.toGreen.color.get());
		this.toBlue.color.set(that.toBlue.color.get());
	}

	public static FloatVector rgba(float red, float green, float blue, float alpha) {
		float[] array = new float[4];
		array[HDRImage.  RED_OFFSET] = red;
		array[HDRImage.GREEN_OFFSET] = green;
		array[HDRImage. BLUE_OFFSET] = blue;
		array[HDRImage.ALPHA_OFFSET] = alpha;
		return FloatVector.fromArray(FloatVector.SPECIES_128, array, 0);
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		HDRImage destination = this.sources.layer.image;
		FloatVector
			red   = this.toRed  .color.get(),
			green = this.toGreen.color.get(),
			blue  = this.toBlue .color.get();
		for (int index = 0; index < source.pixels.length; index += 4) {
			FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
			red.mul(color.lane(HDRImage.RED_OFFSET))
			.add(green.mul(color.lane(HDRImage.GREEN_OFFSET)))
			.add(blue.mul(color.lane(HDRImage.BLUE_OFFSET)))
			.withLane(HDRImage.ALPHA_OFFSET, color.lane(HDRImage.ALPHA_OFFSET))
			.intoArray(destination.pixels, index);
		}
	}

	public static class ColorBox {

		public SimpleObjectProperty<FloatVector> color;
		public CanvasHelper canvas;

		public ColorBox(FloatVector color) {
			this.color = new SimpleObjectProperty<>(this, "color", color);
			this.canvas = new CanvasHelper().fixedSize(16.0D, 16.0D).popOut();
		}

		public void init() {
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

	public static class MutableColorBox extends ColorBox {

		public MutableColorBox(ColorMatrixLayerSource source, FloatVector color) {
			super(color);
			this.canvas.checkerboard();
			this.canvas.canvas.setOnMouseClicked((MouseEvent _) -> {
				source.activeBox.set(source.activeBox.getValue() == this ? null : this);
			});
			//this is supposed to be made easy with CanvasHelper.pop(),
			//but nooooooooooooooooooooooooooooooooooooooooooooooooooo,
			//javaFX decided to make that a pain in the ass instead.
			source.activeBox.addListener(Util.change((ColorBox box) -> this.canvas.setPop(box == this)));
		}

		@Override
		public void init() {
			super.init();
			this.color.addListener(Util.change(this::redraw));
		}
	}
}