package signaliduna;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Formatiert JSON und packt es wieder zusammen. Bewusst ohne Swing, damit die
 * Logik ohne Fenster testbar ist und dieselbe Datei später auch von einem
 * Aufruf von der Kommandozeile benutzt werden kann.
 *
 * <p>Drei Zusagen, an denen die ganze Klasse hängt:
 *
 * <ol>
 *   <li><b>Worttreu.</b> Zahlen, Strings, Schlüsselreihenfolge und doppelte
 *       Schlüssel bleiben, wie sie waren. Der Tokenisierer liefert den
 *       Originaltext jedes Tokens, und beide Ausgaben setzen genau diese
 *       Tokens zusammen. Es wird nichts umgerechnet und nichts gerundet.</li>
 *   <li><b>Prüfbar.</b> Jede Ausgabe wird vor dem Zurückgeben noch einmal
 *       gelesen und Token für Token mit der Eingabe verglichen
 *       ({@link #versprichtGleichenStrom}). Was diese Prüfung nicht findet,
 *       geht nicht nach außen. Ein Fehler dort ist ein Fehler in diesem
 *       Programm und keine Eigenschaft der Eingabe.</li>
 *   <li><b>Ohne Rekursion.</b> Wie beim Lesen: die Tiefe steckt in einem
 *       Stapel, damit ein sehr tief verschachteltes Dokument eine Meldung
 *       mit Zeile und Spalte bekommt und keinen Absturz.</li>
 * </ol>
 */
final class JsonFormat {

    /** Leerzeichen je Ebene. Vier, weil das die übliche Tiefe in .json-Dateien ist. */
    private static final int EINRUEKUNG = 4;

    private JsonFormat() {
    }

    /**
     * Formatiert schön: ein Element je Zeile, Container über mehrere Zeilen,
     * leere Container einzeilig.
     *
     * @throws JsonFehler mit Ort, wenn die Eingabe kein JSON ist
     */
    static String schoen(String eingabe) throws JsonFehler {
        List<JsonToken> tokens = JsonLeser.pruefe(JsonLexer.lex(eingabe));
        String ausgabe = schoenAusgeben(tokens);
        versprichtGleichenStrom(tokens, ausgabe);
        return ausgabe;
    }

    /**
     * Packt die Eingabe auf eine Zeile, ohne Leerraum. Für {@code curl}-Ausgaben
     * und eingepackte Dateien.
     *
     * @throws JsonFehler mit Ort, wenn die Eingabe kein JSON ist
     */
    static String dicht(String eingabe) throws JsonFehler {
        List<JsonToken> tokens = JsonLeser.pruefe(JsonLexer.lex(eingabe));
        String ausgabe = dichtAusgeben(tokens);
        versprichtGleichenStrom(tokens, ausgabe);
        return ausgabe;
    }

    /**
     * Liest die Ausgabe erneut und vergleicht Token für Token mit der Eingabe.
     *
     * <p>Der Vergleich ist billig und lohnt sich trotzdem: Er findet nicht nur
     * einen Fehler in der Ausgabe, sondern auch einen Fehler im Tokenisierer.
     * Beide wären sonst unsichtbar, bis jemand die Datei geöffnet hat, die die
     * App geschrieben hat, und eine Zahl falsch oder eine Klammer verloren
     * wäre. Aus diesem Grund prüft die App das bei jedem Schreiben, nicht nur
     * im Test.
     */
    private static void versprichtGleichenStrom(List<JsonToken> soll, String ausgabe) {
        List<JsonToken> ist;
        try {
            ist = JsonLexer.lex(ausgabe);
        } catch (JsonFehler fehler) {
            throw new IllegalStateException("Die eigene Ausgabe lässt sich nicht lesen: " + fehler.meldung(), fehler);
        }
        if (soll.size() != ist.size()) {
            throw new IllegalStateException("Die eigene Ausgabe hat " + ist.size()
                    + " statt " + soll.size() + " Stücke.");
        }
        for (int i = 0; i < soll.size(); i++) {
            if (soll.get(i).art() != ist.get(i).art() || !soll.get(i).nutzlast().equals(ist.get(i).nutzlast())) {
                throw new IllegalStateException("Die eigene Ausgabe weicht an Stelle " + (i + 1)
                        + " ab: " + soll.get(i).beschreibung() + " wurde zu " + ist.get(i).beschreibung() + ".");
            }
        }
    }

    // ---- Ausgabe ------------------------------------------------------------

    private static String schoenAusgeben(List<JsonToken> tokens) {
        StringBuilder raus = new StringBuilder();
        // Was gerade offen ist. Der Stapel ist auch die Einrückungsebene:
        // ein Element steht eine Ebene tiefer als sein Container.
        Deque<Rahmen> offen = new ArrayDeque<>();
        JsonToken.Art vorher = null;

        for (int i = 0; i < tokens.size(); i++) {
            JsonToken token = tokens.get(i);
            boolean objekt = switch (token.art()) {
                case OBJEKT_AUF -> true;
                default -> false;
            };
            switch (token.art()) {
                case OBJEKT_AUF, ARRAY_AUF -> {
                    boolean direkt = !offen.isEmpty() && offen.peek().objekt() && vorher == JsonToken.Art.DOPPELPUNKT;
                    if (!direkt) {
                        zeilenumbruch(raus, offen.size());
                    }
                    boolean leer = i + 1 < tokens.size()
                            && tokens.get(i + 1).art() == (objekt ? JsonToken.Art.OBJEKT_ZU : JsonToken.Art.ARRAY_ZU);
                    if (leer) {
                        // Ein leerer Container bleibt einzeilig - als eine
                        // Zeile, nicht als zwei mit einem Leerzeichen drin.
                        raus.append(objekt ? "{}" : "[]");
                        i++;
                    } else {
                        raus.append(objekt ? '{' : '[');
                        if (!offen.isEmpty()) {
                            offen.peek().mitInhalt();
                        }
                        offen.push(new Rahmen(objekt));
                    }
                }
                case OBJEKT_ZU, ARRAY_ZU -> {
                    boolean warInhalt = offen.pop().inhalt();
                    if (warInhalt) {
                        zeilenumbruch(raus, offen.size());
                    }
                    raus.append(token.art() == JsonToken.Art.OBJEKT_ZU ? '}' : ']');
                    if (!offen.isEmpty()) {
                        offen.peek().mitInhalt();
                    }
                }
                case DOPPELPUNKT -> {
                    // Steht schon am Schlüssel, siehe unten.
                }
                case KOMMA -> raus.append(',');
                default -> {
                    if (istSchluessel(offen, vorher)) {
                        zeilenumbruch(raus, offen.size());
                        raus.append(token.nutzlast()).append(": ");
                    } else if (!offen.isEmpty() && offen.peek().objekt()) {
                        // Der Wert eines Objekts steht hinter ": ".
                        raus.append(token.nutzlast());
                    } else {
                        zeilenumbruch(raus, offen.size());
                        raus.append(token.nutzlast());
                    }
                    if (!offen.isEmpty()) {
                        offen.peek().mitInhalt();
                    }
                }
            }
            vorher = token.art();
        }
        return raus.append('\n').toString();
    }

    /** Ein String ist ein Schlüssel, wenn er hinter '{' oder ',' im Objekt steht. */
    private static boolean istSchluessel(Deque<Rahmen> offen, JsonToken.Art vorher) {
        return !offen.isEmpty() && offen.peek().objekt()
                && (vorher == JsonToken.Art.OBJEKT_AUF || vorher == JsonToken.Art.KOMMA);
    }

    private static void zeilenumbruch(StringBuilder raus, int ebene) {
        if (raus.isEmpty() || raus.charAt(raus.length() - 1) == '\n') {
            return;
        }
        raus.append('\n').append(" ".repeat(ebene * EINRUEKUNG));
    }

    /** Ein offener Container während der Ausgabe. */
    private static final class Rahmen {
        private final boolean objekt;
        private boolean inhalt;

        Rahmen(boolean objekt) {
            this.objekt = objekt;
        }

        boolean objekt() {
            return objekt;
        }

        boolean inhalt() {
            return inhalt;
        }

        void mitInhalt() {
            inhalt = true;
        }
    }

    private static String dichtAusgeben(List<JsonToken> tokens) {
        StringBuilder raus = new StringBuilder();
        for (JsonToken token : tokens) {
            switch (token.art()) {
                case OBJEKT_AUF -> raus.append('{');
                case OBJEKT_ZU -> raus.append('}');
                case ARRAY_AUF -> raus.append('[');
                case ARRAY_ZU -> raus.append(']');
                case KOMMA -> raus.append(',');
                case DOPPELPUNKT -> raus.append(':');
                default -> raus.append(token.nutzlast());
            }
        }
        return raus.append('\n').toString();
    }
}
