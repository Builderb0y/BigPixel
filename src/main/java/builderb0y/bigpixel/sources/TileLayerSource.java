package builderb0y.bigpixel.sources;

import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class TileLayerSource extends LayerSource {

	public MainDependencies
		dependencies = new MainDependencies(this);
	public Spinner<Integer>
		offsetX = this.parameters.addIntSpinner("offsetX", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80),
		offsetY = this.parameters.addIntSpinner("offsetY", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80);

	public TileLayerSource(LayerSources sources) {
		super(Type.TILE, sources);
		this.dependencies.addExtraNodeRow(new HBox(new Label("Offset X: "), this.offsetX));
		this.dependencies.addExtraNodeRow(new HBox(new Label("Offset Y: "), this.offsetY));
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage destination = this.sources.layer.image;
		switch (this.dependencies.main.getCurrent()) {
			case UniformLayerSourceInput uniform -> {
				FloatVector color = uniform.getColor();
				for (int index = 0; index < destination.pixels.length; index += 4) {
					color.intoArray(destination.pixels, index);
				}
			}
			case VaryingLayerSourceInput varying -> {
				int offsetX = this.offsetX.getValue();
				int offsetY = this.offsetY.getValue();
				HDRImage sourceImage = varying.getBackingLayer().image;
				for (int y = 0; y < destination.height; y++) {
					int modY = Math.floorMod(y - offsetY, sourceImage.height);
					for (int x = 0; x < destination.width; x++) {
						int modX = Math.floorMod(x - offsetX, sourceImage.width);
						destination.setColor(x, y, varying.getColor(modX, modY));
					}
				}
			}
		}
	}
}