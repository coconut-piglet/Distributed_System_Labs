package sjtu.sdic.mapreduce.core;

import sjtu.sdic.mapreduce.common.Channel;
import sjtu.sdic.mapreduce.common.DoTaskArgs;
import sjtu.sdic.mapreduce.common.JobPhase;
import sjtu.sdic.mapreduce.common.Utils;
import sjtu.sdic.mapreduce.rpc.Call;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Cachhe on 2019/4/22.
 */
public class Scheduler {

    /**
     * schedule() starts and waits for all tasks in the given phase (mapPhase
     * or reducePhase). the mapFiles argument holds the names of the files that
     * are the inputs to the map phase, one per map task. nReduce is the
     * number of reduce tasks. the registerChan argument yields a stream
     * of registered workers; each item is the worker's RPC address,
     * suitable for passing to {@link Call}. registerChan will yield all
     * existing registered workers (if any) and new ones as they register.
     *
     * @param jobName job name
     * @param mapFiles files' name (if in same dir, it's also the files' path)
     * @param nReduce the number of reduce task that will be run ("R" in the paper)
     * @param phase MAP or REDUCE
     * @param registerChan register info channel
     */
    public static void schedule(String jobName, String[] mapFiles, int nReduce, JobPhase phase, Channel<String> registerChan) {
        int nTasks = -1; // number of map or reduce tasks
        int nOther = -1; // number of inputs (for reduce) or outputs (for map)
        switch (phase) {
            case MAP_PHASE:
                nTasks = mapFiles.length;
                nOther = nReduce;
                break;
            case REDUCE_PHASE:
                nTasks = nReduce;
                nOther = mapFiles.length;
                break;
        }

        System.out.println(String.format("Schedule: %d %s tasks (%d I/Os)", nTasks, phase, nOther));

        /**
         * All ntasks tasks have to be scheduled on workers. Once all tasks
         * have completed successfully, schedule() should return.
         *
         * Your code here (Part III, Part IV).
         */

        /* init countDownLatch and threads for communicating with workers */
        CountDownLatch countDownLatch = new CountDownLatch(nTasks);
        List<Thread> workers = new ArrayList<>();

        /* init each thread and start them simultaneously */
        for (int i = 0; i < nTasks; i++) {
            /* init values required by current thread */
            final int fileNum = i;
            final int taskNum = i;
            final int otherPhaseNum = nOther;
            Thread worker = new Thread() {
                public void run() {
                    boolean jobDone = false;
                    while (!jobDone) {
                        try {
                            /* try read the worker's RPC address */
                            String RPCaddress = registerChan.read();
                            DoTaskArgs arg = new DoTaskArgs(jobName, mapFiles[fileNum], phase, taskNum, otherPhaseNum);
                            Call.getWorkerRpcService(RPCaddress).doTask(arg);
                            /* there are more tasks than workers, workers must be reused */
                            registerChan.write(RPCaddress);
                            jobDone = true;
                        } catch (InterruptedException e) {
                            /* in case some workers start running after schedule */
                            /* which will cause registerChan being empty when read */
                            /* read() will block and throw an InterruptedException */
                            /* just do nothing and continue looping */
                        }
                    }
                    /* decrease count after finish doing everything and return */
                    countDownLatch.countDown();
                }
            };
            workers.add(worker);
        }
        workers.forEach(Thread::start);

        /* and wait here until all threads finish their job */
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("Schedule: %s done", phase));
    }
}
