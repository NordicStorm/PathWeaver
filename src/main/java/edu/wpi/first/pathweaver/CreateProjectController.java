package edu.wpi.first.pathweaver;

import org.fxmisc.easybind.EasyBind;

import com.sun.javafx.tk.FileChooserType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.Duration;
import tech.units.indriya.format.SimpleUnitFormat;

import javax.measure.Unit;
import javax.measure.quantity.Length;

@SuppressWarnings("PMD")
public class CreateProjectController {
	@FXML
	private Label title;

	@FXML
	private Button browseOutput;
	@FXML
	private Button create;
	@FXML
	private Button cancel;
	@FXML
	private VBox vBox;

	@FXML
	private TextField outputDirectory;

	@FXML
	private TextField trackWidth;
	@FXML
	private ChoiceBox<Game> game;

	@FXML
	public ChoiceBox<ProjectPreferences.ExportUnit> export;
	@FXML
	private Label browseLabel;
	@FXML
	private Label outputLabel;

	@FXML
	private Label trackWidthLabel;


	private boolean editing = false;

	@FXML

	private void initialize() {
		ObservableList<TextField> numericFields = FXCollections.observableArrayList(trackWidth);
		ObservableList<TextField> allFields = FXCollections.observableArrayList(numericFields);
		allFields.add(outputDirectory);
		var outputControls = List.of(outputLabel, outputDirectory, browseOutput);
		
		var trackWidthControls = List.of(trackWidthLabel, trackWidth);

		BooleanBinding bind = new SimpleBooleanProperty(true).not();
		for (TextField field : allFields) {
			bind = bind.or(field.textProperty().isEmpty());
		}
		bind = bind.or(game.valueProperty().isNull());
		create.disableProperty().bind(bind);

		// Validate that numericFields contain decimal numbers
		numericFields.forEach(textField -> textField.setTextFormatter(FxUtils.onlyPositiveDoubleText()));

		game.getItems().addAll(Game.getGames());
		game.getSelectionModel().select(Game.DEFAULT_GAME);
		game.converterProperty().setValue(new StringConverter<>() {
			@Override
			public String toString(Game object) {
				return object.getName();
			}

			@Override
			public Game fromString(String string) {
				return Game.fromPrettyName(string);
			}
		});
		
		
		outputControls
				.forEach(control -> control.setTooltip(new Tooltip("The file you are editing")));
		
		trackWidthControls.forEach(
				control -> control.setTooltip(new Tooltip("The width between the center of each tire of the " +
						"drivebase.  Even better would be a calculated track width from robot characterization.")));
		

		// We are editing a project
		if (ProjectPreferences.getInstance() != null) {
			setupEditProject();
		}
		else {
			setupCreateProject();
		}
	}

	private void setupCreateProject() {
		outputDirectory.setText("");
		create.setText("Create Project");
		title.setText("Create Project...");
		cancel.setText("Cancel");
		
		game.getSelectionModel().select(Game.DEFAULT_GAME);
		
		trackWidth.setText("");
		editing = false;
	}

	@FXML
	private void createProject() {
		
		String outputString = outputDirectory.getText();
		String outputPath = outputString;
		boolean newOutput = !editing
				|| !Objects.equals(outputString, ProjectPreferences.getInstance().getValues().getOutputDir());
		if (outputString != null && !outputString.isEmpty() && newOutput) {
			
		}
		ProgramPreferences.getInstance().addProject(outputPath);
		
		double trackWidthDistance = Double.parseDouble(trackWidth.getText());
		ProjectPreferences.Values values = new ProjectPreferences.Values(trackWidthDistance, game.getValue().getName(), outputPath);
		ProjectPreferences prefs = ProjectPreferences.getInstance(outputPath);
		prefs.setValues(values);
		editing = false;
		FxUtils.loadMainScreen(vBox.getScene(), getClass());
	}


	@FXML
	private void browseOutput() {
		browseForFile(outputDirectory);
	}

	private void browse(TextField location) {
		DirectoryChooser chooser = new DirectoryChooser();
		File selectedDirectory = chooser.showDialog(vBox.getScene().getWindow());
		if (selectedDirectory != null) {
			location.setText(selectedDirectory.getPath());
			location.positionCaret(selectedDirectory.getPath().length());
		}
	}
	private void browseForFile(TextField location) {
		FileChooser chooser = new FileChooser();
		File selectedDirectory = chooser.showOpenDialog(vBox.getScene().getWindow());
		if (selectedDirectory != null) {
			location.setText(selectedDirectory.getPath());
			location.positionCaret(selectedDirectory.getPath().length());
		}
	}

	@FXML
	private void cancel() throws IOException {
		ProjectPreferences.resetInstance();
		Pane root = FXMLLoader.load(getClass().getResource("welcomeScreen.fxml"));
		vBox.getScene().setRoot(root);
	}

	private void setupEditProject() {
		ProjectPreferences.Values values = ProjectPreferences.getInstance().getValues();
		
		outputDirectory.setText(ProjectPreferences.getInstance().getValues().getOutputDir());
		create.setText("Save Project");
		title.setText("Edit Project");
		cancel.setText("Select Project");

		game.setValue(Game.fromPrettyName(values.getGameName()));

		trackWidth.setText(String.valueOf(values.getTrackWidth()));
		editing = true;
	}
}
