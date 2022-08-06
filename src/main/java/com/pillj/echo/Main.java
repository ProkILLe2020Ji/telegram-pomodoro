package com.pillj.echo;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final String TOKEN = "";
    private static final ConcurrentHashMap<PomodoroBot.Timer, Long> userTimers = new ConcurrentHashMap();
    public static void main(String[] args) throws TelegramApiException {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomodoroBot bot = new PomodoroBot();
        telegramBotsApi.registerBot(bot);

        new Thread(()-> {
            try {
                bot.checkTimer();
            } catch (InterruptedException e) {
                System.out.println("Oops");;
            }
        }).run();
    }
    static final class PomodoroBot extends TelegramLongPollingBot{
        enum TimerType{
            WORK,
            BREAK
        }

        record Timer(Instant time, TimerType timerType, int index, Long iterationSize){};

        @Override
        public String getBotUsername() {
            return "Pomodoro Bot";
        }

        @Override
        public String getBotToken() {
            return TOKEN;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if(update.hasMessage() && update.getMessage().hasText()){
                Long chatId = update.getMessage().getChatId();
                if(update.getMessage().getText().equals("/start")){
                    SendMsg("""
                            Pomodoro - make your time more efficient.
                            " Give me the time of work and rest separated by a space. For example, '1 1'.
                            " PS I'm still working in minutes.
                            """, chatId.toString());
                }else{
                    var args = update.getMessage().getText().split(" ");
                    int index = Integer.parseInt(args[2])-1;
                    Long iterationSize = Long.parseLong(args[0]) + Long.parseLong(args[1]);
                    if (args.length >= 1){
                        var workTime = Instant.now().plus(Long.parseLong(args[0]), ChronoUnit.MINUTES);
                        userTimers.put(new Timer(workTime, TimerType.WORK, index, iterationSize), chatId);
                        SendMsg("The timer is running", chatId.toString());
                        if (args.length >= 2){
                            var breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES);
                            userTimers.put(new Timer(breakTime, TimerType.BREAK, index, iterationSize), chatId);
                        }
                    }
                }
            }
        }
        public void checkTimer() throws InterruptedException {
            while(true){
                userTimers.forEach((timer,userId) ->{
                    System.out.printf("Working userId = %d server_time = %s user_timer = %s\n",
                            userId, Instant.now().toString(), timer.time.toString());
                    if (Instant.now().isAfter(timer.time)){
                        userTimers.remove(timer);
                        switch (timer.timerType){
                            case WORK -> {
                                SendMsg("Time to rest", userId.toString());
                                if(timer.index != 0){
                                    int index = timer.index-1;
                                    userTimers.put(new Timer(Instant.now().plus(timer.iterationSize, ChronoUnit.MINUTES),
                                            TimerType.WORK, index, timer.iterationSize), userId);
                                }
                            }
                            case BREAK -> {
                                if(timer.index==0){
                                    SendMsg("The timer has finished working", userId.toString());
                                }else{
                                    int index = timer.index-1;
                                    SendMsg("Time to work", userId.toString());
                                    userTimers.put(new Timer(Instant.now().plus(timer.iterationSize, ChronoUnit.MINUTES),
                                            TimerType.BREAK, index, timer.iterationSize), userId);
                                }
                            }
                        }
                    }
                });
                Thread.sleep(1000);
            }
        }
        private void SendMsg(String text, String chatId) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setProtectContent(true);
            msg.setText(text);

            try {
                execute(msg);
            } catch (TelegramApiException e) {
                System.out.println("Oops");
            }
        }
    }
}