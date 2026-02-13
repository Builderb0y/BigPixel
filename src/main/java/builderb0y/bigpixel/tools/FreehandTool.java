package builderb0y.bigpixel.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.scripting.parsing.ExpressionParser;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.FunctionHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.bigpixel.scripting.parsing.ScriptParsingException;
import builderb0y.bigpixel.scripting.tree.InsnTree;
import builderb0y.bigpixel.scripting.tree.ReturnInsnTree;
import builderb0y.bigpixel.scripting.types.UtilityOperations;
import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.util.MethodInfo;
import builderb0y.bigpixel.sources.LayerSource.RedrawException;
import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.util.RateLimiter;
import builderb0y.bigpixel.util.RateLimiter.NonPeriodicRateLimiter;
import builderb0y.bigpixel.util.Result;
import builderb0y.bigpixel.util.Result.Failure;
import builderb0y.bigpixel.util.Result.Success;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;

import static org.objectweb.asm.Opcodes.*;

public class FreehandTool extends Tool<FreehandTool.Work> {

	public static final ToolType TYPE = new ToolType("freehand", Assets.Tools.FREEHAND, 9.0D, 9.0D);

	public static final SimpleObjectProperty<String> globalCurveScriptSource = new SimpleObjectProperty<>(FreehandTool.class, "globalCurveScriptSource", "smooth(1.0 - distance / 2.0)");
	public static final ObjectBinding<Result<BrushCurveScript, ScriptParsingException>> globalCurveScript = Bindings.createObjectBinding(
		() -> {
			try {
				return Result.success(new BrushCurveScriptParser("return " + globalCurveScriptSource.get() + ';').addBuiltins().parse());
			}
			catch (ScriptParsingException exception) {
				return Result.failure(exception);
			}
		},
		globalCurveScriptSource
	);

	public CheckBox brushEnabled = new CheckBox("Brush Curve: ");
	public TextField brushCurve = new TextField();
	public Spinner<Integer> smoothSteps = Util.setupSpinner(new Spinner<>(0, 100, 0), 256.0D);
	public CheckBox linear = new CheckBox("Linear Brush Blend");
	public GridPane configPane = new GridPane();
	public RateLimiter redrawer = new NonPeriodicRateLimiter(500L, this::requestRedraw);

	public FreehandTool(ManualLayerSource source) {
		super(TYPE, source);
		this.brushCurve.textProperty().bindBidirectional(globalCurveScriptSource);
		this.brushCurve.textProperty().addListener(Util.change(this.redrawer));
		BooleanBinding noBrush = this.brushEnabled.selectedProperty().not();
		this.brushCurve.disableProperty().bind(noBrush);
		this.linear.disableProperty().bind(noBrush);
		this.brushCurve.setPrefWidth(256.0D);
		ChangeListener<Object> fastRedraw = Util.change(this::requestRedraw);
		this.brushEnabled.selectedProperty().addListener(fastRedraw);
		this.smoothSteps.valueProperty().addListener(Util.change((Number _) -> {
			if (this.work != null) this.work.modified = true;
			this.requestRedraw();
		}));
		this.linear.selectedProperty().addListener(fastRedraw);

		this.configPane.add(this.brushEnabled, 0, 0);
		this.configPane.add(this.brushCurve, 1, 0);
		Label smoothStepsLabel = new Label("Smooth Steps: ");
		this.configPane.add(smoothStepsLabel, 0, 1);
		GridPane.setHalignment(smoothStepsLabel, HPos.RIGHT);
		this.configPane.add(this.smoothSteps, 1, 1);
		this.configPane.add(this.linear, 0, 2, 2, 1);
	}

	@Override
	public void onMouseDown(ProjectionResult position, MouseButton button) {
		if (this.work == null) {
			this.source.beginUsingTool();
			LayerNode layer = this.layer();
			this.work = new Work(layer.imageWidth(), layer.imageHeight());
		}
		if (button == MouseButton.PRIMARY) this.work.addCurve();
		this.handleMouse(position, button);
	}

	@Override
	public void onMouseDragged(ProjectionResult position, MouseButton button) {
		this.handleMouse(position, button);
	}

	public void handleMouse(ProjectionResult position, MouseButton button) {
		LayerNode layer = this.layer();
		if (button == MouseButton.PRIMARY) {
			if (this.brushEnabled.isSelected()) {
				this.work.addPoint(position.rawX(), position.rawY());
			}
			else {
				this.work.addPoint(position.x(), position.y());
			}
			layer.requestRedraw();
			this.updateLabelText();
		}
		else if (button == MouseButton.SECONDARY) {
			if (this.brushEnabled.isSelected()) {
				this.work.removePoint(position.rawX(), position.rawY());
			}
			else {
				this.work.removePoint(position.x(), position.y());
			}
			layer.requestRedraw();
			this.updateLabelText();
		}
	}

	@Override
	public void colorChanged() {
		if (this.work != null) this.requestRedraw();
	}

	@Override
	public void redraw() throws RedrawException {
		Work work = this.work;
		if (work == null) return;
		LayerNode layer = this.layer();
		FloatVector color = layer.graph.openImage.mainWindow.colorPicker.currentColor.rgba.get();
		HDRImage destination = layer.getOnlyFrame();
		if (this.brushEnabled.isSelected()) {
			BrushCurveScript brush = switch (globalCurveScript.get()) {
				case Success(BrushCurveScript script) -> script;
				case Failure(ScriptParsingException exception) -> throw new RedrawException(exception.getLocalizedMessage());
			};
			if (work.modified) work.recalculateDistanceMap();
			if (destination.width != work.width || destination.height != work.height) {
				throw new RedrawException("Size mismatch");
			}
			boolean linear = this.linear.isSelected();
			if (linear) color = color.mul(color, Util.RGB_MASK);
			for (int index = 0, length = work.distanceMap.length; index < length; index++) {
				float brushAlpha = brush.apply(work.distanceMap[index]);
				if (brushAlpha > 0.0F) {
					FloatVector oldColor = FloatVector.fromArray(FloatVector.SPECIES_128, destination.pixels, index << 2);
					if (linear) oldColor = oldColor.mul(oldColor, Util.RGB_MASK);
					FloatVector newColor = color.withLane(HDRImage.ALPHA_OFFSET, color.lane(HDRImage.ALPHA_OFFSET) * brushAlpha);
					newColor = Util.blendAlpha(oldColor, newColor);
					if (linear) newColor = newColor.lanewise(VectorOperators.SQRT, Util.RGB_MASK);
					newColor.intoArray(destination.pixels, index << 2);
				}
			}
		}
		else {
			FloatVector color_ = color;
			work.forEachCurve((Curve curve) -> {
				for (int index = 0, size = curve.size; index < size; index++) {
					destination.setColor(
						(int)(Math.floor(curve.getX(index))),
						(int)(Math.floor(curve.getY(index))),
						color_
					);
				}
			});
		}
	}

	@Override
	public Node getConfiguration() {
		return this.configPane;
	}

	@Override
	public void updateLabelText() {
		if (this.work != null) {
			this.labelText.set("Drawing " + this.work.curves.size() + " curves(s) freehand");
		}
		else {
			this.labelText.set("Freehand tool inactive");
		}
	}

	public static float smooth(float x) {
		if (x > 0.0F) {
			if (x < 1.0F) {
				return UtilityOperations.smooth_float(x);
			}
			else {
				return 1.0F;
			}
		}
		else {
			return 0.0F;
		}
	}

	public static float smoother(float x) {
		if (x > 0.0F) {
			if (x < 1.0F) {
				return UtilityOperations.smoother_float(x);
			}
			else {
				return 1.0F;
			}
		}
		else {
			return 0.0F;
		}
	}

	public static class BrushCurveScriptParser extends ExpressionParser<BrushCurveScript> {

		public BrushCurveScriptParser(String source) {
			super(source, BrushCurveScript.class);
		}

		@Override
		public BrushCurveScriptParser addBuiltins() {
			super.addBuiltins();
			this.scope.environment.addKeyword("return", KeywordHandler.returner(VectorType.FLOAT));
			this.scope.environment.addVariable("distance", VariableHandler.builtinParameter("distance", VectorType.FLOAT));
			this.scope.environment.addFunction("smooth", FunctionHandler.invoker(new MethodInfo(FreehandTool.class, "smooth")));
			this.scope.environment.addFunction("smoother", FunctionHandler.invoker(new MethodInfo(FreehandTool.class, "smoother")));
			return this;
		}

		@Override
		public InsnTree implicitReturn(InsnTree script) throws ScriptParsingException {
			return script.type() == VectorType.FLOAT ? new ReturnInsnTree(script) : super.implicitReturn(script);
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

		public BrushCurveScript parse() throws ScriptParsingException {
			ClassNode clazz = this.parseBasic();
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			clazz.accept(writer);
			byte[] bytes = writer.toByteArray();
			try {
				MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);
				return (BrushCurveScript)(lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke());
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

	@FunctionalInterface
	public static interface BrushCurveScript {

		public abstract float apply(float distance);
	}

	@FunctionalInterface
	public static interface PointPredicate {

		public abstract boolean test(double x, double y);
	}

	public static class Curve {

		public double[] positions = new double[32];
		public int size = 0;

		public void set(int index, double x, double y) {
			int start = Objects.checkIndex(index, this.size) << 1;
			this.positions[start] = x;
			this.positions[start | 1] = y;
		}

		public void add(double x, double y) {
			int index = this.size++ << 1;
			if (index >= this.positions.length) {
				this.positions = Arrays.copyOf(this.positions, this.positions.length << 1);
			}
			this.positions[index] = x;
			this.positions[index | 1] = y;
		}

		public void addAll(Curve that) {
			int newSize = this.size + that.size;
			if (newSize << 1 >= this.positions.length) {
				this.positions = Arrays.copyOf(this.positions, Math.max(this.positions.length, newSize) << 1);
			}
			System.arraycopy(that.positions, 0, this.positions, this.size << 1, that.size << 1);
			this.size = newSize;
		}

		public void remove(int index) {
			int start = Objects.checkIndex(index, this.size) << 1;
			System.arraycopy(this.positions, start + 2, this.positions, start, (this.size << 1) - (start + 2));
			this.size--;
		}

		public void removeIf(PointPredicate predicate) {
			int writeIndex = 0, size = this.size;
			for (int readIndex = 0; readIndex < size; readIndex++) {
				double x = this.getX(readIndex);
				double y = this.getY(readIndex);
				if (!predicate.test(x, y)) {
					if (readIndex != writeIndex) this.set(writeIndex, x, y);
					writeIndex++;
				}
			}
			this.size = writeIndex;
		}

		public double getX(int index) {
			return this.positions[Objects.checkIndex(index, this.size) << 1];
		}

		public double getY(int index) {
			return this.positions[(Objects.checkIndex(index, this.size) << 1) | 1];
		}

		public void clear() {
			this.size = 0;
		}

		@Override
		public String toString() {
			if (this.size == 0) return Integer.toHexString(System.identityHashCode(this)) + "[]";
			StringBuilder builder = new StringBuilder(64).append(Integer.toHexString(System.identityHashCode(this))).append("[ ");
			for (int index = 0; index < this.size; index++) {
				builder.append('(').append(this.getX(index)).append(", ").append(this.getY(index)).append("), ");
			}
			builder.setLength(builder.length() - 2);
			return builder.append(" ]").toString();
		}
	}

	public class Work {

		public List<Curve> curves = new ArrayList<>();
		public boolean modified;
		public int width, height;
		public float[] distanceMap;

		public Work(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public int index(int x, int y) {
			return y * this.width + x;
		}

		public synchronized void addCurve() {
			this.curves.add(new Curve());
		}

		public synchronized void addPoint(double x, double y) {
			Curve curve = this.curves.getLast();
			if (curve.size == 0) {
				curve.add(x, y);
			}
			else {
				double lastX = curve.getX(curve.size - 1);
				double lastY = curve.getY(curve.size - 1);
				if (FreehandTool.this.brushEnabled.isSelected()) {
					double distance = Math.sqrt(Util.square(x - lastX) + Util.square(y - lastY));
					int steps = (int)(Math.ceil(distance / 0.5D));
					for (int step = 1; step <= steps; step++) {
						double fraction = ((double)(step)) / ((double)(steps));
						curve.add(
							Util.mix(lastX, x, fraction),
							Util.mix(lastY, y, fraction)
						);
					}
				}
				else {
					curve.add(x, y);
				}
			}
			this.modified = true;
		}

		public synchronized void removePoint(double x, double y) {
			if (FreehandTool.this.brushEnabled.isSelected()) {
				int bestIndex = -1;
				double bestDistance = Double.POSITIVE_INFINITY;
				for (int index = 0, size = this.curves.size(); index < size; index++) {
					double newDistance = this.computeDistance(this.curves.get(index), x, y);
					if (newDistance < bestDistance) {
						bestIndex = index;
						bestDistance = newDistance;
					}
				}
				if (bestIndex >= 0) {
					this.curves.remove(bestIndex);
				}
			}
			else {
				this.curves.removeIf((Curve curve) -> {
					curve.removeIf((double x2, double y2) -> x2 == x && y2 == y);
					return curve.size == 0;
				});
			}
		}

		public double computeDistance(Curve curve, double x, double y) {
			double distanceSquared = Double.POSITIVE_INFINITY;
			for (int index = 0, size = curve.size; index < size; index++) {
				distanceSquared = Math.min(distanceSquared, Util.square(x - curve.getX(index)) + Util.square(y - curve.getY(index)));
			}
			return distanceSquared;
		}

		public PointQuadTreeNode addToRoot(PointQuadTreeNode root, Curve curve) {
			for (int index = 0, size = curve.size; index < size; index++) {
				root = root.add(curve.getX(index), curve.getY(index));
			}
			return root;
		}

		public synchronized void forEachCurve(Consumer<Curve> action) {
			int
				smoothSteps = FreehandTool.this.smoothSteps.getValue();
			Curve
				curve1 = smoothSteps > 0 ? new Curve() : null,
				curve2 = smoothSteps > 0 ? new Curve() : null;
			for (Curve curve : this.curves) {
				if (curve.size == 0) continue;
				if (smoothSteps > 0 && curve.size > 2) {
					curve1.addAll(curve);
					for (int step = curve.size > 3 ? smoothSteps : 1; --step >= 0; ) {
						curve2.add(curve1.getX(0), curve1.getY(0));
						for (int index = 1, limit = curve1.size - 1; index < limit; index++) {
							double
								prevX = curve1.getX(index - 1),
								prevY = curve1.getY(index - 1),
								nextX = curve1.getX(index + 1),
								nextY = curve1.getY(index + 1),
								currX = (prevX + nextX) * 0.5D,
								currY = (prevY + nextY) * 0.5D;
							curve2.add(currX, currY);
						}
						curve2.add(curve1.getX(curve1.size - 1), curve1.getY(curve1.size - 1));
						curve1.clear();
						Curve tmp = curve1;
						curve1 = curve2;
						curve2 = tmp;
					}
					action.accept(curve1);
					curve1.clear();
				}
				else {
					action.accept(curve);
				}
			}
		}

		public void recalculateDistanceMap() {
			if (this.distanceMap == null) {
				this.distanceMap = new float[this.width * this.height];
			}
			if (this.curves.isEmpty()) {
				Arrays.fill(this.distanceMap, Float.POSITIVE_INFINITY);
				return;
			}
			PointQuadTreeNode[] root = new PointQuadTreeNode[] {
				new EmptyNode(0.0D, 0.0D, this.width, this.height)
			};
			this.forEachCurve((Curve curve) -> {
				root[0] = this.addToRoot(root[0], curve);
			});
			if (root[0] instanceof EmptyNode) {
				Arrays.fill(this.distanceMap, Float.POSITIVE_INFINITY);
				return;
			}
			Query query = new Query();
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.width; x++) {
					query.init(x + 0.5D, y + 0.5D);
					root[0].findClosest(query);
					this.distanceMap[this.index(x, y)] = (float)(Math.sqrt(query.distanceSquared));
				}
			}
		}
	}

	public static class Query {

		public double targetX, targetY, foundX, foundY, distanceSquared;

		public Query() {}

		public Query(double targetX, double targetY) {
			this.init(targetX, targetY);
		}

		public void init(double targetX, double targetY) {
			this.targetX = targetX;
			this.targetY = targetY;
			this.distanceSquared = Double.POSITIVE_INFINITY;
		}

		public void update(double x, double y) {
			double newDistanceSquared = (
				+ Util.square(x - this.targetX)
				+ Util.square(y - this.targetY)
			);
			if (newDistanceSquared < this.distanceSquared) {
				this.foundX = x;
				this.foundY = y;
				this.distanceSquared = newDistanceSquared;
			}
		}
	}

	public static abstract class PointQuadTreeNode {

		public final double minX, minY, maxX, maxY, midX, midY;

		public PointQuadTreeNode(double minX, double minY, double maxX, double maxY) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.midX = (minX + maxX) * 0.5D;
			this.midY = (minY + maxY) * 0.5D;
		}

		public abstract PointQuadTreeNode add(double x, double y);

		public abstract void findClosest(Query query);
	}

	public static class EmptyNode extends PointQuadTreeNode {

		public EmptyNode(double minX, double minY, double maxX, double maxY) {
			super(minX, minY, maxX, maxY);
		}

		@Override
		public PointQuadTreeNode add(double x, double y) {
			return new SingleNode(this.minX, this.minY, this.maxX, this.maxY, x, y);
		}

		@Override
		public void findClosest(Query query) {
			//no-op.
		}
	}

	public static class SingleNode extends PointQuadTreeNode {

		public final double x, y;

		public SingleNode(double minX, double minY, double maxX, double maxY, double x, double y) {
			super(minX, minY, maxX, maxY);
			this.x = x;
			this.y = y;
		}

		@Override
		public PointQuadTreeNode add(double x, double y) {
			if (this.x == x && this.y == y) {
				return this;
			}
			else {
				return new SplitNode(this.minX, this.minY, this.maxX, this.maxY).add(this.x, this.y).add(x, y);
			}
		}

		@Override
		public void findClosest(Query query) {
			query.update(this.x, this.y);
		}
	}

	public static class SplitNode extends PointQuadTreeNode {

		public PointQuadTreeNode x0y0, x0y1, x1y0, x1y1;

		public SplitNode(double minX, double minY, double maxX, double maxY) {
			super(minX, minY, maxX, maxY);
			this.x0y0 = new EmptyNode(this.minX, this.minY, this.midX, this.midY);
			this.x0y1 = new EmptyNode(this.minX, this.midY, this.midX, this.maxY);
			this.x1y0 = new EmptyNode(this.midX, this.minY, this.maxX, this.midY);
			this.x1y1 = new EmptyNode(this.midX, this.midY, this.maxX, this.maxY);
		}

		public PointQuadTreeNode get(boolean highX, boolean highY) {
			if (highX) {
				if (highY) {
					return this.x1y1;
				}
				else {
					return this.x1y0;
				}
			}
			else {
				if (highY) {
					return this.x0y1;
				}
				else {
					return this.x0y0;
				}
			}
		}

		public void set(boolean highX, boolean highY, PointQuadTreeNode node) {
			if (highX) {
				if (highY) {
					this.x1y1 = node;
				}
				else {
					this.x1y0 = node;
				}
			}
			else {
				if (highY) {
					this.x0y1 = node;
				}
				else {
					this.x0y0 = node;
				}
			}
		}

		@Override
		public PointQuadTreeNode add(double x, double y) {
			boolean highX = x >= this.midX;
			boolean highY = y >= this.midY;
			this.set(highX, highY, this.get(highX, highY).add(x, y));
			return this;
		}

		@Override
		public void findClosest(Query query) {
			double clampedX = Math.clamp(query.targetX, this.minX, this.maxX) - query.targetX;
			double clampedY = Math.clamp(query.targetY, this.minY, this.maxY) - query.targetY;
			if (clampedX * clampedX + clampedY * clampedY < query.distanceSquared) {
				boolean highX = query.targetX >= this.midX;
				boolean highY = query.targetY >= this.midY;
				this.get( highX,  highY).findClosest(query);
				this.get( highX, !highY).findClosest(query);
				this.get(!highX,  highY).findClosest(query);
				this.get(!highX, !highY).findClosest(query);
			}
		}
	}

	public static record Point(double x, double y) {

		public int floorX() {
			return (int)(Math.floor(this.x));
		}

		public int floorY() {
			return (int)(Math.floor(this.y));
		}
	}
}