package edu.wpi.first.pathweaver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.SingleMethodSingleton")
public class ProjectPreferences {
	public enum ExportUnit {
		METER("Always Meters"), SAME("Same as Project");
		private static final Map<String, ExportUnit> STRING_EXPORT_UNIT_MAP;

		private final String name;

		ExportUnit(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		static {
			STRING_EXPORT_UNIT_MAP = Arrays.stream(values()).collect(Collectors.toMap(n -> n.name, n -> n));
		}

		public static ExportUnit fromString(String s) {
			ExportUnit result = STRING_EXPORT_UNIT_MAP.get(s);

			if (result == null) {
				throw new IllegalArgumentException();
			}

			return result;
		}
	}

	private static ProjectPreferences instance;
	
	private final String fileName;
	private final String KEYWORD="// !PATHWEAVER_INFO: ";
	private Values values;
	private WatchKey fileWatchKey;
	
	private ProjectPreferences(String fileName) {
		this.fileName = fileName;
		Path directoryPath = Paths.get(fileName).getParent();
		FileWatcherThread.getInstance().clearAllWatchedDirsAndFiles();
		FileWatcherThread.getInstance().registerDirectory(directoryPath);
        FileWatcherThread.getInstance().registerFileCallback(fileName, new Runnable() {
        	@Override
            public void run(){
        		Platform.runLater(new Runnable(){ // has to be on the JavaFx thread.
					@Override
					public void run() {		
						FxUtils.getMainControllerInstance().reloadAllPaths();
					}
        		});
            }
        });

		try {
			Gson gson = new Gson();
			List<String> lines = MainIOUtil.readLinesFromFile(fileName);
			List<String> newLines = new ArrayList<String>();
			boolean existing = false;
			for(String line:lines) { 
				  if(line.startsWith(KEYWORD)) {
					  String data = line.substring(KEYWORD.length()); 
					  values = gson.fromJson(data, Values.class);
					  System.out.println("found with keyword");
					  break;
				  }
			}
			if(values == null) {
				System.out.println("not found");
				setDefaults();
			}
			values.outputDir = fileName;

			if (values.gameName == null) {
				values.gameName = Game.INFINTE_RECHARGE_2020.getName();
			}
			
		} catch (JsonParseException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			FxUtils.applyDarkMode(alert);
			alert.setTitle("Preferences import error");
			alert.setContentText(
					"Preferences have been reset due to file corruption. You may have to reconfigure your project.");
			((Stage) alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

			alert.show();
			setDefaults();
		}
	}

	private void setDefaults() {
		values = new Values(2.0, Game.INFINTE_RECHARGE_2020.getName(), fileName);
		updateValues();
	}

	private void updateValues() {
		Gson gson = new GsonBuilder().create();
		
		String data = gson.toJson(values);

		
		  List<String> lines = MainIOUtil.readLinesFromFile(fileName);
		  List<String> newLines = new ArrayList<String>();
		  boolean existing = false;
		  for(String line:lines) { // first
			  if(!existing && line.startsWith(KEYWORD)) {
				  line = KEYWORD+data;
				  existing=true;
			  }
			  newLines.add(line);
		  }
		  if(existing) {
			  MainIOUtil.writeLinesToFile(fileName, newLines);
			  return;
		  }
		  newLines.clear();
		  //OK, so we need to place a new keyword location
		  String spotToPut = "public void initializeCommands()";
		  for(String line:lines) { // first
			  
			  newLines.add(line);
			  if(!existing && line.contains(spotToPut)) {
				  String newLine = KEYWORD+data;
				  newLines.add(newLine); // put the data right after the initializeCommands definition start
				  existing=true;
				  
			  }
		  }
		  if(existing) {
			  MainIOUtil.writeLinesToFile(fileName, newLines);
			  return;
		  }
		  MainIOUtil.writeLinesToFile(fileName, newLines);
		  Alert alert = new Alert(AlertType.ERROR, "Couldn't find a valid place to store pathweaver info. Please make sure that the file chosen is an AutoWithInit.");
		  alert.showAndWait();
			
	}

	/**
	 * Sets the preferences for the current project.
	 *
	 * @param values
	 *            Values to set for preferences.
	 */
	public void setValues(Values values) {
		this.values = values;
		updateValues();
	}

	public String getFileName() {
		return fileName;
	}

	/**
	 * Return the singleton instance of ProjectPreferences for a given project
	 * directory.
	 *
	 * @param name
	 *            Path to project file.
	 * @return Singleton instance of ProjectPreferences.
	 */
	@SuppressWarnings("PMD.NonThreadSafeSingleton")
	public static ProjectPreferences getInstance(String name) {
		if (instance == null || !instance.fileName.equals(name)) {
			instance = new ProjectPreferences(name);
		}
		return instance;
	}

	/**
	 * Returns the singleton instance of ProjectPreferences for the previously
	 * requested directory or the default directory.
	 *
	 * @return Singleton instance of ProjectPreferences.
	 */
	public static ProjectPreferences getInstance() {
		return instance;
	}

	public static void resetInstance() {
			instance = null;
		}

	public static boolean projectExists(String name) {
		return Files.exists(Paths.get(name));
	}

	/**
	 * Returns a Field object for the current project's game year. Defaults to Power
	 * Up.
	 *
	 * @return Field for project's game year.
	 */
	public Field getField() {
		if (values.getGameName() == null) {
			values.gameName = Game.DEEP_SPACE_2019.getName();
		}
		Game game = Game.fromPrettyName(values.gameName);
		if (game == null) {
			throw new UnsupportedOperationException("The referenced game is unknown: \"" + values.gameName + "\"");
		}
		Field field = game.getField();
		field.convertUnit(values.getLengthUnit());
		return field;
	}


	public Values getValues() {
		return values;
	}

	public static class Values {
	
		@SerializedName(value = "trackWidth", alternate = "wheelBase")
		private final double trackWidth;
		private String gameName;
		private String outputDir;

		/**
		 * Constructor for Values of ProjectPreferences.
		 *
		 * @param lengthUnit
		 *            The unit to use for distances
		 * @param maxVelocity
		 *            The maximum velocity the body is capable of travelling at
		 * @param maxAcceleration
		 *            The maximum acceleration to use
		 * @param trackWidth
		 *            The width between the center of each tire of the drivebase.  Even better would be a calculated
		 *            track width from robot characterization.
		 * @param gameName
		 *            The year/FRC game
		 * @param outputDir
		 *            The directory for the output files
		 */
		public Values(double trackWidth, String gameName, String outputDir) {
			
			this.trackWidth = trackWidth;
			this.gameName = gameName;
			this.outputDir = outputDir;
		}

		public Unit<Length> getLengthUnit() {
			return PathUnits.METER;
		}

		public ExportUnit getExportUnit() {
			return ExportUnit.METER;
		}

		
		public double getTrackWidth() {
			return trackWidth;
		}

		public String getGameName() {
			return gameName;
		}

		public String getOutputDir() {
			return outputDir;
		}
	}
}
