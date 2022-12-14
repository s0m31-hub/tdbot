package org.nwolfhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.ibm.icu.text.Transliterator;
import it.tdlight.client.Result;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import okhttp3.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class UpdateHandler {
    public static Set<Long> blacklist;
    private static SimpleTelegramClient client;
    private static final List<String> patterns = Arrays.asList("bubbles", "squares", "gothic", "cursive");
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void initialize(SimpleTelegramClient client) throws IOException {
        UpdateHandler.client = client;
        File blackFile = new File("blackFile");
        if(!blackFile.exists()) {
            blackFile.createNewFile();
            blacklist = new HashSet<>();
        } else {
            try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(blackFile))) {
                // noinspection unchecked
                blacklist = (Set<Long>) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Update handler initialized. Blacklisted users: " + blacklist.size());
    }
    @SuppressWarnings("rawtypes")
    public static void requestFailHandler(Result result) {
        if(result.isError()) {
            System.out.println("Failed to execute request: " + result.getError());
        }
    }
    @SuppressWarnings("SynchronizeOnNonFinalField")
    private static void ignore(long chatId, TdApi.UpdateNewMessage update) {
        if(update.message.senderId instanceof TdApi.MessageSenderUser) {
            TdApi.EditMessageText request = new TdApi.EditMessageText();
            if(chatId>0) {
                blacklist.add(chatId);
                request.chatId=chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(chatId + " was added to ignore list", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                if(update.message.replyToMessageId == 0) {
                    request = new TdApi.EditMessageText();
                    request.chatId=chatId;
                    request.messageId=update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                    return;
                }
                final AtomicLong sender = new AtomicLong();
                client.send(new TdApi.GetMessage(chatId, update.message.replyToMessageId), result -> {
                    sender.set(((TdApi.MessageSenderUser) result.get().senderId).userId);
                    synchronized (blacklist) {
                        blacklist.add(sender.get());
                    }
                    TdApi.EditMessageText request2 = new TdApi.EditMessageText();
                    request2.chatId=chatId;
                    request2.messageId=update.message.id;
                    request2.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(sender.get() + " was added to ignore list", new TdApi.TextEntity[0]), false, false);
                    client.send(request2, UpdateHandler::requestFailHandler);
                });
            }
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField", "SpellCheckingInspection"})
    public static void unignore(long chatId, TdApi.UpdateNewMessage update) {
        if(update.message.senderId instanceof TdApi.MessageSenderUser) {
            TdApi.EditMessageText request = new TdApi.EditMessageText();
            if(chatId>0) {
                blacklist.remove(chatId);
                request.chatId=chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(chatId + " was removed from ignore list", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                if(update.message.replyToMessageId == 0) {
                    request = new TdApi.EditMessageText();
                    request.chatId=chatId;
                    request.messageId=update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                    return;
                }
                final AtomicLong sender = new AtomicLong();
                client.send(new TdApi.GetMessage(chatId, update.message.replyToMessageId), result -> {
                    sender.set(((TdApi.MessageSenderUser) result.get().senderId).userId);
                    synchronized (blacklist) {
                        blacklist.remove(sender.get());
                    }
                    TdApi.EditMessageText request2 = new TdApi.EditMessageText();
                    request2.chatId=chatId;
                    request2.messageId=update.message.id;
                    request2.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(sender.get() + " was removed from ignore list", new TdApi.TextEntity[0]), false, false);
                    client.send(request2, UpdateHandler::requestFailHandler);
                });
            }

        }
    }

    private static String reverse(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    public static void reverse(String text, TdApi.UpdateNewMessage update) {
        TdApi.EditMessageText request = new TdApi.EditMessageText();
        long chatId = update.message.chatId;
        if(text.equalsIgnoreCase("!reverse")) {
            if(update.message.replyToMessageId == 0) {
                request.chatId=chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                client.send(new TdApi.GetMessage(chatId, update.message.replyToMessageId), response -> {
                    String textToInverse = ((TdApi.MessageText)response.get().content).text.text;
                    request.chatId=chatId;
                    request.messageId=update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(reverse(textToInverse), new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                });
            }
        } else if (text.toLowerCase().contains("!reverse")) {
            String[] split = text.split(" ");
            if(split[0].equals("!reverse") && split.length > 1) {
                String textToInverse = text.replace("!reverse ", "");
                request.chatId=chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(reverse(textToInverse), new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            }
        }
    }

    private static String brokenText(String text) {
        StringBuilder ready = new StringBuilder();
        Random r = new Random();
        for(String ch:text.split("")) {
            ready.append(r.nextInt(2)==1?ch.toUpperCase():ch.toLowerCase());
        }
        return ready.toString();
    }

    public static void mix(String text, TdApi.UpdateNewMessage update) {
        TdApi.EditMessageText request = new TdApi.EditMessageText();
        long chatId = update.message.chatId;
        if(text.equalsIgnoreCase("!mix") || text.equalsIgnoreCase("!broke")) {
            if(update.message.replyToMessageId == 0) {
                request.chatId=chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                client.send(new TdApi.GetMessage(chatId, update.message.replyToMessageId), response -> {
                    String newText = ((TdApi.MessageText)response.get().content).text.text;
                    request.chatId = chatId; request.messageId = update.message.id; request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(brokenText(newText), new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                });
            }
        } else {
            String[] split = text.split(" ");
            if(split[0].equalsIgnoreCase("!mix") || split[0].equalsIgnoreCase("!broke")) {
                request.chatId = chatId; request.messageId = update.message.id; request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(brokenText(text.replace("!mix ", "").replace("!broke ", "")), new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            }
        }
    }

    private static String continueText(String text) throws IOException {
        OkHttpClient okClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build();
        Random r = new Random();
        Response response = okClient.newCall(new Request.Builder().url("https://pelevin.gpt.dobro.ai/generate/").post(RequestBody.create(("{\"prompt\": \"" + text.replace("\n", "%0A") + "\", \"length\": " + (r.nextInt(65) + 15) + "}").getBytes(StandardCharsets.UTF_8))).build()).execute();
        if(response.isSuccessful()) {
            JsonArray responses = JsonParser.parseString(Objects.requireNonNull(response.body()).string()).getAsJsonObject().get("replies").getAsJsonArray();
            response.close();
            return responses.get(r.nextInt(responses.size())).getAsString();
        } else {
            response.close();
            throw new IOException(response.message());
        }
    }

    public static void continueText(String text, TdApi.UpdateNewMessage update) {
        TdApi.EditMessageText request = new TdApi.EditMessageText();
        if(text.equals("!continue")) {
            if(update.message.replyToMessageId == 0) {
                request.chatId=update.message.chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                client.send(new TdApi.GetMessage(update.message.chatId, update.message.replyToMessageId), result -> {
                    request.chatId = update.message.chatId;
                    request.messageId = update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Request sent. Processing...", new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                    try {
                        String oldText = ((TdApi.MessageText)result.get().content).text.text;
                        String newText = continueText(oldText);
                        request.chatId = update.message.chatId;
                        request.messageId = update.message.id;
                        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(oldText + newText, new TdApi.TextEntity[0]), false, false);
                        client.send(request, UpdateHandler::requestFailHandler);
                    } catch (IOException e) {
                        request.chatId = update.message.chatId;
                        request.messageId = update.message.id;
                        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Failed to process request: " + e, new TdApi.TextEntity[0]), false, false);
                        client.send(request, UpdateHandler::requestFailHandler);
                    }
                });
            }
        } else {
            String[] split = text.split(" ");
            if(split[0].equalsIgnoreCase("!continue")) {
                request.chatId = update.message.chatId;
                request.messageId = update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Request sent. Processing...", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
                try {
                    String oldText = text.replace("!continue ", "");
                    String newText = continueText(oldText);
                    request.chatId = update.message.chatId;
                    request.messageId = update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(oldText + newText, new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                } catch (IOException e) {
                    request.chatId = update.message.chatId;
                    request.messageId = update.message.id;
                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Failed to process request: " + e, new TdApi.TextEntity[0]), false, false);
                    client.send(request, UpdateHandler::requestFailHandler);
                }
            }
        }
    }

    public static void upgradeText(String text, TdApi.UpdateNewMessage update) {
        TdApi.EditMessageText request = new TdApi.EditMessageText();
        String[] split = text.split(" ");
        if(split.length==1) {
            request.chatId=update.message.chatId;
            request.messageId=update.message.id;
            request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Usage: !upgrade pattern", new TdApi.TextEntity[0]), false, false);
            client.send(request, UpdateHandler::requestFailHandler);
        }
        if(split.length==2 && split[0].equalsIgnoreCase("!upgrade")) {
            if(update.message.replyToMessageId == 0) {
                request.chatId=update.message.chatId;
                request.messageId=update.message.id;
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Please reply to a message", new TdApi.TextEntity[0]), false, false);
                client.send(request, UpdateHandler::requestFailHandler);
            } else {
                client.send(new TdApi.GetMessage(update.message.chatId, update.message.replyToMessageId), response -> {
                    request.chatId = update.message.chatId;
                    request.messageId = update.message.id;
                    String oldText = ((TdApi.MessageText)response.get().content).text.text;
                    if(patterns.contains(split[1].toLowerCase())) {
                        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(upgradeText(oldText, split[1].toLowerCase()), new TdApi.TextEntity[0]), false, false);
                    } else {
                        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Pattern " + split[1] + " not found. Existing patterns:\n" + String.join(", ", patterns), new TdApi.TextEntity[0]), false, false);
                    }
                    client.send(request, UpdateHandler::requestFailHandler);
                });
            }
        } else if(split.length>2 && split[0].equalsIgnoreCase("!upgrade")) {
            request.chatId = update.message.chatId;
            request.messageId = update.message.id;
            String oldText = text.replace("!upgrade " + split[1] + " ", "");
            if(patterns.contains(split[1].toLowerCase())) {
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(upgradeText(oldText, split[1].toLowerCase()), new TdApi.TextEntity[0]), false, false);
            } else {
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Pattern " + split[1] + " not found. Existing patterns:\n" + String.join(", ", patterns), new TdApi.TextEntity[0]), false, false);
            }
            client.send(request, UpdateHandler::requestFailHandler);
        }
    }

    private static String upgradeText(String text, String pattern) {
        var CYRILLIC_TO_LATIN = "Russian-Latin/BGN";
        Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);
        String translate = toLatinTrans.transliterate(text).toLowerCase();
        System.out.println(translate);
        StringBuilder result = new StringBuilder();
        @SuppressWarnings("SpellCheckingInspection") List<String> english = Arrays.asList("abcdefghijklmnopqrstuvwxyz".split(""));
        switch (pattern) {
            case "bubbles" -> {
                List<String> another = Arrays.asList("\uD83C\uDD50 \uD83C\uDD51 \uD83C\uDD52 \uD83C\uDD53 \uD83C\uDD54 \uD83C\uDD55 \uD83C\uDD56 \uD83C\uDD57 \uD83C\uDD58 \uD83C\uDD59 \uD83C\uDD5A \uD83C\uDD5B \uD83C\uDD5C \uD83C\uDD5D \uD83C\uDD5E \uD83C\uDD5F \uD83C\uDD60 \uD83C\uDD61 \uD83C\uDD62 \uD83C\uDD63 \uD83C\uDD64 \uD83C\uDD65 \uD83C\uDD66 \uD83C\uDD67 \uD83C\uDD68 \uD83C\uDD69".split(" "));
                for (String character : translate.split("")) {
                    if (english.contains(character)) result.append(another.get(english.indexOf(character)));
                    else result.append(character);
                }
            }
            case "squares" -> {
                List<String> another = Arrays.asList("\uD83C\uDD70 \uD83C\uDD71 \uD83C\uDD72 \uD83C\uDD73 \uD83C\uDD74 \uD83C\uDD75 \uD83C\uDD76 \uD83C\uDD77 \uD83C\uDD78 \uD83C\uDD79 \uD83C\uDD7A \uD83C\uDD7B \uD83C\uDD7C \uD83C\uDD7D \uD83C\uDD7E \uD83C\uDD7F \uD83C\uDD80 \uD83C\uDD81 \uD83C\uDD82 \uD83C\uDD83 \uD83C\uDD84 \uD83C\uDD85 \uD83C\uDD86 \uD83C\uDD87 \uD83C\uDD88 \uD83C\uDD89".split(" "));
                for (String character : translate.split("")) {
                    if (english.contains(character)) result.append(another.get(english.indexOf(character)));
                    else result.append(character);
                }
            }
            case "gothic" -> {
                List<String> another = Arrays.asList("\uD835\uDD86 \uD835\uDD87 \uD835\uDD88 \uD835\uDD89 \uD835\uDD8A \uD835\uDD8B \uD835\uDD8C \uD835\uDD8D \uD835\uDD8E \uD835\uDD8F \uD835\uDD90 \uD835\uDD91 \uD835\uDD92 \uD835\uDD93 \uD835\uDD94 \uD835\uDD95 \uD835\uDD96 \uD835\uDD97 \uD835\uDD98 \uD835\uDD99 \uD835\uDD9A \uD835\uDD9B \uD835\uDD9C \uD835\uDD9D \uD835\uDD9E \uD835\uDD9F".split(" "));
                for (String character : translate.split("")) {
                    if (english.contains(character)) result.append(another.get(english.indexOf(character)));
                    else result.append(character);
                }
            }
            case "cursive" -> {
                List<String> another = Arrays.asList("\uD835\uDCEA \uD835\uDCEB \uD835\uDCEC \uD835\uDCED \uD835\uDCEE \uD835\uDCEF \uD835\uDCF0 \uD835\uDCF1 \uD835\uDCF2 \uD835\uDCF3 \uD835\uDCF4 \uD835\uDCF5 \uD835\uDCF6 \uD835\uDCF7 \uD835\uDCF8 \uD835\uDCF9 \uD835\uDCFA \uD835\uDCFB \uD835\uDCFC \uD835\uDCFD \uD835\uDCFE \uD835\uDCFF \uD835\uDD00 \uD835\uDD01 \uD835\uDD02 \uD835\uDD03".split(" "));
                result.append("???");
                for (String character : translate.split("")) {
                    if (english.contains(character)) result.append(another.get(english.indexOf(character)));
                    else result.append(character);
                }
                result.append("???");
            }
        }
        return result.toString();
    }

    public static void importFile(TdApi.UpdateNewMessage update, final String rename) {
        TdApi.EditMessageCaption editMessageText = new TdApi.EditMessageCaption();
        editMessageText.messageId = update.message.id;
        editMessageText.chatId = update.message.chatId;
        TdApi.File document = ((TdApi.MessageDocument) update.message.content).document.document;
        TdApi.DownloadFile getFile = new TdApi.DownloadFile();
        getFile.fileId = document.id;
        getFile.priority = 32;
        File documentsFolder = new File("session/documents");
        String[]entries = documentsFolder.list();
        for(String s: Objects.requireNonNull(entries)){
            File currentFile = new File(documentsFolder.getPath(),s);
            currentFile.delete();
        }
        documentsFolder.mkdir();
        client.send(getFile, result -> {
            editMessageText.caption = new TdApi.FormattedText("Downloading file...", new TdApi.TextEntity[0]);
            client.send(editMessageText, UpdateHandler::requestFailHandler);
            if(result.isError()) {
                System.out.println(result.getError());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            editMessageText.caption = new TdApi.FormattedText("Moving file...", new TdApi.TextEntity[0]);
            client.send(editMessageText, UpdateHandler::requestFailHandler);
            try {
                String[] entries2 = documentsFolder.list();
                for(String s: Objects.requireNonNull(entries2)){
                    File downloaded = new File(documentsFolder.getPath(),s);
                    String localRename = rename==null?downloaded.getName():rename;
                    downloaded.renameTo(new File(localRename));
                    editMessageText.caption = new TdApi.FormattedText("Download completed", new TdApi.TextEntity[0]);
                    client.send(editMessageText, UpdateHandler::requestFailHandler);
                }
            } catch (Exception e) {
                e.printStackTrace();
                editMessageText.caption = new TdApi.FormattedText("Download failed", new TdApi.TextEntity[0]);
                client.send(editMessageText, UpdateHandler::requestFailHandler);
            }
        });
    }
    
    public static void animateMessage(String text, TdApi.UpdateNewMessage update) {
        String[] splitMsg = text.split(" ");
        if(splitMsg[0].equals("!animate") && splitMsg.length==2) {
            String name = splitMsg[1];
            TdApi.EditMessageText request = new TdApi.EditMessageText();
            request.chatId = update.message.chatId;
            request.messageId = update.message.id;
            try {
                MessageAnimation animation = MessageAnimationHandler.importAnimation(name);
                for (String action : animation.getActions()) {
                    String[] split = action.split(":");
                    if(split[0].equals("ct")||split[0].equals("cta")) continue;
                    switch (split[0]) {
                        case "e":
                            request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(split[1], new TdApi.TextEntity[0]), true, true);
                            client.send(request, UpdateHandler::requestFailHandler);
                            break;
                        case "d":
                            try {
                                Thread.sleep(Integer.parseInt(split[1]));
                            } catch (InterruptedException ignored) {
                            }
                            break;
                        case "p":
                            System.out.println(split[1]);
                            break;
                        case "a": {
                            StringBuilder now = new StringBuilder();
                            now.append(action.split(":LTB")[1].split("LTE")[0]);
                            for (String c : split[2].split("")) {
                                now.append(c);
                                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(now.toString(), new TdApi.TextEntity[0]), true, true);
                                client.send(request, UpdateHandler::requestFailHandler);
                                try {
                                    Thread.sleep(Integer.parseInt(split[1]));
                                } catch (InterruptedException ignored) {
                                }
                            }
                            break;
                        }
                        case "s": {
                            String now = (action.split(":LTB")[1].split("LTE")[0]);
                            for (int cur = 1; cur <= Integer.parseInt(split[2]); cur++) {
                                try {
                                    request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(now.substring(0, now.length() - cur), new TdApi.TextEntity[0]), true, true);
                                    client.send(request, UpdateHandler::requestFailHandler);
                                    try {
                                        //noinspection BusyWait
                                        Thread.sleep(Integer.parseInt(split[1]));
                                    } catch (InterruptedException ignored) {
                                    }
                                } catch (StringIndexOutOfBoundsException ignored) {
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("Error occurred while importing animation: " + e, new TdApi.TextEntity[0]), true, true);
                client.send(request, UpdateHandler::requestFailHandler);
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static void sendHelp(TdApi.UpdateNewMessage update) {
        TdApi.EditMessageText request = new TdApi.EditMessageText();
        request.chatId = update.message.chatId;
        request.messageId = update.message.id;
        String help = """
                Thanks for using tdbot! Making future in nwolfhub with Java and \u2764\uFE0F
                Here is what you can do now:
                
                !ignore *id* - add user to ignore list
                
                !unignore *id - remove user from ignore list
                
                !reverse *text* - reverse message
                
                !mix, !broke *text* - mix uppercase and lowercase characters
                
                !continue - use AI to continue a phrase. Better with russian texts
                
                !upgrade *style* *text* - write text with another style
                
                !animate *animation name* - apply some magic to your message!
                
                !import - download file from attachment
                """;
        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(help, new TdApi.TextEntity[0]), true, true);
        client.send(request, UpdateHandler::requestFailHandler);
    }

    public static void processUpdate(TdApi.UpdateNewMessage update) {
        try {
            if (update.message.isOutgoing) {
                if(update.message.content instanceof TdApi.MessageText) {
                    String text = ((TdApi.MessageText) update.message.content).text.text;
                    long chatId = update.message.chatId;
                    if (text.toLowerCase(Locale.ROOT).equals("!ignore")) {
                        ignore(chatId, update);
                    } else //noinspection SpellCheckingInspection
                        if (text.toLowerCase(Locale.ROOT).equals("!unignore")) {
                        unignore(chatId, update);
                    } else if (text.toLowerCase().contains("!reverse")) {
                        reverse(text, update);
                    }
                    if(text.contains("!mix") || text.contains("!broke")) mix(text, update);
                    if(text.contains("!continue")) continueText(text, update);
                    if(text.contains("!upgrade")) upgradeText(text, update);
                    if(text.contains("!animate")) animateMessage(text, update);
                    if(text.equals("!help")) sendHelp(update);
                } else if(update.message.content instanceof TdApi.MessageDocument) {
                    String text = ((TdApi.MessageDocument) update.message.content).caption.text;
                    if(text.contains("!import")) {
                        String[] split = text.split(" ");
                        if(split[0].equals("!import")) {
                            importFile(update, split.length>1?split[1]:null);
                        }
                    }
                }
            } else {
                long sender;
                if(update.message.canBeDeletedOnlyForSelf || update.message.canBeDeletedForAllUsers) {
                    if (update.message.senderId instanceof TdApi.MessageSenderUser)
                        sender = ((TdApi.MessageSenderUser) update.message.senderId).userId;
                    else {
                        sender = ((TdApi.MessageSenderChat) update.message.senderId).chatId;
                    }
                    if (blacklist.contains(sender)) {
                        client.send(new TdApi.DeleteMessages(update.message.chatId, new long[]{update.message.id}, update.message.canBeDeletedForAllUsers), UpdateHandler::requestFailHandler);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Ignoring exception:");
            e.printStackTrace();
        }
    }
}
