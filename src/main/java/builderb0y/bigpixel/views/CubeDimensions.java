package builderb0y.bigpixel.views;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.JsonConverter.CubeSizeJsonConverter;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.util.AggregateProperty;
import builderb0y.bigpixel.util.Util;

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
	public SizeProperty
		sizeProperty = this.new SizeProperty();
	public ParameterMultiStorage<CubeSize>
		sizeStorage;
	public static record CubeSize(
		double minX,
		double minY,
		double minZ,
		double maxX,
		double maxY,
		double maxZ
	) {}

	public JsonMap save() {
		return new JsonMap().with("size", this.sizeStorage.save(CubeSizeJsonConverter.INSTANCE));
	}

	public void load(JsonMap saveData) {
		this.sizeStorage.load(saveData.getMap("size"), CubeSizeJsonConverter.INSTANCE);
	}

	public CubeDimensions(OrganizedSelection.Value<?> view) {
		ParameterSetTop top = view.getLayer().graph.openImage.parameterSet;
		this.sizeStorage = new ParameterMultiStorage<>(this.sizeProperty, top);
		this.autoUVButton.setTooltip(new Tooltip("Set UVs of all faces automatically"));
		this.gridPane.add(this.autoUVButton, 0, 0);
		this.addLabel("From:", 1, 0);
		this.addLabel("To:", 2, 0);
		this.addLabel("X:", 0, 1);
		this.addLabel("Y:", 0, 2);
		this.addLabel("Z:", 0, 3);
		ConfigParameters.setupContextMenu(top, this.minXSpinner, this.sizeStorage);
		ConfigParameters.setupContextMenu(top, this.minYSpinner, this.sizeStorage);
		ConfigParameters.setupContextMenu(top, this.minZSpinner, this.sizeStorage);
		ConfigParameters.setupContextMenu(top, this.maxXSpinner, this.sizeStorage);
		ConfigParameters.setupContextMenu(top, this.maxYSpinner, this.sizeStorage);
		ConfigParameters.setupContextMenu(top, this.maxZSpinner, this.sizeStorage);
		this.gridPane.add(this.minXSpinner, 1, 1);
		this.gridPane.add(this.minYSpinner, 1, 2);
		this.gridPane.add(this.minZSpinner, 1, 3);
		this.gridPane.add(this.maxXSpinner, 2, 1);
		this.gridPane.add(this.maxYSpinner, 2, 2);
		this.gridPane.add(this.maxZSpinner, 2, 3);
		this.titledPane.setAnimated(false);
		this.minXSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minX      = value * 0.0625D;
			this.midX      = (this.maxX + this.minX) * 0.5D;
			this.diameterX =  this.maxX - this.minX;
			this.radiusX   =  this.diameterX * 0.5D;
		}));
		this.minYSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minY      = value * 0.0625D;
			this.midY      = (this.maxY + this.minY) * 0.5D;
			this.diameterY =  this.maxY - this.minY;
			this.radiusY   =  this.diameterY * 0.5D;
		}));
		this.minZSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minZ      = value * 0.0625D;
			this.midZ      = (this.maxZ + this.minZ) * 0.5D;
			this.diameterZ =  this.maxZ - this.minZ;
			this.radiusZ   =  this.diameterZ * 0.5D;
		}));
		this.maxXSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxX      = value * 0.0625D;
			this.midX      = (this.maxX + this.minX) * 0.5D;
			this.diameterX =  this.maxX - this.minX;
			this.radiusX   =  this.diameterX * 0.5D;
		}));
		this.maxYSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxY      = value * 0.0625D;
			this.midY      = (this.maxY + this.minY) * 0.5D;
			this.diameterY =  this.maxY - this.minY;
			this.radiusY   =  this.diameterY * 0.5D;
		}));
		this.maxZSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxZ      = value * 0.0625D;
			this.midZ      = (this.maxZ + this.minZ) * 0.5D;
			this.diameterZ =  this.maxZ - this.minZ;
			this.radiusZ   =  this.diameterZ * 0.5D;
		}));
	}

	public void addLabel(String text, int x, int y) {
		Label label = new Label(text);
		this.gridPane.add(label, x, y);
		GridPane.setHalignment(label, HPos.CENTER);
		GridPane.setValignment(label, VPos.CENTER);
	}

	public class SizeProperty extends AggregateProperty<CubeSize> implements InvalidationListener {

		public SizeProperty() {
			WeakInvalidationListener listener = new WeakInvalidationListener(this);
			CubeDimensions.this.minXSpinner.valueProperty().addListener(listener);
			CubeDimensions.this.minYSpinner.valueProperty().addListener(listener);
			CubeDimensions.this.minZSpinner.valueProperty().addListener(listener);
			CubeDimensions.this.maxXSpinner.valueProperty().addListener(listener);
			CubeDimensions.this.maxYSpinner.valueProperty().addListener(listener);
			CubeDimensions.this.maxZSpinner.valueProperty().addListener(listener);
		}

		@Override
		public CubeSize get() {
			return new CubeSize(
				CubeDimensions.this.minXSpinner.getValue(),
				CubeDimensions.this.minYSpinner.getValue(),
				CubeDimensions.this.minZSpinner.getValue(),
				CubeDimensions.this.maxXSpinner.getValue(),
				CubeDimensions.this.maxYSpinner.getValue(),
				CubeDimensions.this.maxZSpinner.getValue()
			);
		}

		@Override
		public void doSet(CubeSize value) {
			CubeDimensions.this.minXSpinner.getValueFactory().setValue(value.minX);
			CubeDimensions.this.minYSpinner.getValueFactory().setValue(value.minY);
			CubeDimensions.this.minZSpinner.getValueFactory().setValue(value.minZ);
			CubeDimensions.this.maxXSpinner.getValueFactory().setValue(value.maxX);
			CubeDimensions.this.maxYSpinner.getValueFactory().setValue(value.maxY);
			CubeDimensions.this.maxZSpinner.getValueFactory().setValue(value.maxZ);
		}

		@Override
		public void invalidated(Observable observable) {
			this.fireValueChangedEvent();
		}

		@Override
		public Object getBean() {
			return CubeDimensions.this;
		}

		@Override
		public String getName() {
			return "sizeProperty";
		}
	}
}