package builderb0y.bigpixel;

import java.io.*;
import java.lang.ProcessHandle.Info;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class BigPixel extends Application {

	public static final String openQueueProperty = "builderb0y.bigpixel.openQueue";
	public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor((Runnable task) -> {
		Thread thread = new Thread(task, "Scheduler Thread");
		thread.setDaemon(true);
		return thread;
	});

	@Override
	public void start(Stage stage) {
		MainWindow mainWindow = new MainWindow(stage);
		mainWindow.init();
		mainWindow.show();
		List<String> toOpen = this.getParameters().getRaw();
		if (!toOpen.isEmpty()) {
			for (String fileName : toOpen) {
				File file = new File(fileName);
				if (file.isFile()) mainWindow.doOpen(file);
				else System.err.println("File not found or not normal: " + file.getAbsolutePath());
			}
		}
		new Thread("Open Queue Reader Thread") {

			{
				this.setDaemon(true);
			}

			@Override
			public void run() {
				while (true) {
					try (Reader reader = new InputStreamReader(new FileInputStream(System.getProperty(openQueueProperty)), StandardCharsets.UTF_8)) {
						StringBuilder current = new StringBuilder();
						while (true) {
							int read = reader.read();
							if (read < 0) {
								break; //inner loop only.
							}
							else if (read == 0) {
								File file = new File(current.toString());
								current.setLength(0);
								Platform.runLater(() -> {
									if (file.isFile()) {
										mainWindow.doOpen(file);
									}
								});
							}
							else {
								current.append((char)(read));
							}
						}
					}
					catch (IOException exception) {
						exception.printStackTrace();
					}
				}
			}
		}
		.start();
	}

	public static void main(String[] args) {
		String ourQueue = System.getProperty(openQueueProperty);
		if (ourQueue == null) {
			System.err.println("Must run with -D" + openQueueProperty + "=/path/to/pipe");
			System.exit(1);
		}
		File ourQueueFile = new File("open_queue");
		if (!ourQueueFile.exists()) try {
			new ProcessBuilder("mkfifo", "open_queue").start().waitFor();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
		record ProcessQueueLocation(ProcessHandle handle, Info info, File openQueue) implements Comparable<ProcessQueueLocation> {

			@Override
			public int compareTo(@NotNull ProcessQueueLocation that) {
				int compare = this.info.startInstant().orElseThrow().compareTo(that.info.startInstant().orElseThrow());
				if (compare != 0) return compare;
				return Long.compare(this.handle.pid(), that.handle.pid());
			}
		}
		ProcessHandle current = ProcessHandle.current();
		ProcessQueueLocation self = new ProcessQueueLocation(current, current.info(), ourQueueFile);
		ProcessQueueLocation first = (
			ProcessHandle.allProcesses().map((ProcessHandle handle) -> {
				Info info = handle.info();
				if (info.startInstant().isPresent()) {
					String command = info.command().orElse(null);
					if (command != null && (command.equals("java") || command.endsWith("/java"))) {
						String[] arguments = info.arguments().orElse(null);
						if (arguments != null) {
							for (String argument : arguments) {
								final String start = "-D" + openQueueProperty + "=";
								if (argument.startsWith(start)) {
									argument = argument.substring(start.length());
									File openQueue = new File(argument);
									if (openQueue.exists()) {
										return new ProcessQueueLocation(handle, info, openQueue);
									}
								}
							}
						}
					}
				}
				return null;
			})
			.filter(Objects::nonNull)
			.min(Comparator.naturalOrder())
			.orElse(null)
		);
		if (first != null && first.compareTo(self) < 0) {
			System.out.println("Found existing process: " + first.handle.pid());
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(first.openQueue), StandardCharsets.UTF_8)) {
				for (String arg : args) {
					writer.append(arg).write(0);
				}
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
			System.exit(0);
		}
		launch(args);
	}
}