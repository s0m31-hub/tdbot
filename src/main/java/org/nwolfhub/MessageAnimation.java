package org.nwolfhub;

import java.io.Serializable;
import java.util.List;

public class MessageAnimation implements Serializable {
    public String name;
    public List<String> actions;

    public MessageAnimation() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
