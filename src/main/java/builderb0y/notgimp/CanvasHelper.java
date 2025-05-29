package builderb0y.notgimp;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

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

	public Pane getRootPane() {
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

	public CanvasHelper pop(ObservableValue<Boolean> inward) {
		ObservableValue<String> stringProperty = inward.map(CanvasHelper::popCsv);
		this.outerPane.getStyleClass().add(stringProperty.getValue());
		stringProperty.addListener(Util.change((String oldValue, String newValue) -> {
			ObservableList<String> classes = this.outerPane.getStyleClass();
			classes.set(classes.indexOf(oldValue), newValue);
		}));
		return this;
	}

	public void setPop(boolean inward) {
		ObservableList<String> classes = this.outerPane.getStyleClass();
		if (!classes.contains(popCsv(inward))) {
			classes.remove(popCsv(!inward));
			classes.add(popCsv(inward));
		}
	}

	public static String popCsv(boolean inward) {
		return inward ? "popin-borders" : "popout-borders";
	}

	public CanvasHelper fixedSize(double width, double height) {
		this.canvas.setWidth(width);
		this.canvas.setHeight(height);
		this.innerPane.setMinWidth(width);
		this.innerPane.setMaxWidth(width);
		this.innerPane.setMinHeight(height);
		this.innerPane.setMaxHeight(height);
		this.outerPane.setMaxWidth(Region.USE_PREF_SIZE);
		this.outerPane.setMaxHeight(Region.USE_PREF_SIZE);
		return this;
	}

	public CanvasHelper resizeable(Consumer<Canvas> redrawer) {
		this.innerPane.setMinWidth(0.0D);
		this.innerPane.setMinHeight(0.0D);
		this.canvas.widthProperty().bind(this.innerPane.widthProperty());
		this.canvas.heightProperty().bind(this.innerPane.heightProperty());
		ChangeListener<Number> listener = Util.change(() -> redrawer.accept(this.canvas));
		this.canvas.widthProperty().addListener(listener);
		this.canvas.heightProperty().addListener(listener);
		return this;
	}
}