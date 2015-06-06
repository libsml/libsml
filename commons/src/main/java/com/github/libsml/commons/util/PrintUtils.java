package com.github.libsml.commons.util;

import java.io.PrintStream;

/**
 * Created by yellowhuang on 2015/4/14.
 */

public class PrintUtils {
    public static void printFloatArray(PrintStream p,float[] array,String sep){
        for (int i=0;i<array.length;i++){
            if (i!=0){
                p.print(sep);
            }
            p.print(array[i]);
        }
        p.println();

    }

    public static void printFloatArray(float[] array,String sep){
        printFloatArray(System.out,array,sep);
    }

    public static void printFloatArray(float[] array){
        printFloatArray(array,",");
    }

    public static void printFloatMatrix(PrintStream p,float[][] matrix,String sep){
        for (float[] tmp:matrix){
            printFloatArray(p,tmp,sep);
        }

    }

    public static void printFloatMatrix(float[][] matrix,String sep){
        printFloatMatrix(System.out,matrix,sep);
    }

    public static void printFloatMatrix(float[][] matrix){
        printFloatMatrix(matrix,",");
    }

}
