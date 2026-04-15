package Common;

import java.util.regex.Pattern;

public final class ValidationPatterns {
    public static final Pattern PERSON_NAME = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");
    public static final Pattern IC_PASSPORT = Pattern.compile("[A-Za-z0-9-]{6,20}");
    public static final Pattern LEGACY_USER_ID = Pattern.compile("(?i)(?:E|H)-\\d{6}");
    public static final Pattern LEGACY_EMPLOYEE_ID = Pattern.compile("(?i)E-\\d{6}");
    public static final Pattern LEGACY_HR_ID = Pattern.compile("(?i)H-\\d{6}");
    public static final Pattern UUID = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    public static final Pattern UUID_COMPACT = Pattern.compile("(?i)[0-9a-f]{6,32}");
    public static final Pattern USER_ID = Pattern.compile("(?i)(?:E|H)-\\d{6}|[0-9a-f]{6,32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    public static final Pattern EMPLOYEE_ID = Pattern.compile("(?i)E-\\d{6}|[0-9a-f]{6,32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    public static final Pattern HR_ID = Pattern.compile("(?i)H-\\d{6}|[0-9a-f]{6,32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    public static final Pattern PHONE = Pattern.compile("\\+?\\d{8,15}");
    public static final Pattern RELATIONSHIP = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");

    private ValidationPatterns() {}
}
