package builderb0y.notgimp.sources;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.RectangleHelper;
import builderb0y.notgimp.Util;

public class ColorMatrixLayerSource extends SingleInputEffectLayerSource {

	public ColorBoxGroup
		activeBox;
	public RectangleHelper
		fromRed   = new RectangleHelper().fixedSize(16.0D, 16.0D).popOut().paint(Color.RED),
		fromGreen = new RectangleHelper().fixedSize(16.0D, 16.0D).popOut().paint(Color.LIME),
		fromBlue  = new RectangleHelper().fixedSize(16.0D, 16.0D).popOut().paint(Color.BLUE);
	public ColorBox
		toRed   = this.addColorBox("to_red",   Util.rgba(1.0F, 0.0F, 0.0F, 1.0F)),
		toGreen = this.addColorBox("to_green", Util.rgba(0.0F, 1.0F, 0.0F, 1.0F)),
		toBlue  = this.addColorBox("to_blue",  Util.rgba(0.0F, 0.0F, 1.0F, 1.0F));
	public GridPane
		gridPane = new GridPane();

	public ColorMatrixLayerSource(LayerSources sources) {
		super(sources, "color_matrix", "Color Matrix");
	}

	@Override
	public void init(boolean fromSave) {
		super.init(fromSave);
		ColorHelper colorHelper = this.sources.layer.openImage.mainWindow.colorPicker.currentColor;
		this.activeBox = new ColorBoxGroup(colorHelper, this.rootNode, this.toRed, this.toGreen, this.toBlue);
		this.gridPane.addRow(0, this.fromRed  .getRootPane(), new Label(" -> "), this.toRed  .box.getRootPane());
		this.gridPane.addRow(1, this.fromGreen.getRootPane(), new Label(" -> "), this.toGreen.box.getRootPane());
		this.gridPane.addRow(2, this.fromBlue .getRootPane(), new Label(" -> "), this.toBlue .box.getRootPane());
		this.rootNode.setCenter(this.gridPane);
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
}