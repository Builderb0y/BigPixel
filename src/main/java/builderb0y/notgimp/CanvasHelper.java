package builderb0y.notgimp;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;

public class CanvasHelper {

	public BorderPane
		outerPane = new BorderPane(),
		innerPane = new BorderPane();
	public Canvas
		canvas = new Canvas();

	public CanvasHelper() {
		this.innerPane.setCenter(this.canvas);
		this.outerPane.setCenter(this.innerPane);
	}

	public Node getRootPane() {
		return this.outerPane;
	}

	public CanvasHelper checkerboard() {
		this.innerPane.getStyleClass().add("checkerboards");
		return this;
	}

	public CanvasHelper popOut() {
		this.outerPane.getStyleClass().add("popout-borders");
		return this;
	}

	public CanvasHelper popIn() {
		this.outerPane.getStyleClass().add("popin-borders");
		return this;
	}

	public CanvasHelper pop(ObservableBooleanValue inward) {
		ObservableValue<String> stringProperty = inward.map((Boolean currentlyInward) -> currentlyInward ? "popin-borders" : "popout-borders");
		this.outerPane.getStyleClass().add(stringProperty.getValue());
		stringProperty.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			ObservableList<String> classes = this.outerPane.getStyleClass();
			classes.set(classes.indexOf(oldValue), newValue);
		});
		return this;
	}

	public CanvasHelper fixedSize(double width, double height) {
		this.canvas.setWidth(width);
		this.canvas.setHeight(height);
		this.innerPane.setMinWidth(width);
		this.innerPane.setMaxWidth(width);
		this.innerPane.setMinHeight(height);
		this.innerPane.setMaxHeight(height);
		return this;
	}

	public CanvasHelper resizeable(Consumer<Canvas> redrawer) {
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.canvas.widthProperty().bind(this.innerPane.widthProperty());
		this.canvas.heightProperty().bind(this.innerPane.heightProperty());
		ChangeListener<Number> listener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> redrawer.accept(this.canvas);
		this.canvas.widthProperty().addListener(listener);
		this.canvas.heightProperty().addListener(listener);
		return this;
	}
}