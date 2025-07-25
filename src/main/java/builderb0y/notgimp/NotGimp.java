package builderb0y.notgimp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Timer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class NotGimp extends Application {

	public static final Timer TIMER = new Timer(true);

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
			}
		}
		new Thread() {

			{
				this.setDaemon(true);
			}

			@Override
			public void run() {
				while (true) {
					try (Reader reader = new InputStreamReader(new FileInputStream("open_queue"))) {
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
		if (!new File("open_queue").exists()) try {
			new ProcessBuilder("mkfifo", "open_queue").start().waitFor();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
		try (DataInputStream stream = new DataInputStream(new FileInputStream("running_pid"))) {
			long pid = stream.readLong();
			long timestamp = stream.readLong();
			System.out.println("Found existing process: " + pid + " started at " + timestamp);
			ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
			if (handle != null) {
				Instant startTime = handle.info().startInstant().orElse(null);
				if ((startTime != null ? startTime.toEpochMilli() : 0L) == timestamp) {
					System.out.println("Existing process is valid.");
					try (Writer writer = new OutputStreamWriter(new FileOutputStream("open_queue"), StandardCharsets.UTF_8)) {
						for (String arg : args) {
							writer.append(arg).write(0);
						}
					}
					System.exit(0); //why is return not sufficient?
				}
			}
		}
		catch (FileNotFoundException ignored) {}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		try (DataOutputStream stream = new DataOutputStream(new FileOutputStream("running_pid"))) {
			ProcessHandle handle = ProcessHandle.current();
			stream.writeLong(handle.pid());
			Instant startTime = handle.info().startInstant().orElse(null);
			stream.writeLong(startTime != null ? startTime.toEpochMilli() : 0L);
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		launch(args);
	}
}