# QoS Implementation with DPDK

## Task 1A  Implement a meter

首先给qos\.c补上缺失的头文件，否则没法用uint32\_t和uint64\_t：

```c++
#include <stdint.h>
```

需要初始化的内容有：

* 流的信息，每个流各一个

  ```c++
  struct rte_meter_srtcm app_flow[APP_FLOWS_MAX];
  ```

* 流的初始时间，每个流各一个：

  ```c++
  uint64_t cpu_time_stamp_reference[APP_FLOWS_MAX];
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

* **关于Point 2**：从函数定义和qos\_meter里的实现来看逻辑上这个time不应该作为参数传入，就应该去取当前的cpu time stamp，但实验中却是从main里传了一个从0开始每次循环增加1毫秒的变量进来，然后又提示说不应该从0开始计时，所以猜测应该是想模拟出两批调用间隔1毫秒的场景，传进来的time只是偏移量，于是在初始化的时候为每个流单独记录了初始化时的cpu time stamp作为基本量。

  ```c++
  uint64_t cpu_time_stamp_reference[APP_FLOWS_MAX];
  cpu_time_stamp_reference[i] = rte_rdtsc();
  time = cpu_time_stamp_reference[i] + cpu_time_stamp_offset;
  ```

另外原代码中兼容不同模式的代码在移植过程中改写为仅针对本实验的代码，暂时保留了原代码中的流参数，在main中增加了单个流的三色统计功能便于调试，详见代码。

## Task 1B  Implement a dropper

需要初始化的内容有：

* 运行时数据，每个流的每种颜色各一个

  ```c++
  struct rte_red app_red[APP_FLOWS_MAX][e_RTE_METER_COLORS];
  ```

* 配置信息，每个流的每种颜色各一个

  ```c++
  struct rte_red_config app_red_config[APP_FLOWS_MAX][e_RTE_METER_COLORS];
  ```

* 队列大小，每个流各一个

  ```c++
  unsigned queue_size[APP_FLOWS_MAX];
  ```

最开始没注意随意取值结果初始化每次都返回\-2后来发现原来数字不能乱填。

基本就是给每一个流的每一种颜色初始化数据，然后初始化它们的队列大小为0。关于超时清空队列，通过输出发现q\_time一开始是0，而time本身就是ns计算的，因此此处没有转换和加参考量，直接用time和q\_time进行了比较，但是传给rte\_red\_enqueue的time依旧是加了参考量的，详见代码。

## Task 2 Deduce parameters

目标是通过调整meter和dropper的参数使得最终flow 0\~4的通过的包总数之比为8:4:2:1。

首先要尽可能正确地认识那些参数的含义：

* srTCM
  * cir：往桶里投放令牌的速率，注意一个令牌对应的是一个字节
  * cbs：突发令牌桶容量，桶中的令牌数表示当前可标绿色通过的包最大为多少字节
  * ebs：超额突发令牌桶容量，桶中的令牌数表示当前可标黄色通过的包最大为多少字节

* WRED
  * wq\_log2：计算平均队列长度时对输入流量变化的反应程度
  * min\_th：最小队列长度，队列长度在该值内不会发生丢包
  * max\_th：最大队列长度，队列长度超过该值一定丢包
  * maxp\_inv：队列长度介于最小与最大之间时的丢包概率

由于flow 0要求的带宽与总带宽一致，因此flow 0发送的所有的包都要顺利通过，于是针对flow 0的参数选择的目标定为在meter中所有包被标记为绿色，在dropper中所有的包被判定通过。限制包不被标记为绿色的因素有：

* cir不够大导致即使桶容量大于包大小，但是填充速度过慢，令牌数不够最终标黄或标红
* cbs不够大导致即使填充够快，但是桶容量小于包大小最终标黄或标红

解决限制1，cir应不小于流单位时间发送的字节量，通过阅读main\.c得到信息：时间间隔1000000ns内，平均发1000个包，平均每个包640字节，所以流单位时间发送字节量平均为：

```
cir = packets total / number of flows x packet size in Byte x scale to 1s
    = 1000 / 4 x 640 x (10^9 / 10^6)
    = 160000000
```

解决限制2，cbs应不小于瞬间涌入包的大小总和最大值，即某一时刻发送的包全来自flow 0：

```
cbs = packets total x packet size
    = 1000 x 640
    = 640000
```

然后ebs随便取一个大数字即可，理论上flow 0的ebs桶第一次充满就不会再变了。测试了一下发现确实全部标记为绿包，并且dropper中随便选的flow 0绿包参数已经能让绿包全部通过了因此没有再动，flow 0不会有黄包红包所以参数选了和绿包一样的，看上去整整齐齐一家人。