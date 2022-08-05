package com.pillj.echo;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;

public class PomodoroBot extends TelegramLongPollingBot {
    private final ConcurrentHashMap<UserTimer, Long> userTimerRepository = new ConcurrentHashMap<>();
    enum TimerTipe {
        WORK,
        BREAK
    }
    record UserTimer(Instant userTimer, TimerTipe timerTipe){

    }

    @Override
    public String getBotUsername() {
        return "Mr. Pomodorkas";
    }

    @Override
    public String getBotToken() {
        return "5599053647:AAEFs5v6oOUb8lLre4PMYrRH6MuYo4OkrXM";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()){
            return;
        }
        var args = update.getMessage().getText().split(" ");
        Instant workTime = Instant.now().plus(Long.parseLong(args [0]), ChronoUnit.MINUTES);
        Instant breakTime = workTime.plus(Long.parseLong(args [1]), ChronoUnit.MINUTES);
        userTimerRepository.put(new UserTimer(workTime, TimerTipe.WORK), update.getMessage().getChatId());
        System.out.printf("[%s]Размер коллекции %d \n", Instant.now().toString(), userTimerRepository.size());
        userTimerRepository.put(new UserTimer(breakTime, TimerTipe.BREAK), update.getMessage().getChatId());
        System.out.printf("[%s]Размер коллекции %d \n", Instant.now().toString(), userTimerRepository.size());
        printMSG(update.getMessage().getChatId(), "Timer start");
    }
    public void CheckTimer() throws InterruptedException {
        while(true){
            System.out.println("Количество таймеров пользователя: " + userTimerRepository.size()+"\r");
            userTimerRepository.forEach((timer, userId) -> {
                if (Instant.now().isAfter(timer.userTimer)){
                    userTimerRepository.remove(timer);
                    switch (timer.timerTipe){
                        case WORK -> printMSG(userId, "Time to take a break");
                        case BREAK -> printMSG(userId, "Timer is stop");
                    }
                    userTimerRepository.remove(timer);
                }
            });
            Thread.sleep(1000);
        }
    }
    private void printMSG(long chatID, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatID);
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
