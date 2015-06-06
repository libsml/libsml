package com.github.libsml.lbfgs.core;


import com.github.libsml.function.LossFunction;

interface LineSearchFunction {

	int lineSearch(int n, float[] x, float[] f,
				   float[] g, float[] s, float[] stp,
				   float[] xp, float[] gp,  int k,
				   LossFunction loss, LBFGSParameter param);
}
