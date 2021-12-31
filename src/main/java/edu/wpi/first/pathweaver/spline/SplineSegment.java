package edu.wpi.first.pathweaver.spline;

import edu.wpi.first.pathweaver.Waypoint;
import edu.wpi.first.pathweaver.global.CurrentSelections;
import edu.wpi.first.pathweaver.path.Path;
import javafx.scene.shape.Polyline;

public class SplineSegment {
	private final Polyline line = new Polyline();
    private final Polyline outerLine = new Polyline();
    private Waypoint start;
    private Waypoint end;

    public SplineSegment(Waypoint start, Waypoint end, Path path) {
        this.start = start;
        this.end = end;
        line.getStyleClass().addAll("pathinner");
        outerLine.getStyleClass().addAll("pathouter");
        line.setOnDragDetected(event -> {
            CurrentSelections.setCurSplineStart(this.start);
            CurrentSelections.setCurSplineEnd(this.end);
            CurrentSelections.setCurPath(path);
        });

        line.setOnMouseClicked(event -> {
            CurrentSelections.setCurPath(path);
            event.consume();
        });
    }

    public Polyline getOuterLine() {
        return outerLine;
    }
    public Polyline getLine() {
        return line;
    }
    public void addPointToLines(double val) {
    	line.getPoints().add(val);
    	outerLine.getPoints().add(val);
    }

    public Waypoint getStart() {
        return start;
    }

    public Waypoint getEnd() {
        return end;
    }

    public void setStart(Waypoint start) {
        this.start = start;
    }

    public void setEnd(Waypoint end) {
        this.end = end;
    }
}
