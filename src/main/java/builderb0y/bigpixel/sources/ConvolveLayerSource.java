package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

import javafx.beans.binding.When;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.util.Util;

public class ConvolveLayerSource extends MainMaskLayerSource {

	public BorderPane borderPane = new BorderPane();
	public CheckBox linear = this.parameters.addCheckbox("linear", "Linear", false);
	public ChoiceBox<ConvolveShape> shape = this.parameters.addEnumChoiceBox("shape", ConvolveShape.class, ConvolveShape.SQUARE);
	public ChoiceBox<BlurWeight> weight = this.parameters.addEnumChoiceBox("weight_type", BlurWeight.class, BlurWeight.CUSTOM);
	public Spinner<Integer> radius = this.parameters.addIntSpinner("radius", 1, 3, 1, 1, 64.0D);
	public GridPane customWeights = new GridPane();
	public CheckBox normalizeCustomWeights = this.parameters.addCheckbox("normalize", "Normalize", true);
	public SplitPane splitPane = new SplitPane();
	public HDRImage separableScratch;

	@Override
	public JsonMap save() {
		JsonMap map = super.save();
		if (this.weight.getValue() == BlurWeight.CUSTOM) {
			float[] weights = BlurWeight.CUSTOM.getWeights(this, false);
			JsonArray jsonWeights = new JsonArray(weights.length);
			for (float weight : weights) jsonWeights.add(weight);
			map.put("custom_weights", jsonWeights);
		}
		return map;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void load(JsonMap map) {
		super.load(map);
		this.layout();
		if (this.weight.getValue() == BlurWeight.CUSTOM) {
			ObservableList<Node> children = this.customWeights.getChildren();
			JsonArray jsonWeights = map.getArray("custom_weights");
			for (int index = 0, size = jsonWeights.size(); index < size; index++) {
				(
					(TextFormatter<Float>)(
						(
							(TextField)(children.get(index))
						)
						.getTextFormatter()
					)
				)
				.setValue(jsonWeights.getFloat(index));
			}
		}
	}

	public ConvolveLayerSource(LayerSources sources) {
		super(LayerSourceType.CONVOLVE, sources);
		this.layout();
		this.normalizeCustomWeights.visibleProperty().bind(this.weight.valueProperty().isEqualTo(BlurWeight.CUSTOM));
		this.borderPane.setTop(new HBox(this.shape, this.weight, this.radius, this.normalizeCustomWeights));
		this.borderPane.setCenter(this.customWeights);
		this.splitPane.setOrientation(Orientation.VERTICAL);
		this.commonSourceSettings.getChildren().add(this.linear);
		this.rootConfigPane.setCenter(null);
		this.splitPane.getItems().setAll(this.dependencies.getConfigPane(), this.borderPane);
		this.rootConfigPane.setCenter(this.splitPane);
		((IntegerSpinnerValueFactory)(this.radius.getValueFactory())).maxProperty().bind(
			new When(this.weight.valueProperty().isEqualTo(BlurWeight.CUSTOM))
			.then(3)
			.otherwise(64)
		);
		ChangeListener<Object> listener = Util.change(() -> {
			this.layout();
			this.redrawLater();
		});
		this.shape .valueProperty().addListener(listener);
		this.weight.valueProperty().addListener(listener);
		this.radius.valueProperty().addListener(listener);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void copyFrom(LayerSource source) {
		super.copyFrom(source);
		ConvolveLayerSource that = (ConvolveLayerSource)(source);
		ObservableList<Node> thisChildren = this.customWeights.getChildren();
		ObservableList<Node> thatChildren = that.customWeights.getChildren();
		for (int index = 0, size = thatChildren.size(); index < size; index++) {
			(
				(TextFormatter<Float>)(
					(
						(TextField)(thisChildren.get(index))
					)
					.getTextFormatter()
				)
			)
			.setValue(
				(
					(TextFormatter<Float>)(
						(
							(TextField)(thatChildren.get(index))
						)
						.getTextFormatter()
					)
				)
				.getValue()
			);
		}
	}

	public void layout() {
		this.customWeights.getChildren().clear();
		if (this.weight.getValue() != BlurWeight.CUSTOM) return;
		int radius = this.radius.getValue();
		int diameter = radius * 2 + 1;
		int sizeX = 1, sizeY = 1;
		boolean editable = false;
		float[] weights;
		switch (this.shape.getValue()) {
			case HORIZONTAL, SEPARABLE, CONCENTRIC -> {
				sizeX = diameter;
				weights = switch (this.weight.getValue()) {
					case BOX, GAUSSIAN -> this.weight.getValue().getWeights(this, this.normalizeCustomWeights.isSelected());
					case CUSTOM -> {
						editable = true;
						float[] w = new float[diameter];
						w[diameter >> 1] = 1.0F;
						yield w;
					}
				};
			}
			case VERTICAL -> {
				sizeY = diameter;
				weights = switch (this.weight.getValue()) {
					case BOX, GAUSSIAN -> this.weight.getValue().getWeights(this, this.normalizeCustomWeights.isSelected());
					case CUSTOM -> {
						editable = true;
						float[] w = new float[diameter];
						w[diameter >> 1] = 1.0F;
						yield w;
					}
				};
			}
			case SQUARE -> {
				sizeX = diameter;
				sizeY = diameter;
				editable = true;
				weights = new float[diameter * diameter];
				weights[(diameter * diameter) >> 1] = 1.0F;
			}
			default -> throw new AssertionError();
		}
		int index = 0;
		for (int y = 0; y < sizeY; y++) {
			for (int x = 0; x < sizeX; x++) {
				this.customWeights.add(this.createNumberTextField(weights[index++], editable), x, y);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TextField createNumberTextField(float value, boolean editable) {
		TextField textField = new TextField();
		textField.setTextFormatter(new TextFormatter<>(
			new StringConverter<>() {

				@Override
				public String toString(Float number) {
					return number.toString();
				}

				@Override
				public Float fromString(String s) {
					return Float.valueOf(s);
				}
			},
			value
		));
		textField.setPrefWidth(48.0D);
		textField.setEditable(editable);
		textField.setOnScroll((ScrollEvent event) -> {
			if (textField.isEditable()) {
				float delta;
				if (event.getDeltaY() > 0.0D) delta = 1.0F;
				else if (event.getDeltaY() < 0.0D) delta = -1.0F;
				else return;
				TextFormatter rawFormatter = textField.getTextFormatter();
				rawFormatter.setValue(((Float)(rawFormatter.getValue())).floatValue() + delta);
			}
		});
		textField.getTextFormatter().valueProperty().addListener(Util.change(() -> {
			if (textField.isEditable()) {
				this.redrawLater();
			}
		}));
		return textField;
	}

	public HDRImage getSeparableScratch() {
		int width = this.sources.layer.imageWidth();
		int height = this.sources.layer.imageHeight();
		if (this.separableScratch == null) {
			this.separableScratch = new HDRImage(width, height);
		}
		else {
			this.separableScratch.checkSize(width, height, false);
		}
		return this.separableScratch;
	}

	public static FloatVector maybeSquare(FloatVector color, boolean linear) {
		return linear ? color.mul(color, Util.RGB_MASK) : color;
	}

	public static FloatVector maybeSqrt(FloatVector color, boolean linear) {
		return linear ? color.lanewise(VectorOperators.SQRT, Util.RGB_MASK) : color;
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
		int radius = this.radius.getValue();
		boolean linear = this.linear.isSelected();
		float[] weights = this.weight.getValue().getWeights(this, this.normalizeCustomWeights.isSelected());
		switch (this.shape.getValue()) {
			case HORIZONTAL -> {
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < destination.width; x++) {
						FloatVector mainColor = main.getColor(x, y);
						FloatVector maskColor = mask.getColor(x, y);
						if (maskColor.compare(VectorOperators.GE, 0.0F).anyTrue()) {
							FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
							for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
								if (weights[index] != 0.0F) {
									sum = sum.add(maybeSquare(main.getColor(Math.floorMod(x + offset, destination.width), y), linear).mul(weights[index]));
								}
							}
							destination.setColor(x, y, carefulMix(mainColor, maybeSqrt(sum, linear), maskColor));
						}
						else {
							destination.setColor(x, y, mainColor);
						}
					}
				});
			}
			case VERTICAL -> {
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < destination.width; x++) {
						FloatVector mainColor = main.getColor(x, y);
						FloatVector maskColor = mask.getColor(x, y);
						if (maskColor.compare(VectorOperators.GE, 0.0F).anyTrue()) {
							FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
							for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
								if (weights[index] != 0.0F) {
									sum = sum.add(maybeSquare(main.getColor(x, Math.floorMod(y + offset, destination.height)), linear).mul(weights[index]));
								}
							}
							destination.setColor(x, y, carefulMix(mainColor, maybeSqrt(sum, linear), maskColor));
						}
						else {
							destination.setColor(x, y, mainColor);
						}
					}
				});
			}
			case SEPARABLE -> {
				HDRImage scratch = this.getSeparableScratch();
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < destination.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							if (weights[index] != 0.0F) {
								sum = sum.add(maybeSquare(main.getColor(Math.floorMod(x + offset, destination.width), y), linear).mul(weights[index]));
							}
						}
						scratch.setColor(x, y, sum);
					}
				});
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < scratch.width; x++) {
						FloatVector mainColor = main.getColor(x, y);
						FloatVector maskColor = mask.getColor(x, y);
						if (maskColor.compare(VectorOperators.GE, 0.0F).anyTrue()) {
							FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
							for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
								if (weights[index] != 0.0F) {
									sum = sum.add(scratch.getColor(x, Math.floorMod(y + offset, scratch.height)).mul(weights[index]));
								}
							}
							destination.setColor(x, y, carefulMix(mainColor, maybeSqrt(sum, linear), maskColor));
						}
						else {
							destination.setColor(x, y, mainColor);
						}
					}
				});
			}
			case CONCENTRIC -> {
				int minSize = Math.min(destination.width, destination.height);
				//int maxSize = Math.max(destination.width, destination.height);
				int imageR = (minSize + 1) >> 1;
				IntStream.range(0, imageR).parallel().forEach((int ring) -> {
					int pixelsInRing = Math.max(pixelsInRing(destination.width, destination.height, ring), 1);
					for (int start = 0; start < pixelsInRing; start++) {
						int packedPos = concentricPos(destination.width, destination.height, ring, start);
						int x = unpackX(packedPos);
						int y = unpackY(packedPos);
						FloatVector mainColor = main.getColor(x, y);
						FloatVector maskColor = mask.getColor(x, y);
						if (maskColor.compare(VectorOperators.GE, 0.0F).anyTrue()) {
							FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
							for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
								if (weights[index] != 0.0F) {
									packedPos = concentricPos(destination.width, destination.height, ring, Math.floorMod(start + offset, pixelsInRing));
									sum = sum.add(maybeSquare(main.getColor(unpackX(packedPos), unpackY(packedPos)), linear).mul(weights[index]));
								}
							}
							destination.setColor(x, y, carefulMix(mainColor, maybeSqrt(sum, linear), maskColor));
						}
						else {
							destination.setColor(x, y, mainColor);
						}
					}
				});
			}
			case SQUARE -> {
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < destination.width; x++) {
						FloatVector mainColor = main.getColor(x, y);
						FloatVector maskColor = mask.getColor(x, y);
						if (maskColor.compare(VectorOperators.GE, 0.0F).anyTrue()) {
							FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
							int index = 0;
							for (int offsetY = -radius; offsetY <= radius; offsetY++) {
								for (int offsetX = -radius; offsetX <= radius; offsetX++) {
									if (weights[index] != 0.0F) {
										sum = sum.add(maybeSquare(main.getColor(Math.floorMod(x + offsetX, destination.width), Math.floorMod(y + offsetY, destination.height)), linear).mul(weights[index]));
									}
									index++;
								}
							}
							destination.setColor(x, y, carefulMix(mainColor, maybeSqrt(sum, linear), maskColor));
						}
						else {
							destination.setColor(x, y, mainColor);
						}
					}
				});
			}
		}
		this.clampImage(destination);
	}

	public static int pixelsInRing(int width, int height, int ring) {
		return ((width + height - 2) << 1) - (ring << 3);
	}

	public static int concentricPos(int width, int height, int ring, int index) {
		int pixelsInWidth = width + ~(ring << 1);
		int pixelsInHeight = height + ~(ring << 1);
		if (index < pixelsInWidth) return pack(ring + index, ring);
		if ((index -= pixelsInWidth) < pixelsInHeight) return pack(width + ~ring, ring + index);
		if ((index -= pixelsInHeight) < pixelsInWidth) return pack(width + ~index - ring, height + ~ring);
		return pack(ring, height + ~(index - pixelsInWidth) - ring);
	}

	public static int pack(int x, int y) {
		return (y << 16) | (x & 0xFFFF);
	}

	public static int unpackX(int packed) {
		return packed & 0xFFFF;
	}

	public static int unpackY(int packed) {
		return packed >> 16;
	}

	public static enum ConvolveShape {
		HORIZONTAL,
		VERTICAL,
		SEPARABLE,
		CONCENTRIC,
		SQUARE;

		public static final ConvolveShape[] VALUES = values();

		public final String lowercaseName, titleCaseName;

		ConvolveShape() {
			this.lowercaseName = this.name().toLowerCase(Locale.ROOT);
			this.titleCaseName = Util.capitalize(this.lowercaseName);
		}

		@Override
		public String toString() {
			return this.titleCaseName;
		}
	}

	public static enum BlurWeight {
		BOX {

			@Override
			public float[] getWeights(ConvolveLayerSource source, boolean normalize) {
				int diameter = source.radius.getValue() * 2 + 1;
				float[] result = new float[diameter];
				Arrays.fill(result, normalize ? 1.0F / diameter : 1.0F);
				return result;
			}
		},
		GAUSSIAN {

			@Override
			public float[] getWeights(ConvolveLayerSource source, boolean normalize) {
				int diameter = source.radius.getValue() * 2 + 1;
				float[] result = new float[diameter];
				result[0] = 1.0F;
				for (int size = 1; size < diameter; size++) {
					//System.out.println("size: " + size + ", array: " + Arrays.toString(result));
					for (int index2 = size; index2 > 0; index2--) {
						int index1 = index2 - 1;
						result[index2] = (result[index2] + result[index1]) * 0.5F;
						//System.out.println("index2: " + index2 + ", array: " + Arrays.toString(result));
					}
					result[0] *= 0.5F;
					//System.out.println("index2: 0, array: " + Arrays.toString(result));
				}
				return result;
			}
		},
		CUSTOM {

			@Override
			public float[] getWeights(ConvolveLayerSource source, boolean normalize) {
				ObservableList<Node> children = source.customWeights.getChildren();
				int length = children.size();
				float[] weights = new float[length];
				float weightSum = 0.0F;
				for (int index = 0; index < length; index++) {
					weightSum += (weights[index] = ((Float)(((TextField)(children.get(index))).getTextFormatter().getValue())).floatValue());
				}
				if (normalize) {
					for (int index = 0; index < length; index++) {
						weights[index] /= weightSum;
					}
				}
				return weights;
			}
		};

		public static final BlurWeight[] VALUES = values();

		public final String lowercaseName, titleCaseName;

		BlurWeight() {
			this.lowercaseName = this.name().toLowerCase(Locale.ROOT);
			this.titleCaseName = Util.capitalize(this.lowercaseName);
		}

		@Override
		public String toString() {
			return this.titleCaseName;
		}

		public abstract float[] getWeights(ConvolveLayerSource source, boolean normalize);
	}
}