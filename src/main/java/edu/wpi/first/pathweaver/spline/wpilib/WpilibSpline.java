package edu.wpi.first.pathweaver.spline.wpilib;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.spline.PoseWithCurvature;
import edu.wpi.first.math.spline.QuinticHermiteSpline;
import edu.wpi.first.math.spline.Spline;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.TrajectoryUtil;
import edu.wpi.first.pathweaver.FxUtils;
import edu.wpi.first.pathweaver.PathUnits;
import edu.wpi.first.pathweaver.ProjectPreferences;
import edu.wpi.first.pathweaver.Waypoint;
import edu.wpi.first.pathweaver.path.Path;
import edu.wpi.first.pathweaver.spline.AbstractSpline;
import edu.wpi.first.pathweaver.spline.SplineSegment;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import javax.measure.UnitConverter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A WpilibSpline interfaces with Wpilib to
 * calculate splines.
 */
public class WpilibSpline extends AbstractSpline {
    private static final Logger LOGGER = Logger.getLogger(WpilibSpline.class.getName());

    private final SimpleDoubleProperty strokeWidth = new SimpleDoubleProperty(1.0);
    private int subchildIdx = 0;

    private final Path path;

    @Override
    public void enableSubchildSelector(int i) {
        this.subchildIdx = i;
        for (Node node : group.getChildren()) {
            FxUtils.enableSubchildSelector(node, subchildIdx);
            node.applyCss();
        }
    }

    @Override
    public void removeFromGroup(Group splineGroup) {
        splineGroup.getChildren().remove(group);
    }

    public WpilibSpline(List<Waypoint> waypoints, Path path) {
        super(waypoints);
        this.path = path;
    }

    @Override
    public void update() {
        ProjectPreferences.Values prefs = ProjectPreferences.getInstance().getValues();
        
        group.getChildren().clear();
        for (int i = 1; i < waypoints.size(); i++) {
            Waypoint segStart = waypoints.get(i - 1).copy();
            Waypoint segEnd = waypoints.get(i).copy();
            QuinticHermiteSpline quintic;

            quintic = getQuinticSplinesFromWaypoints(new Waypoint[]{segStart, segEnd})[0];
            
            SplineSegment seg = new SplineSegment(waypoints.get(i-1), waypoints.get(i), path);

            for (int sample = 0; sample <= 40; sample++) {
                PoseWithCurvature pose = quintic.getPoint(sample / 40.0).get();
                seg.addPointToLines(pose.poseMeters.getTranslation().getX());
                
                //Convert from WPILib to JavaFX coords
                seg.addPointToLines(-pose.poseMeters.getTranslation().getY());
            }


            seg.getLine().strokeWidthProperty().bind(strokeWidth);
            seg.getOuterLine().strokeWidthProperty().set(prefs.getTrackWidth());
        
            FxUtils.enableSubchildSelector(seg.getLine(), subchildIdx);
            
            seg.getLine().applyCss();
            seg.getOuterLine().applyCss();

            group.getChildren().add(seg.getOuterLine());
            group.getChildren().add(seg.getLine()); // this has to come after the above for clicking to work
        }
    }

    @Override
    public void addToGroup(Group splineGroup, double scaleFactor) {
        strokeWidth.set(scaleFactor*4);
        splineGroup.getChildren().add(group);
        group.toBack();
    }

    @Override
    public boolean writeToFile(java.nio.file.Path path) {
        final AtomicBoolean okay = new AtomicBoolean(true);
        TrajectoryGenerator.setErrorHandler((error, stacktrace) -> {
            LOGGER.log(Level.WARNING, "Could not write Spline to file: " + error, stacktrace);
            okay.set(false);
        });
        try {
            var values = ProjectPreferences.getInstance().getValues();
            var prefs = ProjectPreferences.getInstance();
            var trackWidth = values.getTrackWidth();

            

      
            //TrajectoryUtil.toPathweaverJson(null, path.resolveSibling(path.getFileName() + ".wpilib.json"));

            return okay.get();
        }finally{}/* catch (IOException except) {
            LOGGER.log(Level.WARNING, "Could not write Spline to file", except);
            return false;
        }*/
    }

    private static QuinticHermiteSpline[] getQuinticSplinesFromWaypoints(Waypoint[] waypoints) {
        QuinticHermiteSpline[] splines = new QuinticHermiteSpline[waypoints.length - 1];
        for (int i = 0; i < waypoints.length - 1; i++) {
            var p0 = waypoints[i];
            var p1 = waypoints[i + 1];

            double[] xInitialVector =
                    {p0.getX(), p0.getTangentX(), 0.0};
            double[] xFinalVector =
                    {p1.getX(), p1.getTangentX(), 0.0};
            double[] yInitialVector =
                    {p0.getY(), p0.getTangentY(), 0.0};
            double[] yFinalVector =
                    {p1.getY(), p1.getTangentY(), 0.0};

            splines[i] = new QuinticHermiteSpline(xInitialVector, xFinalVector,
                    yInitialVector, yFinalVector);
        }

        return splines;
    }

    private static Trajectory trajectoryFromWaypoints(Iterable<Waypoint> waypoints, TrajectoryConfig config) {
        ProjectPreferences.Values prefs = ProjectPreferences.getInstance().getValues();

        var list = new TrajectoryGenerator.ControlVectorList();
        for(Waypoint wp: waypoints) {
            if(prefs.getExportUnit() == ProjectPreferences.ExportUnit.METER) {
                UnitConverter converter = prefs.getLengthUnit().getConverterTo(PathUnits.METER);
                list.add(new Spline.ControlVector(
                        new double[] {converter.convert(wp.getX()), converter.convert(wp.getTangentX()), 0},
                        new double[] {converter.convert(wp.getY()), converter.convert(wp.getTangentY()), 0}));
            } else {
                list.add(new Spline.ControlVector(
                        new double[] {wp.getX(), wp.getTangentX(), 0},
                        new double[] {wp.getY(), wp.getTangentY(), 0}));
            }

        }

        return TrajectoryGenerator.generateTrajectory(list, config);
    }
}
