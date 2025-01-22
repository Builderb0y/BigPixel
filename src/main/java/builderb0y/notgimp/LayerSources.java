package builderb0y.notgimp;

import java.util.Map;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.scripting.parsing.ExpressionParser;
import builderb0y.notgimp.scripting.parsing.ScriptEnvironment;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.KeywordHandler;
import builderb0y.notgimp.scripting.parsing.ScriptHandlers.VariableHandler;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.scripting.types.VectorType;
import builderb0y.notgimp.scripting.types.VectorType.Vec;
import builderb0y.notgimp.tools.Tool.ToolType;
import builderb0y.notgimp.tools.Tools;

public class LayerSources {

	public Layer
		layer;
	public TabPane
		tabPane = new TabPane();
	public Tab
		derivedTab = new Tab("Derived"),
		proceduralTab = new Tab("Procedural"),
		manualTab = new Tab("Manual");
	public TextArea
		derivedTextArea = new TextArea(),
		proceduralTextArea = new TextArea();
	public VBox
		manualArea = new VBox();
	public Tools
		tools = new Tools(this);
	public BorderPane
		toolConfig = new BorderPane();
	public RateLimiter
		recompiler = new RateLimiter(500L, this::recompile);

	public LayerSources(Layer layer) {
		this.layer = layer;
		this.toolConfig.centerProperty().bind(
			layer.openImage.mainWindow.currentTool.map(
				(@Nullable ToolType type) -> type != null ? type.getTool(this).getConfiguration() : null
			)
		);
		this.manualArea.getChildren().addAll(this.tools.toolSelection, this.toolConfig);
		this.derivedTab.setContent(this.derivedTextArea);
		this.proceduralTab.setContent(this.proceduralTextArea);
		this.manualTab.setContent(this.manualArea);
		this.tabPane.getTabs().addAll(this.derivedTab, this.proceduralTab, this.manualTab);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		this.derivedTextArea.setText(from.derivedTextArea.getText());
		this.proceduralTextArea.setText(from.proceduralTextArea.getText());
	}

	public void init() {
		Font monospace = Font.font("monospace");
		this.derivedTextArea.setFont(monospace);
		this.derivedTextArea.textProperty().addListener(
			Util.change((String _) -> this.recompiler.run())
		);
		this.proceduralTextArea.setFont(monospace);
		this.tabPane.getSelectionModel().selectedItemProperty().addListener(
			Util.change((Tab newTab) -> {
				if (newTab != this.manualTab) {
					this.layer.openImage.mainWindow.currentTool.set(null);
				}
			})
		);
	}

	public void recompile() {
		try {
			Map<String, Layer> layers = this.layer.openImage.collectLayers();
			DerivedImageScript script = (
				new ExpressionParser<>(this.derivedTextArea.getText(), DerivedImageScript.class)
				.addBuiltins()
				.addLayers(layers)
				.configureEnvironment((ScriptEnvironment environment) -> {
					environment
					.addKeyword("return", KeywordHandler.returner(VectorType.FLOAT4))
					.addVariable("uv", VariableHandler.builtinParameter("uv", VectorType.FLOAT2))
					.addVariable("UV", VariableHandler.builtinParameter("UV", VectorType.INT2))
					.addVariable("resolution", VariableHandler.builtinParameter("resolution", VectorType.INT2))
					;
				})
				.parse(layers)
			);
			HDRImage image = this.layer.image;
			int width = image.width;
			int height = image.height;
			IntVector iResolution = VectorOperations.int2_from_int_int(width, height);
			FloatVector fRcpResolution = VectorOperations.float2_from_float(1.0F).div(VectorOperations.float2_from_int2(iResolution));
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					IntVector UV = VectorOperations.int2_from_int_int(x, height - y - 1);
					FloatVector uv = VectorOperations.float2_from_int2(UV).add(0.5F).mul(fRcpResolution);
					script.compute(UV, uv, iResolution).intoArray(image.pixels, image.baseIndex(x, y));
				}
			}
			image.markDirty();
		}
		catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}

	@FunctionalInterface
	public static interface DerivedImageScript {

		public abstract FloatVector compute(
			@Vec(2) IntVector UV,
			@Vec(2) FloatVector uv,
			@Vec(2) IntVector resolution
		);
	}
}