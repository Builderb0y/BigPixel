package builderb0y.bigpixel;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.WritableImage;

public abstract class AnimationView {

	public final AnimationSource animationSource;
	public final ListBinding<CachedImage> frames;

	public AnimationView(AnimationSource animationSource) {
		this.animationSource = animationSource;
		ObservableList<CachedImage> frames = FXCollections.observableArrayList();
		this.frames = new ListBinding<>() {

			{
				this.bind(animationSource.frameCount);
			}

			@Override
			public ObservableList<CachedImage> computeValue() {
				int target = animationSource.getFrameCount();

				if (frames.size() > target) {
					frames.remove(target, frames.size());
				}

				//if the frame count changed, that could invalidate all existing images.
				frames.forEach(CachedImage::markDirty);

				while (frames.size() < target) {
					frames.add(AnimationView.this.createCachedImage(frames.size()));
				}

				return frames;
			}
		};
	}

	public abstract void draw(DrawKey key, WritableImage image);

	public CachedImage createCachedImage(int frame) {
		return new CachedImage(frame);
	}

	public void invalidateAll() {
		this.frames.forEach(CachedImage::markDirty);
	}

	public void invalidate(int frame) {
		this.frames.get(frame).markDirty();
	}

	public WritableImage getImage(DrawKey key) {
		return this.frames.get(key.frame()).getFxImage(key);
	}

	public class CachedImage implements InvalidationListener {

		public final int frame;
		public DrawKey prevDrawKey;
		public WritableImage fxImage;
		public boolean dirty;

		public CachedImage(int frame) {
			this.frame = frame;
		}

		public void markDirty() {
			this.dirty = true;
			if (this.frame == AnimationView.this.animationSource.getFrameIndex()) {
				if (this.prevDrawKey != null && this.fxImage != null) {
					AnimationView.this.draw(this.prevDrawKey, this.fxImage);
					this.dirty = false;
				}
			}
		}

		@Override
		public void invalidated(Observable observable) {
			this.markDirty();
		}

		public WritableImage getFxImage(DrawKey key) {
			if (!key.equals(this.prevDrawKey)) {
				if (this.prevDrawKey == null || this.prevDrawKey.width() != key.width() || this.prevDrawKey.height() != key.height()) {
					this.fxImage = new WritableImage(key.width(), key.height());
				}
				this.prevDrawKey = key;
				this.dirty = true;
			}
			if (this.dirty) {
				AnimationView.this.draw(key, this.fxImage);
				this.dirty = false;
			}
			return this.fxImage;
		}
	}

	public static interface DrawParams {}

	public static record DrawKey(LayerNode layer, int width, int height, int frame, DrawParams params) {}
}