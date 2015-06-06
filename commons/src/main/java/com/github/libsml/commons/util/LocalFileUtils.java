package com.github.libsml.commons.util;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;

/**
 * Created by yellowhuang on 2015/5/20.
 */
public class LocalFileUtils {

    public static void mkdir(String path, boolean overwrite) throws IOException {

        File f = new File(path);
        if (f.exists() && !overwrite) {
            throw new IllegalStateException("Mkdir exception:path=" + path + " exists");
        }
        FileUtils.deleteDirectory(f);
        FileUtils.forceMkdir(f);
    }
}
