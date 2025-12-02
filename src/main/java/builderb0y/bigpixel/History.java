package builderb0y.bigpixel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.sources.ManualLayerSource;

public class History implements Comparable<History> {

	public static final long MAX_MEMORY = 1024L * 1024L * 1024L;
	public static long currentMemory;
	public static final TreeSet<History> ALL_HISTORIES = new TreeSet<>();
	public static int historyCount;

	public ManualLayerSource source;
	public Entry oldestEntry;
	public SimpleObjectProperty<@Nullable Entry> currentEntry = new SimpleObjectProperty<>();
	public int entryCount, uniquifier;

	public History(ManualLayerSource source) {
		this.source = source;
		this.uniquifier = historyCount++;
		ALL_HISTORIES.add(this);
	}

	public void init(boolean fromSave) {
		this.save(fromSave ? "Load from disk" : "Initial image");
	}

	public static void onImageClosed(OpenImage image) {
		for (LayerNode layer : image.layerGraph.layerList) {
			onLayerDeleted(layer);
		}
	}

	public static void onLayerDeleted(LayerNode layer) {
		long oldMemory = currentMemory;
		History history = layer.sources.manualSource().history;
		history.clear();
		ALL_HISTORIES.remove(history);
		long newMemory = currentMemory;
		System.out.println("Deleted history for layer " + layer.getDisplayName() + ": " + oldMemory + " -> " + newMemory + " (" + (newMemory - oldMemory) + ')');
	}

	public void undo() {
		Entry entry = this.currentEntry.get();
		if (entry != null && entry.prev != null) {
			this.currentEntry.set(entry.prev);
			entry.prev.restore(this.source);
		}
	}

	public void redo() {
		Entry entry = this.currentEntry.get();
		if (entry != null && entry.next != null) {
			this.currentEntry.set(entry.next);
			entry.next.restore(this.source);
		}
	}

	public void save(String name) {
		assert this.source.sources.layer.getFrameCount() == 1;
		HDRImage image = this.source.sources.layer.getFrame(0);
		long toAdd = ((long)(image.pixels.length)) * ((long)(Float.BYTES));
		if (toAdd > MAX_MEMORY) {
			System.err.println("Insufficient memory to store undo history for current layer.");
			return;
		}
		if (this.currentEntry.get() != null) {
			for (Entry next; (next = this.currentEntry.get().next) != null;) {
				this.removeEntry(next);
			}
		}
		currentMemory += toAdd;
		outer:
		while (currentMemory > MAX_MEMORY) {
			for (History history : ALL_HISTORIES) {
				if (history.oldestEntry == null) continue;
				System.out.println("Removing oldest history entry from layer " + history.source.sources.layer.getDisplayName() + " to free up memory.");
				history.removeEntry(history.oldestEntry);
				continue outer;
			}
			System.err.println("Insufficient memory to store undo history for current layer.");
			currentMemory -= toAdd;
			return;
		}
		Entry next = new Entry(name, image);
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
		entry.compressionThread.interrupt();
		if (entry.next != null) entry.next.prev = entry.prev;
		if (entry.prev != null) entry.prev.next = entry.next;
		else this.oldestEntry = entry.next;
		entry.prev = null;
		entry.next = null;
		currentMemory += -entry.getUsedMemory();
	}

	public void clear() {
		Entry entry = this.oldestEntry;
		while (entry != null) {
			Entry next = entry.next;
			if (next != null) {
				next.compressionThread.interrupt();
				entry.next = null;
				next.prev = null;
			}
			currentMemory -= entry.getUsedMemory();
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
		public int width, height;
		public float[] uncompressedPixels;
		public byte[] compressedPixels;
		public @Nullable Entry prev, next;
		public Thread compressionThread;

		public Entry(String name, HDRImage image) {
			this.name = name;
			this.width = image.width;
			this.height = image.height;
			float[] uncompressedPixels = this.uncompressedPixels = image.pixels.clone();
			this.compressionThread = new Thread("History compressor") {

				@Override
				public void run() {
					byte[] compressedPixels = compress(uncompressedPixels);
					if (compressedPixels != null) {
						Platform.runLater(() -> {
							Entry.this.compressedPixels = compressedPixels;
							Entry.this.uncompressedPixels = null;
							long oldPixels = ((long)(uncompressedPixels.length)) * ((long)(Float.BYTES));
							long newPixels = (long)(compressedPixels.length);
							long oldUsed = History.currentMemory;
							currentMemory += newPixels - oldPixels;
							System.out.println("Compressed pixels: " + oldPixels + " -> " + newPixels + ", memory: " + oldUsed + " -> " + History.currentMemory);
						});
					}
				}
			};
			this.compressionThread.start();
		}

		public long getUsedMemory() {
			if (this.compressedPixels != null) return (long)(this.compressedPixels.length);
			else return ((long)(this.uncompressedPixels.length)) * ((long)(Float.BYTES));
		}

		public void restore(ManualLayerSource source) {
			HDRImage destination = source.getToollessImage();
			destination.checkSize(this.width, this.height, false);
			if (this.compressedPixels != null) try {
				destination.decompressPixels(this.compressedPixels);
			}
			catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
			else {
				System.arraycopy(this.uncompressedPixels, 0, destination.pixels, 0, this.uncompressedPixels.length);
			}
			source.redrawLater();
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	public static byte[] compress(float[] pixels) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(pixels.length * Float.BYTES);
			GZIPOutputStream compressor = new GZIPOutputStream(baos);
			DataOutputStream dos = new DataOutputStream(compressor);
			for (float pixel : pixels) {
				if (Thread.interrupted()) {
					System.out.println(Thread.currentThread() + " interrupted while compressing.");
					return null;
				}
				dos.writeFloat(pixel);
			}
			dos.close();
			return baos.toByteArray();
		}
		catch (IOException unexpected) {
			unexpected.printStackTrace();
			return null;
		}
	}
}