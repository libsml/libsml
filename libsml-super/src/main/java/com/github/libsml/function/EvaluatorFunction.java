package com.github.libsml.function;

import com.github.libsml.model.Statistics;

/**
 * Created by yellowhuang on 2015/4/17.
 */
public interface EvaluatorFunction {


    Statistics evaluate(float[] w, int k);


}
