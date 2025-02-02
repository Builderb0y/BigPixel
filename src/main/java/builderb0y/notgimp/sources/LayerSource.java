package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Set;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;

public abstract class LayerSource {

	public LayerSources sources;
	public ActiveStateProperty state = new ActiveStateProperty();

	public LayerSource(LayerSources sources) {
		this.sources = sources;
		this.state.addListener(Util.change((ActiveState oldState, ActiveState newState) -> {
			if (newState == ActiveState.ACTIVE && oldState != ActiveState.INACTIVE) {
				this.onInvalidated();
			}
		}));
	}

	public boolean isSelected() {
		return this.sources.getCurrentSource() == this;
	}

	public void invalidate() {
		switch (this.state.get()) {
			case ACTIVE -> this.state.fireValueChangedEvent();
			case INACTIVE -> this.state.setValue(ActiveState.INACTIVE_CHANGED);
			case INACTIVE_CHANGED -> {}
		}
	}

	public abstract void onInvalidated();

	public abstract Collection<Layer> getDependencies();

	public abstract boolean isAnimated();

	public abstract Node getRootNode();

	public abstract void redraw(boolean fromAnimation);

	public abstract @Nullable Cursor getCursor();

	public static enum ActiveState {
		ACTIVE,
		INACTIVE,
		INACTIVE_CHANGED;
	}

	public static class ActiveStateProperty extends SimpleObjectProperty<ActiveState> {

		public ActiveStateProperty() {
			super(ActiveState.INACTIVE);
		}

		@Override
		public void fireValueChangedEvent() {
			super.fireValueChangedEvent();
		}
	}
}