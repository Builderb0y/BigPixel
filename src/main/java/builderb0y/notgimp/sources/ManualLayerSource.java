package builderb0y.notgimp.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.tools.*;

public class ManualLayerSource extends LayerSource {

	public HDRImage toollessImage;
	public Tool<?>
		freehandTool    = new    FreehandTool(this),
		lineTool        = new        LineTool(this),
		rectangleTool   = new   RectangleTool(this),
		moveTool        = new        MoveTool(this),
		colorPickerTool = new ColorPickerTool(this);
	public GridPane
		toolSelection = new GridPane();
	public Button
		freehandButton = this.button(this.freehandTool),
		lineButton     = this.button(this.lineTool),
		rectButton     = this.button(this.rectangleTool),
		moveButton     = this.button(this.moveTool);
	public BorderPane
		toolConfig = new BorderPane();
	public SimpleObjectProperty<@Nullable Tool<?>>
		currentTool = new SimpleObjectProperty<>();

	public ManualLayerSource(LayerSources sources) {
		super(sources);
		this.toolSelection.add(this.rectButton, 0, 0);
		this.toolSelection.add(this.lineButton, 1, 0);
		this.toolSelection.add(this.freehandButton, 2, 0);
		this.toolSelection.add(this.moveButton, 0, 1);
		this.toolSelection.add(this.toolConfig, 0, 2, 3, 1);
		this.toolConfig.centerProperty().bind(this.currentTool.map(Tool::getConfiguration));
	}

	public void init() {
		this.toollessImage = new HDRImage(this.sources.layer.image);
	}

	public void beginUsingTool() {
		System.arraycopy(this.sources.layer.image.pixels, 0, this.toollessImage.pixels, 0, this.toollessImage.pixels.length);
	}

	public void beforeToolChanged() {
		System.arraycopy(this.toollessImage.pixels, 0, this.sources.layer.image.pixels, 0, this.toollessImage.pixels.length);
	}

	public void finishUsingTool() {
		System.arraycopy(this.sources.layer.image.pixels, 0, this.toollessImage.pixels, 0, this.toollessImage.pixels.length);
	}

	public void cancelToolAction() {
		System.arraycopy(this.toollessImage.pixels, 0, this.sources.layer.image.pixels, 0, this.toollessImage.pixels.length);
		this.sources.layer.image.markDirty();
	}

	public void copyFrom(ManualLayerSource that) {
		System.arraycopy(that.toollessImage.pixels, 0, this.toollessImage.pixels, 0, that.toollessImage.pixels.length);
	}

	public Button button(Tool<?> tool) {
		Button button = new Button();
		button.setGraphic(new ImageView(tool.type.icon()));
		button.setOnAction((ActionEvent event) -> this.currentTool.set(tool));
		return button;
	}

	@Override
	public Node getRootNode() {
		return this.toolSelection;
	}

	@Override
	public void redraw() {
		HDRImage layerImage = this.sources.layer.image;
		System.arraycopy(this.toollessImage.pixels, 0, layerImage.pixels, 0, layerImage.pixels.length);
		layerImage.markDirty();
	}

	@Override
	public Cursor getCursor() {
		Tool<?> tool = this.currentTool.get();
		return tool != null ? tool.type.cursor() : null;
	}
}