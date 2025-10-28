package builderb0y.bigpixel.sources;

import javafx.beans.binding.When;
import javafx.beans.value.ObservableObjectValue;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.tools.Tool;

public class LayerSources extends OrganizedSelection<LayerSource, LayerSource.Type, LayerSource.Category> {

	public ObservableObjectValue<@Nullable Tool<?>> toolWithoutColorPicker;

	public LayerSources(LayerNode layer) {
		super(layer, LayerSource.Type.class, LayerSource.Category.class);
	}

	public void init() {
		this.toolWithoutColorPicker = (
			new When(this.selectedType.isEqualTo(LayerSource.Type.MANUAL))
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
	public LayerSource createValue(LayerSource.Type type) {
		LayerSource source = super.createValue(type);
		source.setPossibleDependencies(this.layer.graph.getPossibleDependencies(this.layer));
		return source;
	}

	public ManualLayerSource manualSource() {
		return (ManualLayerSource)(this.getOrCreateValue(LayerSource.Type.MANUAL));
	}
}