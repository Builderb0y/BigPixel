package builderb0y.bigpixel;

import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import builderb0y.bigpixel.ParameterSetBottom.BottomDisposer;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.util.Util;

public class ParameterMultiStorage<T> {

	public final Property<T> property;
	public final ParameterSetTop top;
	public final SimpleObjectProperty<ParameterSetMiddle> middle;
	public final Map<ParameterSetBottom, SimpleObjectProperty<T>> storage;
	public final InvalidationListener bottomCleaner, middleCleaner;
	public final ObservableValue<SimpleObjectProperty<T>> currentStorage;
	public boolean currentChanging;

	public JsonMap save(JsonConverter<T> converter) {
		ParameterSetMiddle middle = this.middle.get();
		JsonMap variations = new JsonMap();
		for (Map.Entry<ParameterSetBottom, SimpleObjectProperty<T>> entry : this.storage.entrySet()) {
			if (entry.getKey().middle == middle) {
				variations.put(entry.getKey().getName(), converter.toJson(entry.getValue().get()));
			}
			else {
				System.err.println("Variation " + entry.getKey() + " was retained after group was changed from " + entry.getKey().middle + " to " + middle);
			}
		}
		return new JsonMap().with("group", middle.getName()).with("variations", variations);
	}

	public void load(JsonMap map, JsonConverter<T> converter) {
		String group = map.getString("group");
		ParameterSetMiddle middle = this.top.middles.get(group);
		if (middle == null) {
			throw new SaveException("Unknown parameter group: " + group);
		}
		this.middle.set(middle); //will implicitly clear storage map.
		ParameterSetBottom selected = middle.selectedBottom.get();
		JsonMap variations = map.getMap("variations");
		for (Map.Entry<String, JsonValue> entry : variations.entrySet()) {
			ParameterSetBottom bottom = middle.bottoms.get(entry.getKey());
			if (bottom == null) {
				throw new SaveException("Missing parameter variation: " + entry.getKey());
			}
			T value = converter.fromJson(entry.getValue());
			if (bottom == selected) {
				//may or may not implicitly modify storage;
				//if the old value equals the new value,
				//then this will not modify the storage.
				this.property.setValue(value);
			}
			//need to ensure that the storage is ALWAYS kept in sync with the property.
			this.getStorage(bottom).set(value);
		}
	}

	public ParameterMultiStorage(Property<T> property, ParameterSetTop top) {
		this.property = property;
		this.top = top;
		this.middle = new SimpleObjectProperty<>(this, "middle", top.getFirstMoveableComponent());
		this.storage = new WeakHashMap<>();
		this.bottomCleaner = (Observable observable) -> {
			this.storage.remove(((BottomDisposer)(observable)).bottom());
		};
		this.middleCleaner = (Observable _) -> {
			this.middle.set(this.top.getFirstMoveableComponent());
		};
		this.middle.addListener(Util.change((ParameterSetMiddle oldMiddle, ParameterSetMiddle newMiddle) -> {
			oldMiddle.onDisposed.removeListener(this.middleCleaner);
			newMiddle.onDisposed.addListener(this.middleCleaner);
			this.storage.clear();
		}));
		this.currentStorage = (
			this
			.middle
			.flatMap((ParameterSetMiddle m) -> m.selectedBottom)
			.map(this::getStorage)
		);
		property.addListener(Util.change((T value) -> {
			if (!this.currentChanging) this.currentStorage.getValue().set(value);
		}));
		this.currentStorage.addListener(Util.change((SimpleObjectProperty<T> newCurrent) -> {
			this.currentChanging = true;
			try {
				this.property.setValue(newCurrent.get());
			}
			finally {
				this.currentChanging = false;
			}
		}));
	}

	public SimpleObjectProperty<T> getStorage(ParameterSetBottom bottom) {
		return this.storage.computeIfAbsent(bottom, (ParameterSetBottom b) -> {
			b.onDisposed.addListener(this.bottomCleaner);
			return new SimpleObjectProperty<>(this.property.getValue());
		});
	}
}