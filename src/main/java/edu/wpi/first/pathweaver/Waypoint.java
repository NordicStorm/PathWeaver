package edu.wpi.first.pathweaver;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import edu.wpi.first.pathweaver.path.Path;

/**
 * The Waypoint class represents a point on the field. This class
 * follows WPILib convention, with X being the long side of the field,
 * and Y being the short side.
 *
 * Viewed from the screen, Y should increase as one moves up the screen, and X
 * should increase as one moves left.
 */
public class Waypoint {
	private static final double SIZE = 90.0;
	private static final double ICON_X_OFFSET = (SIZE * (3 * 30 / SIZE) / 5D) / 16.5;

	private final DoubleProperty x = new SimpleDoubleProperty();
	private final DoubleProperty y = new SimpleDoubleProperty();
	private final DoubleProperty tangentX = new SimpleDoubleProperty();
	private final DoubleProperty tangentY = new SimpleDoubleProperty();
	private final BooleanProperty lockTangent = new SimpleBooleanProperty();
	private final BooleanProperty fromOutsideCommand = new SimpleBooleanProperty();
	private final StringProperty name = new SimpleStringProperty("");
	public int lineNumber = -1;
	public int numberOfLinesInSection = -1;
	public int endingLineParallel = -1;
	private final Line tangentLine;
	private final Polygon icon;
	private final Circle innerCircle;
    private final ObservableList<String> extraList = FXCollections.observableArrayList();

	/**
	 * Creates Waypoint object containing javafx circle.
	 *
	 * @param position
	 *            x and y coordinates in {@link Waypoint} convention
	 * @param tangentVector
	 *            tangent vector in user set units
	 * @param outsideCommand
	 *            
	 */
	public Waypoint(Point2D position, Point2D tangentVector, boolean outsideCommand) {
		fromOutsideCommand.set(outsideCommand);

		setCoords(position);

		icon = new Polygon(0.0, SIZE / 3, SIZE, 0.0, 0.0, -SIZE / 3);
		innerCircle = new Circle();
		setupIcon();
		tangentLine = new Line();
		tangentLine.getStyleClass().add("tangent");
		tangentLine.startXProperty().bind(x);
		//Convert from WPILib to JavaFX coords
		tangentLine.startYProperty().bind(y.negate());
		setTangent(tangentVector);
		tangentLine.endXProperty().bind(Bindings.createObjectBinding(() -> getTangentX() + getX(), tangentX, x));

		//Convert from WPILib to JavaFX coords
		tangentLine.endYProperty().bind(Bindings.createObjectBinding(() -> -getTangentY() + -getY(), tangentY, y));
	}

	public void enableSubchildSelector(int i) {
		FxUtils.enableSubchildSelector(this.icon, i);
		getIcon().applyCss();
	}

	private void setupIcon() {
		icon.setLayoutX(-(icon.getLayoutBounds().getMaxX() + icon.getLayoutBounds().getMinX()) / 2 - ICON_X_OFFSET);
		icon.setLayoutY(-(icon.getLayoutBounds().getMaxY() + icon.getLayoutBounds().getMinY()) / 2);

		icon.translateXProperty().bind(x);
		//Convert from WPILib to JavaFX coords
		icon.translateYProperty().bind(y.negate());
		innerCircle.translateXProperty().bind(x);
		innerCircle.translateYProperty().bind(y.negate());
		innerCircle.setStroke(Color.BLACK);
		innerCircle.setFill(Color.GREEN);
		innerCircle.setVisible(false);
		FxUtils.applySubchildClasses(this.icon);
		this.icon.rotateProperty()
				.bind(Bindings.createObjectBinding(
						() -> getTangent() == null ? 0.0 : Math.toDegrees(Math.atan2(-getTangentY(), getTangentX())),
						tangentX, tangentY));
		icon.getStyleClass().add("waypoint");
		if(isOutsidecommand()){
			icon.getStyleClass().add("outside");

		}
	}

	/**
	 * Convenience function for math purposes.
	 *
	 * @param other
	 *            The other Waypoint.
	 *
	 * @return The coordinates of this Waypoint relative to the coordinates of
	 *         another Waypoint.
	 */
	public Point2D relativeTo(Waypoint other) {
		return new Point2D(this.getX() - other.getX(), this.getY() - other.getY());
	}

	public boolean isLockTangent() {
		return lockTangent.get();
	}

	public BooleanProperty lockTangentProperty() {
		return lockTangent;
	}

	public void setLockTangent(boolean lockTangent) {
		this.lockTangent.set(lockTangent);
	}

	public boolean isOutsidecommand() {
		return fromOutsideCommand.get();
	}


	public void setOutsideCommand(boolean outside) {
		this.fromOutsideCommand.set(outside);
	}

	public Line getTangentLine() {
		return tangentLine;
	}

	public Point2D getTangent() {
		return new Point2D(tangentX.get(), tangentY.get());
	}

	public void setTangent(Point2D tangent) {
		this.tangentX.set(tangent.getX());
		this.tangentY.set(tangent.getY());
	}

	public double getTangentX() {
		return tangentX.get();
	}

	public double getTangentY() {
		return tangentY.get();
	}

	public void setTangentX(double tangentX) {
		this.tangentX.set(tangentX);
	}

	public void setTangentY(double tangentY) {
		this.tangentY.set(tangentY);
	}

	public Polygon getIcon() {
		return icon;
	}

	public double getX() {
		return x.get();
	}

	public DoubleProperty xProperty() {
		return x;
	}

	public void setX(double x) {
		this.x.set(x);
	}

	public double getY() {
		return y.get();
	}

	public DoubleProperty yProperty() {
		return y;
	}

	public void setY(double y) {
		this.y.set(y);
	}

	public Point2D getCoords() {
		return new Point2D(getX(), getY());
	}

	public void setCoords(Point2D coords) {
		setX(coords.getX());
		setY(coords.getY());
	}

	public String getName() {
		return name.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public DoubleProperty tangentXProperty() {
		return tangentX;
	}

	public DoubleProperty tangentYProperty() {
		return tangentY;
	}

	/**
	 * Converts the unit system of a this Waypoint.
	 *
	 * @param from
	 *            Unit to convert from.
	 * @param to
	 *            Unit to convert to.
	 */
	public void convertUnit(Unit<Length> from, Unit<Length> to) {
		var converter = from.getConverterTo(to);
		x.set(converter.convert(x.get()));
		y.set(converter.convert(y.get()));
		tangentX.set(converter.convert(tangentX.get()));
		tangentY.set(converter.convert(tangentY.get()));
	}

	public Waypoint copy() {
		return new Waypoint(getCoords(), getTangent(), isLockTangent());
	}

	@Override
	public String toString() {
		return String.format("%s (%f,%f), (%f,%f), %b %b", getName(), getX(), getY(), getTangentX(), getTangentY(), isLockTangent(), isOutsidecommand());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (getClass() != o.getClass()) {
			return false;
		}
		Waypoint point = (Waypoint) o;

		return x.get() == point.x.get() && y.get() == point.y.get() && tangentX.get() == point.tangentX.get()
				&& tangentY.get() == point.tangentY.get() && name.get().equals(point.name.get())
				&& isLockTangent() == point.isLockTangent() && isOutsidecommand() == point.isOutsidecommand();
	}

    public void addParallel(String text, int line) {
        extraList.add(text);
		if(line > endingLineParallel){
			endingLineParallel = line;
		}
		icon.getStyleClass().add("hasparallel");
    }
	public ObservableList<String> getParallel(){
		return extraList;
	}
}
