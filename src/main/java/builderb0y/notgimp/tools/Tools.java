package builderb0y.notgimp.tools;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import builderb0y.notgimp.LayerSources;

public class Tools {

	public LayerSources
		sources;
	public Tool<?>
		freehandTool    = new    FreehandTool(),
		lineTool        = new        LineTool(),
		rectangleTool   = new   RectangleTool(),
		moveTool        = new        MoveTool(),
		colorPickerTool = new ColorPickerTool();

	public Tool<?>    freehandTool() { return this.freehandTool; }
	public Tool<?>        lineTool() { return this.lineTool; }
	public Tool<?>   rectangleTool() { return this.rectangleTool; }
	public Tool<?>        moveTool() { return this.moveTool; }
	public Tool<?> colorPickerTool() { return this.colorPickerTool; }

	public GridPane
		toolSelection = new GridPane();
	public Button
		freehandButton = this.button(this.freehandTool),
		lineButton     = this.button(this.lineTool),
		rectButton     = this.button(this.rectangleTool),
		moveButton     = this.button(this.moveTool);

	public Tools(LayerSources sources) {
		this.sources = sources;
		this.toolSelection.add(this.freehandButton, 0, 0);
		this.toolSelection.add(this.lineButton, 1, 0);
		this.toolSelection.add(this.rectButton, 2, 0);
		this.toolSelection.add(this.moveButton, 0, 1);
	}

	public Button button(Tool<?> tool) {
		Button button = new Button();
		button.setGraphic(new ImageView(tool.type.icon()));
		button.setOnAction((ActionEvent event) -> this.sources.layer.openImage.mainWindow.currentTool.set(tool.type));
		return button;
	}
}