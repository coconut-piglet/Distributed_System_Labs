# QoS Implementation with DPDK

## Task 1A  Implement a meter

meter使用单速率三颜色标记\(srTCM\)，工作在色盲模式下，关于srTCM有三个参数：

* Committed Information Rate \(CIR\) : 提交信息率，表示每秒IP包地字节数

* Committed Burst Size \(CBS\) : 提交Burst大小，以字节为单位
* Excess Burst Size \(EBS\) : 超量Burst大小，以字节为单位

不超过CBS标绿色，CBS到EBS之间标黄色，超过EBS标红色，色盲模式假定所有incoming packet无色。

## Task 1B  Implement a dropper

待施工