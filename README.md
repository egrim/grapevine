This is a java implementation of the bloomier filter proposed in [Chazelle et al.][paper] (section 3: An Optimal Bloomier Filter).  As far as I am aware this is the only freely available implementation online.  It includes separate classes for both the immutable and mutable structures.  Internally, the kryo serialization library is utilized to efficiently convert the values stored into byte arrays which can be utilized as described in the paper.  Otherwise it is pretty much a straight implementation of the proposed construction and accessor algorithm.  Suggestions and patches/pull requests gladly accepted to improve upon this humble first pass. 

  [paper]: http://webee.technion.ac.il/~ayellet/Ps/nelson.pdf 
