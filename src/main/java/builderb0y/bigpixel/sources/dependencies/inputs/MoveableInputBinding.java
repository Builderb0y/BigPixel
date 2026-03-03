package builderb0y.bigpixel.sources.dependencies.inputs;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.MoveHelper.MoveableComponent;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.MultiLayerDependencies;

public class MoveableInputBinding extends InputBinding implements MoveableComponent {

	public final CheckBox enabled = this.configParameters.addCheckbox("enabled", "Enabled", true);
	public final ImageView dragBarImage = new ImageView(Assets.DRAGBAR);
	public final BorderPane dragBarPane = new BorderPane(this.dragBarImage);
	public final Button delete = new Button("🗑");
	public final HBox leftPane = new HBox(this.enabled, this.selection, this.previewPane);
	public final HBox rightPane = new HBox(this.delete, this.dragBarPane);
	public final BorderPane rootPane = new BorderPane();
	public final SimpleDoubleProperty position = new SimpleDoubleProperty(this, "position");
	public final SlideAnimation slideAnimation = new SlideAnimation(this);

	public MoveableInputBinding(MultiLayerDependencies dependencies, ColorBoxGroup group, Color color) {
		super(dependencies.owner, group, color);
		this.enabled.setSelected(true);
		this.delete.getStyleClass().remove("button");
		this.delete.getStyleClass().add("phantom-button");
		this.delete.setFont(new Font(20.0D));
		this.leftPane.setSpacing(4.0D);
		this.leftPane.setAlignment(Pos.CENTER);
		this.rightPane.visibleProperty().bind(this.rootPane.hoverProperty());
		this.rootPane.setLeft(this.leftPane);
		this.rootPane.setRight(this.rightPane);
		BorderPane.setAlignment(this.leftPane, Pos.CENTER_LEFT);
		BorderPane.setAlignment(this.rightPane, Pos.CENTER_RIGHT);
		this.rootPane.prefWidthProperty().bind(dependencies.inputsView.widthProperty());
		this.delete.setOnAction((ActionEvent _) -> {
			dependencies.removeInput(this, true);
		});
		this.rootPane.layoutYProperty().bind(this.position);
		new MoveMouseHandler<>(this, dependencies).install(this.dragBarPane);
	}

	@Override
	public DoubleProperty positionProperty() {
		return this.position;
	}

	@Override
	public double getHeight() {
		return this.rootPane.getHeight();
	}

	@Override
	public SlideAnimation getSlideAnimation() {
		return this.slideAnimation;
	}

	@Override
	public Pane getRootNode() {
		return this.rootPane;
	}
}