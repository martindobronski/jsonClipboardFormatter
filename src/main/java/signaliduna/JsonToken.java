package signaliduna;

/**
 * Ein Stück JSON, wie es im Eingangstext stand.
 *
 * <p>{@link #nutzlast} ist immer der Originaltext des Tokens, Zeichen für
 * Zeichen: bei Strings inklusive der Anführungszeichen, bei Zahlen so wie
 * geschrieben ({@code 1.50} bleibt {@code 1.50}, nicht {@code 1.5}), bei den
 * Schlüsselwörtern {@code true}, {@code false} und {@code null}. Damit ist das
 * Umformatieren im Wortsinn verlustfrei - es ändert nur die Leerzeichen
 * zwischen den Tokens. Genau das ist der Grund für diese Klasse: eine
 * Formatierung, die Zahlen oder Escapes neu schreibt, macht aus einer
 * Anzeige eine Konvertierung.
 */
record JsonToken(Art art, String nutzlast, int zeile, int spalte) {

    /** Tokenarten. Es gibt keine "Kommentar"-Art: Kommentare fliegen raus. */
    enum Art {
        OBJEKT_AUF('{'),
        OBJEKT_ZU('}'),
        ARRAY_AUF('['),
        ARRAY_ZU(']'),
        KOMMA(','),
        DOPPELPUNKT(':'),
        STRING('"'),
        ZAHL('0'),
        WAHR('t'),
        FALSCH('f'),
        NULL('n');

        private final char kennzeichen;

        Art(char kennzeichen) {
            this.kennzeichen = kennzeichen;
        }

        /** Für Fehlermeldungen, die das Zeichen nennen, das nicht passt. */
        String alsZeichen() {
            return "'" + kennzeichen + "'";
        }
    }

    /**
     * Kurze Form für Meldungen wie "Hier wird ein Komma oder '}' erwartet."
     * Lange Nutzlasten werden abgeschnitten: eine Fehlermeldung, die einen
     * ganzen minifizierten String wiederholt, ist schlimmer als keine.
     */
    String beschreibung() {
        return switch (art) {
            case STRING -> nutzlast.length() <= 20 ? nutzlast : nutzlast.substring(0, 19) + "…";
            case ZAHL, WAHR, FALSCH, NULL -> nutzlast;
            default -> art.alsZeichen();
        };
    }
}
