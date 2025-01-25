package builderb0y.notgimp.sources;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.tools.ColorPickerTool;
import builderb0y.notgimp.tools.Tool;

public class LayerSources {

	public Layer
		layer;
	public TabPane
		tabPane = new TabPane();
	public DerivedLayerSource
		derivedSource = new DerivedLayerSource(this);
	public ProceduralLayerSource
		proceduralSource = new ProceduralLayerSource(this);
	public ManualLayerSource
		manualSource = new ManualLayerSource(this);
	public Tab
		derivedTab    = new Tab("Derived"),
		proceduralTab = new Tab("Procedural"),
		manualTab     = new Tab("Manual");

	public LayerSources(Layer layer) {
		this.layer = layer;
		this.derivedTab.setUserData(this.derivedSource);
		this.derivedTab.setContent(this.derivedSource.getRootNode());
		this.proceduralTab.setUserData(this.proceduralSource);
		this.proceduralTab.setContent(this.proceduralSource.getRootNode());
		this.manualTab.setUserData(this.manualSource);
		this.manualTab.setContent(this.manualSource.getRootNode());
		this.tabPane.getTabs().addAll(this.derivedTab, this.proceduralTab, this.manualTab);
		this.tabPane.getSelectionModel().select(this.manualTab);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		this.derivedSource.copyFrom(from.derivedSource);
		this.proceduralSource.copyFrom(from.proceduralSource);
		this.manualSource.copyFrom(from.manualSource);
	}

	public void init() {
		this.derivedSource.init();
		this.proceduralSource.init();
		this.manualSource.init();
	}

	public LayerSource getCurrentSource() {
		return ((LayerSource)(this.tabPane.getSelectionModel().getSelectedItem().getUserData()));
	}

	public @Nullable Tool<?> getCurrentTool() {
		Tool<?> tool = this.manualSource.currentTool.get();
		if (tool instanceof ColorPickerTool || this.getCurrentSource() == this.manualSource) return tool;
		return null;
	}
}