package builderb0y.bigpixel.sources.dependencies.inputs;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.sources.ColorBoxGroup;

public class UnmovableInputBinding extends InputBinding {

	public Label displayName;

	public UnmovableInputBinding(OrganizedSelection.Value<?> owner, String displayName, ColorBoxGroup group, Color color) {
		super(owner, group, color);
		this.displayName = new Label(displayName);
	}

	public void setDisabled(boolean disabled) {
		this.displayName.setDisable(disabled);
		this.selection.setDisable(disabled);
		this.colorBox.disabled.set(disabled);
	}

	public void bindDisabled(ObservableValue<Boolean> disabled) {
		this.displayName.disableProperty().bind(disabled);
		this.selection.disableProperty().bind(disabled);
		this.colorBox.disabled.bind(disabled);
	}

	public void addRow(GridPane gridPane, int rowIndex) {
		gridPane.add(this.displayName, 0, rowIndex);
		gridPane.add(this.selection, 1, rowIndex);
		gridPane.add(this.thumbnail, 2, rowIndex);
		gridPane.add(this.colorBox.getDisplayPane(), 2, rowIndex);
	}
}