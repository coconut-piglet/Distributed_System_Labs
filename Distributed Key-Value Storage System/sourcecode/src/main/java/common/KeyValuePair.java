package common;

import java.io.Serializable;

public class KeyValuePair implements Serializable {
    private final String key;

    private final String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
