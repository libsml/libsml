package com.github.libsml.Job;

import com.github.libsml.Config;
import com.github.libsml.data.Datas;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.github.libsml.Configs.*;


/**
 * Created by huangyu on 15/5/24.
 */
public abstract class AbstractJob {
    protected final Config config;
    protected final Configuration configuration;

    public AbstractJob(Config config) {

        this.config = config;
        this.configuration=new Configuration();
    }


    protected String getRootPathString(){
        return getOutputPath(config);
    }

    protected String getDataPathsString(){
        return getInputPaths(config);
    }

    protected String getTestPathsString(){
        return config.get("test.paths");
    }

    protected Path getOutPath(String sub){
        return new Path(getRootPathString(),sub);
    }

    protected Configuration getConfiguration(){
        return configuration;
    }

    protected Config getConfig(){
        return config;
    }

    protected boolean lessMemory(){
        return config.getBoolean("loss.lr.mr.less_memory", false);
    }

    protected void addConfigWithPrefix(String ... prefixs){
        if(prefixs!=null){
            for (String prefix:prefixs){
                config.addConfig(configuration, prefix);
            }
        }
    }

    protected void addConfig(String ... keys){
        if(keys!=null){
            for (String key :keys){
                if(config.contains(key)) {
                    configuration.set(key, config.get(key));
                }
            }
        }
    }

    protected void set(String key,String value){
        configuration.set(key,value);
    }

    protected void addFloatArrayCacheFile(float[] array, String sub, String link, Job job)
            throws IOException, URISyntaxException {
        Path tmpPath = getOutPath(sub);
        Datas.writeArrayOverwrite(tmpPath, getConfiguration(), array);
        job.addCacheFile(new URI(tmpPath.toString() + "#" + link));
    }

    protected void waitForCompletion(Job job) throws InterruptedException, IOException, ClassNotFoundException {
        if(!job.waitForCompletion(true)){
            throw new IllegalStateException("Job fail exception:"+job.getJobName());
        }
    }


}
