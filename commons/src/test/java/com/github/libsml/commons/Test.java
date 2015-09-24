package com.github.libsml.commons;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by huangyu on 15/9/17.
 */
public class Test {

    private final static int DAY_MS = 1000 * 60 * 60 * 24;
    private static long FIRST_DAY = 0;

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            FIRST_DAY = dateFormat.parse("20150701").getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public double evaluate(final String startDay, final String endDay,
                           final double lambda, final int interval, final boolean isLong) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date1 = dateFormat.parse(startDay);
            Date date2 = dateFormat.parse(endDay);
            int i1 = (int) ((date1.getTime() - FIRST_DAY) / DAY_MS / interval);
            int i2 = (int) ((date2.getTime() - FIRST_DAY) / DAY_MS / interval);
            System.out.println("i1:"+i1+",i2:"+i2);
            int diff = i2 - i1;
            if (isLong) {
                return Math.pow(1 - lambda, diff);
            } else {
                return lambda * Math.pow(1 - lambda, diff);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static void main(String[] args) {

        System.out.println(new Test().evaluate("20150701", "201507015", 0.2,7, true));
    }
}
