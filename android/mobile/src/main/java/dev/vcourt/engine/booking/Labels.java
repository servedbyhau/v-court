package dev.vcourt.engine.booking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The accessibility labels the Vinhomes app shows, by key: strings of the target app, so
 * they stay in its language. The defaults are Vietnamese, the desktop's {@code Labels}; the
 * keys are its config's {@code [labels]} keys. Immutable: {@link #with} returns a copy.
 */
public final class Labels {
    /** The keys, in the desktop's order. */
    public static final List<String> KEYS = List.of("home", "utility", "continue", "confirm",
            "confirm_page", "prev_month", "next_month", "pick_date", "agree", "fully_booked",
            "venue_list", "court_list", "residents", "notice", "quota_full", "booked_time",
            "booked_place", "ticket", "processing", "registered");

    private static final Labels VIETNAMESE = new Labels(defaults());

    private final Map<String, String> values;

    private Labels(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    private static Map<String, String> defaults() {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("home", "Tiện ích");
        // The tile of the utility to book; empty for the tile of the sport.
        v.put("utility", "");
        v.put("continue", "Tiếp tục");
        v.put("confirm", "Xác nhận");
        // The confirmation page's title: unused since the ticket tells success (desktop 0.6.2).
        v.put("confirm_page", "Xác nhận đăng ký");
        v.put("prev_month", "Tháng trước");
        v.put("next_month", "Tháng sau");
        // Under the calendar until a day is picked; it goes once the app takes the day's tap.
        v.put("pick_date", "Chọn ngày dự kiến sử dụng");
        v.put("agree", "Tôi đã hiểu");
        v.put("fully_booked", "Hết chỗ");
        // The heading of the venues the app may list before the courts.
        v.put("venue_list", "Chọn phân khu");
        v.put("court_list", "Chọn vị trí/dịch vụ");
        // A caption of the booking details page that follows the court list.
        v.put("residents", "Số lượng cư dân");
        // The title of the app's message dialogs: the quota, a refused booking.
        v.put("notice", "Thông báo");
        // Part of the quota notice: "Căn hộ đã hết hạn mức đăng ký tiện ích cho phép."
        v.put("quota_full", "hết hạn mức");
        v.put("booked_time", "Thời gian đặt lịch");
        v.put("booked_place", "Vị trí/dịch vụ");
        // The title of the ticket that follows "Xác nhận".
        v.put("ticket", "Chi tiết đặt lịch");
        // Ticket statuses: while the server handles the booking (seen at 06:00), then done.
        v.put("processing", "Đang xử lý");
        v.put("registered", "Đã đăng ký");
        return v;
    }

    /** Return the Vietnamese labels, the app's default language. */
    public static Labels vietnamese() {
        return VIETNAMESE;
    }

    /** Return {@code base} with {@code changes} applied; refuse an unknown key. */
    public static Labels of(Labels base, Map<String, String> changes) {
        Map<String, String> copy = new LinkedHashMap<>(base.values);
        for (Map.Entry<String, String> change : changes.entrySet()) {
            if (!copy.containsKey(change.getKey())) {
                throw new IllegalArgumentException("Unknown label: '" + change.getKey() + "'");
            }
            copy.put(change.getKey(), change.getValue() == null ? "" : change.getValue());
        }
        return new Labels(copy);
    }

    /** Return a copy with label {@code key} set to {@code value}. */
    public Labels with(String key, String value) {
        return of(this, Map.of(key, value));
    }

    /** Return the label of {@code key}. */
    public String get(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown label: '" + key + "'");
        }
        return value;
    }

    /** Return every label by key, in {@link #KEYS} order. */
    public Map<String, String> asMap() {
        return values;
    }

    public String home() {
        return values.get("home");
    }

    public String utility() {
        return values.get("utility");
    }

    public String continueLabel() {
        return values.get("continue");
    }

    public String confirm() {
        return values.get("confirm");
    }

    public String prevMonth() {
        return values.get("prev_month");
    }

    public String nextMonth() {
        return values.get("next_month");
    }

    public String pickDate() {
        return values.get("pick_date");
    }

    public String agree() {
        return values.get("agree");
    }

    public String fullyBooked() {
        return values.get("fully_booked");
    }

    public String venueList() {
        return values.get("venue_list");
    }

    public String courtList() {
        return values.get("court_list");
    }

    public String residents() {
        return values.get("residents");
    }

    public String notice() {
        return values.get("notice");
    }

    public String quotaFull() {
        return values.get("quota_full");
    }

    public String bookedTime() {
        return values.get("booked_time");
    }

    public String bookedPlace() {
        return values.get("booked_place");
    }

    public String ticket() {
        return values.get("ticket");
    }

    public String processing() {
        return values.get("processing");
    }

    public String registered() {
        return values.get("registered");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Labels && values.equals(((Labels) other).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
