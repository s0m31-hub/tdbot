package org.nwolfhub;

import org.apache.commons.io.FileUtils;
import org.nwolfhub.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DockerIntegrator {
    public static Boolean enabled = false;
    public static final String bash = "bash:5.2";
    public static final String python = "python:3.9.17";
    public static final String java = "amazoncorretto:17";
    public static final String fedora = "fedora:38";
    public static void initDocker() {
        try {
            Process initProcess = Runtime.getRuntime().exec("docker info");
            if(initProcess.waitFor() == 0) {
                System.out.println("Pulling bash...");
                Runtime.getRuntime().exec("docker pull " + bash).waitFor();
                System.out.println("Pulling python...");
                Runtime.getRuntime().exec("docker pull " + python).waitFor();
                System.out.println("Pulling java...");
                Runtime.getRuntime().exec("docker pull " + java).waitFor();
                System.out.println("Pulling fedora...");
                Runtime.getRuntime().exec("docker pull " + fedora).waitFor();
                System.out.println("Finished!");
                enabled = true;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createImage(File file, String lang, String extra, String... cmd) throws IOException {
        File dockerDir = new File("docker" + Utils.generateString(15));
        dockerDir.mkdirs();
        File dockerFile = new File(dockerDir, "Dockerfile");
        dockerFile.createNewFile();
        Files.copy(file.getAbsoluteFile().toPath(), Path.of(dockerDir.getAbsolutePath() + "/" + file.getName()));
        try(FileOutputStream out = new FileOutputStream(dockerFile)) {
            String content = "FROM " + lang + "\n" + "ADD " + file.getName() + " /\n"+ extra + "\n" + "CMD [" + Arrays.stream(cmd).map(e -> {
                e = "\"" + e + "\"";
                return e;
            }).collect(Collectors.joining(", ")) + "]";
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        String name = Utils.generateString(20).toLowerCase();
        System.out.println("Building docker image (name=" + name + ")");
        File logDir = new File("dockerLogs");
        if(!logDir.isDirectory()) logDir.mkdir();
        File logFile = new File(logDir, name + ".log");
        logFile.createNewFile();
        ProcessBuilder builder = new ProcessBuilder("docker", "build", "-t", name, dockerDir.getAbsolutePath());
        builder.redirectOutput(logFile);
        builder.redirectError(logFile);
        Process buildProcess = builder.start();
        try {
            int code = buildProcess.waitFor();
            if(code==0) {
                return name;
            }
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            FileUtils.forceDelete(dockerDir);
            FileUtils.forceDelete(file);
        }
    }

    public static String run(String image, Integer wait) throws IOException {
        String name = Utils.generateString(30);
        File outputFile = new File("docker" + name + ".out");
        ProcessBuilder builder = new ProcessBuilder("docker", "container", "run", "--name", name, image);
        builder.redirectOutput(outputFile);
        builder.redirectError(outputFile);
        Process docker = builder.start();
        try {
            boolean finished = docker.waitFor(wait, TimeUnit.SECONDS);
            if(!finished) {
                docker.destroyForcibly();
            }
            Runtime.getRuntime().exec("docker kill " + name).waitFor();
            Runtime.getRuntime().exec("docker container rm -f " + name).waitFor();
            Runtime.getRuntime().exec("docker image rm " + image).waitFor();
            try (FileInputStream in = new FileInputStream(outputFile)) {
                return new String(in.readAllBytes());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.forceDelete(outputFile);
        }
    }
}
