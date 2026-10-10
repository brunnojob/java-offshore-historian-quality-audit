import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Main {
    static double numeric(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("finite number required");
        return v;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static List<String[]> input(int columns) throws IOException {
        var rows = new ArrayList<String[]>();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            require(rows.size() < 100000, "input limit exceeded");
            var row = line.split(",", -1);
            require(row.length == columns && Arrays.stream(row).noneMatch(String::isBlank), "invalid CSV row");
            rows.add(row);
        }
        require(!rows.isEmpty(), "missing input");
        return rows;
    }
    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--self-test")) { test(); System.out.println("tests passed"); }
            else run();
        } catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }

    record Sample(double time, double value, boolean good) {}
    record Finding(double time, String issue, double value) {}
    static double median(List<Double> values) {
        var sorted = values.stream().sorted().toList(); int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }
    static List<Finding> audit(List<Sample> samples, double maxGap, int window) {
        var result = new ArrayList<Finding>(); var history = new ArrayDeque<Double>(); double previous = -1;
        for (var s : samples) {
            require(s.time >= 0 && s.time > previous, "timestamps must strictly increase");
            if (previous >= 0 && s.time - previous > maxGap) result.add(new Finding(s.time, "GAP", s.time - previous)); previous = s.time;
            if (!s.good) { result.add(new Finding(s.time, "BAD_QUALITY", s.value)); continue; }
            if (history.size() >= 5) {
                double center = median(new ArrayList<>(history));
                var deviations = history.stream().map(v -> Math.abs(v - center)).toList();
                double scale = Math.max(1e-6, 1.4826 * median(deviations));
                if (Math.abs(s.value - center) > 6 * scale) { result.add(new Finding(s.time, "OUTLIER", s.value)); continue; }
            }
            history.addLast(s.value); if (history.size() > window) history.removeFirst();
        }
        return result;
    }
    static void test() {
        var list = new ArrayList<Sample>(); for (int i = 0; i < 6; i++) list.add(new Sample(i, 10, true));
        list.add(new Sample(10, 100, true)); list.add(new Sample(11, 0, false));
        var findings = audit(list, 2, 20);
        require(findings.stream().map(Finding::issue).toList().equals(List.of("GAP", "OUTLIER", "BAD_QUALITY")), "quality audit mismatch");
    }
    static void run() throws IOException {
        var streams = new TreeMap<String, List<Sample>>();
        for (var r : input(4)) { require(r[3].equals("GOOD") || r[3].equals("BAD"), "unknown quality"); streams.computeIfAbsent(r[1], k -> new ArrayList<>()).add(new Sample(numeric(r[0]), numeric(r[2]), r[3].equals("GOOD"))); }
        for (var e : streams.entrySet()) for (var f : audit(e.getValue(), 2, 20)) System.out.printf(Locale.ROOT, "%s,%.3f,%s,%.6f%n", e.getKey(), f.time, f.issue, f.value);
    }

}
