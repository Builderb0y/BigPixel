package builderb0y.bigpixel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import builderb0y.bigpixel.EditableLabel.NameAdjuster;
import builderb0y.bigpixel.MoveHelper.NamedMoveableComponent;
import builderb0y.bigpixel.json.JsonString;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.util.Notifier;
import builderb0y.bigpixel.util.Util;

public class ParameterSetBottom implements NamedMoveableComponent {

	public static final double HEIGHT = 32.0D;

	public final ParameterSetMiddle middle;
	public final BottomDisposer onDisposed;
	public final BorderPane rootPane;
	public final ImageView dragBarImage;
	public final EditableLabel name;
	public final Button deleteButton;
	public final DoubleProperty position;
	public final SlideAnimation slideAnimation;

	public JsonValue save() {
		return new JsonString(this.getName());
	}

	public ParameterSetBottom(ParameterSetMiddle middle, JsonValue value) {
		this(middle, value.asString());
	}

	public ParameterSetBottom(ParameterSetMiddle middle, String name) {
		this.middle = middle;
		this.onDisposed = this.new BottomDisposer();
		this.name = new EditableLabel(name);
		this.name.nameAdjuster = NameAdjuster.eager(middle.bottoms.keySet());
		this.name.textProperty().addListener(Util.change(this::rename));
		this.rootPane = new BorderPane();
		this.rootPane.centerProperty().bind(this.name.getRootPane());
		this.dragBarImage = new ImageView(Assets.DRAGBAR);
		this.dragBarImage.visibleProperty().bind(this.rootPane.hoverProperty());
		this.deleteButton = new Button("🗑");
		this.deleteButton.setFont(new Font(20.0D));
		this.deleteButton.getStyleClass().remove("button");
		this.deleteButton.getStyleClass().add("phantom-button");
		this.deleteButton.visibleProperty().bind(this.rootPane.hoverProperty());
		this.deleteButton.setOnAction((ActionEvent _) -> {
			this.middle.remove(this, true);
		});
		this.rootPane.setRight(new HBox(this.deleteButton, this.dragBarImage));
		this.rootPane.getStyleClass().add("selectable");
		this.position = new SimpleDoubleProperty(this, "position");
		this.slideAnimation = new SlideAnimation(this);
		this.rootPane.setLayoutX(0.0D);
		this.rootPane.layoutYProperty().bind(this.position);
		this.rootPane.prefWidthProperty().bind(middle.bottomsView.widthProperty());
		this.rootPane.setPrefHeight(HEIGHT);
		this.rootPane.setOnMouseClicked((MouseEvent _) -> this.middle.select(this));
		new MoveMouseHandler<>(this, middle).install(this.dragBarImage);
	}

	public void rename(String oldName, String newName) {
		this.middle.rename(this, oldName, newName);
	}

	@Override
	public String getName() {
		return this.name.getText();
	}

	@Override
	public Node getRootNode() {
		return this.rootPane;
	}

	@Override
	public DoubleProperty positionProperty() {
		return this.position;
	}

	@Override
	public double getHeight() {
		return HEIGHT;
	}

	@Override
	public SlideAnimation getSlideAnimation() {
		return this.slideAnimation;
	}

	@Override
	public String toString() {
		return this.name.getText();
	}

	public class BottomDisposer extends Notifier {

		public ParameterSetBottom bottom() {
			return ParameterSetBottom.this;
		}
	}
}