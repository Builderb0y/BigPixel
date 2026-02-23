package builderb0y.bigpixel.views;

import java.util.Locale;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.JsonConverter.UvJsonConverter;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.tools.Symmetry;
import builderb0y.bigpixel.util.AggregateProperty;
import builderb0y.bigpixel.util.Util;

public class FaceInputBinding extends InputBinding {

	public CubeDimensions
		dimensions;
	public Face
		face;
	public CheckBox
		enabled;
	public Spinner<Double>
		minUSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D,  0.0D, 1.0D), 64.0D),
		minVSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D,  0.0D, 1.0D), 64.0D),
		maxUSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D, 16.0D, 1.0D), 64.0D),
		maxVSpinner = Util.setupSpinner(new Spinner<>(0.0D, 16.0D, 16.0D, 1.0D), 64.0D);
	public double
		minU = 0.0D,
		minV = 0.0D,
		maxU = 1.0D,
		maxV = 1.0D;
	public Button
		autoUVButton    = new Button(null, new ImageView(Assets.Tools.AUTO_UV)),
		alternateButton = this.makeSymmetryButton(Symmetry.IDENTITY,   Assets.Tools.Move.AGAIN     ),
		rotate90Button  = this.makeSymmetryButton(Symmetry.ROTATE_CW,  Assets.Tools.Move.ROTATE_CW ),
		rotate180Button = this.makeSymmetryButton(Symmetry.ROTATE_180, Assets.Tools.Move.ROTATE_180),
		rotate270Button = this.makeSymmetryButton(Symmetry.ROTATE_CCW, Assets.Tools.Move.ROTATE_CCW),
		flipHButton     = this.makeSymmetryButton(Symmetry.FLIP_H,     Assets.Tools.Move.FLIP_H    ),
		flipVButton     = this.makeSymmetryButton(Symmetry.FLIP_V,     Assets.Tools.Move.FLIP_V    ),
		flipLButton     = this.makeSymmetryButton(Symmetry.FLIP_L,     Assets.Tools.Move.FLIP_L    ),
		flipRButton     = this.makeSymmetryButton(Symmetry.FLIP_R,     Assets.Tools.Move.FLIP_R    );
	public GridPane
		uvPane = new GridPane();
	public TitledPane
		titledPane = new TitledPane();
	public SimpleObjectProperty<Symmetry>
		rotation = new SimpleObjectProperty<>(this, "rotation", Symmetry.IDENTITY);
	public UvParamsProperty
		uvParams = this.new UvParamsProperty();
	public ParameterMultiStorage<UvParams>
		uvParamsStorage = new ParameterMultiStorage<>(this.uvParams, this.owner.getLayer().graph.openImage.parameterSet);
	public ObjectBinding<Params>
		drawParams;
	public static record UvParams(
		double minU,
		double minV,
		double maxU,
		double maxV,
		Symmetry rotation
	) {}
	public static record Params(
		UvParams uv,
		boolean enabled,
		SamplerProvider input
	) {}

	public FaceInputBinding(OrganizedSelection.Value<?> owner, CubeDimensions dimensions, Face face, ColorBoxGroup group) {
		this.dimensions = dimensions;
		this.face = face;
		super(owner, group, CurveHelper.VIEW);
		this.configParameters.addParameter(new ConfigParameter<>(this.uvParamsStorage, "uv", UvParams.class, UvJsonConverter.INSTANCE));
		this.minUSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minU = value * 0.0625D;
			this.owner.redrawLater();
		}));
		this.minVSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.minV = value * 0.0625D;
			this.owner.redrawLater();
		}));
		this.maxUSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxU = value * 0.0625D;
			this.owner.redrawLater();
		}));
		this.maxVSpinner.valueProperty().addListener(Util.change((Double value) -> {
			this.maxV = value * 0.0625D;
			this.owner.redrawLater();
		}));
		this.autoUVButton.setOnAction(this::autoUV);
		this.autoUVButton.setMinWidth(64.0D);
		this.autoUVButton.setMaxWidth(64.0D);
		this.autoUVButton.setTooltip(new Tooltip("Set UVs of this face automatically"));
		ParameterSetTop top = owner.getLayer().graph.openImage.parameterSet;
		ConfigParameters.setupContextMenu(top, this.minUSpinner, this.uvParamsStorage);
		ConfigParameters.setupContextMenu(top, this.minVSpinner, this.uvParamsStorage);
		ConfigParameters.setupContextMenu(top, this.maxUSpinner, this.uvParamsStorage);
		ConfigParameters.setupContextMenu(top, this.maxVSpinner, this.uvParamsStorage);
		this.uvPane.add(this.minUSpinner, 0, 1);
		this.uvPane.add(this.minVSpinner, 1, 0);
		this.uvPane.add(this.maxUSpinner, 2, 1);
		this.uvPane.add(this.maxVSpinner, 1, 2);
		this.uvPane.add(this.autoUVButton, 1, 1);
		this.uvPane.add(this.alternateButton, 3, 0);
		this.uvPane.add(this.rotate90Button, 4, 0);
		this.uvPane.add(this.rotate180Button, 5, 0);
		this.uvPane.add(this.rotate270Button, 6, 0);
		this.uvPane.add(this.flipHButton, 3, 1);
		this.uvPane.add(this.flipVButton, 4, 1);
		this.uvPane.add(this.flipLButton, 5, 1);
		this.uvPane.add(this.flipRButton, 6, 1);
		this.enabled = new CheckBox(face.displayName + ':');
		this.enabled.setPrefWidth(64.0D);
		this.enabled.setSelected(true);
		this.enabled.selectedProperty().addListener(Util.change(owner::redrawLater));
		this.selection.setPrefWidth(128.0D);
		HBox header = new HBox(this.enabled, this.selection, this.thumbnail, this.colorBox.getDisplayPane());
		header.setAlignment(Pos.CENTER);
		this.titledPane.setGraphic(header);
		this.titledPane.setContent(this.uvPane);
		this.titledPane.setExpanded(false);
		//animation plays too slowly for my preferences and can't be configured.
		this.titledPane.setAnimated(false);
		this.drawParams = Bindings.createObjectBinding(
			() -> new Params(
				this.uvParams.get(),
				this.enabled.isSelected(),
				this.selection.getValue()
			),
			this.uvParams,
			this.enabled.selectedProperty(),
			this.selection.valueProperty()
		);
	}

	public Button makeSymmetryButton(Symmetry symmetry, Image image) {
		Button button = new Button();
		button.setGraphic(new ImageView(image));
		button.setOnAction((ActionEvent _) -> this.symmetrify(symmetry));
		return button;
	}

	public void autoUV(ActionEvent ignored) {
		switch (this.face) {
			case UP -> {
				this.minUSpinner.getValueFactory().setValue(this.dimensions.minXSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(this.dimensions.maxXSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(this.dimensions.minZSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(this.dimensions.maxZSpinner.getValue());
			}
			case DOWN -> {
				this.minUSpinner.getValueFactory().setValue(this.dimensions.minXSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(this.dimensions.maxXSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxZSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(16.0D - this.dimensions.minZSpinner.getValue());
			}
			case NORTH -> {
				this.minUSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxXSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(16.0D - this.dimensions.minXSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxYSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(16.0D - this.dimensions.minYSpinner.getValue());
			}
			case SOUTH -> {
				this.minUSpinner.getValueFactory().setValue(this.dimensions.minXSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(this.dimensions.maxXSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxYSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(16.0D - this.dimensions.minYSpinner.getValue());
			}
			case EAST -> {
				this.minUSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxZSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(16.0D - this.dimensions.minZSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxYSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(16.0D - this.dimensions.minYSpinner.getValue());
			}
			case WEST -> {
				this.minUSpinner.getValueFactory().setValue(this.dimensions.minZSpinner.getValue());
				this.maxUSpinner.getValueFactory().setValue(this.dimensions.maxZSpinner.getValue());
				this.minVSpinner.getValueFactory().setValue(16.0D - this.dimensions.maxYSpinner.getValue());
				this.maxVSpinner.getValueFactory().setValue(16.0D - this.dimensions.minYSpinner.getValue());
			}
		}
		this.rotation.setValue(Symmetry.IDENTITY);
	}

	public double getU(double rawX, double rawY) {
		return Util.mix(
			this.minU,
			this.maxU,
			switch (this.rotation.getValue()) {
				case IDENTITY -> rawX;
				case ROTATE_CW -> rawY;
				case ROTATE_CCW -> -rawY;
				case ROTATE_180 -> -rawX;
				default -> throw new IllegalStateException("rotation set to flip symmetry");
			}
			* 0.5D
			+ 0.5D
		);
	}

	public double getV(double rawX, double rawY) {
		return Util.mix(
			this.minV,
			this.maxV,
			switch (this.rotation.getValue()) {
				case IDENTITY -> rawY;
				case ROTATE_CW -> -rawX;
				case ROTATE_CCW -> rawX;
				case ROTATE_180 -> -rawY;
				default -> throw new IllegalStateException("rotation set to flip symmetry");
			}
			* 0.5D
			+ 0.5D
		);
	}

	public double ungetU(double rawX, double rawY) {
		return Util.unmix(
			this.minU,
			this.maxU,
			switch (this.rotation.getValue()) {
				case IDENTITY -> rawX;
				case ROTATE_CW -> 1.0D - rawY;
				case ROTATE_CCW -> rawY;
				case ROTATE_180 -> 1.0D - rawX;
				default -> throw new IllegalStateException("rotation set to flip symmetry");
			}
		);
	}

	public double ungetV(double rawX, double rawY) {
		return Util.unmix(
			this.minV,
			this.maxV,
			switch (this.rotation.getValue()) {
				case IDENTITY -> rawY;
				case ROTATE_CW -> rawX;
				case ROTATE_CCW -> 1.0D - rawX;
				case ROTATE_180 -> 1.0D - rawY;
				default -> throw new IllegalStateException("rotation set to flip symmetry");
			}
		);
	}

	public void symmetrify(Symmetry symmetry) {
		switch (symmetry) {
			case IDENTITY -> {
				this.flipU();
				this.flipV();
				this.rotate(Symmetry.ROTATE_180);
			}
			case ROTATE_CW, ROTATE_CCW, ROTATE_180 -> {
				this.rotate(symmetry);
				this.owner.redrawLater();
			}
			case FLIP_H -> {
				this.flipU();
				this.owner.redrawLater();
			}
			case FLIP_V -> {
				this.flipV();
				this.owner.redrawLater();
			}
			case FLIP_L -> {
				this.flipU();
				this.rotation.setValue(
					switch (this.rotation.getValue()) {
						case IDENTITY -> Symmetry.ROTATE_CW;
						case ROTATE_CW -> Symmetry.IDENTITY;
						case ROTATE_180 -> Symmetry.ROTATE_CCW;
						case ROTATE_CCW -> Symmetry.ROTATE_180;
						default -> throw new IllegalStateException("rotation set to flip symmetry");
					}
				);
				this.owner.redrawLater();
			}
			case FLIP_R -> {
				this.flipU();
				this.rotation.setValue(
					switch (this.rotation.getValue()) {
						case IDENTITY -> Symmetry.ROTATE_CCW;
						case ROTATE_CCW -> Symmetry.IDENTITY;
						case ROTATE_180 -> Symmetry.ROTATE_CW;
						case ROTATE_CW -> Symmetry.ROTATE_180;
						default -> throw new IllegalStateException("rotation set to flip symmetry");
					}
				);
				this.owner.redrawLater();
			}
		}
	}

	public void flipU() {
		double minU = this.minUSpinner.getValue();
		double maxU = this.maxUSpinner.getValue();
		this.minUSpinner.getValueFactory().setValue(maxU);
		this.maxUSpinner.getValueFactory().setValue(minU);
	}

	public void flipV() {
		double minV = this.minVSpinner.getValue();
		double maxV = this.maxVSpinner.getValue();
		this.minVSpinner.getValueFactory().setValue(maxV);
		this.maxVSpinner.getValueFactory().setValue(minV);
	}

	public void rotate(Symmetry symmetry) {
		this.rotation.setValue(this.rotation.getValue().andThen(symmetry));
	}

	public static enum Face {
		UP,
		DOWN,
		NORTH,
		SOUTH,
		EAST,
		WEST;

		public final String saveName = this.name().toLowerCase(Locale.ROOT);
		public final String displayName = this.name().charAt(0) + this.saveName.substring(1);
	}

	public class UvParamsProperty extends AggregateProperty<UvParams> implements InvalidationListener {

		public UvParamsProperty() {
			WeakInvalidationListener listener = new WeakInvalidationListener(this);
			FaceInputBinding.this.minUSpinner.valueProperty().addListener(listener);
			FaceInputBinding.this.minVSpinner.valueProperty().addListener(listener);
			FaceInputBinding.this.maxUSpinner.valueProperty().addListener(listener);
			FaceInputBinding.this.maxVSpinner.valueProperty().addListener(listener);
			FaceInputBinding.this.rotation.addListener(listener);
		}

		@Override
		public UvParams get() {
			return new UvParams(
				FaceInputBinding.this.minUSpinner.getValue(),
				FaceInputBinding.this.minVSpinner.getValue(),
				FaceInputBinding.this.maxUSpinner.getValue(),
				FaceInputBinding.this.maxVSpinner.getValue(),
				FaceInputBinding.this.rotation.get()
			);
		}

		@Override
		public void doSet(UvParams value) {
			FaceInputBinding.this.minUSpinner.getValueFactory().setValue(value.minU);
			FaceInputBinding.this.minVSpinner.getValueFactory().setValue(value.minV);
			FaceInputBinding.this.maxUSpinner.getValueFactory().setValue(value.maxU);
			FaceInputBinding.this.maxVSpinner.getValueFactory().setValue(value.maxV);
			FaceInputBinding.this.rotation.set(value.rotation);
		}

		@Override
		public void invalidated(Observable observable) {
			this.fireValueChangedEvent();
		}

		@Override
		public Object getBean() {
			return FaceInputBinding.this;
		}

		@Override
		public String getName() {
			return "uvParamsProperty";
		}
	}
}