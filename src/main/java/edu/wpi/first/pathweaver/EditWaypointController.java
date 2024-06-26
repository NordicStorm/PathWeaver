package edu.wpi.first.pathweaver;

import java.util.List;

import edu.wpi.first.pathweaver.global.CurrentSelections;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.NumberStringConverter;

@SuppressWarnings("PMD.UnusedPrivateMethod")
public class EditWaypointController {
  @FXML
  private TextField xPosition;
  @FXML
  private TextField yPosition;
  @FXML
  private Label nameLabel;
  @FXML
  private ListView<String> extraStrings;

  private List<Control> controls;
  private ChangeListener<String> nameListener;

  @FXML
  private void initialize() {
    controls = List.of(xPosition, yPosition);
    controls.forEach(control -> control.setDisable(true));
    List<TextField> textFields = List.of(xPosition, yPosition);
    textFields.forEach(textField -> textField.setTextFormatter(FxUtils.onlyDoubleText()));
  }

  /**
   * Binds the edit fields to the given wp. Allows for the unbinding and rebinding of properties as wp changes.
   * @param wp The ObservableValue for the selected waypoint.
   * @param controller The PathDisplayController to check the bounds of new waypoint values.
   */
  public void bindToWaypoint(ObservableValue<Waypoint> wp, FieldDisplayController controller) {
    double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();
    // When changing X and Y values, verify points are within bounds
    xPosition.textProperty().addListener((observable, oldValue, newValue) -> {
      boolean validText = !("").equals(newValue) && !("").equals(yPosition.getText());
      if (validText && !controller.checkBounds(Double.parseDouble(newValue),
              Double.parseDouble(yPosition.getText()) - height)) {
        xPosition.setText(oldValue);
      }
    });
    yPosition.textProperty().addListener((observable, oldValue, newValue) -> {
      boolean validText = !("").equals(newValue) && !("").equals(xPosition.getText());
      if (validText && !controller.checkBounds(Double.parseDouble(xPosition.getText()),
              Double.parseDouble(newValue) - height)) {
        yPosition.setText(oldValue);
      }
    });
    wp.addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        unbind(oldValue);
      }
      if (newValue != null) {
        bind(newValue);
      }
    });
    enableSaving(wp);
  }

  private void enableDoubleBinding(TextField field, DoubleProperty doubleProperty) {
    NumberStringConverter converter = new NumberStringConverter() {
      @Override
      public Number fromString(String value) {
        // Don't parse the beginning of a negative number
        if ("-".equals(value)) {
          return null;
        } else {
          return super.fromString(value);
        }
      }
    };
    field.textProperty().bindBidirectional(doubleProperty, converter);
  }

  private void yDoubleBinding(TextField field, DoubleProperty doubleProperty) {
    NumberStringConverter converter = new NumberStringConverter() {
      @Override
      public Double fromString(String value) {
        double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();
        return Double.parseDouble(value) - height;
      }

      @Override
      public String toString(Number object){
        double height = ProjectPreferences.getInstance().getField().getRealLength().getValue().doubleValue();
        return String.format("%.3f", height + object.doubleValue());
      }
    };
    field.textProperty().bindBidirectional(doubleProperty, converter);
  }

  private void disableDoubleBinding(TextField field, DoubleProperty doubleProperty) {
    field.textProperty().unbindBidirectional(doubleProperty);
    field.setText("");
  }
  private ObservableList<String> emptyList = FXCollections.observableArrayList();
  private void unbind(Waypoint oldValue) {
    controls.forEach(control -> control.setDisable(true));
    disableDoubleBinding(xPosition, oldValue.xProperty());
    disableDoubleBinding(yPosition, oldValue.yProperty());
    nameLabel.textProperty().set("");
    extraStrings.itemsProperty().set(emptyList);
  }

  private void bind(Waypoint newValue) {
    controls.forEach(control -> control.setDisable(false));

    enableDoubleBinding(xPosition, newValue.xProperty());
    yDoubleBinding(yPosition, newValue.yProperty());
    nameLabel.textProperty().set(newValue.getName());
    extraStrings.setItems(newValue.getParallel());
  }

  private void enableSaving(ObservableValue<Waypoint> wp) {
    // Save values when out of focus
    List.of(xPosition, yPosition)
        .forEach(textField -> {
          textField.setOnKeyReleased(event -> {
            if (!textField.getText().equals("") && wp.getValue() != null) {
              SaveManager.getInstance().addChange(CurrentSelections.getCurPath());
              CurrentSelections.getCurPath().update();
            }
            event.consume();
          });

          textField.setOnMouseClicked(event -> {
            if (!textField.getText().equals("") && wp.getValue() != null) {
              SaveManager.getInstance().addChange(CurrentSelections.getCurPath());
              CurrentSelections.getCurPath().update();
            }
          });
        });
  }
    
}
