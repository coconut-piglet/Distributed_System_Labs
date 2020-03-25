# Send and receive packets with DPDK

# Part 0

基础环境：Ubuntu 18.04 LTS，之前有安装build\-essential。

首先要安装DPDK，去官网下载DPDK，我选了目前LTS分支的最新版本19\.11，解压进入目录后执行：

```shell
 $ make install T=x86_64-native-linuxapp-gcc
```

光速报错`fatal error: numa.h: No such file or directory`，果然缺东西，安装依赖：

```shell
$ sudo apt install libnuma-dev
$ sudo apt install libpcap-dev
```

再执行好像还是有问题，不知道是否有伤大雅：

```
...
Build complete [x86_64-native-linuxapp-gcc]
Installation cannot run with T defined and DESTDIR undefined
```

Lab进行中...