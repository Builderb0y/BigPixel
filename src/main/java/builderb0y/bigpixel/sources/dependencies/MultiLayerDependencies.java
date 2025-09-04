package builderb0y.bigpixel.sources.dependencies;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.MovableInputBinding;

public class MultiLayerDependencies extends LayerDependencies {

	public LayerSource source;
	public ColorBoxGroup colorBoxGroup;
	public ListView<MovableInputBinding> listView = new ListView<>();
	public Button addButton = new Button("+");
	public HBox bottomPane = new HBox(this.addButton);
	public BorderPane configView = new BorderPane();

	@Override
	public JsonMap save() {
		JsonArray array = new JsonArray();
		for (InputBinding input : this.listView.getItems()) {
			array.add(input.save());
		}
		return new JsonMap().with("main", array);
	}

	@Override
	public void load(JsonMap saveData) {
		JsonArray main = saveData.getArray("main");
		ObservableList<MovableInputBinding> inputs = this.listView.getItems();
		int targetSize = main.size();
		int index = 0;
		for (; index < targetSize; index++) {
			if (inputs.size() <= index) this.addInput();
			inputs.get(index).load(main.getMap(index));
		}
		while (inputs.size() > index) inputs.removeLast();
	}

	public MultiLayerDependencies(LayerSource source) {
		this.source = source;
		super();
		ColorHelper colorHelper = source.sources.layer.graph.openImage.mainWindow.colorPicker.currentColor;
		this.colorBoxGroup = new ColorBoxGroup(colorHelper, this.listView);
		this.listView.setSelectionModel(new NoSelection<>());
		this.listView.setCellFactory((ListView<MovableInputBinding> _) -> new ListCell<>() {

			@Override
			public void updateItem(MovableInputBinding item, boolean empty) {
				super.updateItem(item, empty);
				this.setGraphic(item != null && !empty ? item.getDisplayPane() : null);
			}
		});
		this.listView.getItems().addListener((ListChangeListener.Change<? extends MovableInputBinding> change) -> {
			while (change.next()) {
				ObservableList<? extends MovableInputBinding> list = change.getList();
				int from = Math.max(change.getFrom() - 1, 0);
				int to = Math.min(change.getTo() + 1, list.size());
				for (int index = from; index < to; index++) {
					MovableInputBinding binding = list.get(index);
					binding.up.setDisable(index == 0);
					binding.down.setDisable(index == list.size() - 1);
				}
			}
			source.requestRedraw();
		});
		this.configView.setCenter(this.listView);
		this.configView.setBottom(this.bottomPane);
		this.addButton.setOnAction((ActionEvent _) -> this.addInput());
	}

	public void addInput() {
		MovableInputBinding binding = new MovableInputBinding(this, this.colorBoxGroup);
		LayerNode layer = this.source.sources.layer;
		binding.retainAll(layer.graph.getPossibleDependencies(layer));
		this.listView.getItems().add(binding);
		binding.curve.selfSourceIsSelected.set(true);
		binding.enabled.selectedProperty().addListener(Util.change(this.source::requestRedraw));
	}

	public void removeInput(MovableInputBinding binding) {
		this.listView.getItems().remove(binding);
		binding.curve.selfSourceIsSelected.set(false);
	}

	@Override
	public void retainAll(List<LayerNode> layers) {
		for (MovableInputBinding input : this.listView.getItems()) {
			input.retainAll(layers);
		}
	}

	@Override
	public boolean dependsOn(LayerNode layer) {
		for (MovableInputBinding input : this.listView.getItems()) {
			if (input.getSelectedLayer() == layer) return true;
		}
		return false;
	}

	@Override
	public boolean containsAny(Predicate<LayerNode> layers) {
		for (MovableInputBinding input : this.listView.getItems()) {
			LayerNode layer = input.getSelectedLayer();
			if (layer != null && layers.test(layer)) return true;
		}
		return false;
	}

	@Override
	public Stream<CurveHelper> getCurves() {
		return this.listView.getItems().stream().map((InputBinding binding) -> binding.curve);
	}

	public Pane getBottomPane() {
		return this.bottomPane;
	}

	@Override
	public Parent getConfigPane() {
		return this.configView;
	}

	public static class NoSelection<T> extends MultipleSelectionModel<T> {

		@Override
		public ObservableList<Integer> getSelectedIndices() {
			return FXCollections.emptyObservableList();
		}

		@Override
		public ObservableList<T> getSelectedItems() {
			return FXCollections.emptyObservableList();
		}

		@Override
		public void selectIndices(int index, int... indices) {

		}

		@Override
		public void selectAll() {

		}

		@Override
		public void selectFirst() {

		}

		@Override
		public void selectLast() {

		}

		@Override
		public void clearAndSelect(int index) {

		}

		@Override
		public void select(int index) {

		}

		@Override
		public void select(T obj) {

		}

		@Override
		public void clearSelection(int index) {

		}

		@Override
		public void clearSelection() {

		}

		@Override
		public boolean isSelected(int index) {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public void selectPrevious() {

		}

		@Override
		public void selectNext() {

		}
	}
}