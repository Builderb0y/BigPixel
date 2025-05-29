package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.Locale;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonArray;
import builderb0y.notgimp.json.JsonMap;

public class ConvolveLayerSource extends SingleInputEffectLayerSource {

	public BorderPane borderPane = new BorderPane();
	public ChoiceBox<ConvolveShape> shape = new ChoiceBox<>(); { this.shape.getItems().addAll(ConvolveShape.VALUES); this.shape.setValue(ConvolveShape.SQUARE); }
	public ChoiceBox<BlurWeight> weight = new ChoiceBox<>(); { this.weight.getItems().addAll(BlurWeight.VALUES); this.weight.setValue(BlurWeight.CUSTOM); }
	public Spinner<Integer> radius = Util.setupSpinner(new Spinner<>(1, 3, 1, 1), 64.0D);
	public GridPane customWeights = new GridPane();
	public HDRImage separableScratch;

	@Override
	public JsonMap save() {
		JsonMap map = (
			new JsonMap()
			.with("type", "convolve")
			.with("shape", this.shape.getValue().name())
			.with("weight_type", this.weight.getValue().name())
			.with("radius", this.radius.getValue())
		);
		float[] weights = BlurWeight.CUSTOM.getWeights(this);
		JsonArray jsonWeights = new JsonArray(weights.length);
		for (float weight : weights) jsonWeights.add(weight);
		return map.with("custom_weights", jsonWeights);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void load(JsonMap map) {
		this.shape.setValue(ConvolveShape.valueOf(map.getString("shape")));
		this.weight.setValue(BlurWeight.valueOf(map.getString("weight_type")));
		this.radius.getValueFactory().setValue(map.getInt("radius"));
		this.layout();
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

	public ConvolveLayerSource(LayerSources sources) {
		super(sources, "Convolve");
		this.layout();
		this.borderPane.setTop(new HBox(this.shape, this.weight, this.radius));
		this.borderPane.setCenter(this.customWeights);
		this.rootNode.setCenter(this.borderPane);
	}

	@Override
	public void init(boolean fromSave) {
		ChangeListener<Object> listener = Util.change(() -> {
			this.layout();
			this.requestRedraw();
		});
		this.shape.valueProperty().addListener(listener);
		this.weight.valueProperty().addListener(listener);
		this.radius.valueProperty().addListener(listener);
		super.init(fromSave);
	}

	@SuppressWarnings("unchecked")
	public void copyFrom(ConvolveLayerSource that) {
		this.shape.setValue(that.shape.getValue());
		this.weight.setValue(that.weight.getValue());
		this.radius.getValueFactory().setValue(that.radius.getValueFactory().getValue());
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
		int radius = this.radius.getValue();
		int diameter = radius * 2 + 1;
		int sizeX = 1, sizeY = 1;
		boolean editable = false;
		float[] weights;
		switch (this.shape.getValue()) {
			case HORIZONTAL, SEPARABLE, CONCENTRIC -> {
				sizeX = diameter;
				weights = switch (this.weight.getValue()) {
					case BOX, GAUSSIAN -> this.weight.getValue().getWeights(this);
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
					case BOX, GAUSSIAN -> this.weight.getValue().getWeights(this);
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
		this.customWeights.getChildren().clear();
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
		textField.setPrefWidth(40.0D);
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
				this.requestRedraw();
			}
		}));
		return textField;
	}

	public HDRImage getSeparableScratch() {
		if (this.separableScratch == null) {
			HDRImage from = this.sources.layer.image;
			this.separableScratch = new HDRImage(from.width, from.height);
		}
		return this.separableScratch;
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		HDRImage destination = this.sources.layer.image;
		int radius = this.radius.getValue();
		float[] weights = this.weight.getValue().getWeights(this);
		switch (this.shape.getValue()) {
			case HORIZONTAL -> {
				for (int y = 0; y < source.height; y++) {
					for (int x = 0; x < source.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							sum = sum.add(source.getPixel(Math.floorMod(x + offset, source.width), y).mul(weights[index]));
						}
						destination.setPixel(x, y, sum);
					}
				}
			}
			case VERTICAL -> {
				for (int y = 0; y < source.height; y++) {
					for (int x = 0; x < source.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							sum = sum.add(source.getPixel(x, Math.floorMod(y + offset, source.height)).mul(weights[index]));
						}
						destination.setPixel(x, y, sum);
					}
				}
			}
			case SEPARABLE -> {
				HDRImage scratch = this.getSeparableScratch();
				for (int y = 0; y < source.height; y++) {
					for (int x = 0; x < source.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							sum = sum.add(source.getPixel(Math.floorMod(x + offset, source.width), y).mul(weights[index]));
						}
						scratch.setPixel(x, y, sum);
					}
				}
				for (int y = 0; y < scratch.height; y++) {
					for (int x = 0; x < scratch.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							sum = sum.add(scratch.getPixel(x, Math.floorMod(y + offset, scratch.height)).mul(weights[index]));
						}
						destination.setPixel(x, y, sum);
					}
				}
			}
			case CONCENTRIC -> {
				int minSize = Math.min(source.width, source.height);
				//int maxSize = Math.max(source.width, source.height);
				int imageR = (minSize + 1) >> 1;
				for (int ring = 0; ring < imageR; ring++) {
					int pixelsInRing = Math.max(pixelsInRing(source.width, source.height, ring), 1);
					for (int start = 0; start < pixelsInRing; start++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						for (int offset = -radius, index = 0; offset <= radius; offset++, index++) {
							int packedPos = concentricPos(source.width, source.height, ring, Math.floorMod(start + offset, pixelsInRing));
							sum = sum.add(source.getPixel(unpackX(packedPos), unpackY(packedPos)).mul(weights[index]));
						}
						int packedPos = concentricPos(source.width, source.height, ring, start);
						destination.setPixel(unpackX(packedPos), unpackY(packedPos), sum);
					}
				}
			}
			case SQUARE -> {
				for (int y = 0; y < source.height; y++) {
					for (int x = 0; x < source.width; x++) {
						FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
						int index = 0;
						for (int offsetY = -radius; offsetY <= radius; offsetY++) {
							for (int offsetX = -radius; offsetX <= radius; offsetX++) {
								sum = sum.add(source.getPixel(Math.floorMod(x + offsetX, source.width), Math.floorMod(y + offsetY, source.height)).mul(weights[index++]));
							}
						}
						destination.setPixel(x, y, sum);
					}
				}
			}
		}
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
			public float[] getWeights(ConvolveLayerSource source) {
				int diameter = source.radius.getValue() * 2 + 1;
				float[] result = new float[diameter];
				Arrays.fill(result, 1.0F / diameter);
				return result;
			}
		},
		GAUSSIAN {

			@Override
			public float[] getWeights(ConvolveLayerSource source) {
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
			public float[] getWeights(ConvolveLayerSource source) {
				ObservableList<Node> children = source.customWeights.getChildren();
				int length = children.size();
				float[] weights = new float[length];
				float weightSum = 0.0F;
				for (int index = 0; index < length; index++) {
					weightSum += (weights[index] = ((Float)(((TextField)(children.get(index))).getTextFormatter().getValue())).floatValue());
				}
				for (int index = 0; index < length; index++) {
					weights[index] /= weightSum;
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

		public abstract float[] getWeights(ConvolveLayerSource source);
	}
}