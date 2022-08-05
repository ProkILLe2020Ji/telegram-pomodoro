package com.pillj.echo;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomodoroBot pomodorkas = new PomodoroBot();
        telegramBotsApi.registerBot(pomodorkas);
            new Thread(() -> {
                try {
                    pomodorkas.CheckTimer();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).run();
    }
}
