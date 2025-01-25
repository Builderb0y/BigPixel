package builderb0y.notgimp;

import java.io.File;
import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

public class NotGimp extends Application {

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
	}

	public static void main(String[] args) {
		launch(args);
	}
}