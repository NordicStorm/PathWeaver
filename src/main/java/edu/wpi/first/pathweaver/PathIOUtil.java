package edu.wpi.first.pathweaver;

import edu.wpi.first.pathweaver.path.Path;
import edu.wpi.first.pathweaver.path.wpilib.WpilibPath;
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
		List<Integer> newLineNums = new ArrayList<>();
		for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
			String line = lines.get(lineNum);
			boolean edit = false;
			for (int i = 0; i < path.getWaypoints().size(); i++) {
				Waypoint wp = path.getWaypoints().get(i);
				if (wp.lineNumber == lineNum) {
					double xPos = wp.getX();
					double yPos = wp.getY();
					int ignoredLines = 0;
					if (wp.isOutsidecommand() && !line.contains("resetPosition")) {
						String mainLine = line.substring(0, line.indexOf(";") + 1);
						String newMeta = String.format("// ENDPOS:%.3f,%.3f", xPos, height + yPos);

						line = mainLine + newMeta;
					} else {
						String newArgs = String.format("%.3f, %.3f", xPos, height + yPos);

						String currentArgs = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
						line = line.replaceFirst(currentArgs, newArgs);
					}

					int numSpaces = line.length() - (line.stripLeading()).length();
					newLines.add(line);
					newLineNums.add(newLines.size() - 1);
					edit = true;
					while(lineNum < wp.endingLineParallel){
						lineNum++;
						newLines.add(lines.get(lineNum));
					}
					numSpaces = lines.get(lineNum).length() - (lines.get(lineNum).stripLeading()).length();

					for (int j = i + 1; j < path.getWaypoints().size(); j++) {
						// System.out.println("checkline"+j);
						Waypoint possibleNew = path.getWaypoints().get(j);
						if (possibleNew.lineNumber == -1) {
							xPos = possibleNew.getX();
							yPos = possibleNew.getY();
							String newLine = " ".repeat(numSpaces) + String.format("%s.addWaypoint(%.3f, %.3f);",
									path.getPathName(), xPos, height + yPos);
							newLines.add(newLine);
							i += 1;
							newLineNums.add(newLines.size() - 1);

						} else {
							break;
						}
					}
					break;

				} else {

				}

			}

			for (int i = 0; i < path.getDeletedWaypoints().size(); i++) {
				Waypoint wp = path.getDeletedWaypoints().get(i);
				if (wp.lineNumber == lineNum) {
					edit = true; // don't add it to the new version.
				}
			}
			if (!edit) {
				newLines.add(line);
			}

		}
		MainIOUtil.writeLinesToFile(fileLocation, newLines);
		for (int k = 0; k < path.getWaypoints().size(); k++) {
			path.getWaypoints().get(k).lineNumber = newLineNums.get(k);
		}
		path.getDeletedWaypoints().clear();
		// MainController
		// path.getWaypoints().clear();
		// path.getWaypoints().addAll(loadWaypointsFromFile(fileLocation,
		// path.getPathName()));

		return true;
	}

	public static int countChars(String string, String character) {
		return string.length() - string.replace(character, "").length();
	}

	/**
	 * Imports Path object from disk.
	 *
	 * 
	 * @param fileName Name of path file
	 * @param pathName
	 *
	 * @return Path object saved in Path file
	 */
	public static Path importPath(String fileName, String pathName) {
		System.out.println(fileName + ", import " + pathName);

		return new WpilibPath(loadWaypointsFromFile(fileName, pathName), pathName);
	}

	public static List<Waypoint> loadWaypointsFromFile(String fileName, String pathName) {
		String mainMethod = "public void initializeCommands()";
		List<String> lines = MainIOUtil.readLinesFromFile(fileName);
		ArrayList<Waypoint> waypoints = new ArrayList<>();
		boolean hasStarted = false;
		int braceCount = 0;
		int activatedBraceLevel = 1;
		for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
			String line = lines.get(lineNum);

			String content = line.stripLeading();
			if (content.startsWith("//")) {
				continue;
			}
			if (content.contains(mainMethod)) {
				hasStarted = true;
			}
			if (!hasStarted) {
				continue;
			}

			braceCount -= countChars(content, "}");
			if (braceCount < activatedBraceLevel) {
				activatedBraceLevel = braceCount;
			}
			int openBraceNum = countChars(content, "{");
			braceCount += openBraceNum;
			if (openBraceNum > 0) {
				if (braceCount == activatedBraceLevel + 1) {
					if (!content.toLowerCase().replace(" ", "").contains("path off") || content.contains(mainMethod)) {
						activatedBraceLevel = braceCount;
					}
				}
			}
			if (braceCount != activatedBraceLevel) {
				continue;
			}
			if (braceCount == 0) {
				hasStarted = false;
			}
			if (content.startsWith(pathName + ".")) {
				String method = "";
				if (content.startsWith(pathName + "."))
					method = content.substring((pathName + ".").length());
				if (content.contains(".setPose"))
					method = content.substring(content.indexOf(".setPose") + 1);
				System.out.println(method);
				String methodName = method.substring(0, method.indexOf("("));
				String allParams = method.substring(method.indexOf("(") + 1, method.indexOf(";") - 1);
				int parenCount = countChars(allParams, "(");
				if (parenCount > 1) {
					LOGGER.log(Level.WARNING, "Complicated line of code: " + line);
					// continue;
				}
				String[] rawParams = allParams.split(",\\s*(?![^()]*\\))");
				List<String> params = Arrays.stream(rawParams)
						.map(s -> s.strip())
						.collect(Collectors.toList());

				double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();

				if (methodName.equals("addWaypoint") || methodName.equals("resetPosition")) {
					double x = Double.parseDouble(params.get(0));
					double y = Double.parseDouble(params.get(1));

					Waypoint point = new Waypoint(new Point2D(x, y - height), new Point2D(1, 1),
							methodName.equals("resetPosition"));

					point.lineNumber = lineNum;
					point.numberOfLinesInSection = 0;// TODO
					point.setName(lineNum + 1 + ": " + methodName);
					waypoints.add(point);
				} else if (methodName.equals("addSequentialCommand") || methodName.equals("resetPosition")) {
					String text = lineNum + 1 + ": " + params.get(0);
					if (line.toUpperCase().replace(" ", "").indexOf("NOMOVE") != -1) {
						waypoints.get(waypoints.size() - 1).addParallel(text, lineNum);
					} else {
						String metaKey = "ENDPOS:";
						int metaIndex = line.indexOf(metaKey);
						double x = 0;
						double y = 0;
						if (metaIndex == -1) {
							x = waypoints.get(waypoints.size() - 1).getX() + 0.5;
							y = waypoints.get(waypoints.size() - 1).getY() + height + 0.5;
						} else {
							String info = line.substring(metaIndex + metaKey.length());
							String[] rawPos = info.split(",");

							x = Double.parseDouble(rawPos[0]);
							y = Double.parseDouble(rawPos[1]);
						}

						Waypoint point = new Waypoint(new Point2D(x, y - height), new Point2D(1, 1), true);
						if (methodName.equals("addSequentialCommand")) {
							point.setName(text);
						}
						point.lineNumber = lineNum;
						point.numberOfLinesInSection = 0;// TODO
						waypoints.add(point);
					}
				}

			}
		}

		return waypoints;
	}
}
