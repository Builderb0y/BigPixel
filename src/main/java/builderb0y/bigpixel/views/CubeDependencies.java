package builderb0y.bigpixel.views;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.AbstractNamedDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.views.FaceInputBinding.Face;

public class CubeDependencies extends AbstractNamedDependencies {

	public VBox faces;
	public ScrollPane scrollPane;
	public CubeDimensions dimensions = new CubeDimensions(this.owner);
	{
		this.faces.getChildren().add(this.dimensions.titledPane);
	}
	public FaceInputBinding
		up    = this.addBinding(Face.UP),
		north = this.addBinding(Face.NORTH),
		east  = this.addBinding(Face.EAST),
		south = this.addBinding(Face.SOUTH),
		west  = this.addBinding(Face.WEST),
		down  = this.addBinding(Face.DOWN);

	@Override
	public JsonMap save() {
		return super.save().with("dimensions", this.dimensions.save());
	}

	@Override
	public void load(JsonMap saveData) {
		super.load(saveData);
		this.dimensions.load(saveData.getMap("dimensions"));
	}

	public CubeDependencies(LayerView owner) {
		this.scrollPane = new ScrollPane(this.faces = new VBox());
		super(owner);
		this.scrollPane.setFitToWidth(true);
		this.dimensions.autoUVButton.setOnAction((ActionEvent event) -> {
			this.up   .autoUV(event);
			this.north.autoUV(event);
			this.east .autoUV(event);
			this.south.autoUV(event);
			this.west .autoUV(event);
			this.down .autoUV(event);
		});
	}

	public FaceInputBinding addBinding(Face face) {
		FaceInputBinding binding = new FaceInputBinding(this.owner, this.dimensions, face, this.colorBoxGroup);
		if (this.allBindings.putIfAbsent(face.saveName, binding) != null) {
			throw new IllegalArgumentException("Duplicate input binding: " + face.saveName);
		}
		this.faces.getChildren().add(binding.titledPane);
		return binding;
	}

	public void setAll(LayerSourceInput input) {
		for (InputBinding binding : this.allBindings.values()) {
			binding.selection.setValue(input);
		}
	}

	@Override
	public Parent getConfigPane() {
		return this.scrollPane;
	}
}