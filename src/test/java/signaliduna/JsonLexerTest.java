package signaliduna;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer den Tokenisierer. Hier geht es nur darum, was gelesen wird und wo
 * ein Fehler gemeldet wird - das Layout prueft {@link JsonFormatTest}.
 */
class JsonLexerTest {

    /** Kompakte Darstellung eines Tokenstroms, zum Beispiel [OBJEKT_AUF, ZAHL:1]. */
    private static String stuecke(String eingabe) throws JsonFehler {
        return stuecke(JsonLeser.pruefe(JsonLexer.lex(eingabe)));
    }

    private static String stuecke(List<JsonToken> tokens) {
        return tokens.stream()
                .map(t -> t.nutzlast().isEmpty() ? t.art().toString() : t.art() + ":" + t.nutzlast())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static JsonFehler fehler(String eingabe) {
        return assertThrows(JsonFehler.class, () -> JsonFormat.schoen(eingabe));
    }

    @Nested
    @DisplayName("Zahlen")
    class Zahlen {

        @Test
        @DisplayName("Formen werden woertlich uebernommen, nicht umgerechnet")
        void zahlen_woertlich() throws JsonFehler {
            // 1.50 darf nicht zu 1.5 werden: an der Schreibweise haengt, was
            // jemand mit diesem Text gerechnet hat.
            assertEquals("[ARRAY_AUF, ZAHL:1.50, KOMMA, ZAHL:1e+10, KOMMA, ZAHL:-0.0, KOMMA,"
                    + " ZAHL:0.0e-7, ARRAY_ZU]", stuecke("[1.50,1e+10,-0.0,0.0e-7]"));
        }

        @Test
        @DisplayName("0 und -0 sind gueltig")
        void null_und_minus_null() throws JsonFehler {
            assertEquals("[ARRAY_AUF, ZAHL:0, KOMMA, ZAHL:-0, ARRAY_ZU]", stuecke("[0,-0]"));
        }

        @Test
        @DisplayName("fuehrende Null ist ein Fehler")
        void fuehrende_null() {
            assertEquals("Zahlen dürfen keine führende Null haben.", fehler("{\"a\":01}").getMessage());
        }

        @Test
        @DisplayName("Pluszeichen, Punkt ohne Ziffern und Exponent ohne Ziffern sind Fehler")
        void kaputte_zahlen() {
            assertTrue(fehler("{\"a\":+1}").getMessage().contains("'+'"));
            assertTrue(fehler("{\"a\":1.}").getMessage().contains("Nach dem Punkt"));
            assertTrue(fehler("{\"a\":1e}").getMessage().contains("Nach 'e'"));
            assertTrue(fehler("{\"a\":-}").getMessage().contains("Minuszeichen"));
            assertTrue(fehler("[.5]").getMessage().contains("'.'"));
        }
    }

    @Nested
    @DisplayName("Strings")
    class Strings {

        @Test
        @DisplayName("Kommentarzeichen im String sind normale Zeichen")
        void kommentarzeichen_im_string() throws JsonFehler {
            // Der Klassiker: http:// und /* */ sehen wie Kommentare aus, sind
            // aber Inhalt. Wer hier falsch lauscht, verliert eine URL.
            assertEquals("[OBJEKT_AUF, STRING:\"url\", DOPPELPUNKT,"
                    + " STRING:\"http://a.b/*c*/\", OBJEKT_ZU]",
                    stuecke("{\"url\":\"http://a.b/*c*/\"}"));
        }

        @Test
        @DisplayName("maskiertes Anfuehrungszeichen schliesst den String nicht")
        void maskiertes_anfuehrungszeichen() throws JsonFehler {
            assertEquals("[STRING:\"a\\\"b\"]", stuecke("\"a\\\"b\""));
        }

        @Test
        @DisplayName("offener String wird an seinem Anfang gemeldet")
        void offener_string() {
            JsonFehler f = fehler("{\"a\":\"offen}");
            assertEquals("String wird nicht geschlossen.", f.getMessage());
            assertEquals(1, f.zeile());
        }

        @Test
        @DisplayName("Zeilenumbruch im String ist ein Fehler mit Ort")
        void zeilenumbruch_im_string() {
            JsonFehler f = fehler("{\n  \"a\": \"x\ny\"\n}");
            assertTrue(f.getMessage().contains("Unerlaubtes Zeichen 0xa"), f.getMessage());
            assertEquals(3, f.zeile());
            assertEquals(1, f.spalte());
        }

        @Test
        @DisplayName("unbekannte Fluchtfolge wird abgelehnt")
        void unbekannte_fluchtfolge() {
            assertTrue(fehler("\"a\\qb\"").getMessage().contains("Unbekannte Fluchtfolge \\q"));
        }

        @Test
        @DisplayName("unvollstaendige \\u-Folge wird abgelehnt")
        void unvollstaendige_unicode_folge() {
            assertTrue(fehler("\"\\u12\"").getMessage().contains("Unvollständige \\u-Folge"));
            assertTrue(fehler("\"\\uZZZZ\"").getMessage().contains("Unvollständige \\u-Folge"));
        }

        @Test
        @DisplayName("alle erlaubten Fluchtfolgen gehen durch")
        void erlaubte_fluchtfolgen() throws JsonFehler {
            assertEquals("[STRING:\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041\"]",
                    stuecke("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041\""));
        }

        @Test
        @DisplayName("Zeichen ausserhalb der BMP zaehlen als eine Spalte")
        void zeichen_als_eine_spalte() {
            // Ein Emoji besteht aus zwei char, ist aber ein Zeichen.
            JsonFehler f = fehler("[\"\ud83d\ude00\" x]");
            assertEquals(6, f.spalte(), f.meldung());
        }
    }

    @Nested
    @DisplayName("Kommentare und BOM")
    class Kommentare {

        @Test
        @DisplayName("Zeilen- und Blockkommentar verschwinden")
        void kommentare_verschwinden() throws JsonFehler {
            assertEquals("[OBJEKT_AUF, STRING:\"a\", DOPPELPUNKT, ZAHL:1, OBJEKT_ZU]",
                    stuecke("// vorn\n{ /* seitlich */ \"a\": 1 // hinten\n}"));
        }

        @Test
        @DisplayName("BOM am Anfang wird ueberlesen")
        void bom_am_anfang() throws JsonFehler {
            assertEquals("[OBJEKT_AUF, STRING:\"a\", DOPPELPUNKT, ZAHL:1, OBJEKT_ZU]",
                    stuecke("\uFEFF{\"a\":1}"));
        }

        @Test
        @DisplayName("BOM mitten im Text ist ein Fehler")
        void bom_mitten() {
            assertTrue(fehler("{\"a\":1}\uFEFF").getMessage().contains("Unerwartetes Zeichen"));
        }

        @Test
        @DisplayName("offener Blockkommentar zeigt auf das /")
        void offener_blockkommentar() {
            JsonFehler f = fehler("{\"a\":1}\n/* offen");
            assertEquals("Blockkommentar wird nicht geschlossen.", f.getMessage());
            assertEquals(2, f.zeile());
            assertEquals(1, f.spalte());
        }

        @Test
        @DisplayName("Zeilenzaehlung laeuft ueber Kommentare hinweg richtig")
        void zeilenzaehlung_nach_kommentar() {
            JsonFehler f = fehler("{\n  // drei Zeilen\n  // sind es nicht\n  \"a\" 1\n}");
            assertEquals(4, f.zeile());
        }
    }

    @Nested
    @DisplayName("Schluesselwoerter")
    class Schluesselwoerter {

        @Test
        @DisplayName("true, false und null werden gelesen")
        void schluesselwoerter() throws JsonFehler {
            // Als Array, nicht an der Wurzel: "true, false" waeren zwei Werte
            // und damit ein Fehler - das ist eine eigene Regel und eigene Test.
            assertEquals("[ARRAY_AUF, WAHR:true, KOMMA, FALSCH:false, KOMMA, NULL:null, ARRAY_ZU]",
                    stuecke("[true, false, null]"));
        }

        @Test
        @DisplayName("truex ist nicht true mit Rest")
        void kein_praefix() {
            assertTrue(fehler("[truex]").getMessage().contains("Unbekannter Wert"));
        }

        @Test
        @DisplayName("undefined gehoert zu keinem JSON")
        void undefined() {
            assertTrue(fehler("{\"a\":undefined}").getMessage().contains("Unerwartetes Zeichen 'u'"));
        }
    }

    @Nested
    @DisplayName("Struktur")
    class Struktur {

        @Test
        @DisplayName("hinteres Komma wird nicht beachtet")
        void nachlaufkomma() throws JsonFehler {
            assertEquals("[OBJEKT_AUF, STRING:\"a\", DOPPELPUNKT, ZAHL:1, OBJEKT_ZU]",
                    stuecke("{\"a\":1,}"));
        }

        @Test
        @DisplayName("zwei Kommas sind zu viel")
        void zwei_kommas() {
            assertEquals("Zwei Kommas nacheinander sind zu viel.", fehler("{\"a\":1,,}").getMessage());
        }

        @Test
        @DisplayName("unbegrenzter Name ist ein unerwartetes Zeichen")
        void unbegrenzter_name() {
            assertEquals("Unerwartetes Zeichen 'a'.", fehler("{a:1}").getMessage());
        }

        @Test
        @DisplayName("Zahl als Schluessel nennt das Stueck, das dort steht")
        void zahl_als_schluessel() {
            assertEquals("Hier wird ein Schlüssel in Anführungszeichen erwartet, nicht 2.",
                    fehler("{\"a\":1, 2:3}").getMessage());
        }

        @Test
        @DisplayName("Schluessel ohne Doppelpunkt am Ende")
        void schluessel_ohne_doppelpunkt() {
            assertEquals("Hier wird ein Doppelpunkt erwartet.", fehler("{\"a\"}").getMessage());
        }

        @Test
        @DisplayName("Wert ohne Doppelpunkt dazwischen")
        void wert_ohne_doppelpunkt() {
            assertEquals("Hier wird ein Doppelpunkt erwartet.", fehler("{\"a\" 1}").getMessage());
        }

        @Test
        @DisplayName("Doppelpunkt ohne Schluessel davor")
        void doppelpunkt_ohne_schluessel() {
            assertEquals("Hier wird ein Schlüssel und dann ein Doppelpunkt erwartet.",
                    fehler("{\"a\":1, :2}").getMessage());
        }

        @Test
        @DisplayName("Komma in einem Objekt vor dem ersten Schluessel")
        void komma_zu_frueh() {
            assertEquals("Hier wird ein Komma erwartet, weil der Eintrag noch nicht fertig ist.",
                    fehler("{,}").getMessage());
        }

        @Test
        @DisplayName("falsche Klammer nennt die richtige")
        void falsche_klammer() {
            assertEquals("Falsche Klammer: hier gehört '}' hin.", fehler("{\"a\":1]").getMessage());
        }

        @Test
        @DisplayName("Text hinter dem Wert wird abgelehnt")
        void weiterer_wert() {
            assertEquals("Nach dem Wert darf nichts mehr kommen.", fehler("{\"a\":1} {}").getMessage());
        }

        @Test
        @DisplayName("nur Kommentare ist kein Wert")
        void nur_kommentare() {
            assertEquals("Der Text enthält keinen JSON-Wert.",
                    fehler("// nichts\n/* hier auch nichts */").getMessage());
        }

        @Test
        @DisplayName("leerer Text ist kein Wert")
        void leerer_text() {
            assertEquals("Der Text enthält keinen JSON-Wert.", fehler("").getMessage());
        }

        @Test
        @DisplayName("offenes Objekt zeigt auf die offene Klammer")
        void offenes_objekt() {
            JsonFehler f = fehler("{\n  \"a\": {\n    \"b\": 1\n  }\n");
            assertEquals("Ein Objekt wird nicht geschlossen. Es fehlt ein '}'.", f.getMessage());
            assertEquals(1, f.zeile());
            assertEquals(1, f.spalte());
        }
    }
}
