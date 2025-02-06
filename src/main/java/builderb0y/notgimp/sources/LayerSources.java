package builderb0y.notgimp.sources;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.SaveException;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.sources.LayerSource.ActiveState;
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

	public ObjectBinding<LayerSource>
		currentSourceProperty = (ObjectBinding<LayerSource>)(
			this.tabPane.getSelectionModel().selectedItemProperty().map((Tab tab) -> (LayerSource)(tab.getUserData()))
		);
	public ObservableObjectValue<@Nullable Tool<?>>
		toolWithoutColorPicker = (
			new When(Bindings.equal(this.currentSourceProperty, this.manualSource))
			.then(this.manualSource.toolWithoutColorPicker)
			.otherwise((Tool<?>)(null))
		);

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("tab", this.getCurrentSource().save());
		return map;
	}

	public void load(JsonMap saveData) {
		JsonMap tab = saveData.getMap("tab");
		String type = tab.getString("type");
		switch (type) {
			case "derived" -> {
				this.derivedSource.load(tab);
				this.derivedSource.state.set(ActiveState.ACTIVE);
				this.tabPane.getSelectionModel().select(this.derivedTab);
			}
			case "procedural" -> {
				this.proceduralSource.load(tab);
				this.proceduralSource.state.set(ActiveState.ACTIVE);
				this.tabPane.getSelectionModel().select(this.proceduralTab);
			}
			case "manual" -> {
				this.manualSource.load(tab);
				this.manualSource.state.set(ActiveState.ACTIVE);
				this.tabPane.getSelectionModel().select(this.manualTab);
			}
			default -> {
				throw new SaveException("Unknown current layer source: " + type);
			}
		}
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
		this.getCurrentSource().redraw(false);
	}

	public void invalidateAll() {
		this.derivedSource.invalidate();
		this.proceduralSource.invalidate();
		this.manualSource.invalidate();
	}

	public LayerSource getCurrentSource() {
		return this.currentSourceProperty.getValue();
	}
}