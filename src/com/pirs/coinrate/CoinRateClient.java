package com.pirs.coinrate;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_5250;

public class CoinRateClient implements ClientModInitializer {

    private static final int GRAY = 0x808080;

    private static final String[] RUBLE_MARKERS = { "₽", "руб", "руб.", "rub", "рубли", "рублей" };
    private static final String[] PRICE_KEYWORDS = { "цена", "price", "стоим" };

    private static class_1799 cachedStack;
    private static double cachedRate = -1;
    private static String cachedIcon;
    private static List<class_2561> cachedAdditions;
    private static boolean cachedScreen;

    @Override
    public void onInitializeClient() {
        Debug.log("CoinRate initialized");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ExchangeTracker.tick();
            if (ExchangeTracker.tickCount() % 100 == 0) {
                Debug.log("TICK t=" + ExchangeTracker.tickCount()
                        + " player=" + (client.field_1724 != null)
                        + " net=" + (client.method_1562() != null)
                        + " shouldSend=" + ExchangeTracker.shouldSend()
                        + " rate=" + ExchangeTracker.rate);
            }
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

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String s = message.getString();
            Debug.log("GAME" + (overlay ? "[ovl] " : " ") + s.replace('\n', '|'));
            return !ExchangeTracker.onReceive(s);
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, body, profile, params, timestamp) -> {
            String s = message.getString();
            Debug.log("CHAT " + s.replace('\n', '|'));
            return !ExchangeTracker.onReceive(s);
        });

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> addCoinTooltip(stack, lines));
    }

    private static void addCoinTooltip(class_1799 stack, List<class_2561> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        boolean inScreen = class_310.method_1551().field_1729 != null;
        if (stack == cachedStack && ExchangeTracker.rate == cachedRate
                && java.util.Objects.equals(ExchangeTracker.icon, cachedIcon) && inScreen == cachedScreen) {
            if (cachedAdditions != null) {
                lines.addAll(cachedAdditions);
            }
            return;
        }
        cachedStack = stack;
        cachedRate = ExchangeTracker.rate;
        cachedIcon = ExchangeTracker.icon;
        cachedScreen = inScreen;

        boolean rateKnown = ExchangeTracker.rate > 0;
        Price price = findPrice(lines);
        Debug.log("tooltip stack=" + (stack == null ? "null" : stack.toString())
                + " lines=" + lines.size() + " screen=" + inScreen + " rateKnown=" + rateKnown
                + " price=" + (price == null ? "-" : price.value + (price.inCoins ? " coins" : " rub")));

        List<class_2561> additions = new ArrayList<>();
        boolean showRate = rateKnown && (price != null || (inScreen && lines.size() > 1));
        if (showRate) {
            additions.add(gray("Курс койна: " + fmt(ExchangeTracker.rate) + iconSuffix()));
        }
        if (price != null) {
            if (price.inCoins) {
                additions.add(gray("Цена в койнах: " + fmt(price.value) + iconSuffix()));
            } else if (rateKnown) {
                additions.add(gray("Цена в койнах: " + fmt(price.value / ExchangeTracker.rate) + iconSuffix()));
            }
        }
        cachedAdditions = additions;
        lines.addAll(additions);
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
            return String.format(java.util.Locale.ROOT, "%.0f", v);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static class_2561 gray(String text) {
        class_2583 style = class_2583.field_24360.method_36139(GRAY);
        return ((class_5250) class_2561.method_30163(text)).method_10862(style);
    }
}
