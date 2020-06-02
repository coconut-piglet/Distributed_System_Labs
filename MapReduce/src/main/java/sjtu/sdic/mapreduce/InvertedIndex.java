package sjtu.sdic.mapreduce;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import sjtu.sdic.mapreduce.common.KeyValue;
import sjtu.sdic.mapreduce.core.Master;
import sjtu.sdic.mapreduce.core.Worker;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Cachhe on 2019/4/24.
 */
public class InvertedIndex {

    public static List<KeyValue> mapFunc(String file, String value) {
        List<KeyValue> keyValuesRet = new ArrayList<>();

        /* use pattern and matcher to extract words */
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
        Matcher matcher = pattern.matcher(value);

        /* put words into key/value pairs, each with its value set to the file name */
        /* Matcher.find() tells whether next word exists (matches the regex) */
        while (matcher.find()) {
            /* after Matcher.find(), Matcher.group() returns that word */
            String key = matcher.group();
            keyValuesRet.add(new KeyValue(key, file));
        }

        return keyValuesRet;
    }

    public static String reduceFunc(String key, String[] values) {
        String retStr = "";

        /* notice that a word can appear multiple times in a single file */
        /* thus should use Set to prevent duplicates in output */
        Set<String> distinctFiles = new HashSet<>();

        /* create distinctFiles list and append fTotal to retStr */
        for (String file : values) {
            distinctFiles.add(file);
        }
        Integer fTotal = distinctFiles.size();
        retStr += fTotal.toString();
        retStr += " ";

        /* append files in distinctFiles to retStr */
        for (String file : distinctFiles) {
            retStr += file;
            retStr += ",";
        }

        /* remove the extra ',' */
        retStr = retStr.substring(0, retStr.length() - 1);

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
                mr = Master.sequential("iiseq", files, 3, InvertedIndex::mapFunc, InvertedIndex::reduceFunc);
            } else {
                mr = Master.distributed("wcdis", files, 3, args[1]);
            }
            mr.mWait();
        } else {
            Worker.runWorker(args[1], args[2], InvertedIndex::mapFunc, InvertedIndex::reduceFunc, 100, null);
        }
    }
}
