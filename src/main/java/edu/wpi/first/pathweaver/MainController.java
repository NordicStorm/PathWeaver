package edu.wpi.first.pathweaver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.wpi.first.pathweaver.global.CurrentSelections;
import edu.wpi.first.pathweaver.path.Path;
import edu.wpi.first.pathweaver.path.wpilib.WpilibPath;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


//With the creation of a project many of these functions should be moved out of here
//Anything to do with the directory should be part of a Project object

@SuppressWarnings({"PMD.UnusedPrivateMethod","PMD.AvoidFieldNameMatchingMethodName",
  "PMD.GodClass", "PMD.TooManyFields"})
public class MainController {
  private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());


  @FXML private TreeView<String> paths;

  @FXML private Pane fieldDisplay;
  @FXML private FieldDisplayController fieldDisplayController;

  @FXML private GridPane editWaypoint;
  @FXML private EditWaypointController editWaypointController;

  private String directory = ProjectPreferences.getInstance().getFileName();

  private final TreeItem<String> pathRoot = new TreeItem<>("Paths");

  private TreeItem<String> selected = null;

  @FXML
  private void initialize() {
    setupDrag();

    setupTreeView(paths, pathRoot, FxUtils.menuItem("New Path...", event -> createPath()));

    loadAllPathsInFile(pathRoot);
    setupClickablePaths();
    

    paths.setEditable(false);
    setupEditable();

    editWaypointController.bindToWaypoint(CurrentSelections.curWaypointProperty(), fieldDisplayController);
    reloadAllPaths();
  }

  private void setupTreeView(TreeView<String> treeView, TreeItem<String> treeRoot, MenuItem newItem) {
    treeView.setRoot(treeRoot);
    treeView.setContextMenu(new ContextMenu());
    treeView.getContextMenu().getItems().addAll(newItem, FxUtils.menuItem("Delete", event -> delete()));
    treeRoot.setExpanded(true);
    treeView.setShowRoot(false); // Don't show the roots "Paths" and "Autons" - cleaner appearance
  }

  /**
   * Parse the file to find path names and load the names into the tree view
   */
  private void loadAllPathsInFile(TreeItem<String> root) {
	  root.getChildren().clear();
	  List<String> lines = MainIOUtil.readLinesFromFile(directory);
	  System.out.println("len"+lines.size());
    boolean hasStarted = false;
	  int braceCount = 0;
	  int activatedBraceLevel = 1;
    String mainMethod = "public void initializeCommands()";
	  for (String line : lines) {
      if(line.startsWith("//")){
			  continue;
		  }
		  if(line.contains(mainMethod)) {
			  hasStarted = true;
		  }
		  if(!hasStarted) {continue;}
		  
		  braceCount-= PathIOUtil.countChars(line, "}");
		  if(braceCount < activatedBraceLevel){
			  activatedBraceLevel = braceCount;
		  }
		  int openBraceNum = PathIOUtil.countChars(line, "{");
		  braceCount+=openBraceNum;
		  if(openBraceNum>0){
			  if(braceCount == activatedBraceLevel+1){ 
				if(!line.contains("path off") || line.contains(mainMethod)){
					activatedBraceLevel = braceCount;
				}
			  }
		  }
		  if(braceCount != activatedBraceLevel){
			continue;
		  }
		  if(braceCount == 0) {
			  hasStarted = false;
		  }
		  String keyword = "MultiPartPath ";
		  if(line.contains(keyword)) {
			  String name = null;
			  name = line.substring(line.indexOf(keyword)+keyword.length(), line.indexOf(";"));
        //System.out.println("name"+name);

        int equalIndex = name.indexOf("=");
			  if(equalIndex==-1) {
				  //no equal, so it is just a declaration
			  }else {
				  if(name.contains("new ")) { // will be like "path = new MultiPartPath"
					  name = name.substring(0, equalIndex).strip();
            //System.out.println("name2"+name);

				  }else {
				      LOGGER.log(Level.WARNING, "Invalid line for path declaration");
				  }
			  }
			  
			  TreeItem<String> item = new TreeItem<>(name);
			  root.getChildren().add(item);
		  }
		  
	  }
  }
  
  private void setupEditable() {
    //what to do when the name gets edited
    
  }


  @FXML
  private void delete() {
    if (selected == null) {
      // have nothing selected
      return;
    }
    TreeItem<String> root = getRoot(selected);
    if (selected == root) {
      // clicked impossible thing to delete
      return;
    }
    if (pathRoot == root && FxUtils.promptDelete(selected.getValue())) {
      //fieldDisplayController.removeAllPath();
      SaveManager.getInstance().removeChange(CurrentSelections.curPathProperty().get());
      MainIOUtil.deleteItem(directory, selected);
      for (TreeItem<String> path : getAllInstances(selected)) {
        removePath(path);
      }
      
    }
  }

  @FXML
  private void deleteAuton() {
    
  }

  @FXML
  private void keyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.DELETE
        || event.getCode() == KeyCode.BACK_SPACE) {
      delete();
    }
  }

  private List<TreeItem<String>> getAllInstances(TreeItem<String> chosenPath) {
    List<TreeItem<String>> list = new ArrayList<>();
    /*for (TreeItem<String> auton : autonRoot.getChildren()) {
      for (TreeItem<String> path : auton.getChildren()) {
        if (path.getValue().equals(chosenPath.getValue())) {
          list.add(path);
        }
      }
    }*/
    return list;
  }

  private void removePath(TreeItem<String> path) {
    
  }


  private TreeItem<String> getRoot(TreeItem<String> item) {
    TreeItem<String> root = item;
    while (root.getParent() != null) {
      root = root.getParent();
    }
    return root;
  }


  private void setupClickablePaths() {
    ChangeListener<TreeItem<String>> selectionListener =
        new ChangeListener<>() {
          @Override
          public void changed(ObservableValue<? extends TreeItem<String>> observable, TreeItem<String> oldValue,
                              TreeItem<String> newValue) {
            /*if (!SaveManager.getInstance().promptSaveAll()) {
              paths.getSelectionModel().selectedItemProperty().removeListener(this);
              paths.getSelectionModel().select(oldValue);
              paths.getSelectionModel().selectedItemProperty().addListener(this);
              return;
            }*/
            selected = newValue;
            if (newValue != pathRoot && newValue != null) {
              fieldDisplayController.removeAllPath();
              fieldDisplayController.addPath(directory, newValue);
              CurrentSelections.getCurPath().selectWaypoint(CurrentSelections.getCurPath().getStart());
            }
          }
        };
    paths.getSelectionModel().selectedItemProperty().addListener(selectionListener);
  }


  

  private boolean validPathName(String oldName, String newName) {
    return MainIOUtil.isValidRename(directory, oldName, newName);
  }

 


  private void setupDrag() {
    paths.setCellFactory(param -> new PathCell(false, this::validPathName));
    
  }

  @FXML
  private void flipHorizontal() {
    fieldDisplayController.flip(true);
  }

  @FXML
  private void flipVertical() {
    fieldDisplayController.flip(false);
  }

  @FXML
  private void duplicate() {
    Path newPath = fieldDisplayController.duplicate(directory);
    TreeItem<String> stringTreeItem = MainIOUtil.addChild(pathRoot, newPath.getPathName());
    SaveManager.getInstance().saveChange(newPath);
    paths.getSelectionModel().select(stringTreeItem);
  }

  @FXML
  private void createPath() {
    String name = MainIOUtil.getValidFileName(directory, "Unnamed", ".path");
    MainIOUtil.addChild(pathRoot, name);
    Path newPath = new WpilibPath(name);
    // The default path defaults to FEET
    newPath.convertUnit(PathUnits.FOOT, ProjectPreferences.getInstance().getValues().getLengthUnit());
    SaveManager.getInstance().saveChange(newPath);
  }



  @FXML

  private void buildPaths() {
	if(1==1) {return;}//disabled in custom version
    if (!SaveManager.getInstance().promptSaveAll()) {
      return;
    }

    java.nio.file.Path output =  Paths.get(ProjectPreferences.getInstance().getFileName());
    try {
      Files.createDirectories(output);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Could not export to " + output, e);
    }
    for (TreeItem<String> pathName : pathRoot.getChildren()) {
      Path path = PathIOUtil.importPath(directory, pathName.getValue());

      java.nio.file.Path pathNameFile = output.resolve(path.getPathNameNoExtension());

      if(!path.getSpline().writeToFile(pathNameFile)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        FxUtils.applyDarkMode(alert);
        alert.setTitle("Path export failure!");
        alert.setContentText("Could not export to: " + output.toAbsolutePath());
      }
    }
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    FxUtils.applyDarkMode(alert);
    alert.setTitle("Paths exported!");
    alert.setContentText("Paths exported to: " + output.toAbsolutePath());

    alert.show();
  }

  @FXML
  private void editProject() {
    try {
      Pane root = FXMLLoader.load(getClass().getResource("createProject.fxml"));
      Scene scene = fieldDisplay.getScene();
      Stage primaryStage = (Stage) scene.getWindow();
      primaryStage.setMaximized(false);
      primaryStage.setResizable(false);
      scene.setRoot(root);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Couldn't load create project screen", e);
    }
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }
  
  public void reloadAllPaths() {
	  int lastSelection = paths.getSelectionModel().getSelectedIndex();
	  System.out.println("sel:"+lastSelection);
	  pathRoot.getChildren().clear();
	  fieldDisplayController.removeAllPath();
	  loadAllPathsInFile(pathRoot);
	  var allPaths = paths.getRoot().getChildren();
	  System.out.println("paths:"+allPaths.toString());

	  paths.getSelectionModel().clearAndSelect(lastSelection);
	  for(TreeItem<String> pathName : allPaths) {
	      //fieldDisplayController.addPath(directory, pathName);
	  }

  }
}

