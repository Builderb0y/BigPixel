package builderb0y.bigpixel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.Animation.Status;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import builderb0y.bigpixel.EditableLabel.NameAdjuster;
import builderb0y.bigpixel.MoveHelper.NamedMoveableComponent;
import builderb0y.bigpixel.MoveHelper.NamedMoveableContainer;
import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.sources.DerivedLayerSource;
import builderb0y.bigpixel.util.Notifier;
import builderb0y.bigpixel.util.Util;

public class ParameterSetMiddle implements NamedMoveableContainer<ParameterSetBottom>, NamedMoveableComponent {

	public final ParameterSetTop top;
	public final EditableLabel name;
	public final TitledPane titledPane;
	public final BorderPane titledPaneTopArea;
	public final HBox titledPaneTopRightArea;
	public final Button addButton;
	public final ImageView dragBarImage;
	public final BorderPane dragBarPane;
	public final Map<String, ParameterSetBottom> bottoms;
	public final Pane bottomsView;
	public final ObjectProperty<ParameterSetBottom> selectedBottom;
	public final DoubleProperty position;
	public final SlideAnimation slideAnimation;
	public final AddRemoveAnimation<ParameterSetBottom> addRemoveAnimation;
	public final MiddleDisposer onDisposed;

	public JsonValue save() {
		JsonArray variations = new JsonArray();
		for (ParameterSetBottom bottom : this.getSortedMoveableComponents()) {
			variations.add(bottom.save());
		}
		return (
			new JsonMap()
			.with("name", this.getName())
			.with("selected", this.selectedBottom.get().getName())
			.with("variations", variations)
		);
	}

	public ParameterSetMiddle(ParameterSetTop top, JsonMap saveData) {
		this(top, saveData.getString("name"));
		for (JsonValue value : saveData.getArray("variations")) {
			this.add(value);
		}
		String selectedName = saveData.getString("selected");
		ParameterSetBottom selected = this.bottoms.get(selectedName);
		if (selected == null) throw new SaveException("Unknown variation: " + selectedName);
		this.selectedBottom.set(selected);
	}

	public ParameterSetMiddle(ParameterSetTop top, String name) {
		this.top = top;
		this.name = new EditableLabel(name);
		this.name.nameAdjuster = NameAdjuster.eager(top.middles.keySet());
		this.name.textProperty().addListener(Util.change(this::rename));
		this.bottoms = new HashMap<>();
		this.bottomsView = new Pane();
		this.titledPane = new TitledPane(null, this.bottomsView);
		this.titledPane.setAnimated(false);
		this.addButton = new Button("+");
		this.addButton.setFont(new Font(20.0D));
		this.addButton.getStyleClass().remove("button");
		this.addButton.getStyleClass().add("phantom-button");
		this.addButton.setOnAction((ActionEvent _) -> this.add());
		this.dragBarImage = new ImageView(Assets.DRAGBAR);
		this.dragBarPane = new BorderPane(this.dragBarImage);
		this.titledPaneTopRightArea = new HBox(this.addButton, this.dragBarPane);
		this.titledPaneTopArea = new BorderPane();
		this.titledPaneTopArea.centerProperty().bind(this.name.getRootPane());
		this.titledPaneTopArea.setRight(this.titledPaneTopRightArea);
		this.titledPane.setGraphic(this.titledPaneTopArea);
		this.selectedBottom = new SimpleObjectProperty<>(this, "selectedBottom");
		this.addRemoveAnimation = new AddRemoveAnimation<>(this);
		this.selectedBottom.addListener(Util.change((ParameterSetBottom oldBottom, ParameterSetBottom newBottom) -> {
			if (oldBottom != null) {
				oldBottom.getRootNode().pseudoClassStateChanged(LayerNode.SELECTED, false);
			}
			if (newBottom != null) {
				newBottom.getRootNode().pseudoClassStateChanged(LayerNode.SELECTED, true);
			}
		}));
		this.position = new SimpleDoubleProperty(this, "position");
		this.titledPane.setLayoutX(0.0D);
		this.titledPane.layoutYProperty().bind(this.position);
		this.titledPane.prefWidthProperty().bind(top.middlePane.widthProperty());
		this.slideAnimation = new SlideAnimation(this);
		this.onDisposed = this.new MiddleDisposer();
		new MoveMouseHandler<>(this, top).install(this.dragBarPane);
	}

	public void init() {
		this.selectedBottom.set(this.add("Default", false));
	}

	public void rename(String oldName, String newName) {
		this.top.rename(this, oldName, newName);
	}

	public void select(ParameterSetBottom bottom) {
		DerivedLayerSource.loading = true;
		try {
			this.selectedBottom.set(bottom);
		}
		finally {
			DerivedLayerSource.loading = false;
		}
	}

	@Override
	public String getName() {
		return this.name.getText();
	}

	@Override
	public DoubleProperty viewHeightProperty() {
		return this.bottomsView.prefHeightProperty();
	}

	@Override
	public DoubleProperty positionProperty() {
		return this.position;
	}

	@Override
	public double getHeight() {
		return this.titledPane.getHeight();
	}

	@Override
	public SlideAnimation getSlideAnimation() {
		return this.slideAnimation;
	}

	@Override
	public AddRemoveAnimation<ParameterSetBottom> getAddRemoveAnimation() {
		return this.addRemoveAnimation;
	}

	@Override
	public Region getRootNode() {
		return this.titledPane;
	}

	@Override
	public Map<String, ParameterSetBottom> getComponentsByName() {
		return this.bottoms;
	}

	@Override
	public Pane getComponentView() {
		return this.bottomsView;
	}

	public ParameterSetBottom add() {
		ParameterSetBottom bottom = this.add(Util.adjustName("Variation", this.bottoms.keySet()), true);
		if (bottom != null) bottom.name.edit();
		return bottom;
	}

	public ParameterSetBottom add(JsonValue saveData) {
		return this.addComponent(() -> new ParameterSetBottom(this, saveData), false);
	}

	public ParameterSetBottom add(String name, boolean animate) {
		ParameterSetBottom bottom = this.addComponent(() -> new ParameterSetBottom(this, name), animate);
		if (bottom != null) this.selectedBottom.set(bottom);
		return bottom;
	}

	public void remove(ParameterSetBottom bottom, boolean animate) {
		if (!animate || this.addRemoveAnimation.getStatus() != Status.RUNNING) {
			if (this.bottoms.size() > 1) {
				bottom.onDisposed.invalidate();
				this.removeComponent(bottom, animate);
				if (this.selectedBottom.get() == bottom) {
					this.selectedBottom.set(this.bottoms.values().stream().min(Comparator.comparingDouble(ParameterSetBottom::getPosition)).orElseThrow());
				}
			}
			else if (this.top.middles.size() > 1) {
				bottom.onDisposed.invalidate();
				this.top.remove(this, animate);
			}
		}
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public class MiddleDisposer extends Notifier {

		public ParameterSetMiddle middle() {
			return ParameterSetMiddle.this;
		}
	}
}