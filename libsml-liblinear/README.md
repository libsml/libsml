Distributed liblinear.
==================================
  A distributed implementation of liblinear.

1.Support
-----------------------------------
* Trust region Newton method(TRON)</br>
* L2 normalization logistic regression</br>
* Three modes(local, mr(mapreduce) and spark) to train/test a model</br>
* Can be configured to choose run with using less memory(just one array of weight is stored in every node, the memory used in client is the same) but slower, or more memory but faster.
This is useful when the feature number is large.

2.Features
-----------------------------------
* Use configuration file to configure, rather than options.</br>
* Faster than the implementation of [spark liblinear](http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/distributed-liblinear/)

3.Build
-----------------------------------
* You should first install [commons](https://github.com/libsml/libsml/tree/master/commons) and [libsml-super](https://github.com/libsml/libsml/tree/master/libsml-super)</br>
* Run "mvn package"

4.Reference
-----------------------------------
* C.-Y. Lin, C.-H. Tsai, C.-P. Lee, and C.-J. Lin. Large-scale Logistic Regression and Linear Support Vector Machines Using Spark, IEEE International Conference on Big Data 2014