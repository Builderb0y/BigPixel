package builderb0y.bigpixel.util;

import java.util.concurrent.CopyOnWriteArrayList;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

public class Notifier implements Observable {

	public final CopyOnWriteArrayList<InvalidationListener> listeners = new CopyOnWriteArrayList<>();

	@Override
	public void addListener(InvalidationListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		this.listeners.remove(listener);
	}

	public void invalidate() {
		if (!this.listeners.isEmpty()) {
			this.listeners.forEach((InvalidationListener listener) -> listener.invalidated(this));
		}
	}
}