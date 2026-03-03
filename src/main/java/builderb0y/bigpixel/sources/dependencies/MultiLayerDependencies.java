package builderb0y.bigpixel.sources.dependencies;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.util.Duration;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.MoveHelper.MoveableContainer;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.MoveableInputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.util.Util;

public class MultiLayerDependencies extends LayerDependencies implements MoveableContainer<MoveableInputBinding> {

	public final OrganizedSelection.Value<?> owner;
	public final ColorBoxGroup colorBoxGroup;
	public final ObservableList<MoveableInputBinding> inputs = FXCollections.observableArrayList();
	public final Pane inputsView = new Pane();
	public final SimpleDoubleProperty inputsViewHeight = new SimpleDoubleProperty(this, "inputsViewHeight");
	public final Button addButton = new Button("+");
	public final AddRemoveAnimation<MoveableInputBinding> addRemoveAnimation = new AddRemoveAnimation<>(this);
	public final ButtonAnimation buttonAnimation = this.new ButtonAnimation();
	public final AnimatedBinding animated = new AnimatedBinding(this.inputs);
	public final InvalidationListener layerOuter = (Observable _) -> this.layout();

	@Override
	public JsonMap save() {
		JsonArray array = new JsonArray();
		for (InputBinding input : this.inputs) {
			array.add(input.save());
		}
		return new JsonMap().with("main", array);
	}

	@Override
	public void load(JsonMap saveData) {
		ObservableList<MoveableInputBinding> inputs = this.inputs;
		while (!inputs.isEmpty()) {
			this.removeInput(this.inputs.getLast(), false);
		}
		for (JsonValue json : saveData.getArray("main")) {
			this.addInput(json.asMap());
		}
	}

	public MultiLayerDependencies(LayerSource source) {
		this.owner = source;
		super();
		this.colorBoxGroup = new ColorBoxGroup(source.getLayer().graph.openImage, this.inputsView);
		this.inputs.addListener((ListChangeListener.Change<? extends MoveableInputBinding> change) -> {
			source.redrawLater();
		});
		this.addButton.setFont(new Font(20.0D));
		this.addButton.setLayoutX(0.0D);
		this.addButton.prefWidthProperty().bind(this.inputsView.widthProperty());
		this.addButton.setOnAction((ActionEvent _) -> this.addInput(true));
		this.inputsView.getChildren().add(this.addButton);
		this.inputsView.prefHeightProperty().bind(this.inputsViewHeight.add(this.addButton.heightProperty()));

		LayerNode layer = source.sources.layer;
		int index = layer.graph.indexOfPosition(layer.getGridX() - 1, layer.getGridY());
		if (index >= 0) this.addInput(false).selection.setValue(layer.graph.layerList.get(index));
	}

	public MoveableInputBinding addInput(boolean animate) {
		return this.addComponent(() -> new MoveableInputBinding(this, this.colorBoxGroup, CurveHelper.NORMAL), animate);
	}

	public MoveableInputBinding addInput(JsonMap map) {
		MoveableInputBinding binding = this.addInput(false);
		binding.load(map);
		return binding;
	}

	public void removeInput(MoveableInputBinding binding, boolean animate) {
		if (!animate || this.addRemoveAnimation.getStatus() != Status.RUNNING) {
			this.removeComponent(binding, animate);
		}
	}

	@Override
	public double layout() {
		double offset = MoveableContainer.super.layout();
		if (offset != 0.0D && this.buttonAnimation.getStatus() != Status.RUNNING) {
			this.addButton.setLayoutY(offset);
		}
		return offset;
	}

	@Override
	public MoveableInputBinding addComponent(Supplier<MoveableInputBinding> supplier, boolean animate) {
		double height = this.computeSize();
		MoveableInputBinding binding = MoveableContainer.super.addComponent(supplier, animate);
		if (binding != null) {
			binding.getRootNode().heightProperty().addListener(this.layerOuter);
			LayerNode layer = this.owner.getLayer();
			binding.retainAll(layer.graph.getPossibleDependencies(layer));
			binding.curve.selfSourceIsSelected.set(true);
			binding.enabled.selectedProperty().addListener(Util.change(this.owner::redrawLater));
			if (animate) {
				this.buttonAnimation.fromSize = () -> height;
				this.buttonAnimation.toSize = () -> height + binding.getHeight();
				this.buttonAnimation.setRate(1.0D);
				this.buttonAnimation.jumpTo(Duration.ZERO);
				this.buttonAnimation.play();
			}
		}
		return binding;
	}

	@Override
	public void removeComponent(MoveableInputBinding component, boolean animate) {
		double height = this.computeSize();
		MoveableContainer.super.removeComponent(component, animate);
		component.curve.selfSourceIsSelected.set(false);
		if (animate) {
			this.buttonAnimation.fromSize = () -> height - component.getHeight();
			this.buttonAnimation.toSize = () -> height;
			this.buttonAnimation.setRate(-1.0D);
			this.buttonAnimation.jumpTo(this.buttonAnimation.getCycleDuration());
			this.buttonAnimation.play();
		}
	}

	@Override
	public void finishMove(MoveableInputBinding component) {
		MoveableContainer.super.finishMove(component);
		double to = component.getSlideAnimation().to;
		this.inputs.sort(Comparator.comparingDouble((MoveableInputBinding binding) -> binding == component ? to : binding.getPosition()));
	}

	@Override
	public void retainAll(List<LayerNode> layers) {
		for (MoveableInputBinding input : this.inputs) {
			input.retainAll(layers);
		}
	}

	@Override
	public Stream<SamplerProvider> getAll() {
		return this.inputs.stream().map(InputBinding::getCurrent);
	}

	@Override
	public ObservableBooleanValue animatedProperty() {
		return this.animated;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return this.inputs.stream().map((InputBinding binding) -> binding.curve);
	}

	@Override
	public Parent getConfigPane() {
		return this.inputsView;
	}

	@Override
	public Pane getComponentView() {
		return this.inputsView;
	}

	@Override
	public DoubleProperty viewHeightProperty() {
		return this.inputsViewHeight;
	}

	@Override
	public Collection<MoveableInputBinding> getMoveableComponents() {
		return this.inputs;
	}

	@Override
	public AddRemoveAnimation<MoveableInputBinding> getAddRemoveAnimation() {
		return this.addRemoveAnimation;
	}

	public static class AnimatedBinding extends BooleanBinding implements InvalidationListener {

		public ObservableList<MoveableInputBinding> currentBindings;
		public List<MoveableInputBinding> previousBindings = List.of();
		public boolean changed;

		public AnimatedBinding(ObservableList<MoveableInputBinding> currentBindings) {
			this.currentBindings = currentBindings;
			currentBindings.addListener(new WeakInvalidationListener(this));
			this.changed = !currentBindings.isEmpty();
		}

		public void updateBindings() {
			if (this.changed) {
				for (MoveableInputBinding binding : this.previousBindings) {
					if (!this.currentBindings.contains(binding)) {
						this.unbind(binding.animated);
					}
				}
				for (MoveableInputBinding binding : this.currentBindings) {
					if (!this.previousBindings.contains(binding)) {
						this.bind(binding.animated);
					}
				}
				this.previousBindings = List.copyOf(this.currentBindings);
				this.changed = false;
			}
		}

		@Override
		public boolean computeValue() {
			this.updateBindings();
			for (MoveableInputBinding binding : this.currentBindings) {
				if (binding.animated.get()) return true;
			}
			return false;
		}

		@Override
		public void invalidated(Observable currentBindings) {
			this.changed = true;
			this.invalidate();
		}
	}

	public class ButtonAnimation extends Transition {

		public DoubleSupplier fromSize, toSize;

		public ButtonAnimation() {
			this.setInterpolator(Interpolator.LINEAR);
			this.setCycleDuration(AddRemoveAnimation.DEFAULT_DURATION);
			this.setOnFinished((ActionEvent _) -> {
				this.fromSize = this.toSize = null;
			});
		}

		@Override
		protected void interpolate(double frac) {
			double y = frac >= 0.5D ? this.toSize.getAsDouble() : Util.mix(this.fromSize.getAsDouble(), this.toSize.getAsDouble(), Util.smooth(frac * 2.0D));
			MultiLayerDependencies.this.addButton.setLayoutY(y);
			MultiLayerDependencies.this.inputsViewHeight.set(y);
		}
	}
}