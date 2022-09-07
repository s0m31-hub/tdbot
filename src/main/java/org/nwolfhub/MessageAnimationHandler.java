package org.nwolfhub;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MessageAnimationHandler {

    private static final List<String> actions = Arrays.asList("editTo", "delay", "print", "append", "subtract");
    public static boolean verifyAction(String action) {
        String[] split = action.split(":");
        if (actions.contains(split[0])) {
            switch (split[0]) {
                case "editTo":
                case "print":
                    return split.length >= 2;
                case "append":
                    if (split.length >= 3) {
                        try {
                            Integer.parseInt(split[1]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                        return true;
                    }
                    break;
                case "subtract":
                    if (split.length == 3) {
                        try {
                            Integer.parseInt(split[1]);
                            Integer.parseInt(split[2]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                        return true;
                    }
                    break;
                case "delay":
                    try {
                        Integer.valueOf(split[1]);
                        return true;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        return false;
                    }
            }
        }
        return false;
    }
    private static int findPreviousText(List<String> actions, int stopIndex) {
        int lastIndex = -1;
        for(int now = 0; now<stopIndex; now++) {
            String action = actions.get(now);
            String[] split = action.split(":");
            if(split[0].equals("e") || split[0].equals("a") || split[0].equals("s")) {
                lastIndex = now;
            }
        }
        return lastIndex;
    }

    private static String buildPathToCt(List<String> actions) {
        StringBuilder builder = new StringBuilder();
        for(String action:actions) {
            String[] split = action.split(":");
            switch (split[0]) {
                case "ct" -> {
                    builder = new StringBuilder();
                    builder.append(split[1]);
                }
                case "cta" -> builder.append(split[1]);
                case "cts" -> {
                    String removed = builder.substring(0, Integer.parseInt(split[1]));
                    builder = new StringBuilder();
                    builder.append(removed);
                }
            }
        }
        return builder.toString();
    }

    public static MessageAnimation importAnimation(String name) throws IOException {
        File animationFileRaw = new File(name + ".numar");
        File animationFile = new File(name + ".numap");
        if(!animationFile.exists() && !animationFileRaw.exists()) {
            throw new IOException("Animation not found");
        }
        MessageAnimation animation;
        if(animationFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(animationFile))) {
                animation = (MessageAnimation) in.readObject();
                return animation;
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to import animation: wrong numap file");
            }
        } else {
            try (FileInputStream in = new FileInputStream(animationFileRaw)) {
                String rawAnimation = new String(in.readAllBytes()).replace(";\n", ";");
                animation = new MessageAnimation();
                animation.setName(name.replace(" ", "_"));
                List<String> actions = new ArrayList<>();
                for(int now = 0; now<rawAnimation.split(";").length; now++) {
                    String action = rawAnimation.split(";")[now];
                    boolean res = verifyAction(action);
                    if(res) {
                        String[] split = action.split(":");
                        switch (split[0]) {
                            case "editTo" -> {
                                actions.add("e:" + split[1]);
                                actions.add("ct:" + split[1]);
                            }
                            case "delay" -> actions.add("d:" + split[1]);
                            case "print" -> actions.add("p:" + split[1]);
                            case "append" -> {
                                int last = findPreviousText(actions, now);
                                if (last >= 0) {
                                    actions.add("a:" + split[1] + ":" + split[2] + ":LTB" + buildPathToCt(actions) + "LTE");
                                    actions.add("cta:" + split[2]);
                                } else {
                                    actions.add("a:" + split[1] + ":" + split[2] + ":LTB LTE");
                                }
                            }
                            case "subtract" -> {
                                int last = findPreviousText(actions, now);
                                if (last >= 0) {
                                    actions.add("s:" + split[1] + ":" + split[2] + ":LTB" + buildPathToCt(actions) + "LTE");
                                    actions.add("cts:" + split[2]);
                                }
                            }
                        }
                    } else {
                        System.out.println("Skipping wrong action: " + action);
                    }
                }
                animation.setActions(actions);
                System.out.println("Exporting new animation");
                exportAnimation(animation);
                System.out.println("Done!");
                return animation;
            }
        }
    }

    public static void exportAnimation(MessageAnimation animation) throws IOException {
        File animationFile = new File(animation.getName().replace(" ", "_") + ".numap");
        if(animationFile.exists()) {
            animationFile.delete();
        }
        animationFile.createNewFile();
        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(animationFile))) {
            out.writeObject(animation);
            out.flush();
        }
    }
}
