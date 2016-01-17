package edu.arizona.jbv.thesis.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.util.Log;

/**
 * Class contains static method for writing to a file. If the file is not
 * explicity set, it writes to "sdcard/thesis_log.txt"
 * 
 * By default, timestamps are included prior to each log message.
 * 
 * By default, all logs are all written to the Android logcat
 * 
 * @author Jorge Vergara
 */
public class ThesisLog {

	private static String logPath = "sdcard/thesis_log.txt";

	private static boolean includeTimestamps = true;

	private static boolean autoAndroidLogging = true;
	private static String tag = "ThesisActivity";

	private static SimpleDateFormat formatter = new SimpleDateFormat(
			"MM.dd.yyyy hh:mm:ss");

	/**
	 * Sets the log file location
	 * 
	 * @param filePath
	 */
	public static void setLogFile(String filePath) {
		logPath = filePath;
	}

	/**
	 * Toggle whether timestamps are appended to each message or not. By
	 * default, they are enabled.
	 * 
	 * @return Whether timestamps are enabled (after the call to this method)
	 */
	public static boolean toggleTimestamps() {
		return includeTimestamps = !includeTimestamps;
	}

	/**
	 * Toggle whether messages are automatically logged to the android logcat
	 * 
	 * @return Whether android logging is enabled (after the call to this
	 *         method)
	 */
	public static boolean toggleAutoAndroidLoggin() {
		return autoAndroidLogging = !autoAndroidLogging;
	}

	/**
	 * Append the text to the end of the log file.
	 * 
	 * @param text
	 */
	public static void l(String text) {
		File logFile = new File(logPath);
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
					true));
			if (includeTimestamps) {

				text = formatter.format(new GregorianCalendar().getTime())
						+ ": " + text;
			}

			buf.append(text);
			buf.newLine();
			buf.flush();
			buf.close();

			if (autoAndroidLogging) {
				Log.d(tag, text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
