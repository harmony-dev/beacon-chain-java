package org.ethereum.beacon.schedulers;

import java.time.Duration;

/**
 * Special Schedulers implementation which is mostly suitable for testing and simulation.
 * The system time is controlled manually and all the schedulers execute tasks according
 * to this time.
 * Initial system time is equal to 0
 */
public interface ControlledSchedulers extends Schedulers {

  /**
   * Sets the next system timestamp.
   * After the call the {@link #getCurrentTime()} method will return <code>newTime</code>
   * however during this call if a task for some Scheduler is scheduled for time T (T <= <code>newTime</code>)
   * the {@link #getCurrentTime()} should return T until the task completes execution.
   * All the tasks scheduled for the period from the previous till the new time are executed
   * sequentially in time increasing order.
   * Periodic tasks would execute several times if scheduled accordingly
   */
  void setCurrentTime(long newTime);

  /**
   * Just a handy helper method for {@link #setCurrentTime(long)}
   */
  default void addTime(Duration duration) {
    addTime(duration.toMillis());
  }

  /**
   * Just a handy helper method for {@link #setCurrentTime(long)}
   */
  default void addTime(long millis) {
    setCurrentTime(getCurrentTime() + millis);
  }

}
