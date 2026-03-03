package builderb0y.bigpixel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.util.Duration;

import builderb0y.bigpixel.MoveHelper.MoveableContainer;
import builderb0y.bigpixel.MoveHelper.NamedMoveableContainer;
import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.util.Util;

public class ParameterSetTop implements NamedMoveableContainer<ParameterSetMiddle> {

	public final Map<String, ParameterSetMiddle> middles = new HashMap<>();
	public final Pane middlePane = new Pane();
	public final SimpleDoubleProperty middlePaneHeight = new SimpleDoubleProperty(this, "middlePaneHeight");
	public final ScrollPane rootPane = new ScrollPane(this.middlePane);
	public final AddRemoveAnimation<ParameterSetMiddle> addRemoveAnimation = new AddRemoveAnimation<>(this);
	public final Button addButton = new Button("+");
	public final ButtonAnimation buttonAnimation = this.new ButtonAnimation();
	public final InvalidationListener layerOuter = (Observable _) -> this.layout();

	public JsonMap save() {
		JsonArray middles = new JsonArray(this.middles.size());
		for (ParameterSetMiddle middle : this.getSortedMoveableComponents()) {
			middles.add(middle.save());
		}
		return new JsonMap().with("groups", middles);
	}

	public void load(JsonMap map) {
		while (!this.getComponentsByName().isEmpty()) {
			this.remove(this.getComponentsByName().values().iterator().next(), false);
		}
		for (JsonValue middleData : map.getArray("groups")) {
			this.add(middleData.asMap());
		}
	}

	public ParameterSetTop() {
		this.rootPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		this.rootPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		this.rootPane.setFitToWidth(true);
		this.rootPane.setFitToHeight(false);
		this.addButton.setFont(new Font(20.0D));
		this.addButton.setLayoutX(0.0D);
		this.addButton.prefWidthProperty().bind(this.middlePane.widthProperty());
		this.addButton.setOnAction((ActionEvent _) -> this.add());
		this.middlePane.getChildren().add(this.addButton);
		this.middlePane.prefHeightProperty().bind(this.middlePaneHeight.add(this.addButton.heightProperty()));
		this.add("Global", false).init();
	}

	@Override
	public DoubleProperty viewHeightProperty() {
		return this.middlePaneHeight;
	}

	@Override
	public Map<String, ParameterSetMiddle> getComponentsByName() {
		return this.middles;
	}

	@Override
	public Pane getComponentView() {
		return this.middlePane;
	}

	@Override
	public AddRemoveAnimation<ParameterSetMiddle> getAddRemoveAnimation() {
		return this.addRemoveAnimation;
	}

	@Override
	public double layout() {
		double offset = NamedMoveableContainer.super.layout();
		if (offset != 0.0D && this.buttonAnimation.getStatus() != Status.RUNNING) {
			this.addButton.setLayoutY(offset);
		}
		return offset;
	}

	public ParameterSetMiddle add() {
		ParameterSetMiddle middle = this.add(Util.adjustName("New Parameter Set", this.middles.keySet()), true);
		if (middle != null) {
			middle.init();
			middle.name.edit();
			this.buttonAnimation.intendedFocus = middle.name.editor;
		}
		return middle;
	}

	public ParameterSetMiddle add(String name, boolean animate) {
		double height = this.computeSize();
		ParameterSetMiddle middle = this.addComponent(() -> new ParameterSetMiddle(this, name), animate);
		if (middle != null && animate) {
			this.buttonAnimation.fromSize = () -> height;
			this.buttonAnimation.toSize = () -> height + middle.getHeight();
			this.buttonAnimation.setRate(1.0D);
			this.buttonAnimation.jumpTo(Duration.ZERO);
			this.buttonAnimation.play();
		}
		return middle;
	}

	public ParameterSetMiddle add(JsonMap saveData) {
		return this.addComponent(() -> new ParameterSetMiddle(this, saveData), false);
	}

	@Override
	public ParameterSetMiddle addComponent(Supplier<ParameterSetMiddle> supplier, boolean animate) {
		ParameterSetMiddle middle = NamedMoveableContainer.super.addComponent(supplier, animate);
		if (middle != null) {
			middle.getRootNode().heightProperty().addListener(this.layerOuter);
		}
		return middle;
	}

	public void remove(ParameterSetMiddle middle, boolean fromUser) {
		if (!fromUser || (this.middles.size() > 1 && this.addRemoveAnimation.getStatus() != Status.RUNNING)) {
			middle.onDisposed.invalidate();
			double height = this.computeSize();
			this.removeComponent(middle, fromUser);
			if (fromUser) {
				this.buttonAnimation.fromSize = () -> height - middle.getHeight();
				this.buttonAnimation.toSize = () -> height;
				this.buttonAnimation.setRate(-1.0D);
				this.buttonAnimation.jumpTo(this.buttonAnimation.getCycleDuration());
				this.buttonAnimation.play();
			}
		}
	}

	public class ButtonAnimation extends Transition {

		public DoubleSupplier fromSize, toSize;
		public Node intendedFocus;

		public ButtonAnimation() {
			this.setInterpolator(Interpolator.LINEAR);
			this.setCycleDuration(AddRemoveAnimation.DEFAULT_DURATION);
			this.setOnFinished((ActionEvent _) -> {
				this.fromSize = this.toSize = null;
				this.intendedFocus = null;
			});
		}

		@Override
		public void interpolate(double frac) {
			double y = frac >= 0.5D ? this.toSize.getAsDouble() : Util.mix(this.fromSize.getAsDouble(), this.toSize.getAsDouble(), Util.smooth(frac * 2.0D));
			ParameterSetTop.this.addButton.setLayoutY(y);
			ParameterSetTop.this.middlePaneHeight.set(y);
			//for whatever reason, it doesn't stay focused.
			//so, request focus multiple times.
			//I hate this.
			if (this.intendedFocus != null) this.intendedFocus.requestFocus();
		}
	}
}