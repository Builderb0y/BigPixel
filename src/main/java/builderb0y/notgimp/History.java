package builderb0y.notgimp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.TreeSet;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class History implements Comparable<History> {

	public static final long MAX_MEMORY = 1024L * 1024L * 1024L;
	public static long currentMemory;
	public static final TreeSet<History> ALL_HISTORIES = new TreeSet<>();
	public static int historyCount;

	public Layer layer;
	public Entry oldestEntry;
	public SimpleObjectProperty<@Nullable Entry> currentEntry = new SimpleObjectProperty<>();
	public int entryCount, uniquifier;

	public History(Layer layer) {
		this.layer = layer;
		this.uniquifier = historyCount++;
		ALL_HISTORIES.add(this);
	}

	public void init(boolean fromSave) {
		this.save(fromSave ? "Load from disk" : "Initial image");
	}

	public static void onImageClosed(OpenImage image) {
		TreeItem<Layer> root = image.layerTree.getRoot();
		if (root != null) onLayerDeleted(root);
	}

	public static void onLayerDeleted(TreeItem<Layer> layer) {
		System.out.println("Deleting history for layer " + layer.getValue().name.get());
		History history = layer.getValue().history;
		history.clear();
		ALL_HISTORIES.remove(history);
		for (TreeItem<Layer> child : layer.getChildren()) {
			onLayerDeleted(child);
		}
	}

	public void undo() {
		Entry entry = this.currentEntry.get();
		if (entry != null && entry.prev != null) {
			this.currentEntry.set(entry.prev);
			entry.prev.restore(this.layer);
		}
	}

	public void redo() {
		Entry entry = this.currentEntry.get();
		if (entry != null && entry.next != null) {
			this.currentEntry.set(entry.next);
			entry.next.restore(this.layer);
		}
	}

	public void save(String name) {
		long toAdd = ((long)(this.layer.image.pixels.length)) * ((long)(Float.BYTES));
		if (toAdd > MAX_MEMORY) {
			System.err.println("Insufficient memory to store undo history for current layer.");
			return;
		}
		if (this.currentEntry.get() != null) {
			for (Entry next; (next = this.currentEntry.get().next) != null; ) {
				this.removeEntry(next);
			}
		}
		currentMemory += toAdd;
		while (currentMemory > MAX_MEMORY) {
			History history = ALL_HISTORIES.removeFirst();
			System.out.println("Removing oldest history entry from layer " + history.layer.name.get() + " to free up memory.");
			//todo; this will NPE if history is empty.
			history.removeEntry(history.oldestEntry);
			//todo: shouldn't history be re-added to ALL_HISTORIES?
			//	but if an empty history was removed, we will infinite loop.
		}
		Entry next = new Entry(name, this.layer.image);
		Entry current = this.currentEntry.get();
		next.prev = current;
		if (current != null) current.next = next;
		this.currentEntry.set(next);
	}

	public void removeEntry(Entry entry) {
		if (entry == this.currentEntry.get()) {
			this.clear();
			return;
		}
		if (entry.next != null) entry.next.prev = entry.prev;
		if (entry.prev != null) entry.prev.next = entry.next;
		else this.oldestEntry = entry.next;
		entry.prev = null;
		entry.next = null;
		currentMemory -= entry.pixels.length;
	}

	public void clear() {
		Entry entry = this.oldestEntry;
		while (entry != null) {
			Entry next = entry.next;
			if (next != null) {
				entry.next = null;
				next.prev = null;
			}
			currentMemory -= entry.pixels.length;
			entry = next;
		}
		this.oldestEntry = null;
		this.currentEntry.set(null);
	}

	@Override
	public int compareTo(@NotNull History that) {
		int compare = Integer.compare(that.entryCount, this.entryCount);
		if (compare != 0) return compare;
		return Integer.compare(this.uniquifier, that.uniquifier);
	}

	public static class Entry {

		public String name;
		public byte[] pixels;
		public @Nullable Entry prev, next;

		public Entry(String name, HDRImage image) {
			this.name = name;
			try {
				this.pixels = image.compressPixels();
			}
			catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}

		public void restore(Layer layer) {
			try {
				layer.image.decompressPixels(this.pixels);
			}
			catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
			layer.requestRedraw();
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}