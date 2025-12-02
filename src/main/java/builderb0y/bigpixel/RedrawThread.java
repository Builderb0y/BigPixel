package builderb0y.bigpixel;

import java.util.concurrent.locks.LockSupport;

import javafx.application.Platform;

import builderb0y.bigpixel.util.Util;

public class RedrawThread extends Thread {

	public final LayerGraph graph;
	public volatile boolean running = true;

	public RedrawThread(LayerGraph graph) {
		this.graph = graph;
		super("Redraw Thread");
		this.setDaemon(true);
	}

	@Override
	public void run() {
		while (this.running) try {
			LayerNode layer = Util.getAndWait(() -> {
				for (LayerNode layerNode : this.graph.layerList) {
					if (layerNode.redrawRequested) {
						layerNode.redrawRequested = false;
						return layerNode;
					}
				}
				return null;
			});
			if (layer != null) {
				layer.redrawOffThread();
				Platform.runLater(() -> {
					for (HDRImage frame : layer.animation.frames) {
						frame.invalidate();
					}
					this.graph.getDependants(layer).forEach(LayerNode::requestRedraw);
					if (this.graph.getVisibleLayer().views.currentView().getDependencies().dependsOn(layer)) {
						this.graph.openImage.imageDisplay.invalidateAll();
					}
				});
			}
			else {
				LockSupport.park();
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public void wakeup() {
		LockSupport.unpark(this);
	}

	public void shutdown() {
		this.running = false;
		this.wakeup();
	}
}