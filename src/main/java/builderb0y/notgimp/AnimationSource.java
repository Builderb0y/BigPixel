package builderb0y.notgimp;

import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.FloatBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class AnimationSource {

	public OpenImage openImage;

	public Label label = new Label("Frames: ");
	public Spinner<Integer> spinner = Util.setupSpinner(
		new Spinner<>(
			new IntegerSpinnerValueFactory(1, 1000, 1, 5) {

				{
					this.setConverter(new StringConverter<>() {

						@Override
						public String toString(Integer object) {
							return object != null ? object.toString() : "1";
						}

						@Override
						public Integer fromString(String string) {
							if (string != null) try {
								return Integer.valueOf(string);
							}
							catch (NumberFormatException _) {}
							return 1;
						}
					});
				}

				@Override
				public void increment(int steps) {
					int oldValue = this.getValue();
					int newValue = oldValue / 5 * 5 + 5;
					newValue = Math.clamp(newValue, this.getMin(), this.getMax());
					this.setValue(newValue);
				}

				@Override
				public void decrement(int steps) {
					int oldValue = this.getValue();
					int newValue = oldValue / 5 * 5;
					if (oldValue == newValue) newValue -= 5;
					newValue = Math.clamp(newValue, this.getMin(), this.getMax());
					this.setValue(newValue);
				}
			}
		)
	);
	public Button
		play  = new Button("⏵"),
		pause = new Button("⏸"),
		stop  = new Button("⏹");
	public HBox hbox = new HBox();

	public ReadOnlyObjectProperty<Integer> frames = this.spinner.valueProperty();
	public SimpleIntegerProperty frame  = new SimpleIntegerProperty();
	public FloatBinding seconds = this.frame.divide(20.0F);
	public FloatBinding fraction = new FloatBinding() {

		{
			this.bind(AnimationSource.this.frame, AnimationSource.this.frames);
		}

		@Override
		public float computeValue() {
			return AnimationSource.this.frame.floatValue() / AnimationSource.this.frames.get().floatValue();
		}

		@Override
		public void dispose() {
			this.unbind(AnimationSource.this.frame, AnimationSource.this.frames);
		}

		@Override
		public ObservableList<?> getDependencies() {
			return FXCollections.unmodifiableObservableList(
				FXCollections.observableArrayList(
					AnimationSource.this.frame,
					AnimationSource.this.frames
				)
			);
		}
	};
	public Transition timer = new Transition(20.0D) {

		{
			this.setInterpolator(Interpolator.LINEAR);
			this.setCycleCount(Animation.INDEFINITE);
			AnimationSource.this.frames.addListener(Util.change((Integer frames) -> {
				this.setCycleDuration(new Duration(frames.doubleValue() * 50.0D));
			}));
		}

		@Override
		public void interpolate(double frac) {
			int nextFrame = AnimationSource.this.frame.get() + 1;
			if (nextFrame >= AnimationSource.this.frames.get()) nextFrame = 0;
			AnimationSource.this.frame.set(nextFrame);
			AnimationSource.this.openImage.tickAnimation();
		}
	};

	public AnimationSource(OpenImage openImage) {
		this.openImage = openImage;
		this.label.setPadding(new Insets(4.0D));
		this.spinner.setPrefWidth(80.0D);
		this.spinner.setEditable(true);
		this.hbox.getChildren().addAll(this.label, this.spinner, this.play, this.pause, this.stop);
		this.play.setOnAction((ActionEvent event) -> this.timer.play());
		this.pause.setOnAction((ActionEvent event) -> this.timer.pause());
		this.stop.setOnAction((ActionEvent event) -> {
			this.timer.stop();
			this.timer.jumpTo(Duration.ZERO);
			this.frame.set(0);
			this.openImage.tickAnimation();
		});
	}

	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("frames", this.frames.get());
		return object;
	}

	public void load(JsonObject object) {
		this.spinner.getValueFactory().setValue(object.get("frames").getAsInt());
	}
}