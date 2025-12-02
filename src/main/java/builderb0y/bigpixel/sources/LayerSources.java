package builderb0y.bigpixel.sources;

import javafx.beans.binding.When;
import javafx.beans.value.ObservableObjectValue;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.sources.LayerSource.LayerSourceCategory;
import builderb0y.bigpixel.sources.LayerSource.LayerSourceType;
import builderb0y.bigpixel.tools.Tool;

public class LayerSources extends OrganizedSelection<LayerSource, LayerSourceType, LayerSourceCategory> {

	public ObservableObjectValue<@Nullable Tool<?>> toolWithoutColorPicker;

	public LayerSources(LayerNode layer) {
		super(layer, LayerSourceType.class, LayerSourceCategory.class);
	}

	public void init() {
		this.toolWithoutColorPicker = (
			new When(this.selectedType.isEqualTo(LayerSourceType.MANUAL))
			.then(this.manualSource().toolWithoutColorPicker)
			.otherwise((Tool<?>)(null))
		);
	}

	public LayerSources(LayerNode newLayer, LayerSources from) {
		this(newLayer);
		LayerSource fromSource = from.selectedValue.get();
		this.getOrCreateValue(fromSource.type).copyFrom(fromSource);
	}

	@Override
	public LayerSource createValue(LayerSourceType type) {
		LayerSource source = super.createValue(type);
		source.setPossibleDependencies(this.layer.graph.getPossibleDependencies(this.layer));
		return source;
	}

	public LayerSource currentSource() {
		return this.selectedValue.get();
	}

	public ManualLayerSource manualSource() {
		return (ManualLayerSource)(this.getOrCreateValue(LayerSourceType.MANUAL));
	}
}