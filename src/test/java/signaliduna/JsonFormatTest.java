package signaliduna;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer Format und Minify. Der Kern ist nicht das einzelne Layout, sondern
 * die Zusage "dieselben Stuecke in einer anderen Anordnung": Layout, Minify,
 * JSON mit Kommentaren und ein Fehlerfall werden alle ueber dieselbe Corpus
 * geprueft ({@link #strom_bleibt_gleich}).
 */
class JsonFormatTest {

    private static final List<String> CORPUS = List.of(
            "{}",
            "[]",
            "{\"a\":1}",
            "[1,2,3]",
            "{\"a\":{\"b\":{\"c\":[1,{\"d\":null}]}}}",
            "{\"a\":[],\"b\":{},\"c\":[[]],\"d\":[{}]}",
            "{\"z\":1,\"a\":2,\"m\":3}",
            "{\"a\":1,\"a\":2}",
            "[true,false,null]",
            "{\"a\":\"\",\"b\":\" \",\"c\":\"x y\"}",
            "{\"n\":[-0.0,1.50,1e+10,0.0e-7,12345678901234567890]}",
            "{\"s\":\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041\"}",
            "{\"url\":\"http://a.b/*c*/\",\"pfad\":\"C:\\\\Temp\\\\a\"}",
            "{\"emoji\":\"\\ud83d\\ude00 \\u00e4\\u00f6\\u00fc\"}",
            "{\"tief\":{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":[1,2,{\"f\":[]}]}}}}}}",
            "  \t{\"a\" : 1 , \"b\" : [ 1 , 2 ] }  ",
            "/* Kopf */ { \"a\" : 1 /* hinten */ } // Fuß",
            "{\"a\":1,,\"b\":2}".replace(",,", ","),
            "{\"a\":[1,2,],\"b\":{\"c\":1,}}",
            "42",
            "\"nur ein String\"",
            "true");

    private static String schoen(String eingabe) throws JsonFehler {
        return JsonFormat.schoen(eingabe);
    }

    private static String stuecke(String eingabe) throws JsonFehler {
        return JsonLeser.pruefe(JsonLexer.lex(eingabe)).stream()
                .map(t -> t.nutzlast().isEmpty() ? t.art().toString() : t.art() + ":" + t.nutzlast())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Nested
    @DisplayName("Layout")
    class Layout {

        @Test
        @DisplayName("einfaches Objekt: vier Leerzeichen je Ebene, abgeschlossener Zeilenumbruch")
        void einfach() throws JsonFehler {
            assertEquals("""
                    {
                        "a": 1
                    }
                    """, schoen("{\"a\":1}"));
        }

        @Test
        @DisplayName("Wert eines Objekts steht hinter dem Doppelpunkt")
        void wert_hinter_doppelpunkt() throws JsonFehler {
            assertEquals("""
                    {
                        "a": {
                            "b": {
                                "c": 1
                            }
                        }
                    }
                    """, schoen("{\"a\":{\"b\":{\"c\":1}}}"));
        }

        @Test
        @DisplayName("jedes Array-Element steht in einer eigenen Zeile")
        void array_elemente_je_zeile() throws JsonFehler {
            assertEquals("""
                    {
                        "a": [
                            1,
                            2
                        ]
                    }
                    """, schoen("{\"a\":[1,2]}"));
        }

        @Test
        @DisplayName("verschachtelte Arrays")
        void verschachtelte_arrays() throws JsonFehler {
            assertEquals("""
                    [
                        [
                            1,
                            2
                        ],
                        [
                            3
                        ]
                    ]
                    """, schoen("[[1,2],[3]]"));
        }

        @Test
        @DisplayName("Array aus Objekten")
        void array_aus_objekten() throws JsonFehler {
            assertEquals("""
                    [
                        {
                            "id": 1
                        },
                        {
                            "id": 2
                        }
                    ]
                    """, schoen("[{\"id\":1},{\"id\":2}]"));
        }

        @Test
        @DisplayName("leere Container bleiben einzeilig")
        void leere_container() throws JsonFehler {
            assertEquals("""
                    {
                        "a": {},
                        "b": []
                    }
                    """, schoen("{\"a\":{  },\"b\":[  ]}"));
        }

        @Test
        @DisplayName("Schluesselreihenfolge und doppelte Schluessel bleiben erhalten")
        void reihenfolge_und_doppelte() throws JsonFehler {
            assertEquals("""
                    {
                        "z": 1,
                        "a": 2,
                        "z": 3
                    }
                    """, schoen("{\"z\":1,\"a\":2,\"z\":3}"));
        }

        @Test
        @DisplayName("Wert an der Wurzel wird nicht eingerueckt")
        void wurzelwert() throws JsonFehler {
            assertEquals("42\n", schoen("42"));
            assertEquals("\"x\"\n", schoen("\"x\""));
            assertEquals("true\n", schoen("true"));
        }
    }

    @Nested
    @DisplayName("JSON mit Kommentaren")
    class MitKommentaren {

        @Test
        @DisplayName("Kommentare und hintere Kommas verschwinden, der Rest bleibt")
        void aufraeumen() throws JsonFehler {
            assertEquals("""
                    {
                        "compilerOptions": {
                            "target": "es2022",
                            "strict": true
                        },
                        "exclude": [
                            "node_modules"
                        ]
                    }
                    """, schoen("""
                    /* Kopf */
                    {
                      "compilerOptions": { "target": "es2022", "strict": true, }, // reiner Kommentar
                      "exclude": ["node_modules",],
                    }
                    """));
        }

        @Test
        @DisplayName("BOM am Anfang stoert nicht")
        void bom() throws JsonFehler {
            assertEquals("""
                    {
                        "a": 1
                    }
                    """, schoen("\uFEFF{\"a\":1}"));
        }

        @Test
        @DisplayName("Minify raeumt genau so auf wie Format")
        void minify_raeumt_auch_auf() throws JsonFehler {
            assertEquals("{\"a\":1,\"b\":[2]}\n", JsonFormat.dicht("/* x */ { \"a\" : 1, \"b\" : [ 2 , ], } // y"));
        }
    }

    @Nested
    @DisplayName("Worttreue")
    class Worttreue {

        @Test
        @DisplayName("Zahlen stehen unveraendert in der Ausgabe")
        void zahlen() throws JsonFehler {
            assertEquals("""
                    {
                        "a": 1.50,
                        "b": 1e+10,
                        "c": -0.0,
                        "d": 0.0e-7
                    }
                    """, schoen("{\"a\":1.50,\"b\":1e+10,\"c\":-0.0,\"d\":0.0e-7}"));
        }

        @Test
        @DisplayName("Escapes und Umlaute bleiben, auch die als \\u-Folge")
        void escapes() throws JsonFehler {
            assertEquals("""
                    {
                        "a": "\\u00e4\\u00f6\\u00fc",
                        "b": "C:\\\\Temp\\\\a",
                        "c": "\\ud83d\\ude00"
                    }
                    """, schoen("{\"a\":\"\\u00e4\\u00f6\\u00fc\",\"b\":\"C:\\\\Temp\\\\a\",\"c\":\"\\ud83d\\ude00\"}"));
        }

        @Test
        @DisplayName("Leerzeichen und Kommentarzeichen im String bleiben")
        void leerzeichen_im_string() throws JsonFehler {
            assertEquals("""
                    {
                        "url": "http://a.b/x?y=1&z=2"
                    }
                    """, schoen("{\"url\":\"http://a.b/x?y=1&z=2\"}"));
        }
    }

    @Nested
    @DisplayName("Minify")
    class Minify {

        @Test
        @DisplayName("kein Leerraum zwischen den Stuecken")
        void ohne_leerraum() throws JsonFehler {
            assertEquals("{\"a\":{\"b\":[1,2,{\"c\":true}]}}\n",
                    JsonFormat.dicht("{\n  \"a\" : {\n    \"b\" : [ 1 , 2 , { \"c\" : true } ]\n  }\n}"));
        }

        @Test
        @DisplayName("Leerraum im String bleibt erhalten")
        void leerraum_im_string() throws JsonFehler {
            assertEquals("{\"a\":\"x  y\"}\n", JsonFormat.dicht("{ \"a\" : \"x  y\" }"));
        }

        @Test
        @DisplayName("abgeschlossener Zeilenumbruch, damit die Konsole nicht klebt")
        void zeilenumbruch() throws JsonFehler {
            assertTrue(JsonFormat.dicht("{}").endsWith("\n"));
        }
    }

    @Nested
    @DisplayName("Eigenschaften ueber die ganze Corpus")
    class Eigenschaften {

        @Test
        @DisplayName("Strom bleibt gleich: Ausgabe enthaelt exakt die Stuecke der Eingabe")
        void strom_bleibt_gleich() throws JsonFehler {
            for (String eingabe : CORPUS) {
                String soll = stuecke(eingabe);
                for (String ausgabe : List.of(JsonFormat.schoen(eingabe), JsonFormat.dicht(eingabe))) {
                    assertEquals(soll, stuecke(ausgabe), eingabe);
                }
            }
        }

        @Test
        @DisplayName("Format zweimal ist wie einmal - kein Driften ueber Durchgaenge")
        void format_ist_stabil() throws JsonFehler {
            for (String eingabe : CORPUS) {
                String einmal = JsonFormat.schoen(eingabe);
                assertEquals(einmal, JsonFormat.schoen(einmal), eingabe);
                assertEquals(einmal, JsonFormat.schoen(JsonFormat.schoen(einmal)), eingabe);
            }
        }

        @Test
        @DisplayName("Minify zweimal ist wie einmal")
        void minify_ist_stabil() throws JsonFehler {
            for (String eingabe : CORPUS) {
                String einmal = JsonFormat.dicht(eingabe);
                assertEquals(einmal, JsonFormat.dicht(einmal), eingabe);
            }
        }

        @Test
        @DisplayName("Minify von Format ist dasselbe wie Minify direkt")
        void minify_ignoriert_das_layout() throws JsonFehler {
            for (String eingabe : CORPUS) {
                assertEquals(JsonFormat.dicht(eingabe), JsonFormat.dicht(JsonFormat.schoen(eingabe)), eingabe);
            }
        }

        @Test
        @DisplayName("hintere Kommas sind aus der Ausgabe entfernt")
        void keine_hinteren_kommas() throws JsonFehler {
            for (String eingabe : CORPUS) {
                for (String ausgabe : List.of(JsonFormat.schoen(eingabe), JsonFormat.dicht(eingabe))) {
                    List<JsonToken> stuecke = JsonLexer.lex(ausgabe);
                    for (int i = 1; i < stuecke.size(); i++) {
                        boolean schliessend = stuecke.get(i).art() == JsonToken.Art.OBJEKT_ZU
                                || stuecke.get(i).art() == JsonToken.Art.ARRAY_ZU;
                        assertFalse(stuecke.get(i - 1).art() == JsonToken.Art.KOMMA && schliessend,
                                ausgabe);
                    }
                }
            }
        }

        @Test
        @DisplayName("sehr tiefe Verschachtelung laeuft ohne StackOverflow")
        void tiefe_verschachtelung() throws JsonFehler {
            // 100000 Ebenen. Eine rekursive Pruefung stirbt daran im Bereich
            // ein paar tausend - genau darum liegt die Tiefe im Stapel.
            int tiefe = 100_000;
            String eingabe = "[".repeat(tiefe) + "1" + "]".repeat(tiefe);
            assertEquals(2 * tiefe + 1, JsonLeser.pruefe(JsonLexer.lex(eingabe)).size());
        }

        @Test
        @DisplayName("2000 Zeilen sind schnell genug fuer einen Tastendruck")
        void tempo() throws JsonFehler {
            StringBuilder gross = new StringBuilder("[\n");
            for (int i = 0; i < 2000; i++) {
                gross.append("  {\"id\": ").append(i)
                        .append(", \"name\": \"Eintrag ").append(i)
                        .append("\", \"tags\": [\"a\", \"b\"]}").append(i < 1999 ? ",\n" : "\n");
            }
            gross.append("]");
            String eingabe = gross.toString();

            long beste = Long.MAX_VALUE;
            for (int durchgang = 0; durchgang < 20; durchgang++) {
                long von = System.nanoTime();
                JsonFormat.schoen(eingabe);
                beste = Math.min(beste, System.nanoTime() - von);
            }
            // Vorher: 22 Pattern.compile-Aufrufe pro Zeile.
            assertTrue(beste < 300_000_000, "2000 Zeilen brauchten " + beste / 1_000_000 + " ms, erwartet < 300 ms");
        }
    }
}
