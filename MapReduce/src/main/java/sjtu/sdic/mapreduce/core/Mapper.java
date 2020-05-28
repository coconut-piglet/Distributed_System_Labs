package sjtu.sdic.mapreduce.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import sjtu.sdic.mapreduce.common.KeyValue;
import sjtu.sdic.mapreduce.common.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cachhe on 2019/4/19.
 */
public class Mapper {

    /**
     * doMap manages one map task: it should read one of the input files
     * {@code inFile}, call the user-defined map function {@code mapF} for
     * that file's contents, and partition mapF's output into {@code nReduce}
     * intermediate files.
     *
     * There is one intermediate file per reduce task. The file name
     * includes both the map task number and the reduce task number. Use
     * the filename generated by {@link Utils#reduceName(String, int, int)}
     * as the intermediate file for reduce task r. RPCCall
     * {@link Mapper#hashCode(String)} on each key, mod nReduce,
     * to pick r for a key/value pair.
     *
     * {@code mapF} is the map function provided by the application. The first
     * argument should be the input file name, though the map function
     * typically ignores it. The second argument should be the entire
     * input file contents. {@code mapF} returns a list containing the
     * key/value pairs for reduce; see {@link KeyValue} for the definition of
     * KeyValue.
     *
     * Look at Java's File and Files API for functions to read
     * and write files.
     *
     * Coming up with a scheme for how to format the key/value pairs on
     * disk can be tricky, especially when taking into account that both
     * keys and values could contain newlines, quotes, and any other
     * character you can think of.
     *
     * One format often used for serializing data to a byte stream that the
     * other end can correctly reconstruct is JSON. You are not required to
     * use JSON, but as the output of the reduce tasks *must* be JSON,
     * familiarizing yourself with it here may prove useful. There're many
     * JSON-lib for Java, and we recommend and supply with FastJSON powered by
     * Alibaba. You can refer to official docs or other resources to figure
     * how to use it.
     *
     * The corresponding decoding functions can be found in {@link Reducer}.
     *
     * Remember to close the file after you have written all the values!
     *
     * Your code here (Part I).
     *
     * @param jobName the name of the MapReduce job
     * @param mapTask which map task this is
     * @param inFile file name (if in same dir, it's also the file path)
     * @param nReduce the number of reduce task that will be run ("R" in the paper)
     * @param mapF the user-defined map function
     */
    public static void doMap(String jobName, int mapTask, String inFile, int nReduce, MapFunc mapF) {
        File file = new File(inFile);
        InputStream inputStream = null;
        try {
            /* read data from input file */
            inputStream = new FileInputStream(file);
            int length = inputStream.available();
            byte[] data = new byte[length];
            inputStream.read(data);

            /* perform map function provided on input file and its contents */
            /* def: List<KeyValue> map(String file, String contents); */
            String inContents = new String(data);
            List<KeyValue> keyValuesSrc = mapF.map(inFile, inContents);

            /* partition mapF's output into nReduce intermediate files */
            /* 1 file with nTotal key/value pairs --> nReduce files, each with nTotal/nReduce key/value pairs */

            /* due to the use of hashCode, it's hard to create singie keyValuesDst during iteration, prepare it beforehead */
            List<List<KeyValue>> keyValuesDsts = new ArrayList<>(nReduce);
            for (KeyValue keyValue : keyValuesSrc) {
                /* hash code each key, mod nReduce, to pick target for a key/value pair */
                int target = Mapper.hashCode(keyValue.key) % nReduce;
                keyValuesDsts.get(target).add(keyValue);
            }

            /* write prepared key/value pairs to nReduce files */
            for (int i = 0; i < nReduce; i++) {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(Utils.reduceName(jobName, mapTask, i));
                    byte[] outContents = JSON.toJSONString(keyValuesDsts.get(i)).getBytes();
                    outputStream.write(outContents);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[error] failed to write interFile");
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("[error] failed to close interFile");
                        }
                    } else {
                        System.out.println("[info] doMap failed");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("[error] inFile does not exist");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[error] failed to read inFile");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[error] failed to close inFile");
                }
            } else {
                System.out.println("[info] doMap failed");
            }
        }
    }

    /**
     * a simple method limiting hash code to be positive
     *
     * @param src string
     * @return a positive hash code
     */
    private static int hashCode(String src) {
        return src.hashCode() & Integer.MAX_VALUE;
    }
}
