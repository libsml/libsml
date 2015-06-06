package com.github.libsml;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Created by yellowhuang on 2015/4/15.
 */
public class Logger {


    public static PrintStream logger=null;
    /*
    public static PrintStream getInstante(Config config) {

        if (logger == null) {
            String logPath = config.get("log.file");
            try {
                logger = logPath == null ? System.out : new PrintStream(logPath);
                return logger;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return System.out;
            }
        } else {
            return logger;
        }
    }
    */

    public static synchronized void log(String msg){
        logger.print(msg);
    }

    public static synchronized void log(String format, Object... args) {
           logger.printf(format,args);
    }

    public static synchronized void  initLogger(Map<String, String> configMap){
        if (logger == null) {
            String logPath = configMap.get("log.file");
            try {
                logger = logPath == null ? System.out : new PrintStream(logPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
        }
    }


}
