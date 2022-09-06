package org.nwolfhub;

import it.tdlight.client.*;
import it.tdlight.common.Init;
import it.tdlight.common.TelegramClient;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;

import java.io.*;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static SimpleTelegramClient client;
    public static void main(String[] args) throws IOException {
        try {
            Init.start();
        } catch (CantLoadLibrary e) {
            System.out.println("Can't load library. Are all dependencies installed?");
            e.printStackTrace();
        }
        File tokenFile = new File("token");
        if(!tokenFile.exists()) {
            tokenFile.createNewFile();
            System.out.println("File token was created at " + tokenFile.getAbsolutePath() + ", insert data as id:hash");
            System.exit(2);
        }
        FileInputStream in = new FileInputStream(tokenFile);
        String raw = new String(in.readAllBytes()).replace("\n", "");
        int appId =  Integer.parseInt(raw.split(":")[0]);
        String apiHash = raw.split(":")[1];
        APIToken token = new APIToken(appId, apiHash);
        TDLibSettings settings = TDLibSettings.create(token);
        Path session = Path.of("session");
        settings.setDatabaseDirectoryPath(session);
        settings.setDownloadedFilesDirectoryPath(session);
        if(args.length>0 && args[0].equals("testMode")) {
            System.out.println("Working under test name");
            settings.setDeviceModel("Nwolfhub test branch userbot client");
        } else {
            settings.setDeviceModel("Nwolfhub userbot client");
        }
        client = new SimpleTelegramClient(settings);
        client.start(AuthenticationData.consoleLogin());
        File documentsDir = new File("session/documents");
        if(!documentsDir.isDirectory()) documentsDir.mkdir();
        UpdateHandler.initialize(client);
        client.addUpdateHandler(TdApi.UpdateNewMessage.class, UpdateHandler::processUpdate);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File blackFile = new File("blackFile");
            if(!blackFile.exists()) {
                try {
                    blackFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException("Could not create black file: " + e);
                }
            }
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(blackFile))) {
                out.writeObject(UpdateHandler.blacklist);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                client.closeAndWait();
                System.out.println("Client closed");
                File documentsFolder = new File("session/documents");
                String[]entries = documentsFolder.list();
                for(String s:entries){
                    File currentFile = new File(documentsFolder.getPath(),s);
                    currentFile.delete();
                }
                documentsFolder.mkdir();
            } catch (InterruptedException e) {
                System.out.println("Failed to close client");
            }
        }));
    }
}
