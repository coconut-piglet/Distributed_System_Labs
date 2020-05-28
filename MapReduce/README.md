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

施工中