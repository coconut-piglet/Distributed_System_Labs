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

* **关于Point 1**：看了官方文档中函数rte\_meter\_srtcm\_color\_blind\_check的定义注意到time指的是以cpu cycles为单位的当前cpu time stamp，传进来的time是ns为单位，需要做ns到cpu cycles的转换

  ```c++
  uint64_t tsc_frequency = rte_get_tsc_hz();
  uint64_t cpu_time_stamp_offset = time * tsc_frequency / 1000000000;
  ```

* **关于Point 2**：从函数定义和qos\_meter里的实现来看逻辑上这个time不应该作为参数传入，就应该去取当前的cpu time stamp，但实验中却是从main里传了一个从0开始每次循环增加1毫秒的变量进来，然后又提示说不应该从0开始计时，所以猜测应该是想模拟出两次调用间隔1毫秒的场景，传进来的time只是偏移量，于是在初始化的时候为每个流单独记录了初始化时的cpu time stamp作为基本量。

  ```c++
  uint64_t cpu_time_stamp_reference[APP_FLOWS_MAX];
  cpu_time_stamp_reference[i] = rte_rdtsc();
  time = cpu_time_stamp_reference[i] + cpu_time_stamp_offset;
  ```

另外原代码中兼容不同模式的代码在移植过程中改写为仅针对本实验的代码，暂时保留了原代码中的流参数，在main中增加了单个流的三色统计功能便于调试，详见代码。

## Task 1B  Implement a dropper

待施工