package com.github.libsml.model;

import com.github.libsml.commons.util.PrintUtils;
import com.github.libsml.data.FSAvroInputStream;
import com.github.libsml.data.avro.Entry;
import com.google.common.base.Preconditions;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by huangyu on 15/6/7.
 */
public class Models {

    public static void readModelFromLocalText(String path, float[] model) {
        try {
            File d = new File(path);
            Collection<File> fs;
            if (d.isDirectory()) {
                fs = FileUtils.listFiles(new File(path), null, true);
            } else {
                fs = new LinkedList<File>();
                fs.add(d);
            }

            for (File f : fs) {
                LineIterator lineIterator = FileUtils.lineIterator(f);
                while (lineIterator.hasNext()) {
                    processLine(lineIterator.nextLine(), model);
                }
                lineIterator.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Read model exception:" + e.getMessage());
        }
    }

    public static void readModelFromHDFSText(String path, float[] model) {
        try {

            Path ps = new Path(path);
            FileSystem fs = ps.getFileSystem(new Configuration());
            FileStatus[] statuses = fs.listStatus(ps);
            for (FileStatus status : statuses) {
                if (status.isFile()) {
                    FSDataInputStream streaming = fs.open(status.getPath());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streaming));
                    processLine(bufferedReader.readLine(), model);
                    bufferedReader.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Read model exception:" + e.getMessage());
        }
    }

    public static void readModelFromLocalAvro(String path, float[] model) {
        try {
            File d = new File(path);
            Collection<File> fs;
            if (d.isDirectory()) {
                fs = FileUtils.listFiles(new File(path), null, true);
            } else {
                fs = new LinkedList<File>();
                fs.add(d);
            }
            for (File f : fs) {
                DatumReader<Entry> datumReader = new SpecificDatumReader<Entry>(Entry.class);
                DataFileReader<Entry> fileReader = new DataFileReader<Entry>(f, datumReader);
                Entry e = null;
                while (fileReader.hasNext()) {
                    e = fileReader.next(e);
                    model[e.getIndex()] = e.getValue();
                }
                fileReader.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Read model exception:" + e.getMessage());
        }
    }

    public static void readModelFromHDFSAvro(String path, float[] model) {

        Path ps = new Path(path);
        try {
            FileSystem fs = ps.getFileSystem(new Configuration());
            FileStatus[] status = fs.listStatus(ps);
            for (FileStatus statu : status) {
                if (statu.isFile()) {
                    FSDataInputStream streaming = fs.open(statu.getPath());
                    DatumReader<Entry> datumReader = new SpecificDatumReader<Entry>(Entry.class);
                    DataFileReader<Entry> fileReader = new DataFileReader<Entry>(new FSAvroInputStream(streaming), datumReader);
                    Entry e = null;
                    while (fileReader.hasNext()) {
                        e = fileReader.next(e);
                        model[e.getIndex()] = e.getValue();
                    }
                    fileReader.close();
                    streaming.close();
                }
            }
            fs.close();
        } catch (IOException e) {
            throw new IllegalStateException("Read model exception:" + e.getMessage());
        }
    }

    public static void writeModelAsLocalText(String path, float[] model) {

        //TODO:
    }

    public static void writeModelAsHDFSText(String path, float[] model) {
        //TODO:
    }

    public static void writeModelAsLocalAvro(String path, float[] model) {

        //TODO:
    }

    public static void writeModelAsHDFSAvro(String path, float[] model) {

        //TODO:
    }


    private static void processLine(String line, float[] model) {
        if ("".equals(line.trim())) {
            return;
        }
        String[] kv = line.split(":");
        Preconditions.checkState(kv.length == 2, "Read model exception:line=" + line);
        model[Integer.parseInt(kv[0])] = Float.parseFloat(kv[1]);
    }

    public static void main(String[] args) {
        float[] w = new float[6];
        readModelFromLocalText("src/conf/model", w);
        PrintUtils.printFloatArray(w);
    }

}
