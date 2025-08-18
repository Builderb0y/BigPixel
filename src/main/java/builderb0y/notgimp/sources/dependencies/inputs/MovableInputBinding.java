package builderb0y.notgimp.sources.dependencies.inputs;

import java.util.Collections;
import java.util.function.UnaryOperator;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.sources.ColorBoxGroup;
import builderb0y.notgimp.sources.dependencies.MultiLayerDependencies;

public class MovableInputBinding extends InputBinding {

	public Button
		up = new Button("⏶"),
		down = new Button("⏷"),
		delete = new Button("🗑");
	public GridPane
		gridPane = new GridPane();

	public MovableInputBinding(MultiLayerDependencies dependencies, ColorBoxGroup group, UnaryOperator<FloatVector> mapper) {
		super(dependencies.source, group, mapper);
		this.up.getStyleClass().remove("button");
		this.down.getStyleClass().remove("button");
		this.delete.getStyleClass().remove("button");
		this.up.getStyleClass().add("phantom-button");
		this.down.getStyleClass().add("phantom-button");
		this.delete.getStyleClass().add("phantom-button");
		this.up.visibleProperty().bind(this.gridPane.hoverProperty());
		this.down.visibleProperty().bind(this.gridPane.hoverProperty());
		this.delete.visibleProperty().bind(this.gridPane.hoverProperty());
		this.gridPane.add(this.thumbnail, 0, 0, 1, 2);
		this.gridPane.add(this.colorBox.getDisplayPane(), 0, 0, 1, 2);
		this.gridPane.add(this.selection, 1, 0, 1, 2);
		this.gridPane.add(this.up, 2, 0, 1, 1);
		this.gridPane.add(this.down, 2, 1, 1, 1);
		this.gridPane.add(this.delete, 3, 0, 1, 2);
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

	public MovableInputBinding(MultiLayerDependencies dependencies, ColorBoxGroup group) {
		this(dependencies, group, null);
	}

	public Pane getDisplayPane() {
		return this.gridPane;
	}
}