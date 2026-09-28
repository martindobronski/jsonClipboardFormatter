package signaliduna;

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Faerbt JSON im Textfeld ein.
 *
 * <p>Die Zerlegung ist bewusst eine eigene, milde Variante des
 * {@link JsonLexer} und nicht der Lexer selbst: Hier wird auch ein Text
 * eingefaerbt, der gerade halb getippt ist, und der muss keine Ausnahme
 * werfen, sondern still stehen bleiben, wo der Bildschirm nichts anzeigen
 * kann. Was der Lexer strikt ablehnt, faerbt diese Variante bis zum Ende der
 * Zeile und hoert dann auf - sieht nach einer unfertigen Datei aus, statt
 * nach einem Fehler.
 *
 * <p>Kommentare und Strings werden an derselben Stelle erkannt wie beim Lesen.
 * Eine eigene Regex wuerde {@code "http://x"} als Kommentar faerben und
 * {@code \"} als schliessendes Zeichen lesen - beides sieht sofort nach
 * Spielzeug aus, wenn es jemand zum ersten Mal sieht.
 */
final class JsonSyntaxHighlighter {

    /**
     * Oberhalb dieser Zeichenzahl wird nicht mehr eingefaerbt. Die Zerlegung
     * ist linear, aber jeder Tastendruck laeuft ueber sie; bei mehreren MB
     * waere die Verzoegerung spuerbar. Fuer den Rest bleibt die Grundfarbe -
     * das ist ein Text, den niemand liest, sondern einfuegt.
     */
    static final int MAX_ZEICHEN = 100_000;

    /** Was ein Bereich ist. */
    enum Art {
        /** Schluessel: ein String, hinter dem ein Doppelpunkt steht. */
        SCHLUESSEL,
        /** Ein String als Wert. */
        ZEICHENKETTE,
        ZAHL,
        /** true, false, null. */
        WORT,
        /** Zeilen- oder Blockkommentar. */
        KOMMENTAR
    }

    /** Ein einzufaerbender Bereich. */
    record Span(int start, int laenge, Art art) {
    }

    private final Theme theme;

    JsonSyntaxHighlighter(Theme theme) {
        this.theme = theme;
    }

    /**
     * Zerlegt den Text in einzufaerbende Bereiche. Wirft nie eine Ausnahme:
     * ein unfertiger Text ist der Normalfall beim Tippen.
     */
    static List<Span> spans(String text) {
        List<Span> spans = new ArrayList<>();
        int laenge = text.length();
        int i = 0;
        while (i < laenge) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < laenge && (text.charAt(i + 1) == '/' || text.charAt(i + 1) == '*')) {
                int ende = endeKommentar(text, i);
                spans.add(new Span(i, ende - i, Art.KOMMENTAR));
                i = ende;
            } else if (c == '"') {
                int ende = endeString(text, i);
                boolean schluessel = istSchluessel(text, ende);
                spans.add(new Span(i, ende - i, schluessel ? Art.SCHLUESSEL : Art.ZEICHENKETTE));
                i = ende;
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                int ende = endeZahl(text, i);
                spans.add(new Span(i, ende - i, Art.ZAHL));
                i = ende;
            } else if (c == 't' || c == 'f' || c == 'n') {
                int ende = endeWort(text, i);
                if (ende - i == 4 || ende - i == 5) {
                    spans.add(new Span(i, ende - i, Art.WORT));
                }
                i = ende;
            } else {
                i++;
            }
        }
        return spans;
    }

    /**
     * Hinter dem String steht ein Doppelpunkt, dann war er ein Schluessel.
     *
     * <p>Leerraum dazwischen wird uebersprungen, ein Kommentar nicht: das ist
     * eine Randform, die in keiner Datei vorkommt, und fuer sie eine eigene
     * Regel zu pflegen waere Pflege ohne Nutzen.
     */
    private static boolean istSchluessel(String text, int nachString) {
        int i = nachString;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i < text.length() && text.charAt(i) == ':';
    }

    private static int endeKommentar(String text, int start) {
        boolean block = text.charAt(start + 1) == '*';
        int i = start + 2;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n' && !block) {
                return i;
            }
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                return i + 2;
            }
            i++;
        }
        return text.length();
    }

    /**
     * Bis zum schliessenden Anfuehrungszeichen, Fluchtfolgen mitgeschluckt.
     * Ein offener String laeuft bis zum Ende des Textes - so sieht eine
     * angefangene Zeile aus, statt als Fehler.
     */
    private static int endeString(String text, int start) {
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return text.length();
    }

    private static int endeZahl(String text, int start) {
        int i = start;
        while (i < text.length() && istZiffernzeichen(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean istZiffernzeichen(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
    }

    private static int endeWort(String text, int start) {
        int i = start;
        while (i < text.length() && Character.isLetter(text.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Faerbt das gesamte Dokument neu ein. Setzt zuerst die Grundfarbe. */
    void faerben(StyledDocument document) {
        String text;
        try {
            text = document.getText(0, document.getLength());
        } catch (javax.swing.text.BadLocationException ex) {
            return;
        }

        document.setCharacterAttributes(0, document.getLength(), grund(), false);
        for (Span span : spans(text)) {
            int ende = Math.min(span.start() + span.laenge(), MAX_ZEICHEN);
            if (span.start() < MAX_ZEICHEN && ende > span.start()) {
                document.setCharacterAttributes(span.start(), ende - span.start(), stilFuer(span.art()), false);
            }
        }
    }

    private SimpleAttributeSet grund() {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, theme.text);
        StyleConstants.setBackground(set, theme.flaeche);
        return set;
    }

    private SimpleAttributeSet stilFuer(Art art) {
        return switch (art) {
            // Der Schluessel behaelt die Farbe, die in SQL eine Bezeichner
            // traegt: er ist das Geruest, der Wert der Inhalt.
            case SCHLUESSEL -> stil(theme.bezeichner, false);
            case ZEICHENKETTE -> stil(theme.stringFarbe, false);
            case ZAHL -> stil(theme.zahl, false);
            case WORT -> stil(theme.keyword, false);
            case KOMMENTAR -> stil(theme.kommentar, true);
        };
    }

    private static SimpleAttributeSet stil(java.awt.Color farbe, boolean kursiv) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, farbe);
        StyleConstants.setItalic(set, kursiv);
        return set;
    }
}
