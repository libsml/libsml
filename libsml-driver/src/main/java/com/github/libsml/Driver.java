package com.github.libsml;

import com.github.libsml.commons.util.CommandUtils;
import com.github.libsml.data.Datas;
import com.github.libsml.lbfgs.LBFGSMain;
import com.github.libsml.liblinear.LiblinearMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by yellowhuang on 2015/5/19.
 */
public class Driver {
    private static final Logger log = LoggerFactory.getLogger(Driver.class);

    public static void main(String[] args) throws IOException {

        CommandUtils.checkArgument(args != null && args.length > 0, helpString());

        if ("lbfgs".equals(args[0].trim())) {
            LBFGSMain.main(CommandUtils.remainArgs(args));
        }else if("liblinear".equals(args[0].trim())){
            LiblinearMain.main(CommandUtils.remainArgs(args));
        }
        else if("data".equals(args[0].trim())){
            Datas.main(CommandUtils.remainArgs(args));
        }
        else {
            printHelp();
        }

    }

    public static void printHelp() {
        System.out.println(helpString());
    }

    public static String helpString() {
        return null;
    }

}
