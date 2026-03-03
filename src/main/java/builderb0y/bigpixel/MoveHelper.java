package builderb0y.bigpixel;

import java.util.*;
import java.util.function.Supplier;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import builderb0y.bigpixel.MoveHelper.MoveableComponent.SlideAnimation;
import builderb0y.bigpixel.util.MapValues;
import builderb0y.bigpixel.util.Util;

public class MoveHelper {

	public static interface MoveableComponent extends NodeHolder {

		public abstract DoubleProperty positionProperty();

		public default double getPosition() {
			return this.positionProperty().get();
		}

		public default void setPosition(double position) {
			this.positionProperty().set(position);
		}

		public abstract double getHeight();

		public abstract SlideAnimation getSlideAnimation();

		public static class MoveMouseHandler<M extends MoveableComponent> implements EventHandler<MouseEvent> {

			public final M component;
			public final MoveableContainer<M> container;
			public double offsetY;

			public MoveMouseHandler(M component, MoveableContainer<M> container) {
				this.component = component;
				this.container = container;
			}

			public void install(Node node) {
				node.setCursor(Cursor.MOVE);
				node.setOnMouseDragged(this);
				node.setOnMousePressed(this);
				node.setOnMouseReleased(this);
			}

			@Override
			public void handle(MouseEvent event) {
				if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
					double maxY = this.container.computeSize() - this.component.getHeight();
					this.component.setPosition(Math.clamp(event.getScreenY() - this.offsetY, 0.0D, maxY));
					this.container.continueMove(this.component);
				}
				else if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
					this.offsetY = event.getScreenY() - this.component.getPosition();
					this.container.beginMove(this.component);
				}
				else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
					this.container.finishMove(this.component);
				}
				event.consume();
			}
		}

		public static class SlideAnimation extends Transition {

			public static final Duration DEFAULT_DURATION = Duration.seconds(0.25D);

			public final MoveableComponent component;
			public double from, to;

			public SlideAnimation(MoveableComponent component) {
				this.component = component;
				this.setCycleDuration(DEFAULT_DURATION);
				this.setInterpolator(Util.SMOOTH_INTERPOLATOR);
			}

			public void init(
				double from,
				double to,
				boolean forward,
				Duration jump,
				Duration delay
			) {
				this.from = from;
				this.to = to;
				this.setDelay(delay);
				this.setRate(forward ? 1.0D : -1.0D);
				this.jumpTo(jump);
			}

			@Override
			public void interpolate(double frac) {
				this.component.setPosition(Util.mix(this.from, this.to, frac));
			}
		}
	}

	public static interface NamedMoveableComponent extends MoveableComponent {

		public abstract String getName();
	}

	public static interface MoveableContainer<M extends MoveableComponent> {

		public abstract Pane getComponentView();

		public abstract DoubleProperty viewHeightProperty();

		public abstract Collection<M> getMoveableComponents();

		public default List<M> getSortedMoveableComponents() {
			List<M> list = new ArrayList<>(this.getMoveableComponents());
			list.sort(Comparator.comparingDouble(MoveableComponent::getPosition));
			return list;
		}

		public default M getFirstMoveableComponent() {
			Iterator<M> iterator = this.getMoveableComponents().iterator();
			M first = iterator.next();
			while (iterator.hasNext()) {
				M next = iterator.next();
				if (next.getPosition() < first.getPosition()) {
					first = next;
				}
			}
			return first;
		}

		public default double layout() {
			for (MoveableComponent component : this.getMoveableComponents()) {
				if (component.getHeight() == 0.0D) return 0.0D;
			}
			double offset = 0.0D;
			for (MoveableComponent component : this.getSortedMoveableComponents()) {
				component.getSlideAnimation().stop();
				component.setPosition(offset);
				offset += component.getHeight();
			}
			this.viewHeightProperty().set(offset);
			return offset;
		}

		public abstract AddRemoveAnimation<M> getAddRemoveAnimation();

		public default void beginMove(M component) {
			component.getRootNode().toFront();
			for (M other : this.getMoveableComponents()) {
				if (other == component) continue;
				SlideAnimation animation = other.getSlideAnimation();
				if (other.getPosition() < component.getPosition()) {
					animation.init(
						other.getPosition(),
						other.getPosition() + component.getHeight(),
						false,
						Duration.ZERO,
						Duration.ZERO
					);
				}
				else {
					animation.init(
						other.getPosition() - component.getHeight(),
						other.getPosition(),
						true,
						animation.getCycleDuration(),
						Duration.ZERO
					);
				}
			}
		}

		public default void continueMove(M component) {
			double componentSnap = component.getPosition();
			double centerY = component.getPosition() + component.getHeight() * 0.5D;
			for (M other : this.getMoveableComponents()) {
				if (other == component) continue;
				SlideAnimation animation = other.getSlideAnimation();
				if (centerY < (animation.from + animation.to + other.getHeight()) * 0.5D) {
					animation.setRate(1.0D);
					if (animation.getCurrentTime().lessThan(animation.getCycleDuration())) {
						animation.play();
					}
					componentSnap = Math.min(componentSnap, animation.from);
				}
				else {
					animation.setRate(-1.0D);
					if (animation.getCurrentTime().greaterThan(Duration.ZERO)) {
						animation.play();
					}
					componentSnap = Math.max(componentSnap, animation.from + other.getHeight());
				}
			}
			component.getSlideAnimation().to = componentSnap;
		}

		public default void finishMove(M component) {
			SlideAnimation animation = component.getSlideAnimation();
			animation.from = component.getPosition();
			animation.setDelay(Duration.ZERO);
			animation.setRate(1.0D);
			animation.setOnFinished((ActionEvent _) -> {
				double accumulator = 0.0D;
				for (M other : this.getSortedMoveableComponents()) {
					other.getSlideAnimation().stop();
					other.setPosition(accumulator);
					accumulator += other.getHeight();
				}
				animation.setOnFinished(null);
			});
			animation.playFromStart();
		}

		public default double computeSize() {
			double size = 0.0D;
			for (M other : this.getMoveableComponents()) {
				size += Math.max(other.getHeight(), 1.0D);
			}
			return size;
		}

		public default M addComponent(Supplier<M> supplier, boolean animate) {
			AddRemoveAnimation<M> animation = this.getAddRemoveAnimation();
			if (!animate || animation.getStatus() != Status.RUNNING) {
				M component = supplier.get();
				double size = this.computeSize();
				component.setPosition(size);
				if (!this.getMoveableComponents().add(component)) {
					throw new IllegalArgumentException("Adding " + component + " to " + this + " multiple times!");
				}
				this.getComponentView().getChildren().add(component.getRootNode());
				if (animate) {
					animation.fadingBottom = component;
					animation.fromSize = size;
					animation.toSize = size + component.getHeight();
					animation.setRate(1.0D);
					animation.playFromStart();
				}
				else {
					this.viewHeightProperty().set(size + component.getHeight());
				}
				return component;
			}
			else {
				return null;
			}
		}

		public default void removeComponent(M component, boolean animate) {
			double oldSize = this.computeSize();
			if (!this.getMoveableComponents().remove(component)) {
				throw new IllegalArgumentException(component + " not contained within " + this);
			}
			if (animate) {
				AddRemoveAnimation<M> animation = this.getAddRemoveAnimation();
				animation.fadingBottom = component;
				animation.toSize = oldSize;
				animation.fromSize = oldSize - component.getHeight();
				animation.setRate(-1.0D);
				animation.playFrom(animation.getCycleDuration());

				double accumulator = 0.0D;
				for (M other : this.getSortedMoveableComponents()) {
					other.getSlideAnimation().init(
						other.getPosition(),
						accumulator,
						true,
						Duration.ZERO,
						SlideAnimation.DEFAULT_DURATION
					);
					other.getSlideAnimation().play();
					accumulator += other.getHeight();
				}
			}
			else {
				this.getComponentView().getChildren().remove(component.getRootNode());

				double accumulator = 0.0D;
				for (M other : this.getSortedMoveableComponents()) {
					other.setPosition(accumulator);
					accumulator += other.getHeight();
				}

				this.viewHeightProperty().set(accumulator);
			}
		}

		public static class AddRemoveAnimation<M extends MoveableComponent> extends Transition {

			public static final Duration DEFAULT_DURATION = SlideAnimation.DEFAULT_DURATION.multiply(2.0D);

			public final MoveableContainer<M> container;
			public M fadingBottom;
			public double fromSize, toSize;

			public AddRemoveAnimation(MoveableContainer<M> container) {
				this.container = container;
				this.setCycleDuration(DEFAULT_DURATION);
				this.setInterpolator(Interpolator.LINEAR);
				this.setOnFinished((ActionEvent _) -> {
					if (this.getRate() < 0.0D) {
						this.container.getComponentView().getChildren().remove(this.fadingBottom.getRootNode());
					}
				});
			}

			@Override
			public void interpolate(double frac) {
				if (frac >= 0.5D) {
					this.fadingBottom.getRootNode().setOpacity(frac * 2.0D - 1.0D);
					this.container.viewHeightProperty().set(this.toSize);
				}
				else {
					this.fadingBottom.getRootNode().setOpacity(0.0D);
					this.container.viewHeightProperty().set(Util.mix(this.fromSize, this.toSize, Util.smooth(frac * 2.0D)));
				}
			}
		}
	}

	public static interface NamedMoveableContainer<M extends NamedMoveableComponent> extends MoveableContainer<M> {

		public abstract Map<String, M> getComponentsByName();

		public default void rename(M component, String oldName, String newName) {
			if (!this.getComponentsByName().remove(oldName, component)) {
				throw new IllegalStateException("Component map out of sync!");
			}
			if (this.getComponentsByName().putIfAbsent(newName, component) != null) {
				this.getComponentsByName().put(oldName, component);
				throw new IllegalStateException("Invalid component name!");
			}
		}

		@Override
		public default Collection<M> getMoveableComponents() {
			return new MapValues<>(this.getComponentsByName()) {

				@Override
				public String keyOf(M value) {
					return value.getName();
				}
			};
		}
	}
}