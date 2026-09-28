package signaliduna;

/**
 * Fehler in einem JSON-Text. Geprüfte Ausnahme, weil ein Fehler kein
 * Programmierfehler ist, sondern eine Eigenschaft der Eingabe: der Aufrufer
 * soll die Eingabe unverändert lassen und die Meldung anzeigen, nicht eine
 * Ausnahme nach oben werfen, die irgendwo im Aufrufer abbrechen lässt.
 *
 * <p>Zeile und Spalte stehen immer drin, auch wenn sie nichts aussagen (0).
 * Der Aufrufer formatiert sie ohne eigene Sonderfälle, und ein Fehler ohne
 * Ort wäre in einem Editor die halbe Miete wert.
 */
final class JsonFehler extends Exception {

    private static final long serialVersionUID = 1L;

    private final int zeile;
    private final int spalte;

    JsonFehler(String text, int zeile, int spalte) {
        super(text);
        this.zeile = zeile;
        this.spalte = spalte;
    }

    int zeile() {
        return zeile;
    }

    int spalte() {
        return spalte;
    }

    /** Meldung mit Ort, wie sie im Fenster erscheint. */
    String meldung() {
        return "Zeile " + zeile + ", Spalte " + spalte + ": " + getMessage();
    }
}
