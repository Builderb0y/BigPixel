package builderb0y.bigpixel.views;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.json.JsonMap;

public class CubeDimensions {

	public Spinner<Double>
		minXSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D,  0.0D, 1.0D), 64.0D),
		minYSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D,  0.0D, 1.0D), 64.0D),
		minZSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D,  0.0D, 1.0D), 64.0D),
		maxXSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D, 16.0D, 1.0D), 64.0D),
		maxYSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D, 16.0D, 1.0D), 64.0D),
		maxZSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D, 16.0D, 1.0D), 64.0D);
	public Button
		autoUVButton = new Button(null, new ImageView(Assets.Tools.AUTO_UV));
	public GridPane
		gridPane = new GridPane();
	public TitledPane
		titledPane = new TitledPane("Dimensions:", this.gridPane);
	public double
		minX      = 0.0D,
		minY      = 0.0D,
		minZ      = 0.0D,
		maxX      = 1.0D,
		maxY      = 1.0D,
		maxZ      = 1.0D,
		midX      = 0.5D,
		midY      = 0.5D,
		midZ      = 0.5D,
		diameterX = 1.0D,
		diameterY = 1.0D,
		diameterZ = 1.0D,
		radiusX   = 0.5D,
		radiusY   = 0.5D,
		radiusZ   = 0.5D;

	public CubeDimensions(OrganizedSelection.Value<?> view) {
		this.autoUVButton.setTooltip(new Tooltip("Set UVs of all faces automatically"));
		this.gridPane.add(this.autoUVButton, 0, 0);
		this.addLabel("From:", 1, 0);
		this.addLabel("To:", 2, 0);
		this.addLabel("X:", 0, 1);
		this.addLabel("Y:", 0, 2);
		this.addLabel("Z:", 0, 3);
		this.gridPane.add(this.minXSpinner, 1, 1);
		this.gridPane.add(this.minYSpinner, 1, 2);
		this.gridPane.add(this.minZSpinner, 1, 3);
		this.gridPane.add(this.maxXSpinner, 2, 1);
		this.gridPane.add(this.maxYSpinner, 2, 2);
		this.gridPane.add(this.maxZSpinner, 2, 3);
		this.minXSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minX      = value * 0.0625D;
			this.midX      = (this.maxX + this.minX) * 0.5D;
			this.diameterX =  this.maxX - this.minX;
			this.radiusX   =  this.diameterX * 0.5D;
			view.redrawLater();
		}));
		this.minYSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minY      = value * 0.0625D;
			this.midY      = (this.maxY + this.minY) * 0.5D;
			this.diameterY =  this.maxY - this.minY;
			this.radiusY   =  this.diameterY * 0.5D;
			view.redrawLater();
		}));
		this.minZSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minZ      = value * 0.0625D;
			this.midZ      = (this.maxZ + this.minZ) * 0.5D;
			this.diameterZ =  this.maxZ - this.minZ;
			this.radiusZ   =  this.diameterZ * 0.5D;
			view.redrawLater();
		}));
		this.maxXSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxX      = value * 0.0625D;
			this.midX      = (this.maxX + this.minX) * 0.5D;
			this.diameterX =  this.maxX - this.minX;
			this.radiusX   =  this.diameterX * 0.5D;
			view.redrawLater();
		}));
		this.maxYSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxY      = value * 0.0625D;
			this.midY      = (this.maxY + this.minY) * 0.5D;
			this.diameterY =  this.maxY - this.minY;
			this.radiusY   =  this.diameterY * 0.5D;
			view.redrawLater();
		}));
		this.maxZSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxZ      = value * 0.0625D;
			this.midZ      = (this.maxZ + this.minZ) * 0.5D;
			this.diameterZ =  this.maxZ - this.minZ;
			this.radiusZ   =  this.diameterZ * 0.5D;
			view.redrawLater();
		}));
	}

	public void addLabel(String text, int x, int y) {
		Label label = new Label(text);
		this.gridPane.add(label, x, y);
		GridPane.setHalignment(label, HPos.CENTER);
		GridPane.setValignment(label, VPos.CENTER);
	}

	public JsonMap save() {
		return (
			new JsonMap()
			.with("minX", this.minXSpinner.getValue())
			.with("minY", this.minYSpinner.getValue())
			.with("minZ", this.minZSpinner.getValue())
			.with("maxX", this.maxXSpinner.getValue())
			.with("maxY", this.maxYSpinner.getValue())
			.with("maxZ", this.maxZSpinner.getValue())
		);
	}

	public void load(JsonMap saveData) {
		this.minXSpinner.getValueFactory().setValue(saveData.getDouble("minX"));
		this.minYSpinner.getValueFactory().setValue(saveData.getDouble("minY"));
		this.minZSpinner.getValueFactory().setValue(saveData.getDouble("minZ"));
		this.maxXSpinner.getValueFactory().setValue(saveData.getDouble("maxX"));
		this.maxYSpinner.getValueFactory().setValue(saveData.getDouble("maxY"));
		this.maxZSpinner.getValueFactory().setValue(saveData.getDouble("maxZ"));
	}
}