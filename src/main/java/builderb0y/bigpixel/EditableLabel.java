package builderb0y.bigpixel;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import builderb0y.bigpixel.util.Util;

public class EditableLabel {

	public final Label label = new Label();
	public final TextField editor = new TextField();
	public final SimpleObjectProperty<Control> showingControl = new SimpleObjectProperty<>(this, "showingControl", this.label);
	public NameAdjuster nameAdjuster = NameAdjuster.NO_ADJUSTMENTS;
	public boolean editable, editing;

	public EditableLabel() {
		this.editable = true;
		this.editor.setAlignment(Pos.CENTER);
		this.label.setOnMouseClicked((MouseEvent event) -> {
			if (event.getClickCount() == 2 && this.edit()) {
				event.consume();
			}
		});
		this.editor.setOnAction((ActionEvent event) -> {
			if (this.editing) {
				this.editing = false;
				this.label.setText(this.nameAdjuster.adjust(this.editor.getText()));
				this.showingControl.set(this.label);
				event.consume();
			}
		});
		this.editor.focusedProperty().addListener(Util.change((Boolean focused) -> {
			if (!focused && this.editing) {
				this.editing = false;
				this.showingControl.set(this.label);
			}
		}));
		this.editor.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.ESCAPE && this.editing) {
				this.editing = false;
				this.showingControl.set(this.label);
				event.consume();
			}
		});
	}

	public EditableLabel(String initialText) {
		this();
		this.label.setText(initialText);
	}

	public boolean edit() {
		if (this.editable && !this.editing) {
			this.editing = true;
			this.editor.setText(this.label.getText());
			this.editor.selectAll();
			this.showingControl.set(this.editor);
			this.editor.requestFocus();
			return true;
		}
		else {
			return false;
		}
	}

	public String getText() {
		return this.label.getText();
	}

	public StringProperty textProperty() {
		return this.label.textProperty();
	}

	public SimpleObjectProperty<? extends Node> getRootPane() {
		return this.showingControl;
	}

	public static interface NameAdjuster {

		public static final NameAdjuster NO_ADJUSTMENTS = (String name) -> name;

		public abstract String adjust(String name);

		public static NameAdjuster eager(Set<String> names) {
			Objects.requireNonNull(names, "names");
			return (String name) -> Util.adjustName(name, names);
		}

		public static NameAdjuster lazy(Supplier<Set<String>> names) {
			Objects.requireNonNull(names, "names");
			return (String name) -> Util.adjustName(name, names.get());
		}
	}
}