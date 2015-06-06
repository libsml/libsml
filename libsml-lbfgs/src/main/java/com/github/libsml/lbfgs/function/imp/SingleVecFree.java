package com.github.libsml.lbfgs.function.imp;

import com.github.libsml.commons.util.VecUtils;
import com.github.libsml.lbfgs.function.VecFreeFunction;

public class SingleVecFree implements VecFreeFunction {

	public final int m;
	private final float[][] s;
	private final float[][] y;
	// private final float[][] b;
	private final float[] pg;
	private int end;

	public SingleVecFree(int m, int n) {
		this.m = m;
		s = new float[m][n];
		y = new float[m][n];
		pg = new float[n];
		// b = new float[2 * m + 1][2 * m + 1];
		end = 0;
	}

	@Override
	public void vecsDot(float[][] b, int k, float[] x, float[] xp, float[] g,
			float[] gp, float[] pg) {
		VecUtils.vecdiff(s[end], x, xp);
		VecUtils.vecdiff(y[end], g, gp);
		VecUtils.veccpy(this.pg, pg);
		

		int bound = m <= k ? m : k;

		if (k <= m) {
			for (int i = 2 * bound - 3; i >= bound - 1; i--) {
				for (int j = 0; j <=i ; j++) {
					int jt = j >= bound - 1 ? j + 1 : j;
					b[i + 1][jt] = b[i][j];
					b[jt][i + 1] = b[j][i];
				}
			}
		} else {
			for (int i = 1; i < bound; i++) {
				for (int j = i; j < 2 * bound; j++) {
					b[i - 1][j - 1] = b[i][j];
					b[j - 1][i - 1] = b[j][i];
				}
			}

			for (int i = 1 + bound; i < 2 * bound; i++) {
				for (int j = i; j < 2 * bound; j++) {
					b[i - 1][j - 1] = b[i][j];
					b[j - 1][i - 1] = b[j][i];
				}
			}

		}
		
		b[2 * bound][2 * bound] = VecUtils.vecdot(pg, pg);
		for (int i = 0; i < 2 * bound; i++) {
			float[][] tmp = i >= bound ? y : s;
			int it = indexMap(i, bound);
			int j = i % bound;
			
			b[bound-1][it]= VecUtils.vecdot(s[end], tmp[j]);
			b[it][bound-1] = b[bound-1][it];
			
			b[2*bound-1][it]= VecUtils.vecdot(y[end], tmp[j]);
			b[it][2*bound-1] = b[2*bound-1][it];
			
			b[2 * bound][it] = VecUtils.vecdot(pg, tmp[j]);
			b[it][2 * bound] = b[2 * bound][it];

		}


		end = (end + 1) % m;

	}

	private int indexMap(int i, int bound) {
		int tmp = i < bound ? i : i - bound;
		int end2 = (end + 1) % m;
		return tmp < end2 ? bound + i - end2 : i - end2;
	}

	@Override
	public void computeDirect(float[] d, float[] theta, int k) {
		int bound = m <= k ? m : k;

		for (int i = 0; i < d.length; i++) {
			d[i] = 0;
		}

		for (int i = 0; i < 2 * bound; i++) {
			float[][] tmp = i >= bound ? y : s;
			int ii = i%bound;
			int it = ii < end ? bound + i - end : i - end;

//			System.out.println("end:it:"+it);
			VecUtils.vecadd(d, tmp[ii], theta[it]);
		}
		VecUtils.vecadd(d, pg, theta[2 * bound]);

	}

}
