package sjtu.sdic.mapreduce;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import sjtu.sdic.mapreduce.common.KeyValue;
import sjtu.sdic.mapreduce.core.Master;
import sjtu.sdic.mapreduce.core.Worker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Cachhe on 2019/4/21.
 */
public class WordCount {

    public static List<KeyValue> mapFunc(String file, String value) {
        List<KeyValue> keyValuesRet = new ArrayList<>();

        /* use pattern and matcher to extract words */
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
        Matcher matcher = pattern.matcher(value);

        /* put words into key/value pairs, each with its appearance set to 1 */
        /* Matcher.find() tells whether next word exists (matches the regex) */
        while (matcher.find()) {
            /* after Matcher.find(), Matcher.group() returns that word */
            String key = matcher.group();
            keyValuesRet.add(new KeyValue(key, "1"));
        }

        return keyValuesRet;
    }

    public static String reduceFunc(String key, String[] values) {
        Integer retVal = 0;

        /* add all appearances together */
        for (String value : values) {
            retVal += Integer.valueOf(value);
        }

        String retStr = retVal.toString();
        return retStr;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("error: see usage comments in file");
        } else if (args[0].equals("master")) {
            Master mr;

            String src = args[2];
            File file = new File(".");
            String[] files = file.list(new WildcardFileFilter(src));
            if (args[1].equals("sequential")) {
                mr = Master.sequential("wcseq", files, 3, WordCount::mapFunc, WordCount::reduceFunc);
            } else {
                mr = Master.distributed("wcdis", files, 3, args[1]);
            }
            mr.mWait();
        } else {
            Worker.runWorker(args[1], args[2], WordCount::mapFunc, WordCount::reduceFunc, 100, null);
        }
    }
}
