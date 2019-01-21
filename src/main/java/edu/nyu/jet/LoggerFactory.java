package edu.nyu.jet;

import java.util.*;
import java.io.*;

/**
 * a minimal logger factory following the interface of the slf4j logger.
 */

public class LoggerFactory {

    private static Set<Logger> loggers = new HashSet<Logger>();

    private static String savedPrefix = null;

    private static int savedLevel;

    /**
     *  Return a new Logger.
     */

    static public Logger getLogger (String name) {
	Logger logger = new Logger(name);
	loggers.add(logger);
	// if (savedPrefix != null && name.startsWith(savedPrefix)) logger.setLevel(savedLevel);
	return logger;
    }

    /**
     *  Return a new Logger.
     */

    static public Logger getLogger (Class c) {
        return getLogger(c.getName());
    }

    /**
     *  Sets the level of all loggers whose name begins with <CODE>prefix</CODE> to <CODE>level</CODE>.
     */

    static public void setLevel (String prefix, int level) {
        for (Logger logger : loggers) {
            if (logger.name.startsWith(prefix)) {
                logger.setLevel(level);
            }
        }
        savedPrefix = prefix;
        savedLevel = level;
    }

    public static List<String> logList = new ArrayList<String>();

    /**
     *  If there is file "LOG" in the currect directory, make loggimg assignments
     *  based on its contents.  Each line should have the format
     *     className:logLevel 
     *  For example, the entry
     *     a.b.C:warn
     *  causes log messages to be written for all instancewa of logger.error 
     *  amd logger.warn for all classes whose fully qualifieed mame begins with
     *  a.b.C.
     */

    public static void setLoggers () throws IOException {
        if ((new File ("LOG")).exists()) {
            String line = null;
            BufferedReader logReader = new BufferedReader (new FileReader ("LOG"));
            while ((line = logReader.readLine()) != null) {
                logList.add(line);
                String[] field = line.split(":");
                if (field.length == 2)
                    LoggerFactory.setLevel(field[0], field[1]);
                else
                    System.out.println("Invalid logging directive " + line);
            }
            logReader.close();
        }
    }

  static public void setLevel (String prefix, String level) {
      setLevel (prefix, Logger.levelValue(level));
  }
}
