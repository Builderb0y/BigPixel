package builderb0y.notgimp.sources;

import java.util.Collections;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;

public abstract class SingleInputEffectLayerSource extends EffectLayerSource {

	public ChoiceBox<String> input = this.addStringChoiceBox("input_layer");
	public HBox inputBox = new HBox(new Label("Input Layer: "), this.input);
	public BorderPane rootNode = new BorderPane();

	public SingleInputEffectLayerSource(LayerSources sources, String saveName, String displayName) {
		super(sources, saveName, displayName);
		this.input.setOnAction((ActionEvent _) -> this.requestRedraw());
		this.rootNode.setTop(this.inputBox);
	}

	@Override
	public Node getRootNode() {
		return this.rootNode;
	}

	@Override
	public void invalidateStructure() {
		super.invalidateStructure();
		String selection = this.input.getValue();
		this.input.getItems().clear();
		this.collectOptions(this.sources.layer.item);
		this.input.setValue(selection);
	}

	public void collectOptions(TreeItem<Layer> layer) {
		for (TreeItem<Layer> child : layer.getChildren()) {
			this.input.getItems().add(child.getValue().name.get());
			this.collectOptions(child);
		}
	}

	public Layer findLayer() throws RedrawException {
		Layer self = this.sources.layer;
		String name = this.input.getValue();
		Layer layer = name == null ? null : self.openImage.findLayer(name);
		if (layer == null) {
			List<TreeItem<Layer>> directChildren = super.getWatchedItems();
			if (directChildren.size() == 1) {
				layer = directChildren.get(0).getValue();
			}
			else {
				if (name != null) {
					throw new RedrawException("Can't find layer " + name);
				}
				else {
					throw new RedrawException("More than one child to choose from");
				}
			}
		}
		else done: {
			for (TreeItem<Layer> item = layer.item.getParent(); item != null; item = item.getParent()) {
				if (item.getValue() == self) {
					break done;
				}
			}
			throw new RedrawException("Input is not a descendant");
		}
		return layer;
	}

	@Override
	public List<TreeItem<Layer>> getWatchedItems() {
		try {
			Layer layer = this.findLayer();
			return Collections.singletonList(layer.item);
		}
		catch (RedrawException exception) {
			return Collections.emptyList();
		}
	}

	public Layer getSingleInput(boolean resize) throws RedrawException {
		Layer layer = this.findLayer();
		HDRImage source = layer.image;
		HDRImage destination = this.sources.layer.image;
		if (resize && (source.width != destination.width || source.height != destination.height)) {
			destination.resize(source.width, source.height, false);
		}
		return layer;
	}
}