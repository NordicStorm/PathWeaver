package edu.wpi.first.pathweaver;

import edu.wpi.first.pathweaver.path.Path;
import edu.wpi.first.pathweaver.path.wpilib.WpilibPath;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.geometry.Point2D;

public final class PathIOUtil {
  private static final Logger LOGGER = Logger.getLogger(PathIOUtil.class.getName());

  private PathIOUtil() {
  }


  /**
   * Writes the path into the file
   *
   * @param fileLocation the directory and filename to write to
   * @param path         followable Path object to save
   *
   * @return true if successful file write was performed
   */
  public static boolean export(String fileLocation, Path path) {
      double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();

	  List<String> lines = MainIOUtil.readLinesFromFile(fileLocation);
	  List<String> newLines = new ArrayList<>();
	  int currentlyDoneWpIndex = 0;
	  List<Integer> newLineNums = new ArrayList<>();
	  for(int lineNum=0; lineNum<lines.size(); lineNum++) {
		  String line = lines.get(lineNum);
		  boolean edit = false;
		  
		  for (int i=0; i<path.getWaypoints().size(); i++){
			  Waypoint wp = path.getWaypoints().get(i);
			  if(wp.lineNumber == lineNum) {
				  double xPos = wp.getX();
			      double yPos = wp.getY();
			      String newArgs = String.format("%.3f, %.3f", xPos, height + yPos);
			     
				  String currentArgs = line.substring(line.indexOf("(")+1, line.indexOf(")"));
				  line = line.replaceFirst(currentArgs, newArgs);
				  int numSpaces = line.length()-(line.stripLeading()).length();
				  newLines.add(line);
			      newLineNums.add(newLines.size()-1);

				  edit=true;
				  for (int j = i+1; j<path.getWaypoints().size(); j++){
					  //System.out.println("checkline"+j);
					  Waypoint possibleNew = path.getWaypoints().get(j);
					  if(possibleNew.lineNumber == -1) {
						  xPos = possibleNew.getX();
					      yPos = possibleNew.getY();
					      String newLine = " ".repeat(numSpaces)+String.format("%s.addWaypoint(%.3f, %.3f);", path.getPathName(), xPos, height + yPos);
					      newLines.add(newLine);
					      i+=1;
					      newLineNums.add(newLines.size()-1);
					      
					      
					  }else {
						  break;
					  }
				  }
				  break;
				  
			  }else {
				  
			  }
		      
		  }
		  for (int i=0; i<path.getDeletedWaypoints().size(); i++){
			  Waypoint wp = path.getDeletedWaypoints().get(i);
			  if(wp.lineNumber == lineNum) {
				  edit = true; // don't add it to the new version.
			  }
		  }
		  if(!edit) {
			  newLines.add(line);
			  
		  }
		  
	  }
      MainIOUtil.writeLinesToFile(fileLocation, newLines);
      for(int k =0;k<path.getWaypoints().size();k++) {
    	  path.getWaypoints().get(k).lineNumber = newLineNums.get(k);
      }
      path.getDeletedWaypoints().clear();
      //MainController 
      //path.getWaypoints().clear();
      //path.getWaypoints().addAll(loadWaypointsFromFile(fileLocation, path.getPathName()));
    
    return true;
  }

  public static int countChars(String string, String character) {
	return string.length() - string.replace(character, "").length();
	  
  }
  /**
   * Imports Path object from disk.
   *
   * 
   * @param fileName     Name of path file
 * @param pathName 
   *
   * @return Path object saved in Path file
   */
  public static Path importPath(String fileName, String pathName) {
	  System.out.println(fileName+", import "+pathName);
	  
	  return new WpilibPath(loadWaypointsFromFile(fileName, pathName), pathName);
    
  }
  public static List<Waypoint> loadWaypointsFromFile(String fileName, String pathName){
	  String mainMethod = "public void initializeCommands()";
	  List<String> lines = MainIOUtil.readLinesFromFile(fileName);
	  ArrayList<Waypoint> waypoints = new ArrayList<>();
	  boolean hasStarted = false;
	  int braceCount = 0;
	  for(int lineNum = 0; lineNum<lines.size(); lineNum++) {
		  String line= lines.get(lineNum);
		  String content = line.stripLeading();
		  if(content.contains(mainMethod)) {
			  hasStarted = true;
		  }
		  if(!hasStarted) {continue;}
		  
		  
		  braceCount+=countChars(content, "{") ;
		  braceCount-=countChars(content, "}") ;
		  if(braceCount == 0) {
			  hasStarted = false;
		  }
		  if(content.startsWith(pathName+".")) {
			  String method = content.substring((pathName+".").length());
			  String methodName = method.substring(0, method.indexOf("("));
			  String allParams = method.substring(method.indexOf("(")+1, method.indexOf(";")-1); 
			  int parenCount = countChars(allParams, "(");
			  if(parenCount>1) {
				  LOGGER.log(Level.WARNING, "Too complicated line of code: "+line);
				  continue;
			  }
			  String[] rawParams = allParams.split(",");
			  List<String> params = Arrays.stream(rawParams)
                      .map(s->s.strip())
                      .collect(Collectors.toList());
			  System.out.println(method);
			  System.out.println(params);

			  if(methodName.equals("addWaypoint")) {
				  double x = Double.parseDouble(params.get(0));
				  double y = Double.parseDouble(params.get(1));
			      double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();
			      
				  Waypoint point = new Waypoint(new Point2D(x, y - height), new Point2D(1, 1), false, false);
				  
			      point.lineNumber = lineNum;
			      point.numberOfLinesInSection = 0;//TODO
			      waypoints.add(point);
			  }
			  
		  }
	  }

	  return waypoints;
  }
}
