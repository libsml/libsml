package com.github.libsml.model;

/**
 * Created by yellowhuang on 2015/6/15.
 */
public class Statistics {
    private float precision;
    private float recall;
    private float f1Score;
    private float auc;
    private float accuracy;

    public Statistics(float precision, float recall, float f1Score, float auc, float accuracy) {
        this.precision = precision;
        this.recall = recall;
        this.f1Score = f1Score;
        this.auc = auc;
        this.accuracy = accuracy;
    }

    public Statistics(String s) {
        String[] ss = s.split(",");
        if (ss.length < 5) {
            throw new IllegalStateException("Statistics string parse exception:" + s);
        }
        try {
            this.precision = Float.parseFloat(ss[0]);
            this.recall = Float.parseFloat(ss[1]);
            this.f1Score = Float.parseFloat(ss[2]);
            this.auc = Float.parseFloat(ss[3]);
            this.accuracy = Float.parseFloat(ss[4]);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Statistics string parse exception:" + s);
        }
    }

    @Override
    public String toString() {
        return precision + "," + recall + "," + f1Score + "," + auc + "," + accuracy;
    }

    public float getPrecision() {
        return precision;
    }

    public void setPrecision(float precision) {
        this.precision = precision;
    }

    public float getRecall() {
        return recall;
    }

    public void setRecall(float recall) {
        this.recall = recall;
    }

    public float getF1Score() {
        return f1Score;
    }

    public void setF1Score(float f1Score) {
        this.f1Score = f1Score;
    }

    public float getAuc() {
        return auc;
    }

    public void setAuc(float auc) {
        this.auc = auc;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
