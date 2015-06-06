package com.github.libsml.commons.util;

import com.google.common.base.Preconditions;

/**
 * Created by yellowhuang on 2015/5/19.
 */
public class CommandUtils {

    public static String[] remainArgs(String[] args, int start, int end) {

        if (args == null) {
            return new String[0];
        }

        Preconditions.checkArgument(start >= 0 && end <= args.length
                , String.format("Start(%d) should >= 0 and end(%d) should <= the length of args(%d).", start, end, args.length));

        String[] remainArgs = new String[end - start];
        for (int i = start; i < end; i++) {
            remainArgs[i - start] = args[i];
        }
        return remainArgs;
    }

    public static String[] remainArgs(String[] args, int start) {
        return remainArgs(args, start, args.length);
    }

    public static String[] remainArgs(String[] args) {
        return remainArgs(args, 1);
    }

    public static void checkArgument(boolean expression, String helpMessage) {
        if (!expression) {
            System.out.println(helpMessage);
            System.exit(1);
        }

    }
}
