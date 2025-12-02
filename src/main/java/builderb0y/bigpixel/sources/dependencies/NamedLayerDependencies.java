package builderb0y.bigpixel.sources.dependencies;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class NamedLayerDependencies extends AbstractNamedDependencies {

	public GridPane gridPane;

	public NamedLayerDependencies(OrganizedSelection.Value<?> owner) {
		this.gridPane = new GridPane();
		super(owner);
	}

	public UnmovableInputBinding addBinding(String saveName, UnmovableInputBinding binding) {
		int size = this.allBindings.size();
		if (this.allBindings.putIfAbsent(saveName, binding) != null) {
			throw new IllegalArgumentException("Duplicate input binding: " + saveName);
		}
		binding.addRow(this.gridPane, size);
		this.onBindingAdded(binding);
		return binding;
	}

	public UnmovableInputBinding addBinding(String saveName, String displayName, Color color) {
		return this.addBinding(saveName, new UnmovableInputBinding(this.owner, displayName, this.colorBoxGroup, color));
	}

	public void addExtraNodeRow(Node node) {
		int width  = this.gridPane.getColumnCount();
		int height = this.gridPane.getRowCount();
		this.gridPane.add(node, 0, height, width, 1);
	}

	@Override
	public Parent getConfigPane() {
		return this.gridPane;
	}
}