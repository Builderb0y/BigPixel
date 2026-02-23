package builderb0y.bigpixel;

import java.util.concurrent.locks.LockSupport;

import javafx.application.Platform;

import builderb0y.bigpixel.sources.LayerSource.RedrawException;
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
						try {
							layerNode.sources.currentSource().resizeIfNecessary();
							return layerNode;
						}
						catch (RedrawException exception) {
							layerNode.sources.currentSource().setProgress(0);
							layerNode.redrawException.set(exception.getLocalizedMessage());
						}
					}
				}
				return null;
			});
			if (layer != null) {
				layer.redrawOffThread();
				Platform.runLater(() -> {
					layer.afterRedraw();
					this.graph.getDependants(layer).forEach(LayerNode::requestRedraw);
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