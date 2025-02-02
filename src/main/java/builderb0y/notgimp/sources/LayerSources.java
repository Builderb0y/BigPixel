package builderb0y.notgimp.sources;

import com.google.gson.JsonObject;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.SaveException;
import builderb0y.notgimp.sources.LayerSource.ActiveState;
import builderb0y.notgimp.tools.ColorPickerTool;
import builderb0y.notgimp.tools.Tool;

public class LayerSources {

	public Layer
		layer;
	public TabPane
		tabPane = new TabPane();
	public Tab
		derivedTab    = new Tab("Derived"),
		proceduralTab = new Tab("Procedural"),
		manualTab     = new Tab("Manual");
	public DerivedLayerSource derivedSource = new DerivedLayerSource(this);
	public ProceduralLayerSource proceduralSource = new ProceduralLayerSource(this);
	public ManualLayerSource manualSource = new ManualLayerSource(this);

	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.add("derived", this.derivedSource.save());
		object.add("procedural", this.proceduralSource.save());
		object.add("manual", this.manualSource.save());
		LayerSource source = this.getCurrentSource();
		object.addProperty("current", switch (source) {
			case DerivedLayerSource _ -> "derived";
			case ProceduralLayerSource _ -> "procedural";
			case ManualLayerSource _ -> "manual";
			default -> throw new SaveException("Unhandled current layer source: " + source);
		});
		return object;
	}

	public void load(JsonObject saveData) {
		this.derivedSource.load(saveData.getAsJsonObject("derived"));
		this.proceduralSource.load(saveData.getAsJsonObject("procedural"));
		this.manualSource.load(saveData.getAsJsonObject("manual"));
		String current = saveData.get("current").getAsString();
		this.tabPane.getSelectionModel().select(switch (current) {
			case "derived"    -> { this.   derivedSource.state.set(ActiveState.ACTIVE); yield this.   derivedTab; }
			case "procedural" -> { this.proceduralSource.state.set(ActiveState.ACTIVE); yield this.proceduralTab; }
			case "manual"     -> { this.    manualSource.state.set(ActiveState.ACTIVE); yield this.    manualTab; }
			default -> throw new SaveException("Unknown current layer source: " + current);
		});
		this.getCurrentSource().redraw(false);
	}

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
		this.derivedSource   .copyFrom(from.   derivedSource);
		this.proceduralSource.copyFrom(from.proceduralSource);
		this.manualSource    .copyFrom(from.    manualSource);
	}

	public void init(boolean fromSave) {
		if (!fromSave) this.manualSource.state.set(ActiveState.ACTIVE);
		this.tabPane.getSelectionModel().selectedItemProperty().addListener(
			(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) -> {
				LayerSource oldSource = (LayerSource)(oldTab.getUserData());
				if (oldSource.state.get() == ActiveState.ACTIVE) {
					oldSource.state.set(ActiveState.INACTIVE);
				}
				LayerSource newSource = (LayerSource)(newTab.getUserData());
				newSource.state.set(ActiveState.ACTIVE);
			}
		);
		this.   derivedSource.init(fromSave);
		this.proceduralSource.init(fromSave);
		this.    manualSource.init(fromSave);
	}

	public void invalidateAll() {
		this.derivedSource.invalidate();
		this.proceduralSource.invalidate();
		this.manualSource.invalidate();
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