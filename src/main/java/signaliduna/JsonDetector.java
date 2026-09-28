package signaliduna;

import java.util.regex.Pattern;

/**
 * Entscheidet, ob ein Text als JSON behandelt wird, und sperrt die Aktionen
 * andernfalls.
 *
 * <p>Das ist eine Vermutung, keine Pruefung, und zwar aus zwei Gruenden. Erstens
 * waere die volle Pruefung beim Tippen teuer - sie muesste nach jedem
 * Tastendruck laufen, und bei einer grossen Datei kostet das sichtbare Zeit.
 * Zweitens wuerde sie genau den Text sperren, an dem gerade getippt wird, denn
 * eine halbfertige Datei ist noch kein JSON.
 *
 * <p>Deshalb wird nur das erste sinntragende Zeichen angesehen. Kommt dort
 * etwas vor, das JSON beginnen kann, ist es JSON genug fuer die Schaltflaechen.
 * Der Formatter bleibt der Richter: was er nicht lesen kann, meldet er mit
 * Zeile und Spalte und laesst die Eingabe stehen.
 *
 * <p>Die Ausnahme sind die drei Schluesselwoerter. Sie muessen als Wort
 * erkannt werden, sonst wuerde jedes Wort, das mit {@code n} beginnt, als
 * {@code null} durchgehen - und davon gibt es in Fliesstext viele.
 */
final class JsonDetector {

    /** Führender Leerraum sowie Kommentare vor dem ersten Wert. */
    private static final Pattern LEADING_NOISE = Pattern.compile(
            "\\A(?:[ \\t\\r\\n]+|//[^\\n]*|/\\*.*?\\*/)*",
            Pattern.DOTALL);

    /** Nur die drei Wörter, die als Wert für sich stehen dürfen. */
    private static final Pattern WORT_WERT = Pattern.compile("\\A(?:true|false|null)\\b");

    private JsonDetector() {
    }

    /**
     * @return {@code true}, wenn der Text mit etwas beginnen kann, das JSON ist
     */
    static boolean looksLikeJson(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return looksLikeJson(text.substring(1));
        }

        String stripped = LEADING_NOISE.matcher(text).replaceFirst("");
        if (stripped.isEmpty()) {
            // Nur Kommentare, kein Wert.
            return false;
        }
        if (WORT_WERT.matcher(stripped).find()) {
            return true;
        }
        char erstes = stripped.charAt(0);
        return erstes == '{' || erstes == '[' || erstes == '"' || erstes == '-'
                || (erstes >= '0' && erstes <= '9');
    }
}
