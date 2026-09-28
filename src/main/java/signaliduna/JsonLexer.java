package signaliduna;

import java.util.ArrayList;
import java.util.List;

/**
 * Zerlegt JSON-Text in {@link JsonToken}. Kennt drei Erweiterungen, die
 * JSON-C ausliefern und JSON nicht: Kommentare, hängende Kommata und ein BOM am
 * Anfang. Sie werden stillschweigend überlesen, damit eine {@code tsconfig.json}
 * oder ein auskommentiertes Beispiel genauso bearbeitet werden kann wie ein
 * sauberes Dokument.
 *
 * <p>Alles andere ist ein Fehler mit Ort. Der Grund ist die Zusage der App: was
 * nicht sicher gelesen werden kann, wird nicht geraten. Ein Formatter, der bei
 * kaputter Eingabe eine plausibel aussehende Ausgabe liefert, ist schlimmer
 * als einer, der die Eingabe stehen lässt - die Ausgabe würde in einer
 * Konfigurationsdatei oder einer API-Anfrage landen, und niemand könnte
 * unterscheiden, was er sich gerade angesehen hat.
 *
 * <p>Zusätzlich zum JSON ist auch alles streng, was JSON nicht verbietet, aber
 * andere Werkzeuge doch verstehen: eine führende Null ({@code 01}), ein
 * Pluszeichen ({@code +1}), ein fehlender Exponent ({@code 1e}). Solche Zahlen
 * gehen durch Java- oder JavaScript-Parser ohne Meldung hindurch und sehen
 * gültig aus, deshalb sind sie hier ein Fehler.
 *
 * <p>Zeichen für Zeichen wird über CodePoints iteriert, nicht über {@code char}:
 * ein Emoji in einem String ist ein Zeichen und belegt eine Spalte, kein Paar.
 */
final class JsonLexer {

    private final String eingabe;
    private int pos;
    private int zeile = 1;
    private int spalte = 1;

    private final List<JsonToken> tokens = new ArrayList<>();

    private JsonLexer(String eingabe) {
        this.eingabe = eingabe;
    }

    static List<JsonToken> lex(String eingabe) throws JsonFehler {
        return new JsonLexer(eingabe).lauf();
    }

    private List<JsonToken> lauf() throws JsonFehler {
        ueberspringeBOM();

        while (pos < eingabe.length()) {
            int startZeile = zeile;
            int startSpalte = spalte;
            int c = eingabe.codePointAt(pos);

            if (istLeerraum(c)) {
                weiter();
            } else if (c == '/' && (Vorschau('/') || Vorschau('*'))) {
                ueberspringeKommentar(startZeile, startSpalte);
            } else if (c == '"') {
                tokens.add(lesenString(startZeile, startSpalte));
            } else if (c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':') {
                weiter();
                tokens.add(new JsonToken(kennzeichen(c), "", startZeile, startSpalte));
            } else if (c == '-' || istZiffer(c)) {
                tokens.add(lesenZahl(startZeile, startSpalte));
            } else if (c == 't' || c == 'f' || c == 'n') {
                tokens.add(lesenSchluesselwort(startZeile, startSpalte));
            } else {
                throw new JsonFehler("Unerwartetes Zeichen " + beschreibe(c) + ".", startZeile, startSpalte);
            }
        }
        return tokens;
    }

    /** Ein BOM ist eine Unsichtbarkeit, kein Inhalt - er gehört zu keiner Zeile. */
    private void ueberspringeBOM() {
        if (!eingabe.isEmpty() && eingabe.charAt(0) == '\uFEFF') {
            pos = 1;
        }
    }

    private void ueberspringeKommentar(int startZeile, int startSpalte) throws JsonFehler {
        weiter(); // '/'
        if (weiter() == '*') {
            while (true) {
                if (pos >= eingabe.length()) {
                    throw new JsonFehler("Blockkommentar wird nicht geschlossen.", startZeile, startSpalte);
                }
                if (weiter() == '*' && Vorschau('/')) {
                    weiter();
                    return;
                }
            }
        }
        // Bis zum Zeilenende, das der Hauptlauf als Leerraum überliest.
        while (!posAbEnde() && Vorschau() != '\n') {
            weiter();
        }
    }

    private JsonToken lesenString(int startZeile, int startSpalte) throws JsonFehler {
        int von = pos;
        weiter(); // '"'
        while (true) {
            if (pos >= eingabe.length()) {
                throw new JsonFehler("String wird nicht geschlossen.", startZeile, startSpalte);
            }
            int c = eingabe.codePointAt(pos);
            if (c == '"') {
                weiter();
                return new JsonToken(JsonToken.Art.STRING, eingabe.substring(von, pos), startZeile, startSpalte);
            }
            if (c == '\\') {
                pruefeFlucht(startZeile, startSpalte);
            } else if (c < 0x20) {
                // Die Stelle des Zeichens selbst, nicht der Stringanfang: bei
                // einem Text mit 2000 Zeilen ist der Zeilenumbruch hier das
                // brauchbare Argument, nicht die Angabe, wo der String began.
                throw new JsonFehler("Unerlaubtes Zeichen " + beschreibe(c)
                        + " im String. Es gehört als \\u-Folge hinein.",
                        c == '\n' ? zeile + 1 : zeile, c == '\n' ? 1 : spalte + 1);
            } else {
                weiter();
            }
        }
    }

    /**
     * Prüft eine Fluchtsequenz und schluckt sie vollständig. Das
     * Mitschlucken ist der eigentliche Zweck: ein {@code \"} darf den String
     * nicht beenden, und wer nur das {@code \} liest, sieht danach ein
     * schließendes Anführungszeichen, wo keines ist.
     */
    private void pruefeFlucht(int startZeile, int startSpalte) throws JsonFehler {
        weiter(); // der Backslash selbst - der Aufrufer hat nur geschaut
        int escaped = weiter();
        switch (escaped) {
            case '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> weiter();
            case 'u' -> {
                for (int i = 0; i < 4; i++) {
                    if (pos >= eingabe.length() || !istHex(weiter())) {
                        throw new JsonFehler("Unvollständige \\u-Folge: vier Hexadezimalzeichen sind nötig.",
                                startZeile, startSpalte);
                    }
                }
            }
            case -1 -> throw new JsonFehler("String wird nicht geschlossen.", startZeile, startSpalte);
            default -> throw new JsonFehler("Unbekannte Fluchtfolge \\" + zeichen(escaped)
                    + ". Erlaubt sind \\\" \\\\ \\/ \\b \\f \\n \\r \\t und \\u mit vier Hexadezimalzeichen.",
                    startZeile, startSpalte);
        }
    }

    private JsonToken lesenZahl(int startZeile, int startSpalte) throws JsonFehler {
        int von = pos;
        if (Vorschau('-')) {
            weiter();
        }
        if (posAbEnde()) {
            throw new JsonFehler("Nach dem Minuszeichen muss eine Ziffer kommen.", startZeile, startSpalte);
        }
        int erste = weiter();
        if (!istZiffer(erste)) {
            throw new JsonFehler("Nach dem Minuszeichen muss eine Ziffer kommen.", startZeile, startSpalte);
        }
        if (erste == '0' && !posAbEnde() && istZiffer(Vorschau())) {
            throw new JsonFehler("Zahlen dürfen keine führende Null haben.", startZeile, startSpalte);
        }
        while (!posAbEnde() && istZiffer(Vorschau())) {
            weiter();
        }
        if (Vorschau('.')) {
            weiter();
            if (posAbEnde() || !istZiffer(Vorschau())) {
                throw new JsonFehler("Nach dem Punkt muss mindestens eine Ziffer kommen.", startZeile, startSpalte);
            }
            while (!posAbEnde() && istZiffer(Vorschau())) {
                weiter();
            }
        }
        if (Vorschau('e') || Vorschau('E')) {
            weiter();
            if (Vorschau('+') || Vorschau('-')) {
                weiter();
            }
            if (posAbEnde() || !istZiffer(Vorschau())) {
                throw new JsonFehler("Nach 'e' muss mindestens eine Ziffer kommen.", startZeile, startSpalte);
            }
            while (!posAbEnde() && istZiffer(Vorschau())) {
                weiter();
            }
        }
        return new JsonToken(JsonToken.Art.ZAHL, eingabe.substring(von, pos), startZeile, startSpalte);
    }

    private JsonToken lesenSchluesselwort(int startZeile, int startSpalte) throws JsonFehler {
        int von = pos;
        String wort = switch (eingabe.codePointAt(pos)) {
            case 't' -> "true";
            case 'f' -> "false";
            default -> "null";
        };
        for (int i = 0; i < wort.length(); i++) {
            if (posAbEnde() || weiter() != wort.charAt(i)) {
                throw new JsonFehler("Hier wird ein Wert erwartet, zum Beispiel true, false oder null.",
                        startZeile, startSpalte);
            }
        }
        if (!posAbEnde() && istWortZeichen(Vorschau())) {
            throw new JsonFehler("Unbekannter Wert. Nach " + wort
                    + " darf hier kein Buchstabe stehen.", startZeile, startSpalte);
        }
        return new JsonToken(switch (wort) {
            case "true" -> JsonToken.Art.WAHR;
            case "false" -> JsonToken.Art.FALSCH;
            default -> JsonToken.Art.NULL;
        }, wort, startZeile, startSpalte);
    }

    // ---- Lesen und Position ------------------------------------------------

    private boolean posAbEnde() {
        return pos >= eingabe.length();
    }

    private static boolean istLeerraum(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static boolean istZiffer(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean istHex(int c) {
        return istZiffer(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean istWortZeichen(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static JsonToken.Art kennzeichen(int c) {
        return switch (c) {
            case '{' -> JsonToken.Art.OBJEKT_AUF;
            case '}' -> JsonToken.Art.OBJEKT_ZU;
            case '[' -> JsonToken.Art.ARRAY_AUF;
            case ']' -> JsonToken.Art.ARRAY_ZU;
            case ',' -> JsonToken.Art.KOMMA;
            default -> JsonToken.Art.DOPPELPUNKT;
        };
    }

    private int Vorschau() {
        return pos < eingabe.length() ? eingabe.codePointAt(pos) : -1;
    }

    private boolean Vorschau(int erwartet) {
        return Vorschau() == erwartet;
    }

    /** Geht einen CodePoint weiter und hält Zeile und Spalte nach. */
    private int weiter() {
        if (posAbEnde()) {
            return -1;
        }
        int c = eingabe.codePointAt(pos);
        pos += Character.charCount(c);
        if (c == '\n') {
            zeile++;
            spalte = 1;
        } else {
            spalte++;
        }
        return c;
    }

    private static String beschreibe(int c) {
        return Character.isISOControl(c) ? ("0x" + Integer.toHexString(c)) : ("'" + zeichen(c) + "'");
    }

    private static String zeichen(int c) {
        return c < 0 ? "Ende der Eingabe" : String.valueOf((char) c);
    }
}
