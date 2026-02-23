package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.JsonConverter.InputBindingSaveDataJsonConverter;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBox;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.AggregateProperty;
import builderb0y.bigpixel.util.Util;

public class InputBinding {

	public OrganizedSelection.Value<?> owner;
	public ColorBox colorBox;
	public ImageView thumbnail;
	public ChoiceBox<SamplerProvider> selection;
	public SaveDataProperty saveDataProperty;
	public ParameterMultiStorage<SaveData> saveDataStorage;
	public ConfigParameters configParameters;
	public ObservableBooleanValue animated;
	public CurveHelper curve;
	public boolean changing;

	public JsonMap save() {
		JsonMap map = new JsonMap();
		this.configParameters.save(map);
		return map;
	}

	public void load(JsonMap map) {
		this.configParameters.load(map);
	}

	public InputBinding(OrganizedSelection.Value<?> owner, ColorBoxGroup group, Color curveColor) {
		this.owner = owner;
		this.colorBox = group.addBox(Util.WHITE);
		this.thumbnail = new ImageView();
		this.selection = new ChoiceBox<>(FXCollections.observableArrayList(this.colorBox));
		this.selection.setValue(this.colorBox);
		this.selection.valueProperty().addListener(Util.change((SamplerProvider supplier) -> {
			if (supplier == null && !this.changing) new Throwable("stack trace").printStackTrace();
		}));
		this.colorBox.getDisplayPane().visibleProperty().bind(this.selection.valueProperty().isEqualTo(this.colorBox));
		this.thumbnail.visibleProperty().bind(this.selection.valueProperty().map(VaryingSampler.class::isInstance));
		this.thumbnail.imageProperty().bind(this.selection.valueProperty().flatMap((SamplerProvider supplier) -> switch (supplier) {
			case UniformSamplerProvider uniform -> null;
			case VaryingSamplerProvider varying -> varying.getBackingLayer().smallThumbnail.currentFrame;
		}));
		this.curve = new CurveHelper(owner.getLayer(), curveColor);
		this.selection.valueProperty().addListener(Util.change(this.curve::setOtherEnd));
		this.selection.setPrefWidth(128.0D);
		this.saveDataProperty = this.new SaveDataProperty();
		this.saveDataStorage = new ParameterMultiStorage<>(this.saveDataProperty, owner.getLayer().graph.openImage.parameterSet);
		ConfigParameters.setupContextMenu(this.saveDataStorage.top, this.selection, this.saveDataStorage);
		this.configParameters = new ConfigParameters(owner.getLayer().graph.openImage.parameterSet, Util.change(() -> {
			if (!this.changing) {
				this.owner.getLayer().requestRedraw();
			}
		}));
		this.configParameters.addParameter(new ConfigParameter<>(this.saveDataStorage, "input", SaveData.class, InputBindingSaveDataJsonConverter.INSTANCE));
		this.animated = Util.toBoolean(
			this
			.selection
			.valueProperty()
			.flatMap((SamplerProvider input) -> switch (input) {
				case UniformSamplerProvider uniform -> null;
				case VaryingSamplerProvider varying -> varying.getBackingLayer().sources.selectedValue;
			})
			.flatMap((LayerSource source) -> source.getDependencies().animatedProperty()),
			false
		);
	}

	public @Nullable LayerNode getSelectedLayer() {
		return this.selection.getValue() instanceof VaryingSamplerProvider varying ? varying.getBackingLayer() : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void retainAll(List<LayerNode> layers) {
		List inputs = new ArrayList<>(layers.size() + 1);
		inputs.addAll(layers);
		inputs.sort(Comparator.comparing(LayerNode::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		inputs.addFirst(this.colorBox);
		if (!this.selection.getItems().equals(inputs)) {
			SamplerProvider selected = this.selection.getValue();
			boolean oldChanging = this.changing;
			this.changing = true;
			try {
				this.selection.getItems().setAll(inputs);
				this.selection.setValue(
					selected instanceof VaryingSamplerProvider varying
					&& layers.contains(varying.getBackingLayer())
					? selected
					: this.colorBox
				);
			}
			finally {
				this.changing = oldChanging;
			}
			this.saveDataProperty.fireValueChangedEvent();
			if (this.selection.getValue() != selected) {
				this.owner.redrawLater();
			}
		}
	}

	public SamplerProvider getCurrent() {
		return this.selection.getValue();
	}

	public static sealed interface SaveData {}

	public static record UniformSaveData(FloatVector color) implements SaveData {}

	public static record VaryingSaveData(String layerName) implements SaveData {}

	public class SaveDataProperty extends AggregateProperty<SaveData> implements ChangeListener<SaveData> {

		public SaveDataProperty() {
			InputBinding.this.selection.valueProperty().flatMap(SamplerProvider::serializedForm).addListener(new WeakChangeListener<>(this));
		}

		@Override
		public SaveData get() {
			return switch (InputBinding.this.selection.getValue()) {
				case UniformSamplerProvider uniform -> new UniformSaveData(uniform.getColor());
				case VaryingSamplerProvider varying -> new VaryingSaveData(varying.getBackingLayer().getDisplayName());
			};
		}

		@Override
		public void doSet(SaveData value) {
			switch (value) {
				case UniformSaveData(FloatVector color) -> {
					InputBinding.this.colorBox.color.set(color);
					InputBinding.this.selection.setValue(InputBinding.this.colorBox);
				}
				case VaryingSaveData(String name) -> {
					LayerNode layer = InputBinding.this.owner.getLayer().graph.getLayerByName(name);
					if (layer == null) throw new IllegalArgumentException("Unknown layer: " + name);
					InputBinding.this.selection.setValue(layer);
				}
			}
		}

		@Override
		public void changed(ObservableValue<? extends SaveData> observable, SaveData oldValue, SaveData newValue) {
			if (!InputBinding.this.changing) {
				this.fireValueChangedEvent();
			}
		}

		@Override
		public Object getBean() {
			return InputBinding.this;
		}

		@Override
		public String getName() {
			return "saveProperty";
		}
	}
}