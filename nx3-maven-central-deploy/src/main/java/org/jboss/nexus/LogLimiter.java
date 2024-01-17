package org.jboss.nexus;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/** An utility class to suppress spamming in log. The same logs are only reported once per period specified by the loggingPeriod.
 *
 */
public class LogLimiter {

	public static final long HOUR = 3600000;

	public static final long DAY = HOUR*24;

	public static final long YEAR = DAY * 365;

	private final Logger logger;

	private final long loggingPeriod;


	private final Map<String, Long> loggedStuff = new HashMap<>();


	/** Constructs a log limiter object.
	 *
	 * @param loggingPeriod period in millisecond until the log will be suppressed
	 * @param logger logger to be used for logging
	 */
	public LogLimiter(long loggingPeriod, @NotNull Logger logger) {
		this.loggingPeriod = loggingPeriod;
		this.logger = logger;
	}


	/** Maybe logs a given message.
	 *
	 * @param msg message to be logged
	 * @return true if the log was logged and not suppressed
	 */
	public boolean info(String msg) {
		Long loggedLastTime = loggedStuff.get(msg);

		if(loggedLastTime == null || System.currentTimeMillis() > (loggedLastTime+ loggingPeriod)) {
			logger.info(msg);
			loggedStuff.put(msg, System.currentTimeMillis());
			return true;
		}

		return false;
	}

	/** Maybe logs a given message.
	 *
	 * @param msg message to be logged
	 * @return true if the log was logged and not suppressed
	 */
	public boolean warn(String msg) {
		Long loggedLastTime = loggedStuff.get(msg);

		if(loggedLastTime == null || System.currentTimeMillis() > (loggedLastTime+ loggingPeriod)) {
			logger.warn(msg);
			loggedStuff.put(msg, System.currentTimeMillis());
			return true;
		}

		return false;
	}


	/** Maybe logs a given message.
	 *
	 * @param msg message to be logged
	 * @return true if the log was logged and not suppressed
	 */
	public boolean error(String msg) {
		Long loggedLastTime = loggedStuff.get(msg);

		if(loggedLastTime == null || System.currentTimeMillis() > (loggedLastTime+ loggingPeriod)) {
			logger.error(msg);
			loggedStuff.put(msg, System.currentTimeMillis());
			return true;
		}

		return false;
	}


}
