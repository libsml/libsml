package com.github.libsml;

/**
 * Created by yellowhuang on 2015/4/14.
 */
public class StaticParameter {
    public static final String HADOOP_PREFIX = "hadoop";
    //第一类hadoop任务
    public static final String HADOOP_PREFIX1 = Config.prefixN(HADOOP_PREFIX, 1);
    //第二类hadoop任务
    public static final String HADOOP_PREFIX2 = "vd.hadoop";
    //第三类hadoop任务
    public static final String HADOOP_PREFIX3 = "cd.hadoop";

    public static final String HADOOP_PREFIX4 = Config.prefixN(HADOOP_PREFIX, 4);

    public static final String WEIGHT_LINK = "weightFile";

    public static final String W_SUB_PATH = "w";
    public static final String G_SUB_PATH = "g";
    public static final String S_SUB_PATH = "s";
    public static final String Y_SUB_PATH = "y";
    public static final String PG_SUB_PATH = "pg";
    public static final String VECFREE_DOT_SUB_PATH = "vec_free/dot";
    public static final String VECFREE_DIRECT_SUB_PATH = "vec_free/direct";
//    public final static String THETA_SUB_PATH = "theta";
//    public final static String THETA_LINK = "theta_link";
    public final static String THETA_PREFIX = "theta_";
    public final static String TEST_PATH = "test";

}
