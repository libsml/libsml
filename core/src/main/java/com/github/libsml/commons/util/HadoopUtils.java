/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.libsml.commons.util;

import com.google.common.base.Preconditions;
import org.apache.avro.Schema;
import org.apache.avro.mapreduce.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.compress.DeflateCodec;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public final class HadoopUtils {

    private static final Logger log = LoggerFactory.getLogger(HadoopUtils.class);

    private HadoopUtils() {
    }


    /**
     * Create a map-only Hadoop Job out of the passed in parameters.  Does not set the
     * Job name.
     *
     * @see #getCustomJobName(String, JobContext, Class, Class)
     */
    public static Job prepareJob(Path inputPath,
                                 Path outputPath,
                                 Class<? extends InputFormat> inputFormat,
                                 Class<? extends Mapper> mapper,
                                 Class<? extends Writable> mapperKey,
                                 Class<? extends Writable> mapperValue,
                                 Class<? extends OutputFormat> outputFormat, Configuration conf) throws IOException {

//    Job job = new Job(new Configuration(conf));
        Job job = Job.getInstance(conf);
        Configuration jobConf = job.getConfiguration();

        if (mapper.equals(Mapper.class)) {
            throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
        }
        job.setJarByClass(mapper);

        job.setInputFormatClass(inputFormat);
        jobConf.set("mapred.input.dir", inputPath.toString());

        job.setMapperClass(mapper);
        job.setMapOutputKeyClass(mapperKey);
        job.setMapOutputValueClass(mapperValue);
        job.setOutputKeyClass(mapperKey);
        job.setOutputValueClass(mapperValue);
        jobConf.setBoolean("mapred.compress.map.output", true);
        job.setNumReduceTasks(0);

        job.setOutputFormatClass(outputFormat);
        jobConf.set("mapred.output.dir", outputPath.toString());

        return job;
    }

    /**
     * Create a map and reduce Hadoop job.  Does not set the name on the job.
     *
     * @param inputPath    The input {@link Path}
     * @param outputPath   The output {@link Path}
     * @param inputFormat  The {@link InputFormat}
     * @param mapper       The {@link Mapper} class to use
     * @param mapperKey    The {@link Writable} key class.  If the Mapper is a no-op,
     *                     this value may be null
     * @param mapperValue  The {@link Writable} value class.  If the Mapper is a no-op,
     *                     this value may be null
     * @param reducer      The {@link Reducer} to use
     * @param reducerKey   The reducer key class.
     * @param reducerValue The reducer value class.
     * @param outputFormat The {@link OutputFormat}.
     * @param conf         The {@link Configuration} to use.
     * @return The {@link Job}.
     * @throws IOException if there is a problem with the IO.
     * @see #getCustomJobName(String, JobContext, Class, Class)
     * @see #prepareJob(Path, Path, Class, Class, Class, Class, Class,
     * Configuration)
     */
    public static Job prepareJob(Path inputPath,
                                 Path outputPath,
                                 Class<? extends InputFormat> inputFormat,
                                 Class<? extends Mapper> mapper,
                                 Class<? extends Writable> mapperKey,
                                 Class<? extends Writable> mapperValue,
                                 Class<? extends Reducer> reducer,
                                 Class<? extends Writable> reducerKey,
                                 Class<? extends Writable> reducerValue,
                                 Class<? extends OutputFormat> outputFormat,
                                 Configuration conf) throws IOException {
        return prepareJob(
                inputPath.toString()
                , outputPath.toString()
                , inputFormat
                , mapper
                , mapperKey
                , mapperValue
                , reducer
                , reducerKey
                , reducerValue
                , outputFormat, conf);

    }


    public static Job prepareAvroJob(String inputPaths,
                                     String outputPath,
                                     Class<? extends Mapper> mapper,
                                     Object mapperKey,
                                     Object mapperValue,
                                     Class<? extends OutputFormat> outputFormat
    ) throws IOException {
        Configuration conf = new Configuration();
        return prepareAvroJob(inputPaths, outputPath, null, null, null, mapper, mapperKey, mapperValue,
                null, null, null, null, outputFormat, conf, false, false);
    }

    /**
     *
     * @param inputPaths
     * @param outputPath
     * @param inputFormat
     * @param inputKey
     * @param inputValue
     * @param mapper
     * @param mapperKey
     * @param mapperValue
     * @param combiner
     * @param reducer
     * @param outputKey
     * @param outputValue
     * @param outputFormat
     * @param conf
     * @param overwrite
     * @param isCompress
     * @return
     * @throws IOException
     */
    public static Job prepareAvroJob(String inputPaths,
                                     String outputPath,
                                     Class<? extends InputFormat> inputFormat,
                                     Object inputKey,
                                     Object inputValue,
                                     Class<? extends Mapper> mapper,
                                     Object mapperKey,
                                     Object mapperValue,
                                     Class<? extends Reducer> combiner,
                                     Class<? extends Reducer> reducer,
                                     Object outputKey,
                                     Object outputValue,
                                     Class<? extends OutputFormat> outputFormat,
                                     Configuration conf,
                                     boolean overwrite,
                                     boolean isCompress
    ) throws IOException {


        Job job = Job.getInstance(conf);
        Configuration jobConf = job.getConfiguration();
        if (inputKey instanceof Schema) {

            if (inputValue instanceof Schema) {
                inputFormat = inputFormat == null ? AvroKeyValueInputFormat.class : inputFormat;
            }
            inputFormat = inputFormat == null ? AvroKeyInputFormat.class : inputFormat;

        }
        if(inputFormat!=null) {
            job.setInputFormatClass(inputFormat);
        }

        if (inputKey instanceof Schema) {
            AvroJob.setInputKeySchema(job, (Schema) inputKey);
        }

        if (inputValue instanceof Schema) {
            AvroJob.setInputValueSchema(job, (Schema) inputValue);
        }

        if (outputKey instanceof Schema) {

            if (outputValue instanceof Schema) {
                outputFormat = outputFormat == null ? AvroKeyValueOutputFormat.class : outputFormat;
            }
            outputFormat = outputFormat == null ? AvroKeyOutputFormat.class : outputFormat;

        }
        if(outputFormat!=null) {
            job.setOutputFormatClass(outputFormat);
        }

        if (outputKey instanceof Schema) {
            AvroJob.setOutputKeySchema(job, (Schema) outputKey);
        } else if (outputKey instanceof Class) {
            job.setOutputKeyClass((Class) outputKey);
        }

        if (outputValue instanceof Schema) {
            AvroJob.setOutputValueSchema(job, (Schema) outputValue);
        } else if (outputValue instanceof Class) {
            job.setOutputValueClass((Class) outputValue);
        }


        if (reducer == null) {
            job.setNumReduceTasks(0);

            if (mapperKey instanceof Schema) {
                AvroJob.setMapOutputKeySchema(job, (Schema) mapperKey);
            } else if (mapperKey instanceof Class) {
                job.setOutputKeyClass((Class) mapperKey);
            }

            if (mapperValue instanceof Schema) {
                AvroJob.setOutputValueSchema(job, (Schema) mapperValue);
            } else if (mapperKey instanceof Class) {
                job.setOutputValueClass((Class) mapperValue);
            }
            job.setJarByClass(mapper);

        } else if (reducer.equals(Reducer.class)) {
            if (mapper.equals(Mapper.class)) {
                throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
            }
            job.setJarByClass(mapper);

        } else {
            job.setJarByClass(reducer);

        }

        FileInputFormat.setInputPaths(job, inputPaths);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        if (isCompress) {
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, DeflateCodec.class);
        }


        job.setMapperClass(mapper);
        if (mapperKey instanceof Schema) {
            AvroJob.setMapOutputKeySchema(job, (Schema) mapperKey);
        } else if (mapperKey instanceof Class) {
            job.setMapOutputKeyClass((Class) mapperKey);
        }

        if (mapperValue instanceof Schema) {
            AvroJob.setMapOutputValueSchema(job, (Schema) mapperValue);
        } else if (mapperKey instanceof Class) {
            job.setMapOutputValueClass((Class) mapperValue);
        }


        if(reducer!=null){
            job.setReducerClass(reducer);
        }
        if (combiner != null) {
            job.setCombinerClass(combiner);
        }

        if (overwrite) {
            HadoopUtils.delete(jobConf, new Path(outputPath));
        }

        return job;

    }


    public static Job prepareAvroJob(String inputPaths,
                                     Path outputPath,
                                     Schema inputKeySchema,
                                     Class<? extends Mapper> mapper,
                                     Class<? extends Writable> mapperKey,
                                     Class<? extends Writable> mapperValue,
                                     Class<? extends Reducer> combiner,
                                     Class<? extends Reducer> reducer,
                                     Schema outputKeySchema,
                                     Class<? extends Writable> outputValue,
                                     Configuration conf,
                                     boolean overwrite
    ) throws IOException {
        Job job = Job.getInstance(conf);
        Configuration jobConf = job.getConfiguration();


        if (reducer.equals(Reducer.class)) {
            if (mapper.equals(Mapper.class)) {
                throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
            }
            job.setJarByClass(mapper);
        } else {
            job.setJarByClass(reducer);
        }

        FileInputFormat.setInputPaths(job, inputPaths);
        FileOutputFormat.setOutputPath(job, outputPath);

        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, DeflateCodec.class);

        job.setInputFormatClass(AvroKeyInputFormat.class);
        AvroJob.setInputKeySchema(job, inputKeySchema);
        job.setMapperClass(mapper);
        if (mapperKey != null) {
            job.setMapOutputKeyClass(mapperKey);
        }
        if (mapperValue != null) {
            job.setMapOutputValueClass(mapperValue);
        }
        if (combiner != null) {
            job.setCombinerClass(combiner);
        }

        job.setOutputFormatClass(AvroKeyOutputFormat.class);
        job.setReducerClass(reducer);
        AvroJob.setOutputKeySchema(job, outputKeySchema);
        job.setOutputValueClass(outputValue);

        if (overwrite) {
            HadoopUtils.delete(jobConf, outputPath);
        }

        return job;

    }


    public static Job prepareAvroJob(String inputPaths,
                                     Path outputPath,
                                     Schema inputKeySchema,
                                     Class<? extends Mapper> mapper,
                                     Class<? extends Writable> mapperKey,
                                     Class<? extends Writable> mapperValue,
                                     Class<? extends Reducer> combiner,
                                     Class<? extends Reducer> reducer,
                                     Class<? extends Writable> outputKey,
                                     Class<? extends Writable> outputValue,
                                     Configuration conf,
                                     boolean overwrite
    ) throws IOException {
        Job job = Job.getInstance(conf);
        Configuration jobConf = job.getConfiguration();


        if (reducer.equals(Reducer.class)) {
            if (mapper.equals(Mapper.class)) {
                throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
            }
            job.setJarByClass(mapper);
        } else {
            job.setJarByClass(reducer);
        }

        FileInputFormat.setInputPaths(job, inputPaths);
        FileOutputFormat.setOutputPath(job, outputPath);

//        FileOutputFormat.setCompressOutput(job, true);
//        FileOutputFormat.setOutputCompressorClass(job, DeflateCodec.class);

        job.setInputFormatClass(AvroKeyInputFormat.class);
        AvroJob.setInputKeySchema(job, inputKeySchema);
        job.setMapperClass(mapper);
        if (mapperKey != null) {
            job.setMapOutputKeyClass(mapperKey);
        }
        if (mapperValue != null) {
            job.setMapOutputValueClass(mapperValue);
        }
        if (combiner != null) {
            job.setCombinerClass(combiner);
        }

        job.setReducerClass(reducer);
        job.setOutputKeyClass(outputKey);
        job.setOutputValueClass(outputValue);

        if (overwrite) {
            HadoopUtils.delete(jobConf, outputPath);
        }

        return job;

    }

    public static Job prepareJob(String inputPath,
                                 String outputPath,
                                 Class<? extends InputFormat> inputFormat,
                                 Class<? extends Mapper> mapper,
                                 Class<? extends Writable> mapperKey,
                                 Class<? extends Writable> mapperValue,
                                 Class<? extends Reducer> reducer,
                                 Class<? extends Writable> reducerKey,
                                 Class<? extends Writable> reducerValue,
                                 Class<? extends OutputFormat> outputFormat,
                                 Configuration conf) throws IOException {

//    Job job = new Job(new Configuration(conf));
        Job job = Job.getInstance(conf);
        Configuration jobConf = job.getConfiguration();

        if (reducer.equals(Reducer.class)) {
            if (mapper.equals(Mapper.class)) {
                throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
            }
            job.setJarByClass(mapper);
        } else {
            job.setJarByClass(reducer);
        }

        job.setInputFormatClass(inputFormat);
        jobConf.set("mapred.input.dir", inputPath);

        job.setMapperClass(mapper);
        if (mapperKey != null) {
            job.setMapOutputKeyClass(mapperKey);
        }
        if (mapperValue != null) {
            job.setMapOutputValueClass(mapperValue);
        }

        jobConf.setBoolean("mapred.compress.map.output", true);

        job.setReducerClass(reducer);
        job.setOutputKeyClass(reducerKey);
        job.setOutputValueClass(reducerValue);

        job.setOutputFormatClass(outputFormat);
        jobConf.set("mapred.output.dir", outputPath);

        return job;
    }


    public static String getCustomJobName(String className, JobContext job,
                                          Class<? extends Mapper> mapper,
                                          Class<? extends Reducer> reducer) {
        StringBuilder name = new StringBuilder(100);
        String customJobName = job.getJobName();
        if (customJobName == null || customJobName.trim().isEmpty()) {
            name.append(className);
        } else {
            name.append(customJobName);
        }
        name.append('-').append(mapper.getSimpleName());
        name.append('-').append(reducer.getSimpleName());
        return name.toString();
    }


    public static void mkdir(Path path, boolean overwrite) throws IOException {


        Configuration config = new Configuration();
        FileSystem fs = path.getFileSystem(config);
        if (fs.exists(path) && !overwrite) {
            throw new IllegalStateException("Mkdir exception:path=" + path.toString() + " exists");
        }
        if (fs.exists(path)) {
            fs.delete(path, true);
        }
        fs.mkdirs(path);
        fs.close();
    }

    public static void delete(Configuration conf, Iterable<Path> paths) throws IOException {
        if (conf == null) {
            conf = new Configuration();
        }
        for (Path path : paths) {
            FileSystem fs = path.getFileSystem(conf);
            if (fs.exists(path)) {
                fs.delete(path, true);
            }
        }
    }

    public static void delete(Configuration conf, Path... paths) throws IOException {
        delete(conf, Arrays.asList(paths));
    }

    public static String readString(Path path, Configuration conf) throws IOException {
        FileSystem fs = path.getFileSystem(conf);
        FileStatus[] statuses = fs.listStatus(path);
        StringBuilder re = new StringBuilder();
        for (FileStatus status : statuses) {
            if (status.isFile() && !status.getPath().getName().equals("_SUCCESS")) {
                FSDataInputStream streaming = fs.open(status.getPath());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streaming));
                re.append(bufferedReader.readLine() + System.lineSeparator());
            }
        }
        return re.toString();
    }

    public static void main(String[] args) {
        log.info("debug");
    }

}
