package builderb0y.bigpixel.util;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class BorderHelper<T extends Node> {

	public BorderPane
		outerPane = new BorderPane(),
		innerPane = new BorderPane();
	public T
		display;

	public BorderHelper(T display) {
		this.display = display;
		this.innerPane.setCenter(display);
		this.outerPane.setCenter(this.innerPane);
	}

	public Pane getRootPane() {
		return this.outerPane;
	}

	public BorderHelper<T> checkerboard() {
		this.innerPane.getStyleClass().add("checkerboards");
		return this;
	}

	public BorderHelper<T> popOut() {
		this.outerPane.getStyleClass().add("popout-borders");
		return this;
	}

	public BorderHelper<T> popIn() {
		this.outerPane.getStyleClass().add("popin-borders");
		return this;
	}

	public BorderHelper<T> pop(ObservableValue<Boolean> inward) {
		ObservableValue<String> stringProperty = inward.map(BorderHelper::popCsv);
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

	public BorderHelper<T> fixedSize(double width, double height) {
		this.innerPane.setMinWidth(width);
		this.innerPane.setMaxWidth(width);
		this.innerPane.setMinHeight(height);
		this.innerPane.setMaxHeight(height);
		this.outerPane.setMaxWidth(Region.USE_PREF_SIZE);
		this.outerPane.setMaxHeight(Region.USE_PREF_SIZE);
		return this;
	}
}