package common;

import java.io.Serializable;

public class Message implements Serializable {
    private String type;

    private String content;

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Message(String type, String content) {
        this.type = type;
        this.content = content;
    }
}
