package builderb0y.notgimp;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.tools.Tool.ToolType;
import builderb0y.notgimp.tools.Tools;

public class LayerSources {

	public Layer
		layer;
	public TabPane
		tabPane = new TabPane();
	public Tab
		derivedTab = new Tab("Derived"),
		proceduralTab = new Tab("Procedural"),
		manualTab = new Tab("Manual");
	public TextArea
		derivedTextArea = new TextArea(),
		proceduralTextArea = new TextArea();
	public VBox
		manualArea = new VBox();
	public Tools
		tools = new Tools(this);
	public BorderPane
		toolConfig = new BorderPane();

	public LayerSources(Layer layer) {
		this.layer = layer;
		this.toolConfig.centerProperty().bind(
			layer.openImage.mainWindow.currentTool.map(
				(@Nullable ToolType type) -> type != null ? type.getTool(this).getConfiguration() : null
			)
		);
		this.manualArea.getChildren().addAll(this.tools.toolSelection, this.toolConfig);
		this.derivedTab.setContent(this.derivedTextArea);
		this.proceduralTab.setContent(this.proceduralTextArea);
		this.manualTab.setContent(this.manualArea);
		this.tabPane.getTabs().addAll(this.derivedTab, this.proceduralTab, this.manualTab);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		this.derivedTextArea.setText(from.derivedTextArea.getText());
		this.proceduralTextArea.setText(from.proceduralTextArea.getText());
	}

	public void init() {
		Font monospace = Font.font("monospace");
		this.derivedTextArea.setFont(monospace);
		this.derivedTextArea.textProperty().addListener(
			(ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
				float brightness;
				try {
					brightness = Float.parseFloat(newValue);
				}
				catch (NumberFormatException exception) {
					return;
				}
				int width = this.layer.image.width;
				int height = this.layer.image.height;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						this.layer.image.setGrayscale(x, y, brightness, 1.0F);
					}
				}
				this.layer.image.markDirty();
			}
		);
		this.proceduralTextArea.setFont(monospace);
		this.tabPane.getSelectionModel().selectedItemProperty().addListener(
			(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) -> {
				if (newTab != this.manualTab) {
					this.layer.openImage.mainWindow.currentTool.set(null);
				}
			}
		);
	}
}