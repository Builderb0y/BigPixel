package builderb0y.bigpixel;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.Observable;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.util.Util;

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
		),
		80.0D
	);
	public Button
		play  = new Button("⏵"),
		pause = new Button("⏸"),
		stop  = new Button("⏹");
	public HBox hbox = new HBox();

	public ObservableIntegerValue frameCount = Util.toInt(this.spinner.valueProperty(), 1);
	public SimpleIntegerProperty frame = new SimpleIntegerProperty();
	public Transition timer = new Transition(20.0D) {

		{
			this.setInterpolator(Interpolator.LINEAR);
			this.setCycleCount(Animation.INDEFINITE);
			AnimationSource.this.frameCount.addListener(Util.change((Number frames) -> {
				this.setCycleDuration(new Duration(frames.doubleValue() * 50.0D));
			}));
		}

		@Override
		public void interpolate(double frac) {
			int nextFrame = AnimationSource.this.frame.get() + 1;
			if (nextFrame >= AnimationSource.this.frameCount.get()) nextFrame = 0;
			AnimationSource.this.frame.set(nextFrame);
		}
	};

	public AnimationSource(OpenImage openImage) {
		this.openImage = openImage;
		this.label.setPadding(new Insets(4.0D));
		this.hbox.getChildren().addAll(this.label, this.spinner, this.play, this.pause, this.stop);
		this.play.setOnAction((ActionEvent _) -> this.timer.play());
		this.pause.setOnAction((ActionEvent _) -> this.timer.pause());
		this.stop.setOnAction((ActionEvent _) -> {
			this.timer.stop();
			this.timer.jumpTo(Duration.ZERO);
			this.frame.set(0);
		});
		this.frameCount.addListener((Observable frames) -> {
			for (LayerNode layer : openImage.layerGraph.layerList) {
				if (layer.sources.currentSource().getDependencies().animatedProperty().get()) {
					layer.requestRedraw();
				}
			}
		});
	}

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("frames", this.frameCount.get());
		return map;
	}

	public void load(JsonMap map) {
		this.spinner.getValueFactory().setValue(map.getInt("frames"));
	}

	public int getFrameIndex() {
		return this.frame.get();
	}

	public int getFrameCount() {
		return this.frameCount.get();
	}

	public float getSeconds(int frame) {
		return frame / 20.0F;
	}

	public float getFraction(int frame) {
		return frame / this.frameCount.floatValue();
	}

	public Node getRootNode() {
		return this.hbox;
	}
}