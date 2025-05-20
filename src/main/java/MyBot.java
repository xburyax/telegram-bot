// імпорти залишаються без змін
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "@homework3pi_bot";
    private static final String BOT_TOKEN = "7260819199:AAEu2OMjONtSHmSm65Lnwgx4H15eZ3cklEw";

    private static final String ARCHIVE_FOLDER = "archive_files";
    private final Map<String, java.io.File> archiveFiles = new LinkedHashMap<>();

    public MyBot() {
        ensureArchiveFolderExists();
        loadArchiveFiles();
    }

    private void ensureArchiveFolderExists() {
        java.io.File folder = new java.io.File(ARCHIVE_FOLDER);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Папка архіву створена: " + folder.getAbsolutePath());
            } else {
                System.err.println("Не вдалося створити папку архіву: " + folder.getAbsolutePath());
            }
        }
    }

    private void loadArchiveFiles() {
        archiveFiles.clear();
        java.io.File folder = new java.io.File(ARCHIVE_FOLDER);
        if (!folder.exists()) {
            ensureArchiveFolderExists();
        }
        java.io.File[] files = folder.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isFile()) {
                    archiveFiles.put(f.getName(), f);
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (message.hasText()) {
                String text = message.getText().toLowerCase();
                long chatId = message.getChatId();

                if (text.equals("/start")) {
                    sendWelcomeButtons(chatId);
                } else if (text.equalsIgnoreCase("що я зберіг минулого тижня?") || text.equalsIgnoreCase("покажи останні документи")) {
                    loadArchiveFiles();
                    sendRecentDocuments(chatId);
                }
            }

            if (message.hasDocument()) {
                User fromUser = message.getFrom();
                Document document = message.getDocument();
                ensureArchiveFolderExists();
                saveFileFromDocument(document, message.getChatId());
            }
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals("show_archive")) {
                loadArchiveFiles();
                sendArchiveList(chatId);
            } else if (callbackData.equals("show_recent_documents")) {
                loadArchiveFiles();
                sendRecentDocuments(chatId);
            } else if (callbackData.startsWith("get_file:")) {
                String fileName = callbackData.substring("get_file:".length());
                sendArchiveFile(chatId, fileName);
            }
        }
    }

    private void sendWelcomeButtons(long chatId) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "🌅 Доброго ранку!";
        } else if (hour >= 12 && hour < 18) {
            greeting = "🌞 Доброго дня!";
        } else if (hour >= 18 && hour < 22) {
            greeting = "🌇 Доброго вечора!";
        } else {
            greeting = "🌙 Доброї ночі!";
        }

        int count = archiveFiles.size();
        String archiveInfo = "У тебе зараз в архіві " + count + " файл" +
                (count == 1 ? "" : (count >= 2 && count <= 4 ? "и" : "ів")) + " 📂";

        InlineKeyboardButton archiveBtn = new InlineKeyboardButton();
        archiveBtn.setText("📁 Показати архів");
        archiveBtn.setCallbackData("show_archive");

        InlineKeyboardButton recentBtn = new InlineKeyboardButton();
        recentBtn.setText("🕒 Показати останні документи");
        recentBtn.setCallbackData("show_recent_documents");

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(archiveBtn));
        buttons.add(List.of(recentBtn));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(greeting + "\n\n" + archiveInfo + "\n\nОберіть дію 👇");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void saveFileFromDocument(Document document, long chatId) {
        try {
            org.telegram.telegrambots.meta.api.objects.File fileInfo = execute(new GetFile(document.getFileId()));
            String fileUrl = fileInfo.getFileUrl(getBotToken());
            java.io.File destFile = new java.io.File(ARCHIVE_FOLDER + java.io.File.separator + document.getFileName());

            try (InputStream in = new URL(fileUrl).openStream()) {
                Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            archiveFiles.put(destFile.getName(), destFile);
            sendText(chatId, "Файл успішно додано в архів: " + document.getFileName());

        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
            sendText(chatId, "Помилка при завантаженні файлу.");
        }
    }

    private void sendArchiveList(long chatId) {
        if (archiveFiles.isEmpty()) {
            sendText(chatId, "Архів порожній.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Ось список файлів в архіві:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String fileName : archiveFiles.keySet()) {
            InlineKeyboardButton fileButton = new InlineKeyboardButton();
            fileButton.setText(fileName);
            fileButton.setCallbackData("get_file:" + fileName);
            rows.add(List.of(fileButton));
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendRecentDocuments(long chatId) {
        if (archiveFiles.isEmpty()) {
            sendText(chatId, "Архів порожній.");
            return;
        }

        List<java.io.File> files = new ArrayList<>(archiveFiles.values());
        files.sort(Comparator.comparingLong(java.io.File::lastModified).reversed());

        int limit = Math.min(5, files.size());
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        StringBuilder sb = new StringBuilder("Останні документи:\n");
        for (int i = 0; i < limit; i++) {
            sb.append(i + 1).append(". ").append(files.get(i).getName()).append("\n");
        }

        message.setText(sb.toString());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendArchiveFile(long chatId, String fileName) {
        java.io.File file = archiveFiles.get(fileName);
        if (file == null) {
            sendText(chatId, "Файл не знайдено в архіві.");
            return;
        }

        SendDocument document = new SendDocument();
        document.setChatId(String.valueOf(chatId));
        document.setDocument(new InputFile(file));
        document.setCaption("Ось файл: " + fileName);

        try {
            execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
