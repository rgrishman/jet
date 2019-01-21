package edu.nyu.jet;

import java.util.regex.Matcher;

/**
 * a minimal logger providing a subset of the interface of the slf4j logger.
 */

public class Logger {

    public static final int TRACE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;

    String name;
    private int level = INFO;
    private boolean levelAssigned = false;

    /**
     *  Create a new logger with defualt level INFO.
     *  @param  name  name of the logger.
     */

    Logger (String loggerName) {
	name = loggerName;
    }

    public void setLevel (int lvl) {
	level = lvl;
    // System.out.println("Setting level of " + name + " to " + level);
    }

    public void setLevel (String lvl) {
	level = levelValue(lvl);
    // System.out.println("Setting level of " + name + " to " + level);
    }

    /**
     *  If <CODE>levelName</CODE> is the name of a logger level, return the 
     *  corresponding integer, else return 0.
     */

    public static int levelValue (String levelName) {
	if (levelName.equals("TRACE")) return TRACE;
	if (levelName.equals("DEBUG")) return DEBUG;
	if (levelName.equals("INFO")) return INFO;
	if (levelName.equals("WARN")) return WARN;
	if (levelName.equals("ERROR")) return ERROR;
	return 0;
    }

    /**
     *  Print a log message if the current level of the logger is TRACE.
     */

    public void trace (String message, Object... argList) {
	if (getLevel() <= TRACE) 
	    log (message, argList);
    }

    /**
     *  Print a log message if the current level of the logger is DEBUG or less.
     */

    public void debug (String message, Object... argList) {
	if (getLevel() <= DEBUG) 
	    log (message, argList);
    }

    /**
     *  Print a log message if the current level of the logger is INFO or less.
     */

    public void info (String message, Object... argList) {
	if (getLevel() <= INFO) 
	    log (message, argList);
    }

    /**
     *  Print a log message if the current level of the logger is WARN or less.
     */

    public void warn (String message, Object... argList) {
	if (getLevel() <= WARN) 
	    log (message, argList);
    }

    /**
     *  Print a log message.
     */

    public void error (String message, Object... argList) {
	if (getLevel() <= ERROR) 
	    log (message, argList);
    }

    private void log (String message, Object[] argList) {
        for (Object arg : argList) {
            message = message.replaceFirst("\\{\\}", 
                    Matcher.quoteReplacement((arg == null) ? "null" : arg.toString()));
        }
        System.out.println(message);
    }

    private int getLevel () {
        if (levelAssigned) {
            return level;
        } else if (LoggerFactory.logList != null) {
            for (String entry : LoggerFactory.logList) {
                String[] field = entry.split(":");
                String prefix = field[0];
                String value = field[1];
                if (name.startsWith(prefix))
                    setLevel(levelValue(value));
                levelAssigned = true;
            }
        } 
        return level;
    }

}
