package signaliduna;

import java.util.ArrayList;
import java.util.List;

/**
 * Prüft, ob der Token-Strom ein gültiges JSON ergibt, und lehnt dabei alles
 * ab, was ein Parser später anders auslegt als der Leser.
 *
 * <p>Die Prüfung läuft über einen Stapel, nicht über Rekursion. Das ist
 * Absicht und nicht Kosmetik: eine kopierte API-Antwort kann zehntausend
 * Klammerebenen tief verschachtelt sein ({@code [[[[...]]]]}), und rekursiver
 * Absturz wäre dann ein {@code StackOverflowError} mitten in der Anzeige, ohne
 * Zeile, ohne Spalte, ohne brauchbare Meldung. Mit einem Stapel ist die
 * Eingabetiefe durch den Heap begrenzt und die Meldung bleibt die aus der
 * Datei.
 *
 * <p>Der Stapel ist zugleich die Erlaubnisliste: jeder Rahmen merkt sich, ob
 * ein Objekt oder ein Array offen ist und was als Nächstes kommen darf. Damit
 * ist "Was kommt hier?" eine Frage pro Token und nicht ein Blick in den Rest
 * des Textes - ein Zustandsautomat, der beim ersten falschen Token stehen
 * bleibt und dessen Stelle kennt.
 */
final class JsonLeser {

    /** Was ein Rahmen als Nächstes erwartet. */
    private enum Zustand {
        /** Offen, noch nichts gelesen: Schlüssel oder '}' bzw. Wert oder ']'. */
        OBER_OEFFNET,
        /** Nach einem Komma: noch ein Eintrag oder das schließende Zeichen. */
        NACH_KOMMA,
        /** Nach einem Schlüssel: ein Doppelpunkt. */
        NACH_SCHLUESSEL,
        /** Nach dem Doppelpunkt: ein Wert. */
        NACH_DOPPELPUNKT,
        /** Nach einem Eintrag: Komma oder schließendes Zeichen. */
        NACH_WERT
    }

    /** Ein offener Container. */
    private static final class Rahmen {
        final boolean objekt;
        final JsonToken oeffner;
        Zustand zustand = Zustand.OBER_OEFFNET;

        Rahmen(boolean objekt, JsonToken oeffner) {
            this.objekt = objekt;
            this.oeffner = oeffner;
        }
    }

    private JsonLeser() {
    }

    /**
     * Prüft den Strom und gibt ihn ohne Nachlaufkomma zurück.
     *
     * <p>Ein hängendes Komma gehört zur Eingabe, aber nicht zur Ausgabe:
     * Ausgabe ist striktes JSON, und ein Komma vor einer schließenden Klammer
     * ist es nicht. Die Stelle ist hier die einzige, an der der Leser den
     * Tokenstrom überhaupt verändert - deshalb gibt er eine neue Liste zurück
     * und lässt den ursprünglichen Strom unangetastet.
     *
     * @throws JsonFehler mit Ort, wenn der Strom kein JSON ergibt
     */
    static List<JsonToken> pruefe(List<JsonToken> tokens) throws JsonFehler {
        // Der unterste Rahmen steht für die ganze Datei. Er ist formal ein
        // Objekt, damit Schlüssel und Werte auch dort unterschieden werden
        // können - nötig ist das an der Wurzel nicht, schadet aber nicht.
        List<Rahmen> stapel = new ArrayList<>();
        stapel.add(new Rahmen(true, null));
        List<JsonToken> ohneNachlauf = new ArrayList<>(tokens.size());

        for (int i = 0; i < tokens.size(); i++) {
            JsonToken token = tokens.get(i);
            lese(stapel, token);
            boolean nachlauf = token.art() == JsonToken.Art.KOMMA && i + 1 < tokens.size()
                    && istSchliessend(tokens.get(i + 1).art());
            if (!nachlauf) {
                ohneNachlauf.add(token);
            }
        }
        beende(stapel, tokens);
        return ohneNachlauf;
    }

    private static boolean istSchliessend(JsonToken.Art art) {
        return art == JsonToken.Art.OBJEKT_ZU || art == JsonToken.Art.ARRAY_ZU;
    }

    private static void lese(List<Rahmen> stapel, JsonToken token) throws JsonFehler {
        Rahmen rahmen = rahmen(stapel);
        boolean wurzel = stapel.size() == 1;
        JsonToken.Art art = token.art();

        switch (art) {
            case OBJEKT_AUF, ARRAY_AUF -> {
                if (!nimmtWert(rahmen, wurzel)) {
                    throw new JsonFehler(wartetAuf(rahmen, wurzel), token.zeile(), token.spalte());
                }
                // Auch der Wurzelrahmen merkt sich jetzt, dass er gelesen hat:
                // ein zweiter Wurzelwert wäre Text hinter dem Wert.
                rahmen.zustand = Zustand.NACH_WERT;
                stapel.add(new Rahmen(art == JsonToken.Art.OBJEKT_AUF, token));
            }
            case OBJEKT_ZU, ARRAY_ZU -> {
                boolean passt = art == JsonToken.Art.OBJEKT_ZU == rahmen.objekt;
                // Nach dem letzten Eintrag ist das schließende Zeichen genauso
                // erlaubt wie nach einem Komma, das dort nichts mehr erwartet.
                boolean erlaubt = !wurzel && (rahmen.zustand == Zustand.OBER_OEFFNET
                        || rahmen.zustand == Zustand.NACH_KOMMA || rahmen.zustand == Zustand.NACH_WERT);
                if (!passt || !erlaubt) {
                    throw new JsonFehler(geschlossenText(rahmen, art, wurzel), token.zeile(), token.spalte());
                }
                stapel.remove(stapel.size() - 1);
                // Der Eintrag im Elternrahmen ist fertig, auch wenn er selbst
                // gerade geschlossen wurde.
                rahmen(stapel).zustand = Zustand.NACH_WERT;
            }
            case DOPPELPUNKT -> {
                if (wurzel || !rahmen.objekt) {
                    throw new JsonFehler("Ein Doppelpunkt gehört hinter einen Schlüssel.",
                            token.zeile(), token.spalte());
                }
                if (rahmen.zustand != Zustand.NACH_SCHLUESSEL) {
                    throw new JsonFehler("Hier wird ein Schlüssel und dann ein Doppelpunkt erwartet.",
                            token.zeile(), token.spalte());
                }
                rahmen.zustand = Zustand.NACH_DOPPELPUNKT;
            }
            case KOMMA -> {
                if (wurzel) {
                    throw new JsonFehler("Nach dem Wert darf nichts mehr kommen.",
                            token.zeile(), token.spalte());
                }
                if (rahmen.zustand == Zustand.NACH_KOMMA) {
                    throw new JsonFehler("Zwei Kommas nacheinander sind zu viel.",
                            token.zeile(), token.spalte());
                }
                if (rahmen.zustand != Zustand.NACH_WERT) {
                    throw new JsonFehler("Hier wird ein Komma erwartet, weil der Eintrag noch nicht fertig ist.",
                            token.zeile(), token.spalte());
                }
                rahmen.zustand = Zustand.NACH_KOMMA;
            }
            default -> {
                if (nimmtWert(rahmen, wurzel)) {
                    rahmen.zustand = Zustand.NACH_WERT;
                    return;
                }
                if (wurzel) {
                    throw new JsonFehler(nachWertText(rahmen), token.zeile(), token.spalte());
                }
                if (rahmen.objekt) {
                    leseImObjekt(rahmen, token, art);
                } else {
                    throw new JsonFehler("Hier wird ein Komma oder ']' erwartet.",
                            token.zeile(), token.spalte());
                }
            }
        }
    }

    /** Ein Schlüssel ist ein String, ein Wert nicht. */
    /** Der Rahmen, in dem gelesen wird: der oberste der Liste. */
    private static Rahmen rahmen(List<Rahmen> stapel) {
        return stapel.get(stapel.size() - 1);
    }

    private static void leseImObjekt(Rahmen rahmen, JsonToken token, JsonToken.Art art) throws JsonFehler {
        switch (rahmen.zustand) {
            case OBER_OEFFNET, NACH_KOMMA -> {
                if (art != JsonToken.Art.STRING) {
                    throw new JsonFehler("Hier wird ein Schlüssel in Anführungszeichen erwartet, "
                            + "nicht " + token.beschreibung() + ".", token.zeile(), token.spalte());
                }
                rahmen.zustand = Zustand.NACH_SCHLUESSEL;
            }
            case NACH_WERT -> {
                throw new JsonFehler("Hier wird ein Komma oder '}' erwartet.",
                        token.zeile(), token.spalte());
            }
            case NACH_SCHLUESSEL -> throw new JsonFehler("Hier wird ein Doppelpunkt erwartet.",
                    token.zeile(), token.spalte());
            default -> throw new JsonFehler("Hier wird ein Wert erwartet.",
                    token.zeile(), token.spalte());
        }
    }

    private static void beende(List<Rahmen> stapel, List<JsonToken> tokens) throws JsonFehler {
        if (stapel.size() > 1) {
            Rahmen offen = stapel.get(1);
            throw new JsonFehler("Ein " + (offen.objekt ? "Objekt" : "Array") + " wird nicht geschlossen. "
                    + "Es fehlt ein " + (offen.objekt ? "'}'" : "']'") + ".",
                    offen.oeffner.zeile(), offen.oeffner.spalte());
        }
        int zeile = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).zeile();
        switch (stapel.get(0).zustand) {
            case NACH_DOPPELPUNKT -> throw new JsonFehler("Nach dem Doppelpunkt fehlt ein Wert.", zeile, 1);
            case NACH_KOMMA -> throw new JsonFehler("Nach dem Komma fehlt ein Eintrag.", zeile, 1);
            case OBER_OEFFNET -> throw new JsonFehler("Der Text enthält keinen JSON-Wert.", 1, 1);
            default -> {
                // Genau ein Wert gelesen, mehr durfte nicht kommen. Ein offener
                // Schlüssel ist hier nicht moeglich: dafuer muesste noch ein
                // Container offen sein, und der wurde oben schon gemeldet.
            }
        }
    }

    // ---- Erwartungen als Text ------------------------------------------------

    private static boolean nimmtWert(Rahmen rahmen, boolean wurzel) {
        if (wurzel) {
            return rahmen.zustand == Zustand.OBER_OEFFNET;
        }
        if (rahmen.objekt) {
            return rahmen.zustand == Zustand.NACH_DOPPELPUNKT;
        }
        return rahmen.zustand == Zustand.OBER_OEFFNET || rahmen.zustand == Zustand.NACH_KOMMA;
    }

    private static String wartetAuf(Rahmen rahmen, boolean wurzel) {
        if (wurzel) {
            return nachWertText(rahmen);
        }
        if (rahmen.objekt) {
            return "Hier wird ein Schlüssel in Anführungszeichen erwartet.";
        }
        return rahmen.zustand == Zustand.NACH_KOMMA
                ? "Nach dem Komma fehlt ein Wert."
                : "Hier wird ein Komma oder ']' erwartet.";
    }

    /** An der Wurzel: erst ein Wert, danach nichts mehr. */
    private static String nachWertText(Rahmen rahmen) {
        return rahmen.zustand == Zustand.OBER_OEFFNET
                ? "Hier wird ein Wert erwartet."
                : "Nach dem Wert darf nichts mehr kommen.";
    }

    private static String geschlossenText(Rahmen rahmen, JsonToken.Art art, boolean wurzel) {
        if (wurzel) {
            return "Nach dem Wert darf nichts mehr kommen.";
        }
        boolean passt = art == JsonToken.Art.OBJEKT_ZU == rahmen.objekt;
        if (!passt) {
            return "Falsche Klammer: hier gehört " + (rahmen.objekt ? "'}'" : "']'") + " hin.";
        }
        if (rahmen.objekt) {
            return switch (rahmen.zustand) {
                case NACH_DOPPELPUNKT, NACH_WERT -> "Hier wird ein Wert erwartet.";
                case NACH_SCHLUESSEL -> "Hier wird ein Doppelpunkt erwartet.";
                default -> "Hier wird ein Schlüssel in Anführungszeichen erwartet.";
            };
        }
        return "Hier wird ein Komma oder ']' erwartet.";
    }
}
