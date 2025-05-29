package builderb0y.notgimp.sources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.FastRandom;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.tools.Symmetry;

public class WFCLayerSource extends SingleInputEffectLayerSource {

	public Spinner<Integer>
		seed = Util.setupSpinner(new Spinner<>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0), 80.0D),
		kernel = Util.setupSpinner(new Spinner<>(2, 8, 1), 80.0D);
	public GridPane
		mainSettings = new GridPane();
	public CheckBox
		identity  = new CheckBox("None: "),
		rotate90  = new CheckBox("Rot 90: "),
		rotate180 = new CheckBox("Rot 180: "),
		rotate270 = new CheckBox("Rot 270: "),
		flipH     = new CheckBox("Flip H: "),
		flipV     = new CheckBox("Flip V: "),
		flipL     = new CheckBox("Flip L: "),
		flipR     = new CheckBox("Flip R: ");
	public GridPane
		symmetrySettings = new GridPane();
	public VBox
		mainPane = new VBox();
	public WorkerThread
		thread;
	public boolean
		redrawing;

	@Override
	public JsonMap save() {
		return (
			new JsonMap()
			.with("type", "wfc")
			.with("seed", this.seed.getValue())
			.with("kernel", this.kernel.getValue())

			.with("identity", this.identity.isSelected())
			.with("rotate90", this.rotate90.isSelected())
			.with("rotate180", this.rotate180.isSelected())
			.with("rotate270", this.rotate270.isSelected())
			.with("flipH", this.flipH.isSelected())
			.with("flipV", this.flipV.isSelected())
			.with("flipL", this.flipL.isSelected())
			.with("flipR", this.flipR.isSelected())
		);
	}

	@Override
	public void load(JsonMap map) {
		this.seed.getValueFactory().setValue(map.getInt("seed"));
		this.kernel.getValueFactory().setValue(map.getInt("kernel"));

		this.identity.setSelected(map.getBoolean("identity"));
		this.rotate90.setSelected(map.getBoolean("rotate90"));
		this.rotate180.setSelected(map.getBoolean("rotate180"));
		this.rotate270.setSelected(map.getBoolean("rotate270"));
		this.flipH.setSelected(map.getBoolean("flipH"));
		this.flipV.setSelected(map.getBoolean("flipV"));
		this.flipL.setSelected(map.getBoolean("flipL"));
		this.flipR.setSelected(map.getBoolean("flipR"));
	}

	public WFCLayerSource(LayerSources sources) {
		super(sources, "Wave Function Collapse");
		this.mainSettings.add(new Label("Seed: "), 0, 0);
		this.mainSettings.add(this.seed, 1, 0);
		this.mainSettings.add(new Label("Kernel: "), 0, 1);
		this.mainSettings.add(this.kernel, 1, 1);

		this.symmetrySettings.add(this.identity,  0, 0);
		this.symmetrySettings.add(this.rotate180, 0, 1);
		this.symmetrySettings.add(this.rotate90,  0, 2);
		this.symmetrySettings.add(this.rotate270, 0, 3);
		this.symmetrySettings.add(this.flipH,     1, 0);
		this.symmetrySettings.add(this.flipV,     1, 1);
		this.symmetrySettings.add(this.flipL,     1, 2);
		this.symmetrySettings.add(this.flipR,     1, 3);
		Button all = new Button("All");
		all.setOnAction((ActionEvent _) -> this.setAllSymmetries(true));
		Button none = new Button("None");
		none.setOnAction((ActionEvent _) -> this.setAllSymmetries(false));
		this.symmetrySettings.add(all, 0, 4);
		this.symmetrySettings.add(none, 1, 4);

		this.mainPane.getChildren().addAll(this.mainSettings, this.symmetrySettings);

		ChangeListener<Object> listener = Util.change(this::requestRedraw);
		this.seed.getValueFactory().valueProperty().addListener(listener);
		this.kernel.getValueFactory().valueProperty().addListener(listener);

		this.identity.selectedProperty().addListener(listener);
		this.rotate90.selectedProperty().addListener(listener);
		this.rotate180.selectedProperty().addListener(listener);
		this.rotate270.selectedProperty().addListener(listener);
		this.flipH.selectedProperty().addListener(listener);
		this.flipV.selectedProperty().addListener(listener);
		this.flipL.selectedProperty().addListener(listener);
		this.flipR.selectedProperty().addListener(listener);

		this.rootNode.setCenter(this.mainPane);
	}

	public void setAllSymmetries(boolean selected) {
		this.identity.setSelected(selected);
		this.rotate90.setSelected(selected);
		this.rotate180.setSelected(selected);
		this.rotate270.setSelected(selected);
		this.flipH.setSelected(selected);
		this.flipV.setSelected(selected);
		this.flipL.setSelected(selected);
		this.flipR.setSelected(selected);
	}

	public void copyFrom(WFCLayerSource that) {
		this.seed.getValueFactory().setValue(that.seed.getValue());
		this.kernel.getValueFactory().setValue(that.kernel.getValue());

		this.identity.setSelected(that.identity.isSelected());
		this.rotate90.setSelected(that.rotate90.isSelected());
		this.rotate180.setSelected(that.rotate180.isSelected());
		this.rotate270.setSelected(that.rotate270.isSelected());
		this.flipH.setSelected(that.flipH.isSelected());
		this.flipV.setSelected(that.flipV.isSelected());
		this.flipL.setSelected(that.flipL.isSelected());
		this.flipR.setSelected(that.flipR.isSelected());
	}

	@Override
	public void doRedraw() throws RedrawException {
		if (this.redrawing) return;
		if (this.thread != null) {
			this.thread.interrupt();
			try {
				this.thread.join();
			}
			catch (InterruptedException exception) {
				throw new RedrawException("Interrupted");
			}
		}
		(this.thread = new WorkerThread(this)).start();
	}

	public static class Tile {

		public final int kernel;
		public final float[] pixels;
		public final int hashCode;
		public int seen;

		public Tile(
			HDRImage image,
			int baseX,
			int baseY,
			int kernel,
			Symmetry symmetry
		) {
			this.kernel = kernel;
			this.pixels = new float[kernel * kernel * 4];
			for (int y = 0; y < kernel; y++) {
				for (int x = 0; x < kernel; x++) {
					int imageX = Math.floorMod(symmetry.getX(x, y) + baseX, image.width);
					int imageY = Math.floorMod(symmetry.getY(x, y) + baseY, image.height);
					System.arraycopy(image.pixels, image.baseIndex(imageX, imageY), this.pixels, (y * kernel + x) << 2, 4);
				}
			}
			this.hashCode = Arrays.hashCode(this.pixels);
		}

		public static Tile[] generate(HDRImage image, int kernel, int symmetries) {
			ConcurrentHashMap<Tile, Integer> tiles = new ConcurrentHashMap<>(image.width * image.height * Symmetry.VALUES.length);
			LinkedList<CompletableFuture<?>> futures = new LinkedList<>();
			for (int baseY = 0; baseY < image.height; baseY++) {
				int baseY_ = baseY;
				for (int baseX = 0; baseX < image.width; baseX++) {
					int baseX_ = baseX;
					for (Symmetry symmetry : Symmetry.VALUES) {
						if ((symmetries & symmetry.flag()) != 0) {
							futures.add(CompletableFuture.runAsync(() -> {
								tiles.merge(new Tile(image, baseX_, baseY_, kernel, symmetry), 1, Integer::sum);
							}));
						}
					}
				}
				if (Thread.interrupted()) {
					return null;
				}
			}
			for (CompletableFuture<?> future; (future = futures.poll()) != null;) {
				if (Thread.interrupted()) {
					return null;
				}
				else {
					future.join();
				}
			}
			Tile[] result = new Tile[tiles.size()];
			{
				int index = 0;
				for (Map.Entry<Tile, Integer> entry : tiles.entrySet()) {
					(result[index++] = entry.getKey()).seen = entry.getValue();
				}
			}
			Arrays.sort(result, (Tile tile1, Tile tile2) -> {
				return Arrays.compare(tile1.pixels, tile2.pixels);
			});
			//dumpTiles(result);
			return result;
		}

		public static void dumpTiles(Tile[] tiles) {
			try {
				File root = new File("debug");
				root.mkdir();
				for (File existing : root.listFiles()) {
					if (existing.getPath().endsWith(".png") && existing.isFile()) {
						existing.delete();
					}
				}
				for (int index = 0; index < tiles.length; index++) {
					Tile tile = tiles[index];
					HDRImage image = new HDRImage(tile.kernel, tile.kernel, tile.pixels);
					ImageIO.write(image.toAwtImage(), "png", new File(root, index + ".png"));
				}
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}

		public boolean checkValid(int x, int y, FloatVector color) {
			return this.getColor(x, y).equals(color);
		}

		public FloatVector getColor(int x, int y) {
			int index = (y * this.kernel + x) << 2;
			return FloatVector.fromArray(FloatVector.SPECIES_128, this.pixels, index);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Tile that && this.hashCode == that.hashCode && Arrays.equals(this.pixels, that.pixels);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}
	}

	public static class TileList {

		public final int x, y;
		public final Tile[] tiles;
		public final int[] possibilities;
		public int possibilitiesCount;
		public int totalWeight;

		@Override
		public String toString() {
			return "TileList at " + this.x + ", " + this.y + ": possibilities: " + this.possibilitiesCount + ", weight: " + this.totalWeight;
		}

		public TileList(int x, int y, Tile[] tiles) {
			this.x = x;
			this.y = y;
			this.tiles = tiles;
			this.possibilitiesCount = tiles.length;
			this.possibilities = new int[tiles.length];
			for (int index = 0; index < tiles.length; index++) {
				this.possibilities[index] = index;
				this.totalWeight += tiles[index].seen;
			}
		}

		public Tile collapse(RandomGenerator random) {
			if (this.possibilitiesCount == 0) return null;
			if (false) {
				Tile tile = this.tiles[this.possibilities[random.nextInt(this.possibilitiesCount)]];
				this.totalWeight = this.possibilitiesCount = 0;
				return tile;
			}
			int choice = random.nextInt(this.totalWeight);
			for (int index = 0; index < this.possibilitiesCount; index++) {
				Tile tile = this.tiles[this.possibilities[index]];
				if ((choice -= tile.seen) < 0) {
					//System.out.println("collapsed to " + this.possibilities[index]);
					this.totalWeight = this.possibilitiesCount = 0;
					return tile;
				}
			}
			throw new IllegalStateException("RNG fail");
		}

		public void propagate(int x, int y, FloatVector color) {
			for (int index = 0; index < this.possibilitiesCount;) {
				Tile tile = this.tiles[this.possibilities[index]];
				if (!tile.checkValid(x, y, color)) {
					//System.out.println("Eliminated " + this.possibilities[index]);
					this.totalWeight -= tile.seen;
					this.possibilities[index] = this.possibilities[--this.possibilitiesCount];
				}
				else {
					index++;
				}
			}
			//System.out.println("Remaining: " + Arrays.toString(Arrays.copyOfRange(this.possibilities, 0, this.possibilitiesCount)));
		}
	}

	public static class WorkerThread extends Thread {

		public final WFCLayerSource layerSource;
		public final int kernel, symmetries;
		public HDRImage source, intermediate, writing;
		public boolean swapCalled;
		public final RandomGenerator random;
		public PresentationSwapper presentationSwapper;
		public TileList[] tileLists;
		public final BitSet filledPixels;

		public WorkerThread(WFCLayerSource layerSource) throws RedrawException {
			super("Wave Function Collapse Thread");
			this.layerSource = layerSource;
			this.kernel = layerSource.kernel.getValue();
			this.source = layerSource.getSingleInput(false).image;
			HDRImage destination = layerSource.sources.layer.image;
			this.intermediate = new HDRImage(destination.width, destination.height);
			this.writing = new HDRImage(destination.width, destination.height);
			this.filledPixels = new BitSet(destination.width * destination.height);
			this.random = new FastRandom(layerSource.seed.getValue());
			this.presentationSwapper = new PresentationSwapper();
			this.symmetries = (
				(layerSource.identity .isSelected() ? Symmetry.IDENTITY  .flag() : 0) |
				(layerSource.rotate90 .isSelected() ? Symmetry.ROTATE_CW .flag() : 0) |
				(layerSource.rotate180.isSelected() ? Symmetry.ROTATE_180.flag() : 0) |
				(layerSource.rotate270.isSelected() ? Symmetry.ROTATE_CCW.flag() : 0) |
				(layerSource.flipH    .isSelected() ? Symmetry.FLIP_H    .flag() : 0) |
				(layerSource.flipV    .isSelected() ? Symmetry.FLIP_V    .flag() : 0) |
				(layerSource.flipL    .isSelected() ? Symmetry.FLIP_L    .flag() : 0) |
				(layerSource.flipR    .isSelected() ? Symmetry.FLIP_R    .flag() : 0)
			);
		}

		public synchronized void swapPresentation() {
			Layer layer = this.layerSource.sources.layer;
			HDRImage presentation = layer.image;
			layer.image = this.intermediate;
			this.intermediate = presentation;

			this.layerSource.sources.layer.needsRedraw = true;
			this.layerSource.redrawing = true;
			try {
				this.layerSource.sources.layer.openImage.redrawAll(true);
			}
			finally {
				this.layerSource.redrawing = false;
			}
			this.swapCalled = true;
		}

		public synchronized void swapWriting(boolean force) {
			if (force || this.swapCalled) {
				this.swapCalled = false;
				System.arraycopy(this.writing.pixels, 0, this.intermediate.pixels, 0, this.writing.pixels.length);
			}
		}

		@Override
		public void start() {
			super.start();
			Arrays.fill(this.layerSource.sources.layer.image.pixels, 0.0F);
			this.presentationSwapper.play();
		}

		@Override
		public void run() {
			try {
				this.doRun();
			}
			finally {
				Platform.runLater(this.presentationSwapper::stop);
			}
		}

		public void doRun() {
			if (this.symmetries == 0) return;
			Tile[] tiles = Tile.generate(this.source, this.kernel, this.symmetries);
			if (tiles == null) return;
			this.tileLists = new TileList[this.writing.width * this.writing.height];
			int index = 0;
			for (int y = 0; y < this.writing.height; y++) {
				for (int x = 0; x < this.writing.width; x++) {
					this.tileLists[index++] = new TileList(x, y, tiles);
				}
			}
			if (Thread.interrupted()) return;
			this.processList(this.tileLists[this.random.nextInt(this.tileLists.length)]);
			//this.processList(this.nextList());
			//*
			while (true) {
				TileList list = this.nextList();
				if (list == null) break;
				this.processList(list);
				if (Thread.interrupted()) return;
			}
			//*/
			this.swapWriting(true);
		}

		public TileList nextList() {
			TileList chosen = null;
			int chance = 0;
			for (TileList list : this.tileLists) {
				if (list.possibilitiesCount > 0) {
					if (chosen == null) {
						chosen = list;
					}
					else if (list.possibilitiesCount < chosen.possibilitiesCount) {
						chosen = list;
						chance = 0;
					}
					else if (list.possibilitiesCount == chosen.possibilitiesCount) {
						if (chance++ == 0 || this.random.nextInt(chance) == 0) {
							chosen = list;
						}
					}
				}
			}
			//System.out.println("chosen: " + chosen);
			return chosen;
		}

		public void processList(TileList list) {
			//System.out.println("Processing " + list);
			Tile tile = list.collapse(this.random);
			for (int offsetY1 = 0; offsetY1 < this.kernel; offsetY1++) {
				for (int offsetX1 = 0; offsetX1 < this.kernel; offsetX1++) {
					FloatVector color = tile.getColor(offsetX1, offsetY1);
					int imageX1 = Math.floorMod(list.x + offsetX1, this.writing.width);
					int imageY1 = Math.floorMod(list.y + offsetY1, this.writing.height);
					if (this.filledPixels.get(imageY1 * this.writing.width + imageX1)) {
						FloatVector existingColor = this.writing.getPixel(imageX1, imageY1);
						if (!color.equals(existingColor)) {
							throw new IllegalStateException("Overwriting color at " + imageX1 + ", " + imageY1 + " from " + existingColor + " to " + color);
						}
					}
					else {
						this.filledPixels.set(imageY1 * this.writing.width + imageX1);
						//System.out.println("Setting " + imageX1 + ", " + imageY1 + " to " + color);
						this.writing.setColor(imageX1, imageY1, color);
						for (int offsetY2 = 0; offsetY2 < this.kernel; offsetY2++) {
							for (int offsetX2 = 0; offsetX2 < this.kernel; offsetX2++) {
								int imageX2 = Math.floorMod(imageX1 - offsetX2, this.writing.width);
								int imageY2 = Math.floorMod(imageY1 - offsetY2, this.writing.height);
								TileList otherList = this.tileLists[imageY2 * this.writing.width + imageX2];
								//System.out.println("propagating to " + otherList);
								otherList.propagate(offsetX2, offsetY2, color);
							}
						}
					}
				}
			}
			this.swapWriting(false);
		}

		public class PresentationSwapper extends Transition {

			public PresentationSwapper() {
				super(20.0D);
				this.setInterpolator(Interpolator.LINEAR);
				this.setCycleCount(Animation.INDEFINITE);
				this.setCycleDuration(Duration.seconds(1.0D));
			}

			@Override
			public void interpolate(double frac) {
				WorkerThread.this.swapPresentation();
			}

			@Override
			public void stop() {
				super.stop();
				WorkerThread.this.swapPresentation();
			}
		}
	}

	/*
	public static class TileArray {

		public final int width, height;
		public final Symmetry[] symmetries;
		public final float[] weights;

		public TileArray(int width, int height, int symmetryFlags) {
			this.width = width;
			this.height = height;
			this.symmetries = new Symmetry[Integer.bitCount(symmetryFlags)];
			int writeIndex = 0;
			for (Symmetry symmetry : Symmetry.VALUES) {
				if ((symmetryFlags & symmetry.flag()) != 0) {
					this.symmetries[writeIndex++] = symmetry;
				}
			}
			this.weights = new float[width * height * this.symmetries.length];
		}

		public int length() {
			return this.weights.length;
		}

		public int getCenterX(int index) {
			return index % this.width;
		}

		public int getCenterY(int index) {
			return (index / this.width) % this.height;
		}

		public Symmetry getSymmetry(int index) {
			return this.symmetries[index / (this.width * this.height)];
		}

		public FloatVector getColor(int index, HDRImage sourceImage, int x, int y) {
			Symmetry symmetry = this.getSymmetry(index);
			return sourceImage.getPixel(
				Math.floorMod(symmetry.getX(x, y) + this.getCenterX(index), sourceImage.width),
				Math.floorMod(symmetry.getY(x, y) + this.getCenterY(index), sourceImage.height)
			);
		}

		public FloatVector getCenterColor(int index, HDRImage sourceImage) {
			return sourceImage.getPixel(this.getCenterX(index), this.getCenterY(index));
		}

		public float getWeight(int index) {
			return this.weights[index];
		}

		public void setWeight(int index, float weight) {
			this.weights[index] = weight;
		}
	}

	public static class WorkerThread extends Thread {

		public final WFCLayerSource layerSource;
		public final HDRImage source, destination;
		public final FastRandom random;
		public final byte kernel, iterations, symmetryFlags;

		public WorkerThread(WFCLayerSource layerSource) throws RedrawException {
			super("Wave Function Collapse Thread");
			this.source = layerSource.getSingleInput(false).image;
			if (this.source.width > 64 || this.source.height > 64) {
				throw new RedrawException("Child layer too large");
			}
			this.layerSource = layerSource;
			this.destination = layerSource.sources.layer.image;
			Arrays.fill(this.destination.pixels, 0.0F);
			this.random = new FastRandom(layerSource.seed.getValue());
			this.kernel = layerSource.kernel.getValue().byteValue();
			this.iterations = layerSource.iterations.getValue().byteValue();
			this.symmetryFlags = (byte)(
				(layerSource.identity .isSelected() ? Symmetry.IDENTITY  .flag() : 0) |
				(layerSource.rotate90 .isSelected() ? Symmetry.ROTATE_CW .flag() : 0) |
				(layerSource.rotate180.isSelected() ? Symmetry.ROTATE_180.flag() : 0) |
				(layerSource.rotate270.isSelected() ? Symmetry.ROTATE_CCW.flag() : 0) |
				(layerSource.flipH    .isSelected() ? Symmetry.FLIP_H    .flag() : 0) |
				(layerSource.flipV    .isSelected() ? Symmetry.FLIP_V    .flag() : 0) |
				(layerSource.flipL    .isSelected() ? Symmetry.FLIP_L    .flag() : 0) |
				(layerSource.flipR    .isSelected() ? Symmetry.FLIP_R    .flag() : 0)
			);
		}

		@Override
		public void run() {
			if (this.symmetryFlags == 0) return;
			TileArray tiles = new TileArray(this.source.width, this.source.height, Byte.toUnsignedInt(this.symmetryFlags));
			int[] dstPositions = new int[this.destination.width * this.destination.height];
			for (int index = 0; index < dstPositions.length; index++) {
				dstPositions[index] = index;
			}
			BitSet positionBits = new BitSet(this.destination.width * this.destination.height);
			int threadCount = Runtime.getRuntime().availableProcessors();
			int tasksPerThread = Math.ceilDiv(tiles.length(), threadCount);
			long nextSync = System.currentTimeMillis() + 500L;
			for (int iteration = 0; iteration < this.iterations; iteration++) {
				for (int dstPositionCount = dstPositions.length; dstPositionCount > 0;) {
					if (Thread.interrupted()) return;
					int positionIndex = this.random.nextInt(dstPositionCount);
					int position = dstPositions[positionIndex];
					dstPositions[positionIndex] = dstPositions[--dstPositionCount];
					dstPositions[dstPositionCount] = position;
					if (iteration == 0 && positionBits.get(position)) continue;
					int dstCenterX = position % this.destination.width;
					int dstCenterY = position / this.destination.width;
					IntStream.range(0, threadCount).parallel().forEach((int threadIndex) -> {
						int tileMin = threadIndex * tasksPerThread;
						int tileMax = Math.min(threadIndex * tasksPerThread + tasksPerThread, tiles.length());
						float minDiff = Float.POSITIVE_INFINITY;
						for (int tileIndex = tileMin; tileIndex < tileMax; tileIndex++) {
							float similarity = 0.0F;
							offsetLoop:
							for (int offsetY = -this.kernel; offsetY <= this.kernel; offsetY++) {
								int dstY = Math.floorMod(dstCenterY + offsetY, this.destination.height);
								for (int offsetX = -this.kernel; offsetX <= this.kernel; offsetX++) {
									int dstX = Math.floorMod(dstCenterX + offsetX, this.destination.width);
									if (!positionBits.get(dstY * this.destination.width + dstX)) {
										continue;
									}
									FloatVector srcColor = tiles.getColor(tileIndex, this.source, offsetX, offsetY);
									FloatVector dstColor = this.destination.getPixel(dstX, dstY);
									FloatVector diff = srcColor.sub(dstColor);
									similarity += diff.mul(diff).reduceLanes(VectorOperators.ADD);
									if (similarity > minDiff) break offsetLoop;
								}
							}
							if (similarity < minDiff) minDiff = similarity;
							tiles.setWeight(tileIndex, similarity);
						}
					});
					int chosenIndex = 0;
					float chosenWeight = tiles.getWeight(0);
					int chance = 1;
					for (int index = 1; index < tiles.length(); index++) {
						float newWeight = tiles.getWeight(index);
						if (newWeight < chosenWeight || (newWeight == chosenWeight && this.random.nextInt(++chance) == 0)) {
							chosenIndex = index;
							chosenWeight = newWeight;
						}
					}
					if (iteration == 0) {
						for (int offsetY = -this.kernel; offsetY <= this.kernel; offsetY++) {
							int dstY = Math.floorMod(dstCenterY + offsetY, this.destination.height);
							for (int offsetX = -this.kernel; offsetX <= this.kernel; offsetX++) {
								int dstX = Math.floorMod(dstCenterX + offsetX, this.destination.width);
								int dstIndex = dstY * this.destination.width + dstX;
								if (!positionBits.get(dstIndex)) {
									positionBits.set(dstIndex);
									tiles.getColor(chosenIndex, this.source, offsetX, offsetY).intoArray(this.destination.pixels, dstIndex << 2);
								}
							}
						}
					}
					else {
						this.destination.setPixel(dstCenterX, dstCenterY, tiles.getCenterColor(chosenIndex, this.source));
					}
					if (System.currentTimeMillis() >= nextSync) {
						if (this.sync()) return;
						nextSync = System.currentTimeMillis() + 500L;
					}
				}
			}
			this.sync();
		}

		public boolean sync() {
			try {
				Util.invokeAndWait(() -> {
					this.layerSource.sources.layer.needsRedraw = true;
					this.layerSource.redrawing = true;
					try {
						this.layerSource.sources.layer.openImage.redrawAll(true);
					}
					finally {
						this.layerSource.redrawing = false;
					}
				});
				return false;
			}
			catch (InterruptedException ignored) {
				return true;
			}
		}
	}
	*/
}