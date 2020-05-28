package sjtu.sdic.mapreduce.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import sjtu.sdic.mapreduce.common.KeyValue;
import sjtu.sdic.mapreduce.common.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by Cachhe on 2019/4/19.
 */
public class Reducer {

    /**
     * 
     * 	doReduce manages one reduce task: it should read the intermediate
     * 	files for the task, sort the intermediate key/value pairs by key,
     * 	call the user-defined reduce function {@code reduceF} for each key,
     * 	and write reduceF's output to disk.
     * 	
     * 	You'll need to read one intermediate file from each map task;
     * 	{@code reduceName(jobName, m, reduceTask)} yields the file
     * 	name from map task m.
     *
     * 	Your {@code doMap()} encoded the key/value pairs in the intermediate
     * 	files, so you will need to decode them. If you used JSON, you can refer
     * 	to related docs to know how to decode.
     * 	
     *  In the original paper, sorting is optional but helpful. Here you are
     *  also required to do sorting. Lib is allowed.
     * 	
     * 	{@code reduceF()} is the application's reduce function. You should
     * 	call it once per distinct key, with a slice of all the values
     * 	for that key. {@code reduceF()} returns the reduced value for that
     * 	key.
     * 	
     * 	You should write the reduce output as JSON encoded KeyValue
     * 	objects to the file named outFile. We require you to use JSON
     * 	because that is what the merger than combines the output
     * 	from all the reduce tasks expects. There is nothing special about
     * 	JSON -- it is just the marshalling format we chose to use.
     * 	
     * 	Your code here (Part I).
     * 	
     * 	
     * @param jobName the name of the whole MapReduce job
     * @param reduceTask which reduce task this is
     * @param outFile write the output here
     * @param nMap the number of map tasks that were run ("M" in the paper)
     * @param reduceF user-defined reduce function
     */
    public static void doReduce(String jobName, int reduceTask, String outFile, int nMap, ReduceFunc reduceF) {
        /* to sort the intermediate key/value pairs by key, use hashmap */
        Map<String, List<String>> sortedKeyValues = new HashMap<>();

        for (int i = 0; i < nMap; i++) {
            InputStream inputStream = null;
            try {
                /* read data from intermediate file */
                inputStream = new FileInputStream(Utils.reduceName(jobName, i, reduceTask));
                int length = inputStream.available();
                byte[] data = new byte[length];
                inputStream.read(data);

                /* extract intermediate key/value pairs */
                String interContents = new String(data);
                List<KeyValue> keyValuesSrc = JSON.parseArray(interContents, KeyValue.class);

                /* sort extracted intermediate key/value pairs */
                for (KeyValue keyValue : keyValuesSrc) {
                    if (sortedKeyValues.containsKey(keyValue.key)) {
                        sortedKeyValues.get(keyValue.key).add(keyValue.value);
                    } else {
                        List<String> values = new ArrayList<String>() {{add(keyValue.value);}};
                        sortedKeyValues.put(keyValue.key, values);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[error] failed to read interFile");
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("[error] failed to close interFile");
                    }
                } else {
                    System.out.println("[info] doReduce failed");
                }
            }
        }

        /* perform user-defined reduce function on each key */
        JSONObject keyValuesDst = new JSONObject();
        for (String key : sortedKeyValues.keySet()) {
            String[] values = sortedKeyValues.get(key).toArray(new String[0]);
            String reducedValue = reduceF.reduce(key, values);
            keyValuesDst.put(key, reducedValue);
        }

        /* write reduce output to outFile */
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outFile);
            byte[] outContents = keyValuesDst.toString().getBytes();
            outputStream.write(outContents);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[error] failed to write outFile");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[error] failed to close outFile");
                }
            } else {
                System.out.println("[info] doReduce failed");
            }
        }
    }
}
