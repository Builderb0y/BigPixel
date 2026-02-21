package builderb0y.bigpixel.sources;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.JsonConverter.ConvolveSaveDataJsonConverter;
import builderb0y.bigpixel.scripting.parsing.ExpressionParser;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.FunctionHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptParsingException;
import builderb0y.bigpixel.scripting.tree.InsnTree;
import builderb0y.bigpixel.scripting.tree.VectorConstructorInsnTree;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.tools.FreehandTool;
import builderb0y.bigpixel.util.AggregateProperty;
import builderb0y.bigpixel.util.Result;
import builderb0y.bigpixel.util.Result.Failure;
import builderb0y.bigpixel.util.Result.Success;
import builderb0y.bigpixel.util.Util;

import static org.objectweb.asm.Opcodes.*;

public class ConvolveLayerSource extends MainMaskLayerSource {

	public static final int
		MAX_MANUAL_RADIUS = 5,
		MAX_MANUAL_DIAMETER = MAX_MANUAL_RADIUS * 2 + 1,
		MAX_MANUAL_AREA = MAX_MANUAL_DIAMETER * MAX_MANUAL_DIAMETER,
		MAX_PRESET_RADIUS = 256;

	public final CheckBox linear = this.parameters.addCheckbox("linear", "Linear", false);
	public final ChoiceBox<ConvolveShape> shape = new ChoiceBox<>(FXCollections.observableArrayList(ConvolveShape.VALUES));
	{ this.shape.setValue(ConvolveShape.SQUARE); }
	public final ChoiceBox<ConvolveWeightType> preset = new ChoiceBox<>(FXCollections.observableArrayList(ConvolveWeightType.VALUES));
	{ this.preset.setValue(ConvolveWeightType.MANUAL); }
	public final Spinner<Integer> radius = Util.setupSpinner(new Spinner<>(1, 3, 1, 1), 64.0D);
	public final TextField[] customWeights = new TextField[MAX_MANUAL_AREA];
	public final GridPane customWeightsView = new GridPane();
	public final CheckBox normalizeCustomWeights = this.parameters.addCheckbox("normalize", "Normalize", true);
	public final TextArea scriptedWeightsEditor = Util.setupCodeArea(new TextArea());
	public final ObservableValue<Result<WeightProvider, ScriptParsingException>> scriptedWeightProvider = this.scriptedWeightsEditor.textProperty().map((String text) -> {
		try {
			return Result.success(new WeightProviderScriptParser(text).addBuiltins().parse());
		}
		catch (ScriptParsingException exception) {
			return Result.failure(exception);
		}
	});
	public final SaveDataProperty saveDataProperty = this.new SaveDataProperty();
	public final ParameterMultiStorage<SaveData> saveDataStorage = new ParameterMultiStorage<>(this.saveDataProperty, this.sources.layer.graph.openImage.parameterSet);
	public final BorderPane weightConfigPane = new BorderPane();
	public final SplitPane splitPane = new SplitPane();

	public ConvolveLayerSource(LayerSources sources) {
		super(LayerSourceType.CONVOLVE, sources);
		this.commonSourceSettings.getChildren().add(this.linear);
		((IntegerSpinnerValueFactory)(this.radius.getValueFactory())).maxProperty().bind(
			Bindings.createIntegerBinding(() -> this.preset.getValue() == ConvolveWeightType.MANUAL ? MAX_MANUAL_RADIUS : MAX_PRESET_RADIUS, this.preset.valueProperty())
		);
		ChangeListener<Object> layout = Util.change(this::layoutCustomWeights);
		this.shape.valueProperty().addListener(layout);
		this.preset.valueProperty().addListener(layout);
		this.radius.valueProperty().addListener(layout);
		this.layoutCustomWeights();
		this.weightConfigPane.setTop(new HBox(this.shape, this.preset, this.radius, this.normalizeCustomWeights));
		ChangeListener<Object> redraw = Util.change(() -> {
			this.redrawLater();
			this.saveDataProperty.fireValueChangedEvent();
		});
		this.shape.valueProperty().addListener(redraw);
		this.preset.valueProperty().addListener(redraw);
		this.radius.valueProperty().addListener(redraw);
		this.scriptedWeightProvider.addListener(redraw);

		this.splitPane.setOrientation(Orientation.VERTICAL);
		this.rootConfigPane.setCenter(null);
		this.splitPane.getItems().setAll(this.dependencies.getConfigPane(), this.weightConfigPane);
		this.rootConfigPane.setCenter(this.splitPane);

		this.parameters.addParameter(new ConfigParameter<>(this.saveDataStorage, "weights", SaveData.class, ConvolveSaveDataJsonConverter.INSTANCE));
		ParameterSetTop top = sources.layer.graph.openImage.parameterSet;
		ConfigParameters.setupContextMenu(top, this.shape,  this.saveDataStorage);
		ConfigParameters.setupContextMenu(top, this.preset, this.saveDataStorage);
		ConfigParameters.setupContextMenu(top, this.radius, this.saveDataStorage);
		ConfigParameters.setupContextMenu(top, this.scriptedWeightsEditor, this.saveDataStorage);
	}

	public void layoutCustomWeights() {
		switch (this.preset.getValue()) {
			case BOX, GAUSSIAN, BOKEH -> {
				this.weightConfigPane.setCenter(null);
			}
			case MANUAL -> {
				this.customWeightsView.getChildren().clear();
				int radius = this.radius.getValue();
				switch (this.shape.getValue()) {
					case SQUARE -> {
						for (int y = -radius; y <= radius; y++) {
							for (int x = -radius; x <= radius; x++) {
								this.customWeightsView.add(this.getManualWeight(x, y), x + radius, y + radius);
							}
						}
					}
					case SEPARABLE, HORIZONTAL, CONCENTRIC -> {
						for (int x = -radius; x <= radius; x++) {
							this.customWeightsView.add(this.getManualWeight(x, 0), x + radius, radius);
						}
					}
					case VERTICAL -> {
						for (int y = -radius; y <= radius; y++) {
							this.customWeightsView.add(this.getManualWeight(0, y), radius, y + radius);
						}
					}
					case PLUS -> {
						this.customWeightsView.add(this.getManualWeight(0, 0), 0, 0);
						for (int outward = 1; outward < radius; outward++) {
							this.customWeightsView.add(this.getManualWeight(+outward, 0), +outward, 0);
							this.customWeightsView.add(this.getManualWeight(-outward, 0), -outward, 0);
							this.customWeightsView.add(this.getManualWeight(0, +outward), 0, +outward);
							this.customWeightsView.add(this.getManualWeight(0, -outward), 0, -outward);
						}
					}
					case X -> {
						this.customWeightsView.add(this.getManualWeight(0, 0), 0, 0);
						for (int outward = 1; outward < radius; outward++) {
							this.customWeightsView.add(this.getManualWeight(+outward, +outward), +outward, +outward);
							this.customWeightsView.add(this.getManualWeight(+outward, -outward), +outward, -outward);
							this.customWeightsView.add(this.getManualWeight(-outward, +outward), -outward, +outward);
							this.customWeightsView.add(this.getManualWeight(-outward, -outward), -outward, -outward);
						}
					}
					case STAR -> {
						this.customWeightsView.add(this.getManualWeight(0, 0), 0, 0);
						for (int outward = 1; outward < radius; outward++) {
							this.customWeightsView.add(this.getManualWeight(+outward, 0), +outward, 0);
							this.customWeightsView.add(this.getManualWeight(-outward, 0), -outward, 0);
							this.customWeightsView.add(this.getManualWeight(0, +outward), 0, +outward);
							this.customWeightsView.add(this.getManualWeight(0, -outward), 0, -outward);
							this.customWeightsView.add(this.getManualWeight(+outward, +outward), +outward, +outward);
							this.customWeightsView.add(this.getManualWeight(+outward, -outward), +outward, -outward);
							this.customWeightsView.add(this.getManualWeight(-outward, +outward), -outward, +outward);
							this.customWeightsView.add(this.getManualWeight(-outward, -outward), -outward, -outward);
						}
					}
				}
				this.weightConfigPane.setCenter(this.customWeightsView);
			}
			case SCRIPTED -> {
				this.weightConfigPane.setCenter(this.scriptedWeightsEditor);
			}
		}
	}

	public float getManualWeightValue(int x, int y) {
		return ((Float)(this.getManualWeight(x, y).getTextFormatter().getValue())).floatValue();
	}

	public TextField getManualWeight(int x, int y) {
		int index = (y + MAX_MANUAL_RADIUS) * MAX_MANUAL_DIAMETER + (x + MAX_MANUAL_RADIUS);
		TextField weight = this.customWeights[index];
		if (weight == null) weight = this.customWeights[index] = this.createManualWeight(x, y);
		return weight;
	}

	public TextField createManualWeight(int x, int y) {
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
			x == 0 && y == 0 ? 1.0F : 0.0F
		));
		textField.setPrefWidth(48.0D);
		textField.setMinWidth(48.0D);
		textField.setOnScroll((ScrollEvent event) -> {
			boolean up;
			if (event.getDeltaY() > 0.0D) up = true;
			else if (event.getDeltaY() < 0.0D) up = false;
			else return;
			@SuppressWarnings("unchecked")
			TextFormatter<Float> formatter = (TextFormatter<Float>)(textField.getTextFormatter());
			float oldValue = formatter.getValue().floatValue();
			formatter.setValue(up ? ((float)(Math.floor(oldValue))) + 1.0F : ((float)(Math.ceil(oldValue))) - 1.0F);
		});
		textField.getTextFormatter().valueProperty().addListener(Util.change(() -> {
			this.redrawLater();
			this.saveDataProperty.fireValueChangedEvent();
		}));
		ConfigParameters.setupContextMenu(this.sources.layer.graph.openImage.parameterSet, textField, this.saveDataStorage);
		return textField;
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
		switch (main) {
			case UniformSampler uniform -> {
				FloatVector color = uniform.getColor();
				for (int index = 0; index < destination.pixels.length; index += 4) {
					color.intoArray(destination.pixels, index);
				}
			}
			case VaryingSampler varying -> {
				ConvolveWeightType type = this.preset.getValue();
				WeightProvider weightProvider = type != ConvolveWeightType.SCRIPTED ? null : switch (this.scriptedWeightProvider.getValue()) {
					case Success(WeightProvider provider) -> provider;
					case Failure(ScriptParsingException exception) -> throw new RedrawException(exception.getLocalizedMessage());
				};
				int radius = this.radius.getValue();
				class Helper {

					public final PackedWeightList weights;

					public Helper(int total) {
						this.weights = new PackedWeightList(total);
					}

					public void addPos(int x, int y) throws RedrawException {
						this.weights.add(x, y, switch (type) {
							case BOX -> 1.0F / this.weights.values.length;
							case GAUSSIAN -> {
								float[] gaussian = gaussian(radius);
								yield gaussian[x + radius] * gaussian[y + radius];
							}
							case BOKEH -> x * x + y * y < Util.square(radius + 0.5F) ? 1.0F : 0.0F;
							case MANUAL -> ConvolveLayerSource.this.getManualWeightValue(x, y);
							case SCRIPTED -> weightProvider.getWeight(x, y, radius);
						});
					}
				}
				switch (this.shape.getValue()) {
					case SQUARE -> {
						int diameter = radius * 2 + 1;
						int area = diameter * diameter;
						Helper helper = new Helper(area);
						for (int y = -radius; y <= radius; y++) {
							for (int x = -radius; x <= radius; x++) {
								helper.addPos(x, y);
							}
						}
						this.convolve(main, destination, helper.weights);
					}
					case SEPARABLE -> {
						int diameter = radius * 2 + 1;
						Helper helper = new Helper(diameter);
						HDRImage scratch = new HDRImage(destination.width, destination.height);
						for (int x = -radius; x <= radius; x++) {
							helper.addPos(x, 0);
						}
						this.convolve(main, scratch, helper.weights);
						helper.weights.clear();
						for (int y = -radius; y <= radius; y++) {
							helper.addPos(0, y);
						}
						this.convolve(VaryingSampler.of(varying.getBackingLayer(), scratch), destination, helper.weights);
					}
					case HORIZONTAL -> {
						int diameter = radius * 2 + 1;
						Helper helper = new Helper(diameter);
						for (int x = -radius; x <= radius; x++) {
							helper.addPos(x, 0);
						}
						this.convolve(main, destination, helper.weights);
					}
					case VERTICAL -> {
						int diameter = radius * 2 + 1;
						Helper helper = new Helper(diameter);
						for (int y = -radius; y <= radius; y++) {
							helper.addPos(0, y);
						}
						this.convolve(main, destination, helper.weights);
					}
					case PLUS -> {
						int count = radius * 4 + 1;
						Helper helper = new Helper(count);
						helper.addPos(0, 0);
						for (int r = 1; r <= radius; r++) {
							helper.addPos(+r,  0);
							helper.addPos(-r,  0);
							helper.addPos( 0, +r);
							helper.addPos( 0, -r);
						}
						this.convolve(main, destination, helper.weights);
					}
					case X -> {
						int count = radius * 4 + 1;
						Helper helper = new Helper(count);
						helper.addPos(0, 0);
						for (int r = 1; r <= radius; r++) {
							helper.addPos(+r, +r);
							helper.addPos(+r, -r);
							helper.addPos(-r, +r);
							helper.addPos(-r, -r);
						}
						this.convolve(main, destination, helper.weights);
					}
					case STAR -> {
						int count = radius * 8 + 1;
						Helper helper = new Helper(count);
						helper.addPos(0, 0);
						for (int r = 1; r <= radius; r++) {
							helper.addPos(+r,  0);
							helper.addPos(-r,  0);
							helper.addPos( 0, +r);
							helper.addPos( 0, -r);
							helper.addPos(+r, +r);
							helper.addPos(+r, -r);
							helper.addPos(-r, +r);
							helper.addPos(-r, -r);
						}
						this.convolve(main, destination, helper.weights);
					}
					case CONCENTRIC -> {
						int diameter = radius * 2 + 1;
						Helper helper = new Helper(diameter);
						for (int x = -radius; x <= radius; x++) {
							helper.addPos(x, 0);
						}
						if (this.normalizeCustomWeights.isSelected()) {
							helper.weights.normalize();
						}
						int minSize = Math.min(destination.width, destination.height);
						int imageR = (minSize + 1) >> 1;
						boolean linear = this.linear.isSelected();
						IntStream.range(0, imageR).parallel().forEach((int ring) -> {
							int pixelsInRing = Math.max(pixelsInRing(destination.width, destination.height, ring), 1);
							for (int start = 0; start < pixelsInRing; start++) {
								int packedPos = concentricPos(destination.width, destination.height, ring, start);
								int x = unpackRingX(packedPos);
								int y = unpackRingY(packedPos);
								FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
								for (int index = 0; index < helper.weights.size(); index++) {
									int offset = helper.weights.getX(index);
									float weight = helper.weights.getWeight(index);
									packedPos = concentricPos(destination.width, destination.height, ring, Math.floorMod(start + offset, pixelsInRing));
									sum = sum.add(maybeSquare(main.getColor(unpackRingX(packedPos), unpackRingY(packedPos)), linear).mul(weight));
								}
								destination.setColor(x, y, maybeSqrt(sum, linear));
							}
						});
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
		if (index < pixelsInWidth) return packRing(ring + index, ring);
		if ((index -= pixelsInWidth) < pixelsInHeight) return packRing(width + ~ring, ring + index);
		if ((index -= pixelsInHeight) < pixelsInWidth) return packRing(width + ~index - ring, height + ~ring);
		return packRing(ring, height + ~(index - pixelsInWidth) - ring);
	}

	public static int packRing(int x, int y) {
		return (y << 16) | (x & 0xFFFF);
	}

	public static int unpackRingX(int packed) {
		return packed & 0xFFFF;
	}

	public static int unpackRingY(int packed) {
		return packed >>> 16;
	}

	public void convolve(Sampler in, HDRImage out, PackedWeightList weights) throws RedrawException {
		if (this.normalizeCustomWeights.isSelected()) {
			weights.normalize();
		}
		boolean linear = this.linear.isSelected();
		IntStream.range(0, out.height).parallel().forEach((int y) -> {
			for (int x = 0; x < out.width; x++) {
				FloatVector sum = FloatVector.zero(FloatVector.SPECIES_128);
				for (int index = 0, size = weights.size(); index < size; index++) {
					sum = sum.add(maybeSquare(in.getColor(Math.floorMod(x + weights.getX(index), out.width), Math.floorMod(y + weights.getY(index), out.height)), linear).mul(weights.getWeight(index)));
				}
				out.setColor(x, y, maybeSqrt(sum, linear));
			}
		});
	}

	public static FloatVector maybeSquare(FloatVector color, boolean linear) {
		return linear ? color.mul(color, Util.RGB_MASK) : color;
	}

	public static FloatVector maybeSqrt(FloatVector color, boolean linear) {
		return linear ? color.lanewise(VectorOperators.SQRT, Util.RGB_MASK) : color;
	}

	public static float[] box(int radius) {
		class Cache {

			public static final float[][] CACHE = new float[MAX_PRESET_RADIUS + 1][];
		}
		float[] result = Cache.CACHE[radius];
		if (result == null) {
			int diameter = radius * 2 + 1;
			result = new float[diameter];
			Arrays.fill(result, 1.0F / diameter);
			Cache.CACHE[radius] = result;
		}
		return result;
	}

	public static float[] gaussian(int radius) {
		class Cache {

			public static final float[][] CACHE = new float[MAX_PRESET_RADIUS + 1][];
			static {
				CACHE[0] = new float[] { 1.0F };
			}
		}
		float[] result = Cache.CACHE[radius];
		if (result == null) {
			int diameter = radius * 2 + 1;
			result = Arrays.copyOf(gaussian(radius - 1), diameter);
			for (int size = diameter - 2; size < diameter; size++) {
				for (int index = size; index > 0;) {
					result[index] = (result[index] + result[--index]) * 0.5F;
				}
				result[0] *= 0.5F;
			}
			Cache.CACHE[radius] = result;
		}
		return result;
	}

	@FunctionalInterface
	public static interface WeightProvider {

		public abstract float getWeight(int X, int Y, int R, float x, float y, float r) throws Throwable;

		public default float getWeight(int X, int Y, int R) throws RedrawException {
			float rcpR = 1.0F / R;
			try {
				return this.getWeight(X, Y, R, X * rcpR, Y * rcpR, R);
			}
			catch (Throwable throwable) {
				throw new RedrawException(throwable.getLocalizedMessage());
			}
		}
	}

	public static class WeightProviderScriptParser extends ExpressionParser<WeightProvider> {

		public WeightProviderScriptParser(String source) {
			super(source, WeightProvider.class);
		}

		@Override
		public WeightProviderScriptParser addBuiltins() {
			super.addBuiltins();
			for (VectorType type : VectorType.UNITS) {
				this.scope.environment.addFunction(type.name, (ExpressionParser<?> parser, String name, InsnTree[] params) -> {
					VectorType[] types = InsnTree.flattenTypes(params);
					String fullName = name + "_from_" + Arrays.stream(types).map((VectorType paramType) -> paramType.name).collect(Collectors.joining("_"));
					try {
						MethodInfo model = new MethodInfo(VectorOperations.class, fullName);
						return new VectorConstructorInsnTree(type, params, model);
					}
					catch (IllegalArgumentException exception) {
						return null;
					}
				});
			}
			this.scope.environment.addKeyword("if", KeywordHandler.makeIf());
			this.scope.environment.addKeyword("unless", KeywordHandler.makeIf());
			this.scope.environment.addKeyword("switch", KeywordHandler.switcher());
			this.scope.environment.addKeyword("return", KeywordHandler.returner(VectorType.FLOAT));
			this.scope.environment.addFunction("smooth", FunctionHandler.invoker(new MethodInfo(FreehandTool.class, "smooth")));
			this.scope.environment.addFunction("smoother", FunctionHandler.invoker(new MethodInfo(FreehandTool.class, "smoother")));
			this.scope.environment.addVariable("X", VariableHandler.builtinParameter("X", VectorType.INT));
			this.scope.environment.addVariable("Y", VariableHandler.builtinParameter("Y", VectorType.INT));
			this.scope.environment.addVariable("R", VariableHandler.builtinParameter("R", VectorType.INT));
			this.scope.environment.addVariable("x", VariableHandler.builtinParameter("x", VectorType.FLOAT));
			this.scope.environment.addVariable("y", VariableHandler.builtinParameter("y", VectorType.FLOAT));
			this.scope.environment.addVariable("r", VariableHandler.builtinParameter("r", VectorType.FLOAT));
			return this;
		}

		@Override
		public ClassNode parseBasic() throws ScriptParsingException {
			ClassNode clazz = super.parseBasic();
			MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
			constructor.visitVarInsn(ALOAD, 0);
			constructor.visitMethodInsn(INVOKESPECIAL, clazz.superName, "<init>", "()V", false);
			constructor.visitInsn(RETURN);
			clazz.methods.addFirst(constructor);
			return clazz;
		}

		public WeightProvider parse() throws ScriptParsingException {
			ClassNode clazz = this.parseBasic();
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			clazz.accept(writer);
			byte[] bytes = writer.toByteArray();
			try {
				MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);
				return (WeightProvider)(lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke());
			}
			catch (Throwable throwable) {
				StringWriter stringWriter = new StringWriter(1024);
				PrintWriter printWriter = new PrintWriter(stringWriter);
				TraceClassVisitor tracer = new TraceClassVisitor(printWriter);
				clazz.accept(tracer);
				ScriptParsingException toThrow = new ScriptParsingException("Exception defining class!\n\n" + stringWriter, throwable, null);
				toThrow.printStackTrace();
				throw toThrow;
			}
		}
	}

	public static enum ConvolveShape {
		SQUARE,
		SEPARABLE,
		HORIZONTAL,
		VERTICAL,
		PLUS,
		X,
		STAR,
		CONCENTRIC;

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

	public static enum ConvolveWeightType {
		BOX,
		GAUSSIAN,
		BOKEH,
		MANUAL,
		SCRIPTED;

		public static final ConvolveWeightType[] VALUES = values();

		public final String lowercaseName, titleCaseName;

		ConvolveWeightType() {
			this.lowercaseName = this.name().toLowerCase(Locale.ROOT);
			this.titleCaseName = Util.capitalize(this.lowercaseName);
		}

		@Override
		public String toString() {
			return this.titleCaseName;
		}
	}

	public static class PackedWeightList {

		public final long[] values;
		public int size;

		public PackedWeightList() {
			this.values = new long[16];
		}

		public PackedWeightList(int initialCapacity) {
			this.values = new long[initialCapacity];
		}

		public int size() {
			return this.size;
		}

		public void add(int x, int y, float value) {
			if (value != 0.0F && !Float.isNaN(value)) {
				this.set(this.size++, x, y, value);
			}
		}

		public void set(int index, int x, int y, float value) {
			this.values[index] = (((long)(y)) << 48) | ((x & 0xFFFFL) << 32) | Integer.toUnsignedLong(Float.floatToRawIntBits(value));
		}

		public void normalize() {
			float totalWeight = 0.0F;
			for (int index = 0; index < this.size; index++) {
				totalWeight += this.getWeight(index);
			}
			float scalar = 1.0F / totalWeight;
			for (int index = 0; index < this.size; index++) {
				long packed = this.values[index];
				int bits = (int)(packed);
				float value = Float.intBitsToFloat(bits);
				value *= scalar;
				bits = Float.floatToRawIntBits(value);
				packed = (packed & 0xFFFF_FFFF_0000_0000L) | Integer.toUnsignedLong(bits);
				this.values[index] = packed;
			}
		}

		public void clear() {
			this.size = 0;
		}

		public int getX(int index) {
			return (short)(this.values[index] >> 32);
		}

		public int getY(int index) {
			return (int)(this.values[index] >> 48);
		}

		public float getWeight(int index) {
			return Float.intBitsToFloat((int)(this.values[index]));
		}
	}

	public static abstract sealed class SaveData {

		public final ConvolveShape shape;
		public final int radius;

		public SaveData(ConvolveShape shape, int radius) {
			this.shape = shape;
			this.radius = radius;
		}

		public abstract ConvolveWeightType type();
	}

	public static non-sealed class BoxSaveData extends SaveData {

		public BoxSaveData(ConvolveShape shape, int radius) {
			super(shape, radius);
		}

		@Override
		public ConvolveWeightType type() {
			return ConvolveWeightType.BOX;
		}
	}

	public static non-sealed class GaussianSaveData extends SaveData {

		public GaussianSaveData(ConvolveShape shape, int radius) {
			super(shape, radius);
		}

		@Override
		public ConvolveWeightType type() {
			return ConvolveWeightType.GAUSSIAN;
		}
	}

	public static non-sealed class BokehSaveData extends SaveData {

		public BokehSaveData(ConvolveShape shape, int radius) {
			super(shape, radius);
		}

		@Override
		public ConvolveWeightType type() {
			return ConvolveWeightType.BOKEH;
		}
	}

	public static non-sealed class ManualSaveData extends SaveData {

		public final PackedWeightList weights;

		public ManualSaveData(ConvolveShape shape, int radius, PackedWeightList weights) {
			super(shape, radius);
			this.weights = weights;
		}

		@Override
		public ConvolveWeightType type() {
			return ConvolveWeightType.MANUAL;
		}
	}

	public static non-sealed class ScriptedSaveData extends SaveData {

		public final String scriptSource;

		public ScriptedSaveData(ConvolveShape shape, int radius, String scriptSource) {
			super(shape, radius);
			this.scriptSource = scriptSource;
		}

		@Override
		public ConvolveWeightType type() {
			return ConvolveWeightType.SCRIPTED;
		}
	}

	public class SaveDataProperty extends AggregateProperty<SaveData> {

		@Override
		public SaveData get() {
			ConvolveShape shape = ConvolveLayerSource.this.shape.getValue();
			int radius = ConvolveLayerSource.this.radius.getValue();
			return switch (ConvolveLayerSource.this.preset.getValue()) {
				case BOX -> new BoxSaveData(shape, radius);
				case GAUSSIAN -> new GaussianSaveData(shape, radius);
				case BOKEH -> new BokehSaveData(shape, radius);
				case MANUAL -> new ManualSaveData(shape, radius, switch (shape) {
					case SQUARE -> {
						int diameter = radius * 2 + 1;
						int area = diameter * diameter;
						PackedWeightList weights = new PackedWeightList(area);
						for (int y = -radius; y <= radius; y++) {
							for (int x = -radius; x <= radius; x++) {
								weights.add(x, y, ConvolveLayerSource.this.getManualWeightValue(x, y));
							}
						}
						yield weights;
					}
					case SEPARABLE, HORIZONTAL, CONCENTRIC -> {
						int diameter = radius * 2 + 1;
						PackedWeightList weights = new PackedWeightList(diameter);
						for (int x = -radius; x <= radius; x++) {
							weights.add(x, 0, ConvolveLayerSource.this.getManualWeightValue(x, 0));
						}
						yield weights;
					}
					case VERTICAL -> {
						int diameter = radius * 2 + 1;
						PackedWeightList weights = new PackedWeightList(diameter);
						for (int y = -radius; y <= radius; y++) {
							weights.add(0, y, ConvolveLayerSource.this.getManualWeightValue(0, y));
						}
						yield weights;
					}
					case PLUS -> {
						int count = radius * 4 + 1;
						PackedWeightList weights = new PackedWeightList(count);
						weights.add(0, 0, ConvolveLayerSource.this.getManualWeightValue(0, 0));
						for (int r = 1; r <= radius; r++) {
							weights.add(-r,  0, ConvolveLayerSource.this.getManualWeightValue(-r,  0));
							weights.add(+r,  0, ConvolveLayerSource.this.getManualWeightValue(+r,  0));
							weights.add( 0, -r, ConvolveLayerSource.this.getManualWeightValue( 0, -r));
							weights.add( 0, +r, ConvolveLayerSource.this.getManualWeightValue( 0, +r));
						}
						yield weights;
					}
					case X -> {
						int count = radius * 4 + 1;
						PackedWeightList weights = new PackedWeightList(count);
						weights.add(0, 0, ConvolveLayerSource.this.getManualWeightValue(0, 0));
						for (int r = 1; r <= radius; r++) {
							weights.add(-r, -r, ConvolveLayerSource.this.getManualWeightValue(-r, -r));
							weights.add(-r, +r, ConvolveLayerSource.this.getManualWeightValue(-r, +r));
							weights.add(+r, -r, ConvolveLayerSource.this.getManualWeightValue(+r, -r));
							weights.add(+r, +r, ConvolveLayerSource.this.getManualWeightValue(+r, +r));
						}
						yield weights;
					}
					case STAR -> {
						int count = radius * 8 + 1;
						PackedWeightList weights = new PackedWeightList(count);
						weights.add(0, 0, ConvolveLayerSource.this.getManualWeightValue(0, 0));
						for (int r = 1; r <= radius; r++) {
							weights.add(-r,  0, ConvolveLayerSource.this.getManualWeightValue(-r,  0));
							weights.add(+r,  0, ConvolveLayerSource.this.getManualWeightValue(+r,  0));
							weights.add( 0, -r, ConvolveLayerSource.this.getManualWeightValue( 0, -r));
							weights.add( 0, +r, ConvolveLayerSource.this.getManualWeightValue( 0, +r));
							weights.add(-r, -r, ConvolveLayerSource.this.getManualWeightValue(-r, -r));
							weights.add(-r, +r, ConvolveLayerSource.this.getManualWeightValue(-r, +r));
							weights.add(+r, -r, ConvolveLayerSource.this.getManualWeightValue(+r, -r));
							weights.add(+r, +r, ConvolveLayerSource.this.getManualWeightValue(+r, +r));
						}
						yield weights;
					}
				});
				case SCRIPTED -> {
					yield new ScriptedSaveData(shape, radius, ConvolveLayerSource.this.scriptedWeightsEditor.getText());
				}
			};
		}

		@Override
		@SuppressWarnings("unchecked")
		public void doSet(SaveData value) {
			ConvolveLayerSource.this.shape.setValue(value.shape);
			ConvolveLayerSource.this.preset.setValue(value.type());
			ConvolveLayerSource.this.radius.getValueFactory().setValue(value.radius);
			switch (value) {
				case BoxSaveData _ -> {}
				case GaussianSaveData _ -> {}
				case BokehSaveData _ -> {}
				case ManualSaveData manual -> {
					for (TextField editor : ConvolveLayerSource.this.customWeights) {
						if (editor != null) {
							(
								(TextFormatter<Float>)(
									editor.getTextFormatter()
								)
							)
							.setValue(0.0F);
						}
					}
					for (int index = 0; index < manual.weights.size(); index++) {
						(
							(TextFormatter<Float>)(
								ConvolveLayerSource.this.getManualWeight(
									manual.weights.getX(index),
									manual.weights.getY(index)
								)
								.getTextFormatter()
							)
						)
						.setValue(manual.weights.getWeight(index));
					}
				}
				case ScriptedSaveData scripted -> {
					ConvolveLayerSource.this.scriptedWeightsEditor.setText(scripted.scriptSource);
				}
			}
		}

		@Override
		public Object getBean() {
			return ConvolveLayerSource.this;
		}

		@Override
		public String getName() {
			return "saveDataProperty";
		}
	}
}