package org.ethereum.beacon.simulator.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.LoggerMDCExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.schedulers.TimeController;
import org.ethereum.beacon.schedulers.TimeControllerImpl;

public class MDCControlledSchedulers {
  private DateFormat localTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

  private TimeController timeController = new TimeControllerImpl();

  public ControlledSchedulers createNew(String validatorId) {
    return createNew(validatorId, 0);
  }

  public ControlledSchedulers createNew(String validatorId, long timeShift) {
    ControlledSchedulers[] newSched = new ControlledSchedulers[1];
    LoggerMDCExecutor mdcExecutor = new LoggerMDCExecutor()
        .add("validatorTime", () -> localTimeFormat.format(new Date(newSched[0].getCurrentTime())))
        .add("validatorIndex", () -> "" + validatorId);
    newSched[0] = Schedulers.createControlled(() -> mdcExecutor);
    newSched[0].getTimeController().setParent(timeController);
    newSched[0].getTimeController().setTimeShift(timeShift);

    return newSched[0];
  }

  public void setCurrentTime(long time) {
    timeController.setTime(time);
  }

  public void addTime(Duration duration) {
    addTime(duration.toMillis());
  }

  public void addTime(long millis) {
    setCurrentTime(timeController.getTime() + millis);
  }

  public long getCurrentTime() {
    return timeController.getTime();
  }
}
