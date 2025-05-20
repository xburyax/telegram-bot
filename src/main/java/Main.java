import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

              // Спроба видалити webhook перед запуском LongPolling бота
            try {
                MyBot tempBot = new MyBot();
                tempBot.execute(new DeleteWebhook());
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("404")) {
                    throw e;
                }
            }

            botsApi.registerBot(new MyBot());
            System.out.println("Бот успішно запущений.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
