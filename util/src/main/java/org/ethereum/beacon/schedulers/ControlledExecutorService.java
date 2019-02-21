package org.ethereum.beacon.schedulers;

import java.util.concurrent.ScheduledExecutorService;

/**
 * The <code>ScheduledExecutorService</code> which functions based on the
 * current system time supplied with {@link #setCurrentTime(long)} instead of
 * <code>System.currentTimeMillis()</code>
 *
 * Initial current time is 0
 */
public interface ControlledExecutorService extends ScheduledExecutorService {

  /**
   * Sets internal clock time and executes any tasks scheduled in period from
   * the previous time till new <code>currentTime</code> inclusive.
   * Periodic tasks are executed several times if scheduled so.
   * @param currentTime should be >= the last set time
   */
  void setCurrentTime(long currentTime);

  /**
   * Return the nearest time a task is scheduled for inside this executor
   * @return the nearest task time or {@link Long#MAX_VALUE} if no tasks scheduled at the moment
   */
  long getNextScheduleTime();

}
