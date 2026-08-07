package com.pirs.coinrate;

import java.util.List;
import java.util.Locale;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_5250;

public class CoinRateClient implements ClientModInitializer {

    private static final int GRAY = 0x808080;

    private static final String[] RUBLE_MARKERS = { "₽", "руб", "руб.", "rub", "рубли", "рублей" };
    private static final String[] PRICE_KEYWORDS = { "цена", "price", "стоим" };

    @Override
    public void onInitializeClient() {
        Debug.log("CoinRate initialized");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ExchangeTracker.tick();
            if (!ExchangeTracker.shouldSend()) {
                return;
            }
            if (client.method_1562() == null) {
                return;
            }
            ExchangeTracker.markSent();
            Debug.log("sending /exchange");
            client.method_1562().method_45730("/exchange");
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !ExchangeTracker.onReceive(message.getString()));

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, body, profile, params, timestamp) ->
                !ExchangeTracker.onReceive(message.getString()));

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> addCoinTooltip(lines));
    }

    private static void addCoinTooltip(List<class_2561> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        boolean rateKnown = ExchangeTracker.rate > 0;
        boolean inScreen = class_310.method_1551().field_1729 != null;
        Price price = findPrice(lines);
        Debug.log("tooltip lines=" + lines.size() + " screen=" + inScreen + " rateKnown=" + rateKnown
                + " price=" + (price == null ? "-" : price.value + (price.inCoins ? " coins" : " rub")));

        boolean showRate = rateKnown && (price != null || (inScreen && lines.size() > 1));
        if (showRate) {
            lines.add(gray("Курс койна: " + fmt(ExchangeTracker.rate) + iconSuffix()));
        }
        if (price != null) {
            if (price.inCoins) {
                lines.add(gray("Цена в койнах: " + fmt(price.value) + iconSuffix()));
            } else if (rateKnown) {
                lines.add(gray("Цена в койнах: " + fmt(price.value / ExchangeTracker.rate) + iconSuffix()));
            }
        }
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
            for (String keyword : PRICE_KEYWORDS) {
                if (lower.contains(keyword)) {
                    return new Price(n, false);
                }
            }
        }
        return null;
    }

    private static String iconSuffix() {
        return ExchangeTracker.icon.isEmpty() ? "" : " " + ExchangeTracker.icon;
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
