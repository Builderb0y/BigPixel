package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.function.UnaryOperator;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;

public class UnmovableInputBinding extends InputBinding {

	public Label displayName;

	public UnmovableInputBinding(LayerSource source, String displayName, ColorBoxGroup group, UnaryOperator<FloatVector> mapper) {
		super(source, group, mapper);
		this.displayName = new Label(displayName);
	}

	public UnmovableInputBinding(LayerSource source, String displayName, ColorBoxGroup group) {
		this(source, displayName, group, null);
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