package com.pirs.coinrate;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_5250;

public class CoinRateClient implements ClientModInitializer {

    private static final int GRAY = 0x808080;

    private static final Pattern PRICE_NUM = Pattern.compile("\\d+(?:[.,]\\d+)*");
    private static final String[] RUBLE_MARKERS = { "₽", "руб", "руб.", "rub", "рубли", "рублей" };

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ExchangeTracker.tick();
            if (!ExchangeTracker.shouldSend()) {
                return;
            }
            if (client.method_1562() == null) {
                return;
            }
            ExchangeTracker.markSent();
            client.method_1562().method_45730("/exchange");
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !ExchangeTracker.onReceive(message.getString()));

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, body, profile, params, timestamp) ->
                !ExchangeTracker.onReceive(message.getString()));

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> addCoinTooltip(lines));
    }

    private static void addCoinTooltip(List<class_2561> lines) {
        if (ExchangeTracker.rate <= 0 || lines == null || lines.isEmpty()) {
            return;
        }
        Price price = findPrice(lines);
        if (price == null) {
            return;
        }
        double coins = price.inCoins ? price.value : price.value / ExchangeTracker.rate;
        String iconSuffix = ExchangeTracker.icon.isEmpty() ? "" : " " + ExchangeTracker.icon;
        lines.add(gray("Курс койна: " + fmt(ExchangeTracker.rate) + iconSuffix));
        lines.add(gray("Цена в койнах: " + fmt(coins) + iconSuffix));
    }

    private static Price findPrice(List<class_2561> lines) {
        String icon = ExchangeTracker.icon;
        for (class_2561 line : lines) {
            String s = line.getString();
            if (s == null || s.isEmpty()) {
                continue;
            }
            double n = ExchangeTracker.parseFirstNumber(s);
            if (n < 0) {
                continue;
            }
            if (!icon.isEmpty() && s.contains(icon)) {
                return new Price(n, true);
            }
            String lower = s.toLowerCase();
            for (String marker : RUBLE_MARKERS) {
                if (lower.contains(marker)) {
                    return new Price(n, false);
                }
            }
        }
        return null;
    }

    private static class Price {
        final double value;
        final boolean inCoins;

        Price(double value, boolean inCoins) {
            this.value = value;
            this.inCoins = inCoins;
        }
    }

    private static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "?";
        }
        if (v == Math.floor(v) && Math.abs(v) < 1e12) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static class_2561 gray(String text) {
        class_2583 style = class_2583.field_24360.method_36139(GRAY);
        return ((class_5250) class_2561.method_30163(text)).method_10862(style);
    }
}
