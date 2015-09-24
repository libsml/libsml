package com.github.libsml.job;

import com.github.libsml.Config;
import com.github.libsml.MLContext;
import com.github.libsml.data.Datas;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Created by huangyu on 15/5/24.
 */
public abstract class AbstractJobs {
//    protected final Config config;
//    protected final Configuration configuration;

    protected MLContext ctx;

    public AbstractJobs(MLContext ctx) {
        this.ctx=ctx;

    }


    protected String getRootPathString(){
        return ctx.getOutputPath();
    }

    protected String getDataPathsString(){
        return ctx.getInputPaths();
    }

    protected String getTestPathsString(){
        return ctx.getTestPath();
    }

    protected Path getOutPath(String sub){
        return new Path(getRootPathString(),sub);
    }

    protected Configuration getConfiguration(String name){
        return ctx.getHadoopConfWithInitation(name);
    }

    protected Config getConfig(){
        return ctx.getConf();
    }

    protected boolean lessMemory(){
        return ctx.isLessMemory();
    }

    protected void addConfig(Configuration configuration,String ... keys){
        if(keys!=null){
            for (String key :keys){
                if(getConfig().contains(key)) {
                    configuration.set(key, getConfig().get(key));
                }
            }
        }
    }

    protected void set(Configuration configuration,String key,String value){
        configuration.set(key,value);
    }

    protected void addFloatArrayCacheFile(Configuration configuration,float[] array, String sub, String link, Job job)
            throws IOException, URISyntaxException {
        Path tmpPath = getOutPath(sub);
        Datas.writeArrayOverwrite(tmpPath, configuration, array);
        job.addCacheFile(new URI(tmpPath.toString() + "#" + link));
    }

    protected void waitForCompletion(Job job) throws InterruptedException, IOException, ClassNotFoundException {
        if(!job.waitForCompletion(true)){
            throw new IllegalStateException("Job fail exception:"+job.getJobName());
        }
    }


}
