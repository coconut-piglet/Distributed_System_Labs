# QoS Implementation with DPDK

## Task 1A  Implement a meter

meter使用单速率三颜色标记\(srTCM\)，工作在色盲模式下，关于srTCM有三个参数：

* Committed Information Rate \(CIR\) : 提交信息率，表示每秒IP包地字节数

* Committed Burst Size \(CBS\) : 提交Burst大小，以字节为单位
* Excess Burst Size \(EBS\) : 超量Burst大小，以字节为单位

不超过CBS标绿色，CBS到EBS之间标黄色，超过EBS标红色，色盲模式假定所有incoming packet无色。

首先给qos\.h补上缺失的头文件，否则没法用uint32\_t和uint64\_t：

```c++
#include <stdint.h>
```

然后基本上就是从qos\_meter移植代码了，函数对应关系如下：

| qos\_meter                      | qos                  |
| ------------------------------- | -------------------- |
| app\_configure\_flow\_table\(\) | qos\_meter\_init\(\) |
| app\_pkt\_handle\(\)            | qos\_meter\_run\(\)  |

原代码中兼容不同模式的代码在移植过程中改写为仅针对本实验的代码，暂时保留了原代码中的流参数，在main中增加了单个流的三色统计功能便于调试，详见代码。

## Task 1B  Implement a dropper

待施工