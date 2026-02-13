package builderb0y.bigpixel;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.util.Util;

public class HDRAnimation {

	public LayerNode layer;
	public SimpleIntegerProperty width, height;
	public ListBinding<HDRImage> frames;
	public ObservableBooleanValue animated;
	public ObservableIntegerValue frameCount;
	public ObservableIntegerValue currentFrameIndex;
	public ObjectBinding<HDRImage> currentFrame;
	public HDRImage currentFrameCache;

	public HDRAnimation(LayerNode layer, int width, int height) {
		this.layer = layer;
		this.width = new SimpleIntegerProperty(this, "width", width);
		this.height = new SimpleIntegerProperty(this, "height", height);
		ObjectBinding<LayerSource> source = layer.sources.selectedValue;
		AnimationSource animationSource = layer.graph.openImage.animationSource;
		this.animated = Util.toBoolean(source.flatMap((LayerSource layerSource) -> layerSource.getDependencies().animatedProperty()), false);
		this.frameCount = Util.toInt(
			new When(this.animated)
			.then(animationSource.frameCount)
			.otherwise(1),
			1
		);
		this.frames = new ListBinding<>() {

			public final ObservableList<HDRImage> list = FXCollections.observableArrayList();

			{
				this.bind(HDRAnimation.this.frameCount);
			}

			@Override
			public ObservableList<HDRImage> computeValue() {
				int frameCount = HDRAnimation.this.frameCount.intValue();
				if (this.list.size() > frameCount) {
					this.list.remove(frameCount, this.list.size());
				}
				else while (this.list.size() < frameCount) this.list.add(new HDRImage(
					HDRAnimation.this.width.intValue(),
					HDRAnimation.this.height.intValue()
				));
				return this.list;
			}

			@Override
			public void dispose() {
				this.unbind(HDRAnimation.this.frameCount);
			}
		};
		this.currentFrameIndex = Util.toInt(
			new When(this.animated)
			.then(animationSource.frame)
			.otherwise(0),
			0
		);
		this.currentFrame = Bindings.valueAt(this.frames, this.currentFrameIndex);
		this.currentFrameCache = this.currentFrame.get();
		this.currentFrame.addListener(Util.change((HDRImage frame) -> this.currentFrameCache = frame));
	}

	public int width() {
		return this.width.get();
	}

	public int height() {
		return this.height.get();
	}

	public HDRImage getFrame(int frame) {
		return this.frames.get(this.animated.get() ? frame : 0);
	}

	public HDRImage getFrame() {
		return this.currentFrameCache;
	}

	public int getFrameIndex() {
		return this.currentFrameIndex.get();
	}

	public int getFrameCount() {
		return this.frameCount.intValue();
	}

	public boolean isAnimated() {
		return this.animated.get();
	}

	public void checkSize(int width, int height, boolean copy) {
		if (this.width() != width || this.height() != height) {
			this.resize(width, height, copy);
		}
	}

	public void resize(int width, int height, boolean copy) {
		this.width.set(width);
		this.height.set(height);
		for (HDRImage image : this.frames.get()) {
			image.resize(width, height, copy);
		}
	}
}