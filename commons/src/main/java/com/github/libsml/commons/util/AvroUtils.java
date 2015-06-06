package com.github.libsml.commons.util;

import com.google.common.base.Preconditions;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

/**
 * Created by yellowhuang on 2015/5/19.
 */
public class AvroUtils {

    public static String getStringAvro(GenericRecord data, String key,
                                       boolean isNullOK) {
        Object temp = data.get(key);
        if (temp == null) {
            Preconditions.checkArgument(isNullOK, key + " is null");
            return "";
        }
        Preconditions.checkArgument(temp instanceof String || temp instanceof Utf8, key + "=" + temp.toString()
                + " is not a string");
        return temp.toString();
    }

    public static String getStringAvro(GenericRecord data, String key,
                                       String defaultValue) {
        Object temp = data.get(key);
        if (temp == null) {
            return defaultValue;
        }
        Preconditions.checkArgument(temp instanceof String || temp instanceof Utf8, key + "=" + temp.toString()
                + " is not a string");
        return temp.toString();
    }

    public static int getIntAvro(GenericRecord data, String key,
                                 boolean isNullOK) {
        Object temp = data.get(key);
        if (temp == null) {
            Preconditions.checkArgument(isNullOK, key + " is null");
        }
        Preconditions.checkArgument(temp instanceof Integer, key + "=" + temp.toString()
                + " is not a string");
        return (Integer) temp;
    }

    public static int getIntAvro(GenericRecord data, String key,
                                 int defaultValue) {
        Object temp = data.get(key);
        if (temp == null) {
            return defaultValue;
        }
        Preconditions.checkArgument(temp instanceof Integer, key + "=" + temp.toString()
                + " is not a string");

        return (Integer) temp;
    }

    public static float getFloatAvro(GenericRecord data, String key,
                                     boolean isNullOK) {
        Object temp = data.get(key);
        if (temp == null) {
            Preconditions.checkArgument(isNullOK, key + " is null");
        }
        Preconditions.checkArgument(temp instanceof Float, key + "=" + temp.toString()
                + " is not a string");
        return (Float) temp;
    }

    public static float getFloatAvro(GenericRecord data, String key,
                                     float defaultValue) {
        Object temp = data.get(key);
        if (temp == null) {
            return defaultValue;
        }
        Preconditions.checkArgument(temp instanceof Float, key + "=" + temp.toString()
                + " is not a string");

        return (Float) temp;
    }

    public static double getDoubleAvro(GenericRecord data, String key,
                                     boolean isNullOK) {
        Object temp = data.get(key);
        if (temp == null) {
            Preconditions.checkArgument(isNullOK, key + " is null");
        }
        Preconditions.checkArgument(temp instanceof Double, key + "=" + temp.toString()
                + " is not a string");
        return (Double) temp;
    }

    public static double getDoubleAvro(GenericRecord data, String key,
                                      Double defaultValue) {
        Object temp = data.get(key);
        if (temp == null) {
            return defaultValue;
        }
        Preconditions.checkArgument(temp instanceof Double, key + "=" + temp.toString()
                + " is not a string");

        return (Double) temp;
    }


}
