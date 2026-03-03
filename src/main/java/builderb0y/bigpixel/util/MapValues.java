package builderb0y.bigpixel.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public abstract class MapValues<K, V> implements Collection<V> {

	public final Map<K, V> backingMap;

	public MapValues(Map<K, V> backingMap) {
		this.backingMap = backingMap;
	}

	public abstract K keyOf(V value);

	@Override
	public int size() {
		return this.backingMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.backingMap.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.backingMap.containsValue(o);
	}

	@Override
	public @NotNull Iterator<V> iterator() {
		return this.backingMap.values().iterator();
	}

	@Override
	public Object @NotNull [] toArray() {
		return this.backingMap.values().toArray();
	}

	@Override
	public <T> T @NotNull [] toArray(@NotNull T[] a) {
		return this.backingMap.values().toArray(a);
	}

	@Override
	public boolean add(V v) {
		return this.backingMap.putIfAbsent(this.keyOf(v), v) == null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		return this.backingMap.remove(this.keyOf((V)(o)), o);
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return this.backingMap.values().containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends V> c) {
		boolean changed = false;
		for (V v : c) {
			changed |= this.add(v);
		}
		return changed;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			changed |= this.remove(o);
		}
		return changed;
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return this.backingMap.values().retainAll(c);
	}

	@Override
	public void clear() {
		this.backingMap.clear();
	}

	@Override
	public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
		return this.backingMap.values().toArray(generator);
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super V> filter) {
		return this.backingMap.values().removeIf(filter);
	}

	@Override
	public @NotNull Spliterator<V> spliterator() {
		return this.backingMap.values().spliterator();
	}

	@Override
	public @NotNull Stream<V> stream() {
		return this.backingMap.values().stream();
	}

	@Override
	public @NotNull Stream<V> parallelStream() {
		return this.backingMap.values().parallelStream();
	}

	@Override
	public void forEach(Consumer<? super V> action) {
		this.backingMap.values().forEach(action);
	}
}