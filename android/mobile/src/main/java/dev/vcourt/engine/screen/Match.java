package dev.vcourt.engine.screen;

import java.util.regex.Pattern;

/**
 * Criteria an element must meet; unset criteria are ignored. Immutable: each {@code with}
 * method returns a copy. {@code pattern} must match the whole label, line breaks included,
 * like {@code descriptionMatches} in uiautomator.
 */
public final class Match {
    private static final Match ANY = new Match(null, null, null, null, null);

    private final String desc;
    private final String prefix;
    private final String contains;
    private final Pattern pattern;
    private final Boolean clickable;

    private Match(String desc, String prefix, String contains, Pattern pattern,
            Boolean clickable) {
        this.desc = desc;
        this.prefix = prefix;
        this.contains = contains;
        this.pattern = pattern;
        this.clickable = clickable;
    }

    /** Match an element labelled exactly {@code desc}. */
    public static Match desc(String desc) {
        return ANY.withDesc(desc);
    }

    /** Match an element whose label starts with {@code prefix}. */
    public static Match prefix(String prefix) {
        return ANY.withPrefix(prefix);
    }

    /** Match an element whose label holds {@code text}. */
    public static Match contains(String text) {
        return ANY.withContains(text);
    }

    /** Match an element whose whole label matches {@code regex}. */
    public static Match pattern(String regex) {
        return ANY.withPattern(regex);
    }

    public Match withDesc(String value) {
        return new Match(value, prefix, contains, pattern, clickable);
    }

    public Match withPrefix(String value) {
        return new Match(desc, value, contains, pattern, clickable);
    }

    public Match withContains(String value) {
        return new Match(desc, prefix, value, pattern, clickable);
    }

    public Match withPattern(String regex) {
        return new Match(desc, prefix, contains, Pattern.compile(regex, Pattern.DOTALL),
                clickable);
    }

    public Match withClickable(boolean value) {
        return new Match(desc, prefix, contains, pattern, value);
    }

    /** Tell whether {@code node} meets every set criterion. */
    public boolean test(Node node) {
        String text = node.desc;
        return (desc == null || text.equals(desc))
                && (prefix == null || text.startsWith(prefix))
                && (contains == null || text.contains(contains))
                && (pattern == null || pattern.matcher(text).matches())
                && (clickable == null || node.clickable == clickable);
    }
}
