package com.pirs.coinrate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExchangeTracker {
    public static final int AUTO_INTERVAL_TICKS = 3600;

    private static final int SUPPRESS_WINDOW_TICKS = 200;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+(?:[.,][0-9]+)*");

    public static double rate = -1.0;
    public static String icon = "";

    private static int tick = 0;
    private static int lastSendTick = -AUTO_INTERVAL_TICKS;
    private static int suppressTicksLeft = 0;

    private ExchangeTracker() {
    }

    public static int tickCount() {
        return tick;
    }

    public static void tick() {
        tick++;
        if (suppressTicksLeft > 0) {
            suppressTicksLeft--;
        }
    }

    public static boolean shouldSend() {
        return tick - lastSendTick >= AUTO_INTERVAL_TICKS;
    }

    public static void markSent() {
        lastSendTick = tick;
        suppressTicksLeft = SUPPRESS_WINDOW_TICKS;
    }

    public static boolean onReceive(String plain) {
        if (plain == null || plain.isEmpty()) {
            return false;
        }
        boolean relevant = isExchangeRelated(plain);
        parse(plain);
        boolean suppress = relevant && suppressTicksLeft > 0;
        if (relevant) {
            Debug.log("RECV " + (suppress ? "[suppressed] " : "") + plain.replace('\n', '|'));
        }
        return suppress;
    }

    private static void parse(String s) {
        String lower = s.toLowerCase();
        boolean hasRateWord = lower.contains("курс") || lower.contains("обмен") || lower.contains("exchange")
                || (lower.contains("коин") && lower.contains("="))
                || (lower.contains("койн") && lower.contains("="));
        if (hasRateWord) {
            double r = parseRate(s);
            if (r > 0) {
                rate = r;
                Debug.log("rate=" + r);
            }
        }
        if (lower.contains("коин") || lower.contains("койн") || lower.contains("баланс") || lower.contains("обмен")) {
            String ic = extractIcon(s);
            if (ic != null && !ic.isEmpty() && !ic.equals(icon)) {
                icon = ic;
                Debug.log("icon=" + icon);
            }
        }
    }

    private static boolean isExchangeRelated(String s) {
        String lower = s.toLowerCase();
        return lower.contains("курс") || lower.contains("коин") || lower.contains("койн")
                || lower.contains("баланс") || lower.contains("обмен") || lower.contains("exchange");
    }

    private static double parseRate(String s) {
        int eq = s.indexOf('=');
        if (eq >= 0) {
            Matcher m = NUMBER_PATTERN.matcher(s.substring(eq + 1));
            if (m.find()) {
                double v = normalize(m.group());
                if (v > 0) {
                    return v;
                }
            }
        }
        Matcher m = NUMBER_PATTERN.matcher(s);
        if (m.find()) {
            return normalize(m.group());
        }
        return -1;
    }

    private static String extractIcon(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7E) continue;
            if (c == 0x00A7) continue;
            if (c >= 0x0400 && c <= 0x04FF) continue;
            if (Character.isWhitespace(c)) continue;
            if (Character.isISOControl(c)) continue;
            sb.append(c);
        }
        return sb.toString();
    }

    public static double parseFirstNumber(String s) {
        if (s == null) {
            return -1;
        }
        Matcher m = NUMBER_PATTERN.matcher(s);
        if (!m.find()) {
            return -1;
        }
        return normalize(m.group());
    }

    private static double parseNumber(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double normalize(String token) {
        if (token.matches("\\d{1,3}([.,]\\d{3})+")) {
            token = token.replaceAll("[.,]", "");
        } else if (token.contains(",")) {
            token = token.replace(',', '.');
        }
        return parseNumber(token);
    }
}
