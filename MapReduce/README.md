# MapReduce

## Part I: Map/Reduce input and output

* doMap
  * 使用inputStream读取inFile的内容
  * 转换为字符串之后调用mapF中的map函数获得inFile中的键值对
  * doMap要做的事情可以概括为把1个文件中的nTotal个键值对拆分到nReduce个中间文件中，平均每个文件有nTotal/nReduce个键值对
  * 考虑到键值对和对应的中间文件的映射关系里有hashcode操作，无法在对nReduce个文件的迭代中分别计算自己的键值对，因此在写文件前先开好了nReduce个键值对组
  * 开nReduce个键值对组不能只靠初始化的时候给一个size，那个指定的其实是capacity，真实size仍为0会导致下标越界，手动循环了nReduce次完成键值对组的初始化
  * 遍历nTotal个键值对完成分配工作
  * 再迭代nReduce次把分配好的键值对组以JSON格式写进中间文件，doMap工作完毕
* doReduce
  * 使用hashmap自动排序
  * 读取中间文件并将JSON格式的数据转回键值对组，此时仍是多对多
  * 遍历键值对组整理进hashmap，转为一对多
  * 遍历hashmap对每个key对应的多个value执行reduceF中的reduce函数得到一个输出value
  * 生成最终的键值对组，写入outFile，doReduce工作完毕

## Part II: Single-worker word count

* mapFunc
  * 使用Pattern和Matcher取下文章中的word作为key，value设为1，表示又出现了1次
  * 返回生成的键值对组
* reduceFunc
  * 对单个word的出现次数进行累加
  * 返回总和

## Part III: Distributing MapReduce tasks

* schedule
  * 初始化CountDownLatch，计数器需计数nTasks次
  * 初始化nTasks个线程
    * 在循环体内读registerChan
    * registerChan内部实现采用了BlockingQueue，不需要自己折腾锁什么的
    * 当有workers起得比schedule晚时registerChan会读着读着就空了，再去读就会抛异常，异常不需要做任何处理接着循环回去读即可
    * 读到RPC地址后就照着文档提示的流程准备参数，调RPC做任务等返回
    * 由于task比worker多，而read操作用到了take，worker会从registerChan中被移除，为了复用数量不够的worker需要把它write回去
    * 此时跳出循环，工作完成，执行countDown后返回
  * 启动nTasks个线程后await等待它们完成工作，最后结束调度任务
  * 疑问：目前没有用到interrupt，但是暂时没有想到用例，或许和后面的部分有关？