#Large Scale Machine Learning Library.

LIBSML is a large-scale machine learning library. Its goal is to make practical machine learning scalable and easy.
It consists of common learning models, including logistic regression, linear supporting vector machine,
K-Means, collaborative filtering, as well as effective optimization methods, including L-BFGS, TRON, SGD, FTRL.

##Building LIBSML

LIBSML is built using Apache Maven. To build LIBSML, run

    build/mvn -DskipTests clean package

##[Distributed L-BFGS](https://github.com/libsml/libsml/tree/master/libsml-lbfgs)

A distributed implemention of L-BFGS, supporting L1-regularization, L2-regularization and three modes(local, mr and spark) to train/test a model.

##[Distributed liblinear](https://github.com/libsml/libsml/tree/master/libsml-liblinear)

A distributed implemention of liblinear, supporting TRON and three modes(local, mr and spark) to train/test a model.
