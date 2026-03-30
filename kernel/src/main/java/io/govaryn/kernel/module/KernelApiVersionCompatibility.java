package io.govaryn.kernel.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class KernelApiVersionCompatibility {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$"
    );

    private KernelApiVersionCompatibility() {
    }

    public static boolean isSupportedVersionRange(String range) {
        if (range == null || range.isBlank()) {
            return false;
        }
        String trimmed = range.trim();
        if ("*".equals(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("^")) {
            return isSemVer(trimmed.substring(1).trim());
        }
        if (trimmed.startsWith(">=")) {
            return isSemVer(trimmed.substring(2).trim());
        }
        return isSemVer(trimmed);
    }

    public static void validateRunningKernelApiVersion(String runningKernelApiVersion) {
        if (runningKernelApiVersion == null || runningKernelApiVersion.isBlank()) {
            throw new IllegalStateException("runningKernelApiVersion must not be blank for module compatibility checks");
        }
        if (!isSemVer(runningKernelApiVersion.trim())) {
            throw new IllegalStateException(
                "runningKernelApiVersion '" + runningKernelApiVersion + "' is not a valid semantic version"
            );
        }
    }

    public static boolean isCompatible(String requiredRange, String runningVersion) {
        if (requiredRange == null || runningVersion == null) {
            return false;
        }
        String trimmedRange = requiredRange.trim();
        if (!isSupportedVersionRange(trimmedRange)) {
            return false;
        }
        SemVer running = SemVer.parse(runningVersion.trim());

        if ("*".equals(trimmedRange)) {
            return true;
        }
        if (trimmedRange.startsWith("^")) {
            SemVer base = SemVer.parse(trimmedRange.substring(1).trim());
            if (running.compareTo(base) < 0) {
                return false;
            }
            return inCaretUpperBound(base, running);
        }
        if (trimmedRange.startsWith(">=")) {
            SemVer minimum = SemVer.parse(trimmedRange.substring(2).trim());
            return running.compareTo(minimum) >= 0;
        }

        SemVer required = SemVer.parse(trimmedRange);
        return running.compareTo(required) == 0;
    }

    private static boolean isSemVer(String value) {
        return SEMVER_PATTERN.matcher(value).matches();
    }

    private static boolean inCaretUpperBound(SemVer base, SemVer running) {
        if (base.major > 0) {
            return base.major == running.major;
        }
        if (base.minor > 0) {
            return running.major == 0 && running.minor == base.minor;
        }
        return running.major == 0 && running.minor == 0 && running.patch == base.patch;
    }

    private static final class SemVer implements Comparable<SemVer> {
        private final int major;
        private final int minor;
        private final int patch;
        private final List<String> preRelease;

        private SemVer(int major, int minor, int patch, List<String> preRelease) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.preRelease = preRelease;
        }

        static SemVer parse(String version) {
            var matcher = SEMVER_PATTERN.matcher(version);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid semantic version: " + version);
            }
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            String preRaw = matcher.group(4);
            List<String> pre = preRaw == null ? List.of() : splitPreRelease(preRaw);
            return new SemVer(major, minor, patch, pre);
        }

        private static List<String> splitPreRelease(String value) {
            String[] parts = value.split("\\.");
            List<String> identifiers = new ArrayList<>(parts.length);
            for (String part : parts) {
                identifiers.add(part);
            }
            return List.copyOf(identifiers);
        }

        @Override
        public int compareTo(SemVer other) {
            if (major != other.major) {
                return Integer.compare(major, other.major);
            }
            if (minor != other.minor) {
                return Integer.compare(minor, other.minor);
            }
            if (patch != other.patch) {
                return Integer.compare(patch, other.patch);
            }
            return comparePreRelease(preRelease, other.preRelease);
        }

        private static int comparePreRelease(List<String> left, List<String> right) {
            if (left.isEmpty() && right.isEmpty()) {
                return 0;
            }
            if (left.isEmpty()) {
                return 1;
            }
            if (right.isEmpty()) {
                return -1;
            }
            int max = Math.max(left.size(), right.size());
            for (int i = 0; i < max; i++) {
                if (i >= left.size()) {
                    return -1;
                }
                if (i >= right.size()) {
                    return 1;
                }
                String a = left.get(i);
                String b = right.get(i);
                boolean aNumeric = isNumericIdentifier(a);
                boolean bNumeric = isNumericIdentifier(b);
                if (aNumeric && bNumeric) {
                    int cmp = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                    if (cmp != 0) {
                        return cmp;
                    }
                    continue;
                }
                if (aNumeric != bNumeric) {
                    return aNumeric ? -1 : 1;
                }
                int cmp = a.compareTo(b);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }

        private static boolean isNumericIdentifier(String value) {
            if (value.isEmpty()) {
                return false;
            }
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SemVer semVer)) return false;
            return major == semVer.major
                && minor == semVer.minor
                && patch == semVer.patch
                && preRelease.equals(semVer.preRelease);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, preRelease);
        }
    }
}
