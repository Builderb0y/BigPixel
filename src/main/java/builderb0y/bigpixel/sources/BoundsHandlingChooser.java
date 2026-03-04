package builderb0y.bigpixel.sources;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.controlsfx.control.PopOver;

import builderb0y.bigpixel.Assets;
import builderb0y.bigpixel.NodeHolder;
import builderb0y.bigpixel.sources.BoundsHandling.DualBoundsHandling;
import builderb0y.bigpixel.util.AggregateProperty;
import builderb0y.bigpixel.util.Util;

public class BoundsHandlingChooser implements NodeHolder {

	public static final PseudoClass
		SELECTED = PseudoClass.getPseudoClass("selected");

	public final SimpleObjectProperty<BoundsHandling>
		horizontalHandling = new SimpleObjectProperty<>(this, "horizontalHandling", BoundsHandling.WRAP),
		verticalHandling   = new SimpleObjectProperty<>(this, "verticalHandling", BoundsHandling.WRAP);
	public final ObjectProperty<DualBoundsHandling>
		dualHandling = this.new DualProperty();
	public final BorderPane
		horizontalIgnorePreview = new BorderPane(new ImageView(Assets.BoundsHandling.HORIZONTAL_IGNORE)),
		horizontalClampPreview  = new BorderPane(new ImageView(Assets.BoundsHandling.HORIZONTAL_CLAMP)),
		horizontalWrapPreview   = new BorderPane(new ImageView(Assets.BoundsHandling.HORIZONTAL_WRAP)),
		verticalIgnorePreview   = new BorderPane(new ImageView(Assets.BoundsHandling.VERTICAL_IGNORE)),
		verticalClampPreview    = new BorderPane(new ImageView(Assets.BoundsHandling.VERTICAL_CLAMP)),
		verticalWrapPreview     = new BorderPane(new ImageView(Assets.BoundsHandling.VERTICAL_WRAP));
	public final GridPane
		gridPane = new GridPane();
	public final PopOver
		popOver = Util.setupPopOver(new PopOver(this.gridPane));
	public final Button
		showButton = new Button("  ⋮  ");

	public BoundsHandlingChooser() {
		Label title = new Label("Bounds Handling");
		title.setFont(new Font(20.0D));
		GridPane.setHalignment(title, HPos.CENTER);
		this.gridPane.add(title, 0, 0, 7, 1);

		Separator horizontalSeparator = new Separator(Orientation.HORIZONTAL);
		horizontalSeparator.setPrefHeight(4.0D);
		horizontalSeparator.setValignment(VPos.BOTTOM);
		this.gridPane.add(horizontalSeparator, 0, 1, 7, 1);

		Label horizontal = new Label("Horizontal");
		horizontal.setFont(new Font(16.0D));
		horizontal.setPadding(new Insets(4.0D, 0.0D, 0.0D, 0.0D));
		GridPane.setHalignment(horizontal, HPos.CENTER);
		this.gridPane.add(horizontal, 0, 2, 3, 1);

		this.gridPane.add(this.horizontalIgnorePreview, 0, 3, 3, 1);
		this.gridPane.add(this.horizontalClampPreview,  0, 4, 3, 1);
		this.gridPane.add(this.horizontalWrapPreview,   0, 5, 3, 1);

		Separator verticalSeparator = new Separator(Orientation.VERTICAL);
		verticalSeparator.setPrefWidth(16.0D);
		this.gridPane.add(verticalSeparator, 3, 2, 1, 4);

		Label vertical = new Label("Vertical");
		vertical.setFont(new Font(16.0D));
		vertical.setPadding(new Insets(4.0D, 0.0D, 0.0D, 0.0D));
		GridPane.setHalignment(vertical, HPos.CENTER);
		this.gridPane.add(vertical, 4, 2, 3, 1);

		this.gridPane.add(this.verticalIgnorePreview, 4, 3, 1, 3);
		this.gridPane.add(this.verticalClampPreview,  5, 3, 1, 3);
		this.gridPane.add(this.verticalWrapPreview,   6, 3, 1, 3);

		this.horizontalIgnorePreview.getStyleClass().add("bounds-handling-preview");
		this.horizontalClampPreview .getStyleClass().add("bounds-handling-preview");
		this.horizontalWrapPreview  .getStyleClass().add("bounds-handling-preview");
		this.verticalIgnorePreview  .getStyleClass().add("bounds-handling-preview");
		this.verticalClampPreview   .getStyleClass().add("bounds-handling-preview");
		this.verticalWrapPreview    .getStyleClass().add("bounds-handling-preview");

		GridPane.setFillHeight(this.verticalIgnorePreview, Boolean.FALSE );
		GridPane.setFillHeight(this.verticalClampPreview,  Boolean.FALSE );
		GridPane.setFillHeight(this.verticalWrapPreview,   Boolean.FALSE );
		GridPane.setValignment(this.verticalIgnorePreview,    VPos.CENTER);
		GridPane.setValignment(this.verticalClampPreview,     VPos.CENTER);
		GridPane.setValignment(this.verticalWrapPreview,      VPos.CENTER);

		this.horizontalWrapPreview.pseudoClassStateChanged(SELECTED, true);
		this.horizontalHandling.addListener(Util.change((BoundsHandling oldValue, BoundsHandling newValue) -> {
			this.getHorizontalPreview(oldValue).pseudoClassStateChanged(SELECTED, false);
			this.getHorizontalPreview(newValue).pseudoClassStateChanged(SELECTED, true);
		}));

		this.verticalWrapPreview.pseudoClassStateChanged(SELECTED, true);
		this.verticalHandling.addListener(Util.change((BoundsHandling oldValue, BoundsHandling newValue) -> {
			this.getVerticalPreview(oldValue).pseudoClassStateChanged(SELECTED, false);
			this.getVerticalPreview(newValue).pseudoClassStateChanged(SELECTED, true);
		}));

		this.horizontalIgnorePreview.setOnMouseClicked((MouseEvent _) -> this.horizontalHandling.set(BoundsHandling.IGNORE));
		this.horizontalClampPreview .setOnMouseClicked((MouseEvent _) -> this.horizontalHandling.set(BoundsHandling.CLAMP));
		this.horizontalWrapPreview  .setOnMouseClicked((MouseEvent _) -> this.horizontalHandling.set(BoundsHandling.WRAP));
		this.verticalIgnorePreview  .setOnMouseClicked((MouseEvent _) -> this.verticalHandling  .set(BoundsHandling.IGNORE));
		this.verticalClampPreview   .setOnMouseClicked((MouseEvent _) -> this.verticalHandling  .set(BoundsHandling.CLAMP));
		this.verticalWrapPreview    .setOnMouseClicked((MouseEvent _) -> this.verticalHandling  .set(BoundsHandling.WRAP));

		this.showButton.setFont(new Font(20.0D));
		this.showButton.getStyleClass().remove("button");
		this.showButton.getStyleClass().add("phantom-button");
		this.showButton.setOnAction((ActionEvent _) -> {
			if (!this.popOver.isShowing()) {
				this.popOver.show(this.showButton);
			}
		});
	}

	public BorderPane getHorizontalPreview(BoundsHandling handling) {
		return switch (handling) {
			case IGNORE -> this.horizontalIgnorePreview;
			case CLAMP  -> this.horizontalClampPreview;
			case WRAP   -> this.horizontalWrapPreview;
		};
	}

	public BorderPane getVerticalPreview(BoundsHandling handling) {
		return switch (handling) {
			case IGNORE -> this.verticalIgnorePreview;
			case CLAMP  -> this.verticalClampPreview;
			case WRAP   -> this.verticalWrapPreview;
		};
	}

	@Override
	public GridPane getRootNode() {
		return this.gridPane;
	}

	public class DualProperty extends AggregateProperty<DualBoundsHandling> implements InvalidationListener {

		public DualProperty() {
			WeakInvalidationListener listener = new WeakInvalidationListener(this);
			BoundsHandlingChooser.this.horizontalHandling.addListener(listener);
			BoundsHandlingChooser.this.verticalHandling.addListener(listener);
		}

		@Override
		public DualBoundsHandling get() {
			return new DualBoundsHandling(
				BoundsHandlingChooser.this.horizontalHandling.get(),
				BoundsHandlingChooser.this.verticalHandling.get()
			);
		}

		@Override
		public void doSet(DualBoundsHandling value) {
			BoundsHandlingChooser.this.horizontalHandling.set(value.horizontal());
			BoundsHandlingChooser.this.verticalHandling.set(value.vertical());
		}

		@Override
		public void invalidated(Observable observable) {
			this.fireValueChangedEvent();
		}

		@Override
		public Object getBean() {
			return BoundsHandlingChooser.this;
		}

		@Override
		public String getName() {
			return "dualHandling";
		}
	}
}