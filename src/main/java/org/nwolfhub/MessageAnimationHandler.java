package org.nwolfhub;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageAnimationHandler {

    //messageAnimation: action1;action2;action3

    /*
    Actions:
    editTo:text;
    delay:time(ms);
     */

    private static List<String> actions = Arrays.asList("editTo", "delay", "print");
    public static boolean verifyAction(String action) {
        String[] split = action.split(":");
        if(actions.contains(split[0])) {
            if(split[0].equals("editTo")) {
                return split.length == 2;

            } else if(split[0].equals("print")) {
                return split.length == 2;
            } else if(split[0].equals("delay")) {
                try {
                    Integer time = Integer.valueOf(split[1]);
                    return true;
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    return false;
                }
            }
        }
        return false;
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
                for(String action:rawAnimation.split(";")) {
                    boolean res = verifyAction(action);
                    if(res) {
                        String[] split = action.split(":");
                        if (split[0].equals("editTo")) {
                            actions.add("e:" + split[1]);
                        } else if(split[0].equals("delay")) {
                            actions.add("d:" + split[1]);
                        } else if(split[0].equals("print")) {
                            actions.add("p:" + split[1]);
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
