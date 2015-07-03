Distributed L-BFGS.
==================================
  A distributed implemention of L-BFGS.

1.Features
-----------------------------------
* Use configuration file to configure, rather than options.</br>
* Can be configured to choose run with less memory(just one array of weight is stored in every node, the memory used in client is the same) but slower, or more memory but faster.
This is useful when the feature number is large.
* L2 and L1 normalization logistic regression</br>

2.Build
-----------------------------------
* You should first install [commons](https://github.com/libsml/libsml/tree/master/commons) and [libsml-super](https://github.com/libsml/libsml/tree/master/libsml-super)</br>
* Run "mvn package"

3.Reference
* Chen W, Wang Z, Zhou J. Large-scale L-BFGS using MapReduce[C] Advances in Neural Information Processing Systems. 2014: 1332-1340
* Andrew G, Gao J. Scalable training of L 1-regularized log-linear models[C] Proceedings of the 24th international conference on Machine learning. ACM, 2007: 33-40