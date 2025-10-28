package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.Collections;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.MultiLayerDependencies;

public class MovableInputBinding extends InputBinding {

	public CheckBox
		enabled = new CheckBox();
	public Button
		up = new Button("⏶"),
		down = new Button("⏷"),
		delete = new Button("🗑");
	public GridPane
		gridPane = new GridPane();

	@Override
	public JsonMap save() {
		return super.save().with("enabled", this.enabled.isSelected());
	}

	@Override
	public void load(JsonMap map) {
		super.load(map);
		this.enabled.setSelected(map.getBoolean("enabled"));
	}

	public MovableInputBinding(MultiLayerDependencies dependencies, ColorBoxGroup group, Color color) {
		super(dependencies.owner, group, color);
		this.enabled.setSelected(true);
		this.up.getStyleClass().remove("button");
		this.down.getStyleClass().remove("button");
		this.delete.getStyleClass().remove("button");
		this.up.getStyleClass().add("phantom-button");
		this.down.getStyleClass().add("phantom-button");
		this.delete.getStyleClass().add("phantom-button");
		this.up.visibleProperty().bind(this.gridPane.hoverProperty());
		this.down.visibleProperty().bind(this.gridPane.hoverProperty());
		this.delete.visibleProperty().bind(this.gridPane.hoverProperty());
		this.gridPane.add(this.enabled, 0, 0, 1, 2);
		this.gridPane.add(this.thumbnail, 1, 0, 1, 2);
		this.gridPane.add(this.colorBox.getDisplayPane(), 1, 0, 1, 2);
		this.gridPane.add(this.selection, 2, 0, 1, 2);
		this.gridPane.add(this.up, 3, 0, 1, 1);
		this.gridPane.add(this.down, 3, 1, 1, 1);
		this.gridPane.add(this.delete, 4, 0, 1, 2);
		this.gridPane.setHgap(4.0D);
		this.up.setOnAction((ActionEvent _) -> {
			ObservableList<MovableInputBinding> items = dependencies.listView.getItems();
			int index = items.indexOf(this);
			Collections.swap(items, index, index - 1);
		});
		this.down.setOnAction((ActionEvent _) -> {
			ObservableList<MovableInputBinding> items = dependencies.listView.getItems();
			int index = items.indexOf(this);
			Collections.swap(items, index, index + 1);
		});
		this.delete.setOnAction((ActionEvent _) -> {
			dependencies.removeInput(this);
		});
	}

	public Pane getDisplayPane() {
		return this.gridPane;
	}
}