package com.github.libsml;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
    final Map<String, String> configMap;
    private final static Logger log = LoggerFactory.getLogger(Config.class);

    private Config(String confPath) throws IOException {
        configMap = new HashMap<String, String>();
        InputStream input = new BufferedInputStream(new FileInputStream(
                new File(confPath).getAbsolutePath()));
        Properties properties = new Properties();
        properties.load(input);

        for (String propName : properties.stringPropertyNames()) {
            configMap.put(propName, properties.getProperty(propName));
        }
        input.close();
//        PropertyConfigurator.configure(
//                Config.class.getResourceAsStream("/mllib_log4j.properties"));

    }

    public static String prefixN(String prefix, int n) {
        return "n" + n + "." + prefix;
    }

    public void addConfig(Configuration conf, String prefix) {
        prefix += ".";
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                conf.set(entry.getKey().replace(prefix, ""), entry.getValue());
            }
        }
    }


    public static Config createFromFile(String confPath) {
        try {
            return new Config(confPath);
        } catch (IOException e) {
            throw new IllegalStateException("Configurtaion exception:" + e.getMessage());
        }
    }



    public String get(String key) {
        return configMap.get(key);
    }

    public String get(String key, String d) {

        return configMap.containsKey(key) ? configMap.get(key) : d;
    }


    public boolean contains(String key) {
        return configMap.containsKey(key);
    }

    public int getInt(String key, int d) {
        if (!contains(key)) {
            return d;
        }
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            log.error(String.format("Parameter int parse exception:key=%s,value=%s", key, get(key)));
            return d;
        }
    }

    public float getFloat(String key, float d) {
        if (!contains(key)) {
            return d;
        }
        try {
            return Float.parseFloat(get(key));
        } catch (NumberFormatException e) {
            log.error(String.format("Parameter float parse exception:key=%s,value=%s", key, get(key)));
            return d;
        }
    }

    public double getDouble(String key, double d) {
        if (!contains(key)) {
            return d;
        }
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException e) {
            log.error(String.format("Parameter float parse exception:key=%s,value=%s", key, get(key)));
            return d;
        }
    }

    public boolean getBoolean(String key, boolean d) {
        if (!contains(key)) {
            return d;
        }
        try {
            return Boolean.parseBoolean(get(key));
        } catch (NumberFormatException e) {
            log.error(String.format("Parameter boolean parse exception:key=%s,value=%s", key, get(key)));
            return d;
        }
    }
}
