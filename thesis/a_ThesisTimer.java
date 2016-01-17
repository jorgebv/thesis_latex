package edu.arizona.jbv.thesis.utils;

/**
 * A simple class for timing various activities.
 * 
 * @author Jorge Vergara
 * 
 */
public class ThesisTimer {

	private static long startTime;

	/**
	 * Starts the timer. If this is called twice in a row without calling
	 * stopTimer the timer will be restarted.
	 */
	public static void startTimer() {
		startTime = System.currentTimeMillis();
	}

	/**
	 * Stops the timer. If this is called twice in a row without calling
	 * startTimer, the method will return the time since the last call of the
	 * start method. If the start method is never called, the return value will
	 * be negative.
	 * 
	 * @return Time passed since last call to startTimer
	 */
	public static long stopTimer() {
		long endTime = System.currentTimeMillis();
		return endTime - startTime;
	}

}
