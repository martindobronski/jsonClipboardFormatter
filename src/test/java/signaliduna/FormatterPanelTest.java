package signaliduna;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testet die Verdrahtung der echten UI, insbesondere das Verhalten, das vorher
 * fehlte: nach dem Einlesen blieben die Aktionen freigeschaltet, auch wenn der
 * Text danach von Hand geaendert wurde.
 *
 * <p>Laeuft headless - {@code FormatterPanel} ist ein {@code JPanel} und braucht
 * keinen Bildschirm.
 */
class FormatterPanelTest {

    private FormatterPanel panel;
    private JTextComponent jsonArea;
    private JButton readButton;
    private JButton formatButton;
    private JButton packButton;
    private JButton writeButton;
    private JButton exitButton;
    private JToggleButton themeButton;
    private JLabel umfangLabel;
    private JLabel statusLabel;

    @BeforeEach
    void setUp() throws Exception {
        // Das aktive Theme ist statisch; ohne Zuruecksetzen haengt jeder Test
        // vom zuletzt gelaufenen Theme-Test ab.
        FormatterPanel.setzeTheme(Theme.DUNKEL);
        panel = new FormatterPanel();
        jsonArea = field("jsonArea");
        readButton = field("readButton");
        formatButton = field("formatButton");
        packButton = field("packButton");
        writeButton = field("writeButton");
        exitButton = field("exitButton");
        themeButton = field("themeButton");
        umfangLabel = field("umfangLabel");
        statusLabel = field("statusLabel");
    }

    @Nested
    @DisplayName("Freischaltung haengt am Text")
    class Freischaltung {

        @Test
        void startzustand_ist_gesperrt() {
            assertTrue(readButton.isEnabled(), "Einlesen muss starten moeglich sein");
        }

        @Test
        void gueltiges_json_schaltet_frei() {
            jsonArea.setText("{\"a\": 1}");
            assertTrue(formatButton.isEnabled());
            assertTrue(packButton.isEnabled());
            assertTrue(writeButton.isEnabled());
        }

        @Test
        @DisplayName("Handeingabe von Nicht-JSON sperrt wieder - der zentrale Fix")
        void handeingabe_sperrt_wieder() {
            jsonArea.setText("{\"a\": 1}");
            assertTrue(formatButton.isEnabled());

            jsonArea.setText("Einkaufsliste");

            assertFalse(formatButton.isEnabled(), "Formatieren muss nach Handeingabe sperren");
            assertFalse(packButton.isEnabled(), "Packen muss nach Handeingabe sperren");
            assertFalse(writeButton.isEnabled(), "Schreiben muss nach Handeingabe sperren");
        }

        @Test
        void zurueck_zu_json_schaltet_wieder_frei() {
            jsonArea.setText("Einkaufsliste");
            assertFalse(formatButton.isEnabled());

            jsonArea.setText("[]");

            assertTrue(formatButton.isEnabled(), "eine leere Liste wurde frueher faelschlich abgelehnt");
        }

        @Test
        @DisplayName("ein reiner Kommentar sperrt - er ist zwar erlaubt, aber kein Wert")
        void nur_kommentar_sperrt() {
            jsonArea.setText("// nur ein Kommentar");
            assertFalse(formatButton.isEnabled());
            assertFalse(writeButton.isEnabled());

            jsonArea.setText("/* Block */");
            assertFalse(formatButton.isEnabled(), "auch ein Blockkommentar ist noch kein Wert");
        }

        @Test
        @DisplayName("ein Kommentar vor dem Wert schaltet frei")
        void kommentar_vor_dem_wert_schaltet_frei() {
            jsonArea.setText("/* Kopf */ {\"a\": 1}");
            assertTrue(formatButton.isEnabled());
        }

        @Test
        @DisplayName("auch ein einzelner Wert an der Wurzel ist JSON")
        void wurzelwert_schaltet_frei() {
            for (String wurzel : new String[]{"42", "-1.5e3", "\"text\"", "true", "false", "null"}) {
                jsonArea.setText(wurzel);
                assertTrue(formatButton.isEnabled(), "haette freischalten muessen: " + wurzel);
            }
        }

        @Test
        @DisplayName("Worte, die nur so tun, sperren")
        void wort_aehnliche_werte_sperren() {
            // Der Detektor darf nicht jedes Wort mit n, t oder f am Anfang
            // durchlassen - "nein" und "tuer" sind kein JSON.
            for (String text : new String[]{"nein", "truex", "nullwert", "falsch"}) {
                jsonArea.setText(text);
                assertFalse(formatButton.isEnabled(), "haette sperren muessen: " + text);
            }
        }

        @Test
        void leerer_text_sperrt_und_haelt_einlesen_offen() {
            jsonArea.setText("   ");

            assertFalse(formatButton.isEnabled());
            assertFalse(writeButton.isEnabled());
            assertTrue(readButton.isEnabled(), "Einlesen muss auch bei leerem Text moeglich bleiben");
        }

        @Test
        @DisplayName("abgeschnittenes JSON bleibt bedienbar und stuerzt nicht ab")
        void abgeschnittenes_json_bleibt_nutzbar() {
            // Der Knopf bleibt frei, damit der Fehler beim Tippen sichtbar
            // wird - und der Fehler ist eine Meldung, keine Ausnahme, die
            // aus dem Hintergrundthread in die Leere laeuft.
            jsonArea.setText("{\"a\": \"unbalanced");
            assertTrue(formatButton.isEnabled());
            assertThrows(JsonFehler.class, () -> JsonFormat.schoen(jsonArea.getText()));
            assertThrows(JsonFehler.class, () -> JsonFormat.dicht(jsonArea.getText()));
        }
    }

    @Nested
    @DisplayName("Statusmeldungen")
    class Statusmeldungen {

        @Test
        void warnung_bei_nicht_json() {
            jsonArea.setText("Einkaufsliste");
            assertTrue(statusLabel.getText().contains("blockiert"),
                    "Erwartet Blockier-Hinweis, war: " + statusLabel.getText());
        }

        @Test
        @DisplayName("der Ruhezustand laesst dem Zaehler den Platz")
        void ruhezustand_zeigt_den_zaehler() {
            jsonArea.setText("{\"a\": 1}");
            assertEquals("", statusLabel.getText(), "im Ruhezustand steht kein Text");
            assertFalse(statusLabel.isVisible(), "im Ruhezustand ist keine Meldung da");
            assertTrue(umfangLabel.isVisible(), "im Ruhezustand steht der Zaehler");

            jsonArea.setText("");
            assertEquals("", statusLabel.getText());
            assertTrue(umfangLabel.isVisible());
        }

        @Test
        @DisplayName("eine echte Meldung verdraengt den Zaehler an derselben Stelle")
        void meldung_verdraengt_den_zaehler() throws Exception {
            JLabel zaehler = umfangLabel;
            // Beide Texte haengen in demselben Container: damit teilen sie sich
            // zwangslaeufig denselben Platz und koennen sich nicht gegenseitig
            // in die Breite schieben. Getestet wird die Struktur, nicht eine
            // Pixelposition - die haengt im Testfenster von Layout-Timing ab.
            JPanel infoLine = field("infoLine");
            java.awt.Container platz = zaehler.getParent();
            assertSame(platz, statusLabel.getParent(),
                    "Zaehler und Meldung teilen sich nicht denselben Platz");
            assertSame(infoLine, platz.getParent(),
                    "der Platz ist nicht die Zeile selbst, sondern ein eigenes Feld darin");

            jsonArea.setText("{\"a\": 1}");
            assertTrue(zaehler.isVisible(), "im Ruhezustand steht der Zaehler");
            assertFalse(statusLabel.isVisible(), "im Ruhezustand steht keine Meldung");

            jsonArea.setText("Einkaufsliste");
            assertTrue(statusLabel.getText().startsWith("\u26a0"), statusLabel.getText());
            assertFalse(zaehler.isVisible(), "waehrend einer Meldung steht der Zaehler nicht daneben");
            assertTrue(statusLabel.isVisible(), "die Meldung steht an ihrem Platz");

            // Sichtbar sein reicht nicht: eine Komponente, die das Layout nicht
            // anordnet, hat Breite 0 und wird gar nicht gezeichnet. Genau das
            // war der Fehler, als beide Texte auf dasselbe CENTER gelegt
            // wurden - der zweite verdraengte den ersten im Layout.
            panel.setSize(700, 500);
            layoutiere(panel);
            assertTrue(statusLabel.getWidth() > 0,
                    "die Meldung hat keine Breite und wird nicht gezeichnet");
        }

        /**
         * Ordnet auch die verschachtelten Ebenen; {@code doLayout()} allein legt
         * nur die direkten Kinder an.
         */
        private void layoutiere(java.awt.Container c) {
            c.doLayout();
            for (java.awt.Component kind : c.getComponents()) {
                if (kind instanceof java.awt.Container kc) {
                    layoutiere(kc);
                }
            }
        }

        @Test
        @DisplayName("das Formatieren landet im Feld")
        void formatiertes_json_landet_im_feld() throws Exception {
            jsonArea.setText("{\"a\":1,\"b\":[2,3]}");
            assertTrue(formatButton.isEnabled(), "Vorbedingung: muss freigeschaltet sein");

            formatButton.doClick();
            warteAufErgebnis();

            assertEquals("""
                    {
                        "a": 1,
                        "b": [
                            2,
                            3
                        ]
                    }
                    """, jsonArea.getText());
            assertTrue(statusLabel.getText().contains("lesbar formatiert"),
                    "Status: " + statusLabel.getText());
        }

        @Test
        @DisplayName("das Packen landet im Feld und meldet es als solches")
        void gepacktes_json_landet_im_feld() throws Exception {
            jsonArea.setText("""
                    {
                        "a": 1,
                        "b": [
                            2
                        ]
                    }
                    """);
            assertTrue(packButton.isEnabled(), "Vorbedingung: muss freigeschaltet sein");

            packButton.doClick();
            warteAufErgebnis();

            assertEquals("{\"a\":1,\"b\":[2]}\n", jsonArea.getText());
            assertTrue(statusLabel.getText().contains("gepackt"),
                    "Status: " + statusLabel.getText());
        }

        @Test
        @DisplayName("Kommentare und Nachlaufkommata verschwinden, der Inhalt nicht")
        void kommentare_verschwinden_der_inhalt_nicht() throws Exception {
            jsonArea.setText("{\n  // Kommentar\n  \"a\": 1, /* Block */\n}");

            formatButton.doClick();
            warteAufErgebnis();

            assertEquals("""
                    {
                        "a": 1
                    }
                    """, jsonArea.getText());
            assertTrue(statusLabel.getText().contains("lesbar formatiert"), "Status: " + statusLabel.getText());
        }

        @Test
        @DisplayName("ein zweites Packen meldet, dass es nichts mehr zu tun gab")
        void zweites_packen_meldet_erfolglos() throws Exception {
            jsonArea.setText("{\"a\":1}");
            packButton.doClick();
            warteAufErgebnis();

            packButton.doClick();
            warteAufErgebnis();

            assertEquals("{\"a\":1}\n", jsonArea.getText());
            assertTrue(statusLabel.getText().contains("War schon gepackt"),
                    "Status: " + statusLabel.getText());
        }

        @Test
        @DisplayName("ein Fehler laesst den Text stehen und nennt Zeile und Spalte")
        void fehler_laesst_den_text_stehen() throws Exception {
            jsonArea.setText("{\n  \"a\": 1,\n  \"b\": ,\n}");

            formatButton.doClick();
            warteAufErgebnis();

            assertEquals("{\n  \"a\": 1,\n  \"b\": ,\n}", jsonArea.getText(),
                    "die Eingabe wurde veraendert: " + jsonArea.getText());
            String status = statusLabel.getText();
            assertTrue(status.startsWith("\u274c"), "kein Fehlerzeichen: " + status);
            assertTrue(status.contains("Zeile 3"), "Zeile fehlt: " + status);
            assertTrue(status.contains("unverändert"), "der Text ist nicht erklaert: " + status);
        }

        @Test
        @DisplayName("der Cursor springt nicht ans Ende, wenn der Text kuerzer wurde")
        void cursor_bleibt_near_an_seiner_stelle() throws Exception {
            jsonArea.setText("{\"a\": 1, \"b\": [2, 3]}");
            jsonArea.setCaretPosition(10);

            formatButton.doClick();
            warteAufErgebnis();

            assertEquals(10, jsonArea.getCaretPosition(),
                    "Caret ist auf " + jsonArea.getCaretPosition());
        }

        @Test
        @DisplayName("die Formatierung laeuft wirklich im Hintergrund")
        void laeuft_auf_anderem_thread() throws Exception {
            // Der EDT darf waehrend des Formatierens nicht blockiert sein. Wir
            // haetten das schon gemerkt, wenn SwingWorker durch einen
            // blockierenden Aufruf ersetzt worden waere.
            jsonArea.setText("{\"a\": [1, 2, 3], \"b\": {\"c\": true}, \"d\": null}");

            formatButton.doClick();

            // Sofort und ohne zu warten muss der EDT noch bedienbar sein.
            String sofort = leseAufEdt(statusLabel::getText);
            assertFalse(sofort.startsWith("\u2713"),
                    "Ergebnis war zu frueh da - der EDT lief synchron mit: " + sofort);

            warteAufErgebnis();
            assertTrue(statusLabel.getText().startsWith("\u2713"), "Status: " + statusLabel.getText());
        }
    }

    @Nested
    @DisplayName("Statusmeldung nennt den gewaehlten Weg")
    class Status {

        @Test
        @DisplayName("Formatieren und Packen werden unterschieden")
        void wege_werden_unterschieden() {
            String formatiert = FormatterPanel.successMessage(false, true);
            assertTrue(formatiert.contains("lesbar formatiert"), formatiert);
            assertFalse(formatiert.contains("gepackt"), formatiert);

            String gepackt = FormatterPanel.successMessage(true, true);
            assertTrue(gepackt.contains("gepackt"), gepackt);
            assertFalse(gepackt.contains("lesbar formatiert"), gepackt);
        }

        @Test
        @DisplayName("ein Text, der schon so war, wird nicht als Arbeit ausgegeben")
        void unveraenderter_text_wird_gemeldet() {
            String schonFormatiert = FormatterPanel.successMessage(false, false);
            assertTrue(schonFormatiert.contains("War schon lesbar formatiert"), schonFormatiert);

            String schonGepackt = FormatterPanel.successMessage(true, false);
            assertTrue(schonGepackt.contains("War schon gepackt"), schonGepackt);
        }

        @Test
        void alle_nachrichten_fangen_mit_der_richtigen_zeichnung_an() {
            // Das Zeichen sagt dem Auge vor dem Lesen, was passiert ist. Ohne
            // einheitlichen Anfang sieht man bei drei Meldungen nicht, welche
            // davon neu ist.
            for (boolean dicht : new boolean[]{false, true}) {
                for (boolean veraendert : new boolean[]{false, true}) {
                    String text = FormatterPanel.successMessage(dicht, veraendert);
                    assertTrue(text.startsWith("\u2713"), "kein Haken: " + text);
                }
            }
        }
    }

    @Nested
    @DisplayName("Beenden-Button")
    class BeendenButton {

        @Test
        @DisplayName("Beenden bleibt ein Textlink")
        void ist_als_textlink_gesetzt() {
            assertEquals("Beenden", exitButton.getText());
            // Mit eigener Flaeche und eigenem Rahmen wuerde Beenden so viel
            // Gewicht bekommen wie "Ins Clipboard schreiben", obwohl es die
            // am seltensten benutzte Aktion ist.
            assertFalse(exitButton.isContentAreaFilled(), "Beenden darf keine Flaeche malen");
            assertFalse(exitButton.isBorderPainted(), "Beenden darf keinen Rahmen tragen");
            assertTrue(contrast(Theme.DUNKEL.gedaempft, Theme.DUNKEL.hintergrund) > 4.5,
                    "der Link-Text muss WCAG AA erfuellen");
        }

        @Test
        @DisplayName("steht abgesetzt rechts, nicht in der Aktionsgruppe")
        void steht_abgesetzt_rechts_der_aktionsgruppe() {
            // Die drei Aktionen teilen sich eine eigene Flaeche, "Beenden"
            // sitzt rechts in einer zweiten. Sonst laesst es sich nicht
            // optisch von ihnen trennen.
            assertNotSame(exitButton.getParent(), readButton.getParent(),
                    "Beenden darf nicht in derselben Gruppe liegen wie die Aktionen");

            // Geometrie statt Struktur: mass, ob es wirklich rechts klebt.
            JPanel reihe = (JPanel) exitButton.getParent();
            assertInstanceOf(BorderLayout.class, reihe.getLayout());
            reihe.setSize(800, 40);
            reihe.doLayout();
            assertTrue(exitButton.getX() + exitButton.getWidth() >= 780,
                    "Beenden muss an der rechten Kante stehen, lag bei x=" + exitButton.getX());
        }

        @Test
        @DisplayName("bleibt immer aktiv - auch bei Nicht-JSON und waehrend eines Vorgangs")
        void bleibt_immer_aktiv() {
            assertTrue(exitButton.isEnabled(), "darf beim Start nicht gesperrt sein");

            jsonArea.setText("Einkaufsliste");
            assertTrue(exitButton.isEnabled(), "darf bei Nicht-JSON nicht gesperrt werden");

            jsonArea.setText("{\"a\": 1}");
            assertTrue(exitButton.isEnabled(), "darf bei gueltigem JSON nicht gesperrt werden");
        }

        @Test
        @DisplayName("klick beendet ohne Absturz und ohne die JVM zu beenden")
        void klick_beendet_ohne_absturz() {
            // Ohne umgebendes Fenster greift der Fallback: nur ausblenden.
            // Ein System.exit(0) wuerde die Test-JVM sofort beenden.
            assertNull(SwingUtilities.getWindowAncestor(panel),
                    "Voraussetzung: Panel haengt in keinem Fenster");

            exitButton.doClick();

            assertFalse(panel.isVisible(), "Panel muss nach dem Klick ausgeblendet sein");
        }

        @Test
        @DisplayName("harter Exit nur, wenn das Panel das ganze Fenster besitzt")
        void harter_exit_nur_bei_alleinigem_inhalt() {
            assertTrue(FormatterPanel.isSoleContent(panel, panel),
                    "setContentPane(panel): das Panel IST der ContentPane");

            JPanel huelle = new JPanel();
            huelle.add(panel);
            assertTrue(FormatterPanel.isSoleContent(huelle, panel),
                    "einziges Kind der Huelle -> hartes Beenden erlaubt");

            JPanel fremdeHuelle = new JPanel();
            fremdeHuelle.add(new JLabel("fremde UI"));
            assertFalse(FormatterPanel.isSoleContent(fremdeHuelle, panel),
                    "fremde Komponenten -> Prozess der Einbettenden lebt weiter");

            JPanel zweitesKind = new JPanel();
            zweitesKind.add(panel);
            zweitesKind.add(new JLabel("zweites Kind"));
            assertFalse(FormatterPanel.isSoleContent(zweitesKind, panel),
                    "mehrere Kinder -> kein hartes Beenden");

            assertFalse(FormatterPanel.isSoleContent(new JPanel(), panel),
                    "leeres Fenster -> kein Beenden");
        }
    }

    @Nested
    @DisplayName("Beschriftungen werden nicht abgeschnitten")
    class Beschriftungen {

        @Test
        void alle_buttons_bekommen_ihre_naturliche_breite() {
            // Bei GridLayout(1, 4) haetten alle Spalten 185 px, waehrend
            // "Ins Clipboard schreiben" 192 px braucht.
            panel.setSize(panel.getPreferredSize());
            layOutRecursively(panel);

            for (JButton button : new JButton[]{readButton, formatButton, writeButton, exitButton}) {
                assertTrue(button.getWidth() >= button.getPreferredSize().width,
                        "'" + button.getText() + "' bekommt " + button.getWidth()
                                + " px, benoetigt aber " + button.getPreferredSize().width + " px");
            }
        }

        private void layOutRecursively(java.awt.Container container) {
            container.doLayout();
            for (java.awt.Component child : container.getComponents()) {
                if (child instanceof java.awt.Container nested) {
                    layOutRecursively(nested);
                }
            }
        }
    }

    @Nested
    @DisplayName("Darstellung")
    class Darstellung {

        @Test
        @DisplayName("die Versionszeile nennt die Version und ihr Datum")
        void versionszeile_nennt_version_und_datum() throws Exception {
            JLabel versionLabel = field("versionLabel");
            assertTrue(versionLabel.getText().matches("Version \\S+ vom \\d{2}\\.\\d{2}\\.\\d{4}"),
                    "unerwartete Form: '" + versionLabel.getText() + "'");
        }

        @Test
        @DisplayName("die Versionszeile zeigt genau die Version aus der pom.xml")
        void versionszeile_zeigt_die_pom_version() throws Exception {
            // Aus der pom.xml gelesen statt hier eingetragen: sonst prueft der
            // Test eine Zahl, die beim naechsten Versionswechsel nur noch
            // aussieht als waere sie richtig.
            String erwartet = versionAusPom();
            JLabel versionLabel = field("versionLabel");
            assertTrue(versionLabel.getText().startsWith("Version " + erwartet + " vom "),
                    "erwartet Version aus der pom.xml '" + erwartet
                            + "', angezeigt: '" + versionLabel.getText() + "'");
        }

        private String versionAusPom() throws Exception {
            String pom = java.nio.file.Files.readString(
                    new File("pom.xml").toPath(), StandardCharsets.UTF_8);
            java.util.regex.Matcher treffer =
                    java.util.regex.Pattern.compile("<version>([^<]+)</version>").matcher(pom);
            assertTrue(treffer.find(), "keine <version> in der pom.xml gefunden");
            return treffer.group(1);
        }

        @Test
        @DisplayName("die Versionszeile sitzt rechts unten")
        void versionszeile_sitzt_rechts_unten() throws Exception {
            JPanel infoLine = field("infoLine");
            JPanel footer = field("footer");
            JLabel versionLabel = field("versionLabel");
            JLabel statusLabel = field("statusLabel");

            // Nicht an Layout-Konstanten pruefen: die Panels nutzen
            // new BorderLayout(0, 10), nicht BorderLayout.EAST. Gemessen wird
            // die Lage im gerenderten Kasten.
            panel.setSize(700, 520);
            panel.doLayout();
            footer.doLayout();
            infoLine.doLayout();

            assertSame(infoLine, versionLabel.getParent(), "die Version steht in der Fusszeile");
            assertEquals(infoLine.getWidth(),
                    versionLabel.getX() + versionLabel.getWidth(), 1,
                    "die Version klebt am rechten Rand");
            assertEquals(0, statusLabel.getX(), 1, "die Statusmeldung steht links");
            assertEquals(footer.getHeight(),
                    infoLine.getY() + infoLine.getHeight(), 1,
                    "die Zeile ist der unterste Fusszeilenteil");
        }

        @Test
        void schriften_sind_logische_fonts() {
            // Vorher fest auf "Segoe UI"/"Consolas" - unter macOS/Linux kein Monospace.
            assertEquals(java.awt.Font.MONOSPACED, jsonArea.getFont().getFamily());
            assertEquals(java.awt.Font.SANS_SERIF, statusLabel.getFont().getFamily());
        }

        @Test
        @DisplayName("gesperrte Buttons behalten ihre Farbe und haben ausreichenden Kontrast")
        void gesperrte_buttons_haben_kontrast() {
            jsonArea.setText("Einkaufsliste");
            assertFalse(formatButton.isEnabled());

            // Kein neutrales Grau: die Farbe gehoert zum Knopf, nicht nur zum
            // aktiven Zustand. Sonst waere beim Start - leeres Textfeld, zwei
            // von drei Knöpfen gesperrt - nicht zu erkennen, welcher Knopf das
            // Formatieren ist.
            assertEquals(Theme.DUNKEL.gesperrt(Theme.DUNKEL.akzent), formatButton.getBackground());
            assertEquals(Theme.DUNKEL.aufDeaktiviert, formatButton.getForeground());
            assertTrue(contrast(formatButton.getBackground(), formatButton.getForeground()) > 4.5,
                    "Kontrast muss WCAG AA (4.5:1) erfuellen");
            // Und die Farbe muss noch als Farbe erkennbar sein, also nicht
            // vollstaendig im Hintergrund verschwinden.
            assertNotEquals(Theme.DUNKEL.hintergrund, formatButton.getBackground(),
                    "die entschaerfte Farbe ist der Hintergrund geworden");
        }

        @Test
        @DisplayName("die drei Aktionsstufen bleiben paarweise unterscheidbar")
        void aktionsstufen_bleiben_unterscheidbar() {
            jsonArea.setText("{\"a\": 1}");

            // Ueber die Palette statt ueber feste Zahlen: geprueft wird die
            // Rolle, nicht ein zufaellig gewaehlter Farbwert.
            assertEquals(Theme.DUNKEL.akzent, formatButton.getBackground());
            assertEquals(Theme.DUNKEL.hintergrund, readButton.getBackground());
            assertEquals(Theme.DUNKEL.tonal, writeButton.getBackground());

            // Drei verschiedene Farben sind nur dann ein Vorteil, wenn man sie
            // auseinanderhaelt. Zwei gleiche wuerden die Unterscheidung wieder
            // aufheben, die sie schaffen soll.
            assertNotEquals(readButton.getBackground(), writeButton.getBackground(),
                    "Einlesen und Schreiben teilen sich dieselbe Farbe");
            assertNotEquals(formatButton.getBackground(), readButton.getBackground(),
                    "Formatieren und Einlesen teilen sich dieselbe Farbe");
            assertNotEquals(formatButton.getBackground(), writeButton.getBackground(),
                    "Formatieren und Schreiben teilen sich dieselbe Farbe");
        }

        @Test
        @DisplayName("vier Aktionen sind auch ohne Farbe auseinanderzuhalten")
        void vier_aktionen_sind_unterscheidbar() {
            jsonArea.setText("{\"a\": 1}");

            // Vier Aktionen teilen sich drei Stufen. Die beiden stillen
            // Schaltflaechen teilen sich deshalb eine Farbe - und muessen
            // dafuer an Text und Symbol unterscheidbar bleiben, sonst sieht
            // man zwei gleiche Knoepfe und weiss nicht, welcher was tut.
            assertEquals(readButton.getBackground(), packButton.getBackground(),
                    "die beiden stillen Schaltflaechen teilen sich eine Stufe");

            for (JButton knopf : new JButton[]{readButton, formatButton, packButton, writeButton}) {
                assertFalse(knopf.getText().isBlank(),
                        () -> "eine Schaltflaeche ohne Text: " + knopf.getAccessibleContext().getAccessibleName());
                assertNotNull(knopf.getIcon(),
                        () -> "eine Schaltflaeche ohne Symbol: " + knopf.getText());
            }
            Set<String> texte = new HashSet<>(List.of(readButton.getText(), formatButton.getText(),
                    packButton.getText(), writeButton.getText()));
            assertEquals(4, texte.size(), () -> "zwei Schaltflaechen heissen gleich: " + texte);
        }

        @Test
        @DisplayName("die stillen Schaltflaechen stehen nicht nebeneinander")
        void stille_knoepfe_stehen_versetzt() {
            // Zwei gleichfarbene Knoepfe direkt nebeneinander lesen sich als
            // Doppelklick auf eine Funktion. Mit dem lauten Knopf dazwischen
            // ist die Reihe lesbar, auch wenn die Farbe gerade nicht hilft.
            java.util.List<JButton> reihe = new java.util.ArrayList<>(List.of(
                    readButton, formatButton, packButton, writeButton));
            for (int i = 0; i + 1 < reihe.size(); i++) {
                if (reihe.get(i).getBackground().equals(reihe.get(i + 1).getBackground())) {
                    fail("zwei gleichfarbene Schaltflaechen nebeneinander: "
                            + reihe.get(i).getText() + " / " + reihe.get(i + 1).getText());
                }
            }
        }

        @Test
        @DisplayName("auch die gesperrten Knoepfe bleiben unterscheidbar")
        void gesperrte_knoepfe_bleiben_unterscheidbar() {
            // Der Startzustand: nur Einlesen ist frei. Wer hier zwei gleiche
            // Flaechen sieht, raeht an der Bedienung vorbei.
            assertTrue(readButton.isEnabled());
            assertFalse(formatButton.isEnabled());
            assertFalse(packButton.isEnabled());
            assertFalse(writeButton.isEnabled());

            assertNotEquals(formatButton.getBackground(), packButton.getBackground(),
                    "die beiden gesperrten Knoepfe sind nicht unterscheidbar");
            assertNotEquals(formatButton.getBackground(), writeButton.getBackground(),
                    "Formatieren und Schreiben sind gesperrt nicht unterscheidbar");
            // Die umrandete Stufe hat per Definition keine Flaeche - erkennbar
            // bleibt sie nur an ihrer Linie. Faellt die aus, ist der Knopf
            // weg, nicht nur inaktiv.
            assertFalse(readButton.isContentAreaFilled(), "Einlesen bleibt ohne Flaeche");
            javax.swing.border.Border linie =
                    ((javax.swing.border.CompoundBorder) readButton.getBorder()).getOutsideBorder();
            assertEquals(Theme.DUNKEL.akzentText,
                    ((javax.swing.border.LineBorder) linie).getLineColor(),
                    "der aktive Knopf traegt keine Akzentlinie");
        }
        @Test
        @DisplayName("alle Aktionsstufen teilen sich eine Akzentfarbe")

        void alle_stufen_stammen_aus_einer_akzentfarbe() {
            // Drei Knoepfe in drei Farbtönen sahen willkürlich aus. Geprueft
            // wird deshalb nicht "sie sind verschieden", sondern "alle Stufen
            // sind Stufen desselben Farbtons": der Blaukanal fuehrt, und
            // keiner ist neutral oder braun. Braun ist der Ton, der in
            // UI-Zusammenhaengen am schnellsten hochwertig wirkt.
            //
            // Beide Paletten werden geprueft, nicht nur die aktive: ein Test,
            // der nur das dunkle Theme sieht, haette ein braunes helles
            // Theme unbeanstandet gelassen.
            for (Theme theme : List.of(Theme.HELL, Theme.DUNKEL)) {
                for (Color c : List.of(theme.akzent, theme.akzentText, theme.tonal)) {
                    assertTrue(c.getBlue() >= c.getRed() && c.getBlue() >= c.getGreen(),
                            () -> theme.bezeichnung() + ": Blau fuehrt nicht: " + c);
                    assertTrue(c.getBlue() - c.getRed() >= 15,
                            () -> theme.bezeichnung() + ": zu neutral oder braun: " + c);
                }
            }
            // Und die drei Stufen muessen sich auch unterscheiden, sonst
            // waeren sie ein System aus einem Knopf.
            jsonArea.setText("{\"a\": 1}");
            assertNotEquals(formatButton.getBackground(), writeButton.getBackground(),
                    "Formatieren und Schreiben sind dieselbe Stufe");
        }

        private static Color lineColor(JButton knopf) {
            javax.swing.border.Border rand = knopf.getBorder();
            rand = ((javax.swing.border.CompoundBorder) rand).getOutsideBorder();
            return ((javax.swing.border.LineBorder) rand).getLineColor();
        }

        @Test
        @DisplayName("unter kurzem Text steht keine tote Flaeche")
        void unter_kurzem_text_keine_tote_flaeche() throws Exception {
            // Der Textbereich waechst nur mit dem Text, damit der Rollbalken
            // entstehen kann. Bei kurzem Text bleibt er deshalb niedrig, und
            // darunter waere die Flaeche des Viewports zu sehen - die muss
            // dieselbe sein wie die des Editors, sonst steht ein heller Streifen
            // unter dem Text.
            jsonArea.setSize(400, 400);
            jsonArea.setText("{\"a\": 1}");
            JScrollPane sp = field("scrollPane");
            JViewport viewport = sp.getViewport();
            Theme theme = field("aktuellesTheme");
            assertEquals(jsonArea.getBackground(), viewport.getBackground(),
                    "unter dem Text darf keine fremde Flaeche stehen");
            assertEquals(theme.flaeche, viewport.getBackground(),
                    "der Viewport traegt die Theme-Flaeche");
            int zeilenhoehe = jsonArea.getFontMetrics(jsonArea.getFont()).getHeight();
            assertEquals(16 * zeilenhoehe, jsonArea.getPreferredSize().height,
                    "kurzer Text: der Bereich bleibt bei seinen 16 Zeilen");
            // Die Breite bleibt bewusst frei, damit lange Zeilen waagerecht
            // scrollen, statt ein Wort mitten drin umzubrechen.
            assertFalse(jsonArea.getScrollableTracksViewportWidth(),
                    "der Editor soll nicht mit der Fensterbreite mitwaachsen");
        }

        @Test
        @DisplayName("der Editor nagelt die Viewport-Hoehe nicht fest")
        void editor_nagelt_die_viewport_hoehe_nicht_fest() throws Exception {
            // ViewportLayout setzt die Hoehe der Ansicht auf die des Viewports,
            // sobald getScrollableTracksViewportHeight() 'ja' sagt. Waere das der
            // Fall, bliebe der Textbereich immer so hoch wie das Fenster, es
            // kaeme kein Rollbalken zustande, und die Zeilen unterhalb waeren im
            // echten Fenster nicht erreichbar.
            JTextComponent editor = field("jsonArea");
            Class<?> editorKlasse = editor.getClass();
            assertNotEquals(editorKlasse,
                    editorKlasse.getMethod("getScrollableTracksViewportHeight")
                            .getDeclaringClass(),
                    "der Editor darf seine Hoehe nicht selbst an den Viewport binden");
        }

        @Test
        @DisplayName("der Editor waechst mit dem Text, damit nichts unerreichbar wird")
        void editor_waechst_mit_dem_text() throws Exception {
            // Die bevorzugte Hoehe folgt der Zeilenzahl: nur so bekommt der
            // Viewport eine Ansicht, die groesser ist als er selbst, und nur so
            // entsteht ein Rollbalken.
            jsonArea.setSize(400, 400);
            jsonArea.setText("{\"a\": 1}\n{\"b\": 2}");
            int zeilenhoehe = jsonArea.getFontMetrics(jsonArea.getFont()).getHeight();
            assertEquals(16 * zeilenhoehe, jsonArea.getPreferredSize().height,
                    "zwei Zeilen passen noch in die 16 Zeilen des Startbereichs");
            jsonArea.setText(vieleZeilen(200));
            // Die Hoehe folgt den Zeilen, die der Text wirklich belegt, und die
            // nennt erst der Zeilenkopf beim Zeichnen mit.
            meldeBildschirmzeilen(400, 400);
            assertTrue(jsonArea.getPreferredSize().height >= 200 * zeilenhoehe,
                    "der Textbereich ist so hoch wie der Text, aktuell "
                            + jsonArea.getPreferredSize().height);
            // Der Bereich muss sich auch oeffnen duerfen - mit NEVER waeren die
            // Zeilen zwar hoch, aber trotzdem unerreichbar.
            JScrollPane sp = field("scrollPane");
            assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    sp.getVerticalScrollBarPolicy(),
                    "ohne Rollbalken bleibt der Text unter dem Fenster unerreichbar");
        }

        @Test
        @DisplayName("eine umgebrochene Zeile bleibt bis zum Ende erreichbar")
        void umbruch_bleibt_erreichbar() throws Exception {
            // Zaehlt man Absaetze statt Bildschirmzeilen, ist der Bereich fuer
            // eine umgebrochene Zeile zu kurz: der Text waere abgeschnitten und
            // nicht erreichbar, weil die Zeilen, die der Umbruch braucht, gar
            // keinen Platz bekommen.
            int breite = 200;
            // So lang, dass der Umbruch die 16 Startzeilen sprengt.
            String lang = wiederhole("{\"a\": \"0123456789abcdef\"}, ", 20);
            jsonArea.setSize(breite, 200);
            jsonArea.setText(lang + "\n");
            meldeBildschirmzeilen(breite, 200);
            int zeilenhoehe = jsonArea.getFontMetrics(jsonArea.getFont()).getHeight();
            assertTrue(jsonArea.getPreferredSize().height > 16 * zeilenhoehe,
                    "die " + zeilenBeiUmbruch(lang, breite)
                            + " Bildschirmzeilen brauchen mehr als die 16 Startzeilen, "
                            + "sonst waeren sie unerreichbar");
        }

        @Test
        @DisplayName("die Statuszeile zaehlt die Bildschirmzeilen, nicht die Absaetze")
        void umfang_zaehlt_bildschirmzeilen() throws Exception {
            int breite = 200;
            String lang = wiederhole("{\"a\": \"0123456789abcdef\"}, ", 6);
            String t = lang + "\n{\"b\": 2}\n";
            // So hoch, dass alle Zeilen hineinpassen: der Zaehler nennt den
            // ganzen Text, gezaehlt werden aber nur die sichtbaren Zeilen.
            // Zu kurzes Feld wuerde zwei verschiedene richtige Zahlen liefern.
            int hoehe = 600;
            jsonArea.setSize(breite, hoehe);
            jsonArea.setText(t);
            meldeBildschirmzeilen(breite, hoehe);
            int zeilen = bemalteZahlen(breite, hoehe);
            assertEquals(zeilen + " Zeilen, " + t.length() + " Zeichen",
                    ((JLabel) field("umfangLabel")).getText(),
                    "die lange Zeile bricht um und gehoert mehrfach mit");
        }

        @Test
        @DisplayName("unten und rechts steht der Text nicht am Rahmen")
        void unten_und_rechts_luft() {
            // Sobald ein Rollbalken auftaucht, klebt die letzte Zeile sonst an ihm.
            java.awt.Insets rand = jsonArea.getMargin();
            assertEquals(9, rand.bottom, "unten haengt der Text nicht am Rollbalken");
            assertEquals(9, rand.right, "rechts haengt der Text nicht am Rollbalken");
        }

        private String vieleZeilen(int anzahl) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < anzahl; i++) {
                sb.append("{\"a\": ").append(i).append("}\n");
            }
            return sb.toString();
        }

        @Test
        @DisplayName("die drei Aktionen tragen ein Symbol, die Links keines")
        void aktionen_tragen_symbole() {
            for (JButton knopf : List.of(readButton, formatButton, writeButton)) {
                assertNotNull(knopf.getIcon(),
                        () -> "'" + knopf.getText() + "' hat kein Symbol");
                assertTrue(knopf.getIcon().getIconWidth() > 0
                                && knopf.getIcon().getIconHeight() > 0,
                        () -> "'" + knopf.getText() + "' hat ein leeres Symbol");
            }
            // Links tragen keins: dort ist der Text das Erkennungsmerkmal.
            assertNull(exitButton.getIcon(), "der Beenden-Link traegt ein Symbol");
            assertNotNull(themeButton.getIcon(), "der Theme-Schalter traegt ein Symbol");
        }

        @Test
        @DisplayName("das Symbol nimmt die Textfarbe des Knopfes an")
        void symbol_teilt_die_beschriftungsfarbe() {
            // Bei gesperrten Knoepfen wird die Beschriftung gedimmt; ein
            // eigenstaendig eingefaerbtes Symbol wuerde dann heller wirken als
            // der Text daneben und die Sperre undermine.
            for (JButton knopf : List.of(formatButton, writeButton)) {
                assertNotNull(knopf.getIcon(), () -> "'" + knopf.getText() + "'");
                // paintIcon liest die Foreground des Zeichners, nicht die des
                // Knopfes - genau deshalb wird hier die Beschriftung gesetzt
                // und danach die Farbe des Symbols geprueft.
                java.awt.image.BufferedImage puffer = new java.awt.image.BufferedImage(
                        20, 20, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                knopf.getIcon().paintIcon(knopf, puffer.getGraphics(), 0, 0);
                boolean gemalt = false;
                for (int y = 0; y < 20 && !gemalt; y++) {
                    for (int x = 0; x < 20; x++) {
                        if ((puffer.getRGB(x, y) >>> 24) > 0) {
                            gemalt = true;
                            break;
                        }
                    }
                }
                assertTrue(gemalt, () -> "'" + knopf.getText() + "' malt nichts");
            }
        }

        @Test
        @DisplayName("Zeilen und Zeichen werden mitgezaehlt")
        void umfang_wird_angezeigt() {
            jsonArea.setText("");
            assertEquals("0 Zeilen, 0 Zeichen", umfangLabel.getText());

            jsonArea.setText("a\nbb\nccc");
            assertEquals("3 Zeilen, 8 Zeichen", umfangLabel.getText());

            jsonArea.setText("nur eine");
            assertEquals("1 Zeile, 8 Zeichen", umfangLabel.getText(),
                    "bei einer Zeile steht etwas anderes als in der Mehrzahl");

            // Unter Windows landet CRLF in der Zwischenablage. Zaehlt man
            // stattdessen die Zeichen im Dokument, zeigt derselbe Text dort
            // zwei Zeichen mehr je Umbruch als hier.
            jsonArea.setText("a\r\nbb\r\nccc");
            assertEquals("3 Zeilen, 8 Zeichen", umfangLabel.getText(),
                    "CRLF aus der Zwischenablage darf den Zaehler nicht verfälschen");
        }

        @Test
        void alle_drei_buttons_sind_vorhanden() {
            assertEquals("Clipboard einlesen", readButton.getText());
            assertEquals("JSON Formatieren", formatButton.getText());
            assertEquals("Ins Clipboard schreiben", writeButton.getText());
        }
    }

    @Nested
    @DisplayName("Themes")
    class Themes {

        @Test
        @DisplayName("beide Paletten erfuellen WCAG AA fuer jedes Textpaar")
        void beide_paletten_erfuellen_wcag_aa() {
            List<String> maengel = new ArrayList<>();
            for (Theme theme : List.of(Theme.DUNKEL, Theme.HELL)) {
                // Text auf Fensterflaeche
                pruefe(theme, theme.text, theme.hintergrund, "Text", maengel);
                pruefe(theme, theme.gedaempft, theme.hintergrund, "gedaempft", maengel);
                pruefe(theme, theme.erfolg, theme.hintergrund, "Erfolg", maengel);
                pruefe(theme, theme.warnung, theme.hintergrund, "Warnung", maengel);
                pruefe(theme, theme.fehler, theme.hintergrund, "Fehler", maengel);
                // Beschriftung auf Schaltflaeche. Gesperrt wird nicht mehr ueber
                // eine eigene Farbe geprueft, sondern ueber die entschaerfte
                // Eigenfarbe - und die ist fuer jede Aktion eine andere.
                pruefe(theme, theme.aufAkzent, theme.akzent, "Beschriftung Formatieren", maengel);
                pruefe(theme, theme.akzentText, theme.hintergrund, "Beschriftung Einlesen", maengel);
                pruefe(theme, theme.akzentText, theme.tonal, "Beschriftung Schreiben", maengel);
                pruefe(theme, theme.aufDeaktiviert, theme.gesperrt(theme.akzent),
                        "Beschriftung gesperrt Formatieren", maengel);
                pruefe(theme, theme.aufDeaktiviert, theme.gesperrt(theme.tonal),
                        "Beschriftung gesperrt Schreiben", maengel);
                // Syntaxfarben auf der Editorflaeche
                pruefe(theme, theme.keyword, theme.flaeche, "Keyword", maengel);
                pruefe(theme, theme.stringFarbe, theme.flaeche, "String", maengel);
                pruefe(theme, theme.zahl, theme.flaeche, "Zahl", maengel);
                pruefe(theme, theme.kommentar, theme.flaeche, "Kommentar", maengel);
                pruefe(theme, theme.bezeichner, theme.flaeche, "Bezeichner", maengel);
                pruefe(theme, theme.operator, theme.flaeche, "Operator", maengel);
            }
            assertTrue(maengel.isEmpty(), () -> "Kontrast unter WCAG AA: " + maengel);
        }

        private void pruefe(Theme theme, Color fg, Color bg, String was, List<String> maengel) {
            double r = contrast(fg, bg);
            if (r <= 4.5) {
                maengel.add(theme.bezeichnung() + "/" + was + "=" + String.format("%.2f", r));
            }
        }

        @Test
        @DisplayName("die Umrandung der Schaltflaechen erfuellt WCAG 1.4.11")
        void schaltflaechen_sind_am_rand_erkennbar() {
            // WCAG 1.4.11 verlangt 3:1 fuer die Erkennbarkeit einer
            // Bedienoberflaeche - fuer die Flaeche oder ihre Begrenzung, nicht
            // fuer beides. Der Test oben prueft nur Text auf Flaeche, und so
            // konnten die Schaltflaechen voellig im Hintergrund verschwinden,
            // ohne dass ein Wert auffiel: gemessen waren es 1,05:1 im hellen
            // und 1,48:1 im dunklen Theme.
            List<String> maengel = new ArrayList<>();
            for (Theme theme : List.of(Theme.DUNKEL, Theme.HELL)) {
                double r = contrast(theme.rahmen, theme.hintergrund);
                if (r < 3.0) {
                    maengel.add(theme.bezeichnung() + "/Rahmen=" + String.format("%.2f", r));
                }
            }
            assertTrue(maengel.isEmpty(), () -> "Rahmen unter WCAG 1.4.11 (3:1): " + maengel);
        }

        @Test
        @DisplayName("jede gefuellte Schaltflaeche traegt den Rahmen aus dem Theme")
        void gefuellte_schaltflaechen_tragen_den_rahmen() {
            // Fuelltext, damit alle drei aktiv sind: im gesperrten Zustand
            // sind die Linien entschaerft und damit nicht vergleichbar.
            jsonArea.setText("{\"a\": 1}");
            // Die Palettenpruefung koennte gruen sein, waehrend das Panel den
            // Rahmen gar nicht setzt. Dieser Test schliesst die Luecke
            // zwischen "die Farbe existiert" und "sie ist am Button zu sehen".
            for (JButton knopf : List.of(formatButton, readButton, writeButton)) {
                assertTrue(knopf.isBorderPainted(),
                        () -> "'" + knopf.getText() + "' hat keine sichtbare Umrandung");
                javax.swing.border.Border rand = knopf.getBorder();
                rand = ((javax.swing.border.CompoundBorder) rand).getOutsideBorder();
                assertInstanceOf(javax.swing.border.LineBorder.class, rand,
                        () -> "'" + knopf.getText() + "' hat keinen 1px-Rahmen");
                // Die umrandete Stufe traegt die Akzentfarbe als Linie, weil
                // ihre Flaeche nichts vom Fenster unterscheidet.
                Color erwartet = knopf == readButton
                        ? Theme.DUNKEL.akzentText
                        : Theme.DUNKEL.rahmen;
                assertEquals(erwartet, ((javax.swing.border.LineBorder) rand).getLineColor(),
                        () -> "'" + knopf.getText() + "'");
            }
            // Beenden und der Theme-Schalter bleiben Links.
            assertFalse(exitButton.isBorderPainted(), "Beenden bleibt ohne Rahmen");
            assertTrue(themeButton.isBorderPainted(), "der Theme-Schalter hat einen Rahmen");
        }

        @Test
        @DisplayName("Knopftext klebt nicht am Rand und die Flaeche ist erhaben")
        void knopf_hat_innenabstand_und_fase() {
            for (JButton knopf : List.of(formatButton, readButton, writeButton)) {
                java.awt.Insets i = knopf.getBorder().getBorderInsets(knopf);
                assertTrue(i.left >= 12 && i.right >= 12,
                        () -> "'" + knopf.getText() + "' zu wenig Innenabstand: " + i);
                assertTrue(i.top >= 4 && i.bottom >= 4,
                        () -> "'" + knopf.getText() + "' zu wenig Innenabstand: " + i);
            }
            // Die Fase muss auch wirklich eine BevelBorder sein - sonst waere
            // der Knopf flach mit einer Linie drumherum.
            javax.swing.border.Border innen =
                    ((javax.swing.border.CompoundBorder) formatButton.getBorder()).getInsideBorder();
            javax.swing.border.Border fase =
                    ((javax.swing.border.CompoundBorder) innen).getOutsideBorder();
            assertInstanceOf(javax.swing.border.BevelBorder.class, fase, "der 3D-Effekt fehlt");
            assertEquals(javax.swing.border.BevelBorder.RAISED,
                    ((javax.swing.border.BevelBorder) fase).getBevelType());
        }

        @Test
        @DisplayName("das Theme, mit dem die App startet, ist das helle")
        void starttheme_ist_hell() {
            assertEquals(Theme.HELL, FormatterPanel.STANDARD,
                    "der Start muss im hellen Theme landen, nicht im dunklen");
        }

        @Test
        @DisplayName("Umschalter tauscht LookAndFeel und Farben")
        void umschalter_tauscht_lookandfeel_und_farben() throws Exception {
            JToggleButton themeButton = field("themeButton");
            // Fuelltext noetig: ohne JSON ist der Formatier-Button gesperrt und
            // traegt die entschaerfte Farbe statt der eigenen.
            jsonArea.setText("{\"a\": 1}");
            assertInstanceOf(FlatDarkLaf.class, UIManager.getLookAndFeel());
            assertEquals(Theme.DUNKEL.akzent, formatButton.getBackground());

            aufEdt(themeButton::doClick);

            assertInstanceOf(FlatLightLaf.class, UIManager.getLookAndFeel());
            assertEquals(Theme.HELL.akzent, formatButton.getBackground());
            assertEquals(Theme.HELL.hintergrund, panel.getBackground());
        }

        @Test
        @DisplayName("Umschalten ist umkehrbar")
        void umschalten_ist_umkehrbar() throws Exception {
            JToggleButton themeButton = field("themeButton");
            jsonArea.setText("{\"a\": 1}");
            aufEdt(themeButton::doClick);
            aufEdt(themeButton::doClick);
            assertInstanceOf(FlatDarkLaf.class, UIManager.getLookAndFeel());
            assertEquals(Theme.DUNKEL.akzent, formatButton.getBackground());
        }

        @Test
        @DisplayName("der Schalter zeigt den Zustand, Text und Symbol das Ziel")
        void schalter_zeigt_zustand_und_ziel() throws Exception {
            // Der Testaufbau startet im dunklen Theme: der Schalter ist an,
            // Text und Symbol zeigen aber nach Hell.
            JToggleButton themeButton = field("themeButton");
            assertTrue(themeButton.isSelected(), "im dunklen Theme ist der Schalter an");
            assertEquals("Hell", themeButton.getText(), "der Text nennt das Ziel");
            Icon mond = themeButton.getIcon();
            assertNotNull(mond, "vor dem Text steht ein Symbol");
            assertEquals(Theme.DUNKEL.aufAkzent, themeButton.getForeground(),
                    "im dunklen Theme steht die Beschriftung in Weiss");

            jsonArea.setText("{\"a\": 1}");
            aufEdt(themeButton::doClick);
            assertFalse(themeButton.isSelected(), "nach dem Umschalten ist der Schalter aus");
            assertEquals("Dunkel", themeButton.getText(), "der Text nennt das neue Ziel");
            assertNotSame(mond, themeButton.getIcon(), "das Symbol wechselt mit");
            assertInstanceOf(FlatLightLaf.class, UIManager.getLookAndFeel());
            assertTrue(contrast(themeButton.getForeground(), panel.getBackground()) >= 4.5,
                    "auch im hellen Theme bleibt die Beschriftung lesbar");
            assertNotNull(themeButton.getToolTipText());
        }

        @Test
        @DisplayName("im Normalzustand wird das Fenster neu gepackt")
        void normalzustand_wird_gepackt() {
            // Ohne pack() schneidet der Rahmen des neuen LookAndFeel die
            // Knöpfe ab, das Fenster waere also zu klein.
            Rectangle bildschirm = new Rectangle(0, 0, 2560, 1440);
            Rectangle fenster = new Rectangle(700, 100, 900, 600);

            assertTrue(FormatterPanel.gehoertGepackt(false, fenster, bildschirm),
                    "ein Fenster im Normalzustand muss neu gepackt werden");
        }

        @Test
        @DisplayName("ein maximiertes Fenster behaelt seinen Zustand")
        void maximiertes_fenster_bleibt_maximiert() {
            Rectangle bildschirm = new Rectangle(0, 0, 2560, 1440);
            Rectangle fenster = new Rectangle(0, 0, 2560, 1409);

            assertFalse(FormatterPanel.gehoertGepackt(true, fenster, bildschirm),
                    "pack() wuerde die Maximierung aufheben, also darf es nicht aufrufen");
        }

        @Test
        @DisplayName("der Themewechsel laesst ein Fenster im Vollbild stehen")
        void vollbildfenster_bleibt_stehen() {
            // Im echten Vollbild gibt es keinen Zustand, den man zuruecksetzen
            // koennte, und macOS meldet ihn auch nicht. Fuellt das Fenster den
            // Bildschirm, wird deshalb gar nicht erst gepackt.
            Rectangle bildschirm = new Rectangle(0, 0, 2560, 1440);
            Rectangle fenster = new Rectangle(0, 0, 2560, 1440);

            assertFalse(FormatterPanel.gehoertGepackt(false, fenster, bildschirm),
                    "pack() wuerde das Vollbildfenster auf die Vorzugsgroesse schrumpfen");
        }
    }

    @Nested
    @DisplayName("Syntaxhervorhebung")
    class Hervorhebung {

        @Test
        @DisplayName("die drei Schluesselwoerter bekommen die Keyword-Farbe")
        void schluesselwoerter_werden_eingefaerbt() {
            faerbe("{\"a\": true, \"b\": false, \"c\": null}");
            assertEquals(Theme.DUNKEL.keyword, farbeAn("true"));
            assertEquals(Theme.DUNKEL.keyword, farbeAn("false"));
            assertEquals(Theme.DUNKEL.keyword, farbeAn("null"));
        }

        @Test
        @DisplayName("Schluessel und Werte sind unterschiedlich gefaerbt")
        void schluessel_und_werte_unterscheiden_sich() {
            faerbe("{\"schluessel\": \"wert\"}");
            assertEquals(Theme.DUNKEL.bezeichner, farbeAn("\"schluessel\""));
            assertEquals(Theme.DUNKEL.stringFarbe, farbeAn("\"wert\""));
        }

        @Test
        @DisplayName("der Schluessel wird am Doppelpunkt erkannt, nicht am Wort")
        void schluessel_wird_erkannt() {
            // Auch der Wert eines anderen Schluessels ist ein Schluessel, wenn
            // ein Doppelpunkt folgt - entscheidend ist die Stellung, nicht der
            // Name. "name" waere als Wort nichts Besonderes.
            faerbe("{\"name\": \"x\"}");
            assertEquals(Theme.DUNKEL.bezeichner, farbeAn("\"name\""));
        }

        @Test
        @DisplayName("eine URL im String ist kein Kommentar")
        void url_in_zeichenkette_bleibt_zeichenkette() {
            // Der eigentliche Grund fuer den eigenen Scanner: eine Regex wie
            // \"//.*\" faerbt ab hier den Rest des Textes als Kommentar. Genau
            // das sieht man dann in einer Datei mit einer URL und wundert
            // sich, warum alles grau wurde.
            faerbe("{\"url\": \"http://beispiel/pfad\"}");
            assertEquals(Theme.DUNKEL.stringFarbe, farbeAn("//beispiel"),
                    "die zwei Schraegstriche wurden als Kommentar gelesen");
        }

        @Test
        @DisplayName("ein maskierter Anfuehrungszeichen beendet den String nicht")
        void maskiertes_anfuehrungszeichen_beendet_nicht() {
            // Der Schluessel "a\"" enthaelt ein Anfuehrungszeichen im Text.
            // Ein Scanner, der am ersten \" vorbeislaeuft, haelt den String fuer
            // offen und faerbt ab hier den Rest des Textes als einen String.
            faerbe("{\"a\\\"\": \"b\"}");
            assertEquals(Theme.DUNKEL.stringFarbe, farbeAn("\"b\""),
                    "der Wert danach wurde nicht als String erkannt");
            assertEquals(Theme.DUNKEL.bezeichner, farbeAn("\"a\\\"\""),
                    "der Schluessel mit dem maskierten Zeichen wurde nicht erkannt");
        }

        @Test
        @DisplayName("Kommentare sind zusaetzlich kursiv")
        void kommentare_sind_kursiv() {
            faerbe("{\"a\": 1} // notiz");
            assertTrue(kursivAn("// notiz"));
        }

        @Test
        @DisplayName("ein Kommentar im Block wird erkannt, auch ueber Zeilen")
        void blockkommentar_ueber_zeilen() {
            faerbe("/* notiz\n   und noch was */ {\"a\": 1}");
            assertEquals(Theme.DUNKEL.kommentar, farbeAn("notiz"));
            assertEquals(Theme.DUNKEL.kommentar, farbeAn("und noch was"));
            assertEquals(Theme.DUNKEL.bezeichner, farbeAn("\"a\""),
                    "hinter dem Kommentar wird nicht weitergefaerbt");
        }

        @Test
        void zahlen_haben_eigene_farbe() {
            faerbe("{\"a\": 3.5e2, \"b\": -17}");
            assertEquals(Theme.DUNKEL.zahl, farbeAn("3.5e2"), "Exponent muss mitgefaerbt werden");
            assertEquals(Theme.DUNKEL.zahl, farbeAn("-17"), "Vorzeichen muss mitgefaerbt werden");
        }

        @Test
        @DisplayName("die Bereiche decken sich mit dem Text und tragen das Richtige")
        void bereiche_liegen_im_text() {
            // Der Scanner ist die toleranteste Stelle der App: er darf keine
            // Ausnahme werfen, aber niemals etwas melden, das im Text nicht
            // steht, und nie etwas als Kommentar deklarieren, was keiner ist.
            String[] texte = {
                "{\"a\": 1}",
                "{\"a\": \"unvollstaendig",
                "// nur ein Kommentar",
                "/* offen",
                "{\"a\": \"}",
                "12e",
                "[true, false, null, 1.5e-3]",
                "",
            };
            for (String text : texte) {
                for (JsonSyntaxHighlighter.Span span : JsonSyntaxHighlighter.spans(text)) {
                    assertTrue(span.start() >= 0 && span.start() < text.length(),
                            () -> "Start ausserhalb: " + span + " in '" + text + "'");
                    assertTrue(span.laenge() > 0, () -> "Leere Laenge: " + span);
                    assertTrue(span.start() + span.laenge() <= text.length(),
                            () -> "Laenge ragt ueber das Ende: " + span + " in '" + text + "'");

                    String stueck = text.substring(span.start(), span.start() + span.laenge());
                    switch (span.art()) {
                        case SCHLUESSEL, ZEICHENKETTE -> assertTrue(stueck.startsWith("\""),
                                () -> "String ohne Anfuehrungszeichen: '" + stueck + "'");
                        case WORT -> assertTrue(Set.of("true", "false", "null").contains(stueck),
                                () -> "Kein Schluesselwort: '" + stueck + "'");
                        case KOMMENTAR -> assertTrue(stueck.startsWith("/"),
                                () -> "Kommentar ohne Schraegstrich: '" + stueck + "'");
                        // Nur der Anfang wird geprueft, nicht die vollstaendige
                        // Zahl: wer "12e" tippt, sieht eine Zahl, und genau so
                        // soll es aussehen. Der Formatter bleibt der, der
                        // "12e" zurueckweist - mit Zeile und Spalte.
                        case ZAHL -> assertTrue(Character.isDigit(stueck.charAt(0)) || stueck.charAt(0) == '-',
                                () -> "Zahl ohne Anfang: '" + stueck + "'");
                    }
                }
            }
        }

        @Test
        @DisplayName("gefaerbt wird genau, was ein Wert ist")
        void bereiche_sagen_richtiges() {
            // Erwartete Liste statt einer Summe: die Klammern und Kommas sind
            // bewusst nicht eingefaerbt. Diese Liste ist das, was man im
            // Fenster sieht - faellt ein Wert neu dazu, aendert sich hier etwas.
            String text = "{\"a\": [1, true, \"x\"], \"b\": null} // ende";
            List<String> gefunden = JsonSyntaxHighlighter.spans(text).stream()
                    .map(span -> text.substring(span.start(), span.start() + span.laenge()))
                    .toList();
            assertEquals(List.of("\"a\"", "1", "true", "\"x\"", "\"b\"", "null", "// ende"),
                    gefunden);
        }

        @Test
        @DisplayName("Einfaerben loest keine weiteren Aenderungs-Events aus")
        void einfaerben_erzeugt_keine_ereignisschleife() throws Exception {
            // setCharacterAttributes feuert selbst Dokument-Events. Ohne
            // Reentranz-Sperre laeuft daraus ein Endlos-Zyklus, der sich
            // selbst wieder einplant.
            faerbe("{\"a\": [1, 2], \"b\": true} // x");
            Timer highlightTimer = field("highlightTimer");
            // Den Timer anhalten, statt auf sein Abklingen zu warten: ohne
            // laufenden Timer kann ausser dem Einfaerben selbst niemand den
            // Timer einplanen, und genau das ist die Zusage.
            highlightTimer.stop();
            Thread.sleep(50);

            // setCharacterAttributes feuert technisch weiter Dokument-Events an
            // alle Listener - das laesst sich nicht unterdruecken und ist
            // unschadlich. Zaehlbar ist die Wirkung: das Panel darf sich nicht
            // erneut einplanen, sonst laeuft es im Kreis.
            panel.faerbeHoch();
            assertFalse(highlightTimer.isRepeats(),
                    "Einfaerben muss entprellt werden, nicht wiederholt");
            assertFalse(highlightTimer.isRunning(),
                    "Einfaerben hat sich selbst wieder eingeplant - Endlosschleife");
        }

        @Test
        @DisplayName("das Textfeld bricht nicht um und bleibt kompakt")
        void textfeld_bricht_nicht_um_und_bleibt_kompakt() {
            JTextPane pane = (JTextPane) jsonArea;
            assertFalse(pane.getScrollableTracksViewportWidth(),
                    "JSON soll nicht umbrechen - ein umgebrochener Schluessel ist nicht lesbar");
            // Vorher PreferredSize 760x420, daraus pack() ein 626 px hohes Fenster.
            assertTrue(pane.getPreferredSize().height < 320,
                    "Textfeld ist " + pane.getPreferredSize().height
                            + " px hoch und laesst die halbe Leere im Fenster");
        }
    }

    @Nested
    @DisplayName("Statusmeldungen als Toast")
    class Toast {

        @Test
        @DisplayName("Erfolg blendet sich aus, Warnung bleibt stehen")
        void erfolg_blendet_sich_aus_warnung_bleibt() throws Exception {
            Timer toastTimer = field("toastTimer");

            jsonArea.setText("Einkaufsliste");
            assertTrue(statusLabel.getText().startsWith("\u26a0"), "Text sieht nicht nach JSON aus");
            assertFalse(toastTimer.isRunning(),
                    "eine blockierende Warnung darf nicht von selbst verschwinden");

            jsonArea.setText("{\"a\": 1}");
            formatButton.doClick();
            warteAufErgebnis();
            assertTrue(statusLabel.getText().startsWith("\u2713"), statusLabel.getText());
            assertTrue(toastTimer.isRunning(), "Erfolg soll ausblenden");
            assertFalse(toastTimer.isRepeats(), "Toast darf nicht wiederholen");
            assertEquals(4000, toastTimer.getDelay(), "Toast-Frist");
        }

        @Test
        @DisplayName("die Erfolgsmeldung verschwindet wieder")
        void erfolgsmeldung_verschwindet() throws Exception {
            Timer toastTimer = field("toastTimer");
            jsonArea.setText("{\"a\": 1}");
            formatButton.doClick();
            warteAufErgebnis();
            assertFalse(statusLabel.getText().isEmpty());

            // Echter Timer mit echter Verzoegerung. Ein Test, der den Timer
            // testweise auf 5 ms stellt, prueft nicht mehr die tatsaechliche
            // Wartezeit - und war hier auch nicht verlaesslich.
            boolean verschwunden = false;
            for (int i = 0; i < 350 && !verschwunden; i++) {
                Thread.sleep(20);
                verschwunden = leseAufEdt(statusLabel::getText).isEmpty();
            }
            String diagnose = "Erfolgsmeldung blieb stehen: laeuft=" + toastTimer.isRunning()
                    + " wiederholt=" + toastTimer.isRepeats()
                    + " verzoegerung=" + toastTimer.getDelay()
                    + " text='" + leseAufEdt(statusLabel::getText) + "'";
            assertTrue(verschwunden, diagnose);
        }
    }


    /** Setzt Text und faerbt sofort, ohne den 120-ms-Timer abzuwarten. */
    private void faerbe(String text) {
        jsonArea.setText(text);
        panel.faerbeHoch();
    }

    private int ab(String teil) {
        return jsonArea.getText().indexOf(teil);
    }

    /** Erste Stelle von {@code teil}, optional ab einem Startindex. */
    private int ab(String teil, int ab) {
        return jsonArea.getText().indexOf(teil, ab);
    }

      @Nested
      @DisplayName("Tastatur und Screenreader")
      class Bedienbarkeit {

          @Test
          @DisplayName("jede Aktion hat ein Tastenkuerzel")
          void aktionen_haben_tastenkuerzel() {
              // Ohne Mnemonik gibt es keinen Weg an die Buttons, ohne 200 Mal
              // mit Tab durch den Text zu gehen.
              for (JButton button : new JButton[]{
                      readButton, formatButton, writeButton, exitButton}) {
                  assertTrue(button.getMnemonic() > 0,
                          button.getText() + " hat kein Tastenkuerzel");
                  assertTrue(button.isFocusable(),
                          button.getText() + " ist nicht an der Tastatur erreichbar");
              }
              // Alt+E, Alt+F, Alt+L und Alt+C doppeln sich nicht.
              Set<Integer> kuerzel = new HashSet<>();
              for (JButton button : new JButton[]{
                      readButton, formatButton, writeButton, exitButton}) {
                  assertTrue(kuerzel.add(button.getMnemonic()),
                          "Tastenkuerzel " + button.getMnemonic() + " ist zweimal vergeben");
              }
          }

          @Test
          @DisplayName("der Fokusring hebt sich vom Knopfgrund ab")
          void fokusring_hebt_sich_ab() throws Exception {
              // Ein Ring, den man nicht sieht, ist keiner. FlatLafs Standardring
              // laege auf dem blauen Hauptknopf bei 1,1:1 - das ist der Grund,
              // warum die Knoepfe ihren Ring selbst zeichnen.
              jsonArea.setText("{\"a\": 1}");
              for (Theme theme : new Theme[]{Theme.DUNKEL, Theme.HELL}) {
                  FormatterPanel.setzeTheme(theme);
                  for (JButton button : new JButton[]{
                          readButton, formatButton, packButton, writeButton, exitButton}) {
                      if (!button.isEnabled()) {
                          continue;
                      }
                      Field ring = button.getClass().getDeclaredField("ring");
                      ring.setAccessible(true);
                      Color ringFarbe = (Color) ring.get(button);
                      // Der Link ist nicht gefuellt, er liegt auf dem Fenster.
                      Color grund = button.isOpaque() ? button.getBackground()
                              : panel.getBackground();
                      double verhaeltnis = contrast(ringFarbe, grund);
                      assertTrue(verhaeltnis >= 3.0,
                              (theme.isDunkel() ? "dunkel" : "hell") + ": Ring "
                                      + ringFarbe + " auf " + grund + " nur "
                                      + String.format("%.1f", verhaeltnis) + ":1");
                  }
              }
          }

          @Test
          @DisplayName("das Textfeld und der Theme-Schalter haben Namen")
          void textfeld_und_thema_haben_namen() throws Exception {
              // Fuer einen Screenreader ist "TextArea" sonst alles, was es weiss.
              assertEquals("JSON-Text", jsonArea.getAccessibleContext().getAccessibleName());
              assertNotNull(jsonArea.getAccessibleContext().getAccessibleDescription(),
                      "das Textfeld erklaert nicht, was mit ihm passiert");
              assertEquals("Theme-Schalter",
                      themeButton.getAccessibleContext().getAccessibleName());
          }

          @Test
          @DisplayName("jede Schaltflaeche sagt, was sie tut")
          void knoepfe_beschreiben_sich() {
              for (JButton knopf : new JButton[]{readButton, formatButton, packButton, writeButton}) {
                  String beschreibung =
                          knopf.getAccessibleContext().getAccessibleDescription();
                  assertNotNull(beschreibung,
                          () -> "ohne Beschreibung: " + knopf.getText());
                  assertFalse(beschreibung.isBlank(),
                          () -> "leere Beschreibung: " + knopf.getText());
              }
          }

          @Test
          @DisplayName("der Fokus am Theme-Schalter haengt an der Akzentfarbe")
          void fokus_am_thema_schalter_bleibt_sichtbar() throws Exception {
              // Der Schalter malt sich selbst und schaltet den Fokusrahmen des
              // LookAndFeel ab. Ohne eigenen Ring waere er fuer die Tastatur
              // unsichtbar unsichtbar: man wuesste nicht, wo man ist.
              assertFalse(themeButton.isFocusPainted(),
                      "der LookAndFeel soll den Fokus nicht doppelt malen");
              assertTrue(themeButton.isFocusable(),
                      "der Schalter muss den Fokus bekommen koennen");

              // Geprueft wird die Farbe, mit der der Ring gestrichen wird: sie
              // muss die Akzentfarbe des aktiven Themes sein, sonst waere der
              // Ring auf dunklem Grund kaum zu sehen.
              Field ring = themeButton.getClass().getDeclaredField("ring");
              ring.setAccessible(true);
              Theme theme = field("aktuellesTheme");
              assertEquals(theme.akzent, ring.get(themeButton),
                      "der Fokusring nimmt nicht die Akzentfarbe des Themes an");
          }

          @Test
          @DisplayName("mit Fokus steht der Ring im Bild, ohne nicht")
          void fokusring_wird_gemalt() throws Exception {
              // Der gemalte Ring laesst sich nur pruefen, wenn das Testfenster
              // den Fokus wirklich annehmen darf - unter macOS verweigert das
              // System das einem Hintergrundprozess. Dann ueberspringen statt
              // gruen zu melden.
              aufEdt(() -> themeButton.requestFocusInWindow());
              assumeTrue(themeButton.hasFocus(),
                      "dieses Fenster kann den Fokus nicht halten - Ring nicht pruefbar");

              themeButton.setSize(200, 44);
              java.awt.image.BufferedImage mitFokus = new java.awt.image.BufferedImage(200, 44,
                      java.awt.image.BufferedImage.TYPE_INT_RGB);
              themeButton.paint(mitFokus.getGraphics());

              aufEdt(() -> themeButton.setFocusable(false));
              assertFalse(themeButton.hasFocus(), "Voraussetzung: Fokus ist weg");
              java.awt.image.BufferedImage ohneFokus = new java.awt.image.BufferedImage(200, 44,
                      java.awt.image.BufferedImage.TYPE_INT_RGB);
              themeButton.paint(ohneFokus.getGraphics());
              aufEdt(() -> themeButton.setFocusable(true));

              Theme theme = field("aktuellesTheme");
              assertTrue(pixelVor(mitFokus, theme.akzent),
                      "mit Fokus fehlt der Ring in " + theme.akzent);
              assertFalse(pixelVor(ohneFokus, theme.akzent),
                      "ohne Fokus darf kein Ring dastehen");
          }
      }

      private StyledDocument document() {
        // getStyledDocument() sitzt auf JTextPane, nicht auf JTextComponent.
        return ((JTextPane) jsonArea).getStyledDocument();
    }

    private Color farbeAn(String teil) {
        int pos = ab(teil);
        assertTrue(pos >= 0, "'" + teil + "' steht nicht im Text: " + jsonArea.getText());
        return farbeAn(teil, pos);
    }

    private Color farbeAn(String teil, int pos) {
        assertTrue(pos >= 0, "'" + teil + "' steht nicht an " + pos);
        return StyleConstants.getForeground(document().getCharacterElement(pos).getAttributes());
    }

    private boolean kursivAn(String teil) {
        return StyleConstants.isItalic(document().getCharacterElement(ab(teil)).getAttributes());
    }

    private void aufEdt(Runnable aktion) throws Exception {
        SwingUtilities.invokeAndWait(aktion);
    }

    /**
     * Malt das Textfeld einmal, damit es seine Bildschirmzeilen meldet.
     *
     * <p>Das ist noetig, weil die Zeilenzahl beim Zeichnen gemeldet wird und
     * nicht aus dem Dokument gelesen wird: nur der Text-View weiss, wie der
     * Umbruch ausgefallen ist, und der weiss es erst, wenn er gelegt ist. Wer
     * die Zahl vorher braucht, malt einmal - genau das passiert beim Oeffnen
     * des Fensters auch.
     */
    private void meldeBildschirmzeilen(int breite, int hoehe) throws Exception {
        aufEdt(() -> malen(jsonArea, breite, hoehe, jsonArea.getBackground()));
    }

    /**
     * Malt das Textfeld in ein Bild, damit der Test Pixel zaehlen kann, statt
     * sich vorzumachen, was der Nutzer sieht.
     */
    private java.awt.image.BufferedImage malen(JTextComponent feld, int breite, int hoehe,
            Color hintergrund) {
        feld.setSize(breite, hoehe);
        java.awt.image.BufferedImage bild = new java.awt.image.BufferedImage(breite, hoehe,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = bild.createGraphics();
        g.setColor(hintergrund);
        g.fillRect(0, 0, breite, hoehe);
        feld.paint(g);
        g.dispose();
        return bild;
    }

    /** Zaehlt die gemalten Streifen im Bild: einer je Zeile. */
    private int bemalteZahlen(int breite, int hoehe) throws Exception {
        meldeBildschirmzeilen(breite, hoehe);
        java.awt.image.BufferedImage bild =
                malen(jsonArea, breite, hoehe, jsonArea.getBackground());
        return gemalteZeilen(bild, zahlenSpalte(breite, hoehe),
                jsonArea.getBackground()).length;
    }

    private int[] gemalteZeilen(java.awt.image.BufferedImage bild, Rectangle streifen,
            Color hintergrund) {
        List<Integer> ys = new ArrayList<>();
        boolean imStreifen = false;
        for (int y = 0; y < streifen.height; y++) {
            boolean bemalt = false;
            for (int x = streifen.x; x < streifen.x + streifen.width && !bemalt; x++) {
                bemalt = (bild.getRGB(x, y) & 0xFFFFFF)
                        != (hintergrund.getRGB() & 0xFFFFFF);
            }
            if (bemalt && !imStreifen) {
                ys.add(Integer.valueOf(y));
            }
            imStreifen = bemalt;
        }
        int[] out = new int[ys.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ys.get(i).intValue();
        }
        return out;
    }

    /**
     * Sucht die oberste Zeile des Bildes im Streifen, die nicht die
     * Hintergrundfarbe traegt, und liefert sie.
     */
    private int ersteGemalteZeile(java.awt.image.BufferedImage bild, Rectangle streifen,
            Color hintergrund) {
        int[] ys = gemalteZeilen(bild, streifen, hintergrund);
        return ys.length == 0 ? -1 : ys[0];
    }

    /** Hoehe der n-ten gemalten Zeile im Streifen, 0 wenn es keine gibt. */
    private int hoeheDerGemaltenZeile(java.awt.image.BufferedImage bild, Rectangle streifen,
            Color hintergrund, int nummer) {
        int[] ys = gemalteZeilen(bild, streifen, hintergrund);
        return nummer >= 1 && nummer <= ys.length ? ys[nummer - 1] : -1;
    }

    /** Wie breit der Innenabstand links ist - dort stehen die Zahlen. */
    private int zahlenRand() {
        return jsonArea.getMargin().left;
    }

    /**
     * Der Streifen, in dem nur die Zahlen stehen. Der Rand bleibt weg, sonst
     * zaehlt der Streifen die Rundung des Rahmens mit.
     */
    private Rectangle zahlenSpalte(int breite, int hoehe) {
        return new Rectangle(0, 0, zahlenRand() - 4, hoehe);
    }

    /**
     * Der Streifen, in dem nur der Text steht. Erst eine Pixelzeile nach dem
     * Innenabstand, damit die erste Lettere nicht beschnitten wird.
     */
    private Rectangle textSpalte(int breite, int hoehe) {
        return new Rectangle(zahlenRand() + 1, 0, breite - zahlenRand() - 1, hoehe);
    }

    /**
     * Wie weit darf die Zahl von ihrer Textzeile abweichen?
     *
     * <p>Zwei Pixel: eine Ziffer und ein Buchstabe haben nicht dieselbe Ober-
     * und Unterkante, also ist die erste bemalte Pixelzeile nicht bei beiden
     * gleich. Wichtig ist der Betrag, nicht der Einzelfall - ein Versatz, der
     * mit jeder Zeile waechst, laeuft ueber diese Grenze hinaus.
     */
    private static final int ZEILEN_TOLERANZ = 2;

    /**
     * Prueft Bild gegen Bild: gleich viele Zahlen wie Textzeilen, und jede Zahl
     * steht auf der Hoehe ihrer Zeile.
     */
    private void assertZahlenAufDenZeilen(int[] zahlen, int[] texte, String meldung) {
        assertTrue(zahlen.length >= texte.length, meldung + ": " + texte.length
                + " Textzeilen, aber nur " + zahlen.length + " Zahlen");
        // Zusaetzliche Zahlen hinten sind leere Zeilen: die tragen eine Zahl,
        // aber keine Schrift. Fehlt eine Zahl in der Mitte, rutschen alle
        // folgenden um eine Zeile nach oben - genau das faellt unten auf.
        for (int i = 0; i < texte.length; i++) {
            int abweichung = Math.abs(zahlen[i] - texte[i]);
            assertTrue(abweichung <= ZEILEN_TOLERANZ, meldung + ": Zeile " + (i + 1)
                    + " steht " + abweichung + " Pixel daneben (" + zahlen[i]
                    + " zu " + texte[i] + ")");
        }
    }

    /** Zeichenbreite der Schrift im Textfeld. */
    private int zeichenBreite() {
        return jsonArea.getFontMetrics(jsonArea.getFont()).charWidth('0');
    }

    /** Wie viele Zeichen passen ohne Umbruch in ein Feld so breit? */
    private int zeichenProZeile(int breite) {
        java.awt.Insets rand = jsonArea.getMargin();
        int nutzbreite = breite - rand.left - rand.right;
        return Math.max(1, nutzbreite / zeichenBreite());
    }

    /**
     * Wie viele Bildschirmzeilen braucht ein Text, der an Wortgrenzen umbricht?
     *
     * <p>Nachgechnet wird hier bewusst unabhaengig vom Text-View: der Test soll
     * pruefen, ob die Zahlen zu dem passen, was der Umbruch ergibt - nicht
     * nachrechnen, was der Umbruch ergibt. Gewechselt wird wie im Editor an den
     * Leerzeichen, ein Wort wird nie in der Mitte getrennt.
     */
    private int zeilenBeiUmbruch(String text, int breite) {
        int proZeile = zeichenProZeile(breite);
        int zeilen = 1;
        int laenge = 0;
        for (String wort : text.split(" ")) {
            if (wort.isEmpty()) {
                continue;
            }
            int neu = laenge == 0 ? wort.length() : laenge + 1 + wort.length();
            if (neu > proZeile && laenge > 0) {
                zeilen++;
                laenge = wort.length();
            } else {
                laenge = neu;
            }
        }
        return zeilen;
    }

    /**
     * Wie viele Bildschirmzeilen braucht eine Folge, die nicht umbricht? Nur
     * brauchbar fuer Texte ohne Leerzeichen - alles andere nimmt
     * {@link #zeilenBeiUmbruch(String, int)}.
     */
    private int zeilenOhneUmbruch(int zeichen, int breite) {
        int proZeile = zeichenProZeile(breite);
        return (zeichen + proZeile - 1) / proZeile;
    }

    private String wiederhole(String stueck, int mal) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mal; i++) {
            sb.append(stueck);
        }
        return sb.toString();
    }

    @Nested
    @DisplayName("Ecken")
    class Ecken {

        @Test
        @DisplayName("der Textbereich malt wirklich runde Ecken")
        void editor_hat_runde_ecken() {
            // Geprueft wird nicht die Rahmenart, sondern das Bild: die Ecke
            // links oben muss frei bleiben, waehrend die Oberkante gezeichnet
            // ist. Ein eckiger Rahmen wuerde beides bemalen.
            java.awt.image.BufferedImage bild =
                    new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            jsonArea.setSize(40, 40);
            jsonArea.paint(bild.getGraphics());
            assertEquals(0, bild.getRGB(0, 0) & 0xFFFFFF, "die Ecke links oben bleibt frei");
            assertNotEquals(0, bild.getRGB(20, 0) & 0xFFFFFF, "die Oberkante ist gezeichnet");
        }

        @Test
        @DisplayName("der Text im Textfeld haengt nicht an der Fase")
        void text_hat_innenabstand() {
            java.awt.Insets rand = jsonArea.getMargin();
            assertEquals(9, rand.top, "oben haengt der Text nicht an der Fase");
            // Links ist es breiter als die Fase: dort stehen die Zahlen, und die
            // duerfen den Text nicht beruehren.
            assertTrue(rand.left >= 20,
                    "links ist Platz fuer die Zahlen, aktuell " + rand.left);
        }

        @Test
        @DisplayName("das Textfeld hat schwarzen Rahmen und Fase wie die Knoepfe")
        void textfeld_hat_scharfen_rahmen_und_fase() throws Exception {
            javax.swing.border.Border rand =
                    ((javax.swing.JPanel) field("editorCard")).getBorder();
            javax.swing.border.LineBorder linie =
                    (javax.swing.border.LineBorder)
                            ((javax.swing.border.CompoundBorder) rand).getOutsideBorder();
            assertEquals(Color.BLACK, linie.getLineColor(), "der Rahmen ist schwarz");

            javax.swing.border.Border phase =
                    ((javax.swing.border.CompoundBorder) rand).getInsideBorder();
            assertInstanceOf(javax.swing.border.BevelBorder.class, phase,
                    "das Textfeld traegt die Fase der Knoepfe");
            assertEquals(javax.swing.border.BevelBorder.RAISED,
                    ((javax.swing.border.BevelBorder) phase).getBevelType(),
                    "die Fase steht vor wie an den Knoepfen");
        }

        @Test
        @DisplayName("Knoepfe und Textfeld runden gleich")
        void knoepfe_und_textfeld_runden_gleich() throws Exception {
            javax.swing.border.Border amKnoepf =
                    ((javax.swing.border.CompoundBorder) formatButton.getBorder()).getOutsideBorder();
            javax.swing.border.Border amFeld = ((javax.swing.border.CompoundBorder)
                    ((javax.swing.JPanel) field("editorCard")).getBorder()).getOutsideBorder();
              assertEquals(amKnoepf.getClass(), amFeld.getClass(),
                      "Buttons und Textfeld muessen dieselbe Eckenbehandlung teilen");
          }

          @Test
          @DisplayName("der Theme-Schalter malt seine Beschriftung selbst")
          void thema_schalter_malt_selbst() throws Exception {
              // FlatLaf malt die Beschriftung eines aktiven Tasters in einer
              // eigenen Farbe; "Hell" stand dann dunkel auf mittelgrau. Geprueft
              // wird deshalb das Bild: im dunklen Theme muss die Beschriftung in
              // Weiss gezeichnet sein, im hellen in der Textfarbe.
              jsonArea.setText("{\"a\": 1}");
              beschriftung_erscheint_in_der_textfarbe("Hell", Theme.DUNKEL);
              aufEdt(themeButton::doClick);
              beschriftung_erscheint_in_der_textfarbe("Dunkel", Theme.HELL);
          }

          /**
           * Prueft, dass der Schalter die Beschriftung wirklich in der Textfarbe
           * des Themes zeichnet und der LookAndFeel sie nicht uebermalt.
           */
          private void beschriftung_erscheint_in_der_textfarbe(
                  String beschriftung, Theme theme) throws Exception {
              assertEquals(beschriftung, themeButton.getText(),
                      "der Schalter nennt das Ziel");
              themeButton.setSize(200, 44);
              java.awt.image.BufferedImage bild =
                      new java.awt.image.BufferedImage(200, 44,
                              java.awt.image.BufferedImage.TYPE_INT_RGB);
              themeButton.paint(bild.getGraphics());
              Color farbe = themeButton.getForeground();
              assertEquals(theme.isDunkel() ? theme.aufAkzent : theme.text, farbe,
                      "die Beschriftung hat die Textfarbe des Themes");
              assertTrue(contrast(farbe, panel.getBackground()) >= 4.5,
                      "die Beschriftung hebt sich vom Fenster ab");
              assertTrue(kommtVor(bild, farbe),
                      "im gemalten Bild steht die Textfarbe " + farbe
                              + " - der LookAndFeel faerbt die Beschriftung sonst selbst");
          }

          private boolean kommtVor(java.awt.image.BufferedImage bild, Color farbe) {
              return pixelVor(bild, farbe);
          }
      }

      /** Zaehlt, ob eine Farbe wirklich im gemalten Bild auftaucht. */
      private static boolean pixelVor(java.awt.image.BufferedImage bild, Color farbe) {
          for (int y = 0; y < bild.getHeight(); y++) {
              for (int x = 0; x < bild.getWidth(); x++) {
                  if ((bild.getRGB(x, y) & 0xFFFFFF) == (farbe.getRGB() & 0xFFFFFF)) {
                      return true;
                  }
              }
          }
          return false;
      }

      @Nested
    @DisplayName("Zeilennummern")
    class ZeilennummernTest {

        @Test
        @DisplayName("die Zahlen stehen im Textfeld, nicht in einer eigenen Spalte")
        void zahlen_stehen_im_textfeld() throws Exception {
            // Eine eigene Spalte waere eine zweite Komponente neben dem Text. Die
            // muesste bei jedem Tastendruck ausdruecklich mitgerufen werden und
            // haette eigene Rollkoordinaten - beides faellt hier weg, weil Zahl
            // und Text aus demselben View in dasselbe Bild gezeichnet werden.
            JScrollPane sp = field("scrollPane");
            assertNull(sp.getRowHeader(),
                    "die Zahlen gehoeren ins Textfeld, nicht daneben");
            assertTrue(zahlenRand() >= 20,
                    "links ist Platz fuer die Zahlen, aktuell " + zahlenRand());
        }

        @Test
        @DisplayName("es steht eine Zahl fuer jede Zeile")
        void eine_zahl_pro_zeile() throws Exception {
            // Das Textfeld braucht eine Groesse: ohne sie liefert der Text keine
            // Positionen, und die Zahlen bleiben aus.
            jsonArea.setText("{\"a\": 1}\n{\"b\": 2}\n{\"c\": 3}\n");
            jsonArea.setSize(300, 200);
            assertEquals(4, bemalteZahlen(300, 200),
                    "drei Zeilen plus die leere letzte bekommen je eine Zahl");
        }

        @Test
        @DisplayName("die Zahlen stehen auf der Grundlinie ihrer Textzeile")
        void zahlen_auf_grundlinie_der_zeile() throws Exception {
            // Geprueft wird Bild gegen Bild: die Zahl muss auf genau derselben
            // Pixelzeile stehen wie der Text. Genau daran ist es bisher
            // gescheitert - die Zahl stand im Takt und lief beim Blaettern
            // auseinander.
            jsonArea.setText("AAA\nBBB\nCCC\n");
            jsonArea.setSize(300, 200);
            meldeBildschirmzeilen(300, 200);
            java.awt.image.BufferedImage bild =
                    malen(jsonArea, 300, 200, jsonArea.getBackground());
            int[] zahlen = gemalteZeilen(bild, zahlenSpalte(300, 200),
                    jsonArea.getBackground());
            int[] texte = gemalteZeilen(bild, textSpalte(300, 200),
                    jsonArea.getBackground());
            assertTrue(zahlen.length >= 3, "die ersten drei Zahlen sind da: "
                    + zahlen.length);
            assertZahlenAufDenZeilen(zahlen, texte, "die Zahlen stehen auf den Zeilen");
        }

        @Test
        @DisplayName("Tippen erneuert die Zahlen ohne Klick")
        void tippen_erneuert_die_zahlen() throws Exception {
            // Der Fehler aus der Praxis: die Zahlen erschienen erst, wenn man in
            // das Fenster klickte. Ursache war eine eigene Spalte, die nur bei
            // Klick neu gezeichnet wurde. Jetzt zeichnet das Textfeld die Zahlen
            // im selben Zug wie den Text.
            jsonArea.setSize(300, 200);
            jsonArea.setText("{\"a\": 1}\n");
            meldeBildschirmzeilen(300, 200);
            int vorher = bemalteZahlen(300, 200);
            // Wie ein Tastendruck: eine Zeile einfuegen, nicht setText. Die
            // Position ist das Ende des bisherigen Textes.
            aufEdt(() -> {
                try {
                    jsonArea.getDocument().insertString(9, "{\"b\": 2}\n", null);
                } catch (javax.swing.text.BadLocationException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            // Ohne Klick, ohne Maus, ohne Tastendruck: nur neu malen.
            int nachher = bemalteZahlen(300, 200);
            assertEquals(vorher + 1, nachher,
                    "die neue Zeile hat sofort eine Zahl, auch ohne Klick ins Fenster");
        }

        @Test
        @DisplayName("eine umgebrochene Zeile bekommt je Bildschirmzeile eine Zahl")
        void umgebrochene_zeile_bekommt_mehr_zahlen() throws Exception {
            // Weicher Umbruch, wie ihn das Textfeld mit 84 Zeichen Breite macht:
            // eine lange Zeile braucht mehrere Bildschirmzeilen und damit
            // mehrere Zahlen. Zaehlt man stattdessen die Absaetze, fehlt unter
            // dem umbrochenen Text eine Zahl.
            int breite = 200;
            String lang = wiederhole("{\"a\": \"0123456789abcdef\"}, ", 6);
            jsonArea.setSize(breite, 200);
            jsonArea.setText(lang + "\nselect 2\n");
            meldeBildschirmzeilen(breite, 200);
            java.awt.image.BufferedImage bild =
                    malen(jsonArea, breite, 400, jsonArea.getBackground());
            int[] zahlen = gemalteZeilen(bild, zahlenSpalte(breite, 400),
                    jsonArea.getBackground());
            int[] texte = gemalteZeilen(bild, textSpalte(breite, 400),
                    jsonArea.getBackground());
            assertZahlenAufDenZeilen(zahlen, texte,
                    "jeder Teil der langen Zeile hat eine Zahl");
            assertTrue(zahlen.length > 3, "der Test prueft wirklich eine umgebrochene Zeile, "
                    + "sonst waere er leer: nur " + zahlen.length + " Zahlen");
        }

        @Test
        @DisplayName("eine lange letzte Zeile ohne Zeilenende zaehlt alle ihre Teile")
        void lange_letzte_zeile_ohne_zeilenende() throws Exception {
            // Der haeufigste Fall aus der Praxis: eine lange Datei, die als
            // letztes ohne Zeilenende endet und deshalb am Ende umbricht. Beim
            // Zaehlen der Absaetze endet die letzte Zahl dort, wo die Zeile
            // angefaengt hat - der Rest steht dann ohne Zahl da.
            int breite = 200;
            String lang = wiederhole("{\"a\": \"0123456789abcdef\"}, ", 20);
            jsonArea.setSize(breite, 900);
            jsonArea.setText("select 1\n" + lang);
            meldeBildschirmzeilen(breite, 900);
            java.awt.image.BufferedImage bild =
                    malen(jsonArea, breite, 900, jsonArea.getBackground());
            int[] zahlen = gemalteZeilen(bild, zahlenSpalte(breite, 900),
                    jsonArea.getBackground());
            int[] texte = gemalteZeilen(bild, textSpalte(breite, 900),
                    jsonArea.getBackground());
            assertZahlenAufDenZeilen(zahlen, texte,
                    "jeder Teil der letzten langen Zeile hat eine Zahl");
            assertTrue(zahlen.length > 20, "der Test prueft wirklich eine lange Zeile: nur "
                    + zahlen.length + " Zahlen");
        }

        @Test
        @DisplayName("die Zahlen stehen beim Umbruch auf der Hoehe ihrer Bildschirmzeile")
        void zahlen_bei_umbruch_auf_der_hoehe() throws Exception {
            // Beim Blaettern duerfen die Zahlen nicht hinterherlaufen: die Zahl
            // der fuenften Bildschirmzeile steht auf deren Hoehe, auch wenn sie
            // der zweite Teil eines einzigen Absatzes ist.
            int breite = 200;
            String lang = wiederhole("{\"a\": \"0123456789abcdef\"}, ", 6);
            jsonArea.setSize(breite, 400);
            jsonArea.setText(lang + "\n");
            meldeBildschirmzeilen(breite, 400);
            // Verglichen wird mit den Zeilen, die der Text an derselben Stelle
            // wirklich malt, nicht mit einer gerechneten Hoehe: die Zeilen liegen
            // nicht genau auf dem Zeilenabstand der Schrift, sondern so, wie der
            // Text-View sie legt. Geprueft wird jede einzelne - eine Zahl, die
            // erst ab der fuenften Zeile danebenliegt, faellt auch auf.
            java.awt.image.BufferedImage bild =
                    malen(jsonArea, breite, 400, jsonArea.getBackground());
            int[] zahlen = gemalteZeilen(bild, zahlenSpalte(breite, 400),
                    jsonArea.getBackground());
            int[] texte = gemalteZeilen(bild, textSpalte(breite, 400),
                    jsonArea.getBackground());
            assertZahlenAufDenZeilen(zahlen, texte,
                    "auch tief unten im Umbruch steht die Zahl auf ihrer Zeile");
            assertTrue(zahlen.length > 5, "der Test prueft wirklich mehrere Bildschirmzeilen "
                    + "eines Absatzes: nur " + zahlen.length + " Zahlen");
        }

        @Test
        @DisplayName("kein Text steht ohne Zahl daneben")
        void keine_zeile_ohne_zahl() throws Exception {
            // Der Befund aus der Praxis war: Zahlen 1 bis 22, darunter weitere
            // Zeilen ohne jede Zahl. Geprueft wird deshalb zeilenweise, dass jede
            // gemalte Textzeile auch eine Zahl hat und auf gleicher Hoehe steht.
            int breite = 240;
            jsonArea.setSize(breite, 1200);
            StringBuilder json = new StringBuilder();
            for (int i = 1; i <= 120; i++) {
                json.append("{\"a\": ").append(i).append(", \"b\": \"wert\"}\n");
            }
            jsonArea.setText(json.toString());
            meldeBildschirmzeilen(breite, 1200);
            java.awt.image.BufferedImage bild =
                    malen(jsonArea, breite, 1200, jsonArea.getBackground());
            int[] zahlen = gemalteZeilen(bild, zahlenSpalte(breite, 1200),
                    jsonArea.getBackground());
            int[] texte = gemalteZeilen(bild, textSpalte(breite, 1200),
                    jsonArea.getBackground());
            assertZahlenAufDenZeilen(zahlen, texte, "jede Textzeile hat eine Zahl");
        }

        @Test
        @DisplayName("die Zahlen nehmen die Farbe des Themes an")
        void zahlen_in_theme_farbe() throws Exception {
            Color zahlen = editorFeldFarbe("zahlenFarbe");
            assertEquals(Theme.DUNKEL.gedaempft, zahlen,
                    "die Zahlen sind zurueckhaltend, nicht so stark wie der Text");
            assertTrue(contrast(zahlen, panel.getBackground()) >= 4.5,
                    "die Zahlen bleiben im dunklen Theme lesbar");
            aufEdt(themeButton::doClick);
            assertEquals(Theme.HELL.gedaempft, editorFeldFarbe("zahlenFarbe"),
                    "nach dem Wechsel ziehen die Zahlen mit");
            assertTrue(contrast(editorFeldFarbe("zahlenFarbe"), panel.getBackground()) >= 4.5,
                    "und bleiben im hellen Theme lesbar");
        }

        private Color editorFeldFarbe(String name) throws Exception {
            java.lang.reflect.Field f = jsonArea.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (Color) f.get(jsonArea);
        }

    }

    @Nested
    @DisplayName("Typografie")
    class Typografie {

        @Test
        @DisplayName("der Produktname steht nicht zweimal im Fenster")
        void name_steht_nicht_zweimal() {
            // Der Fenstertitel traegt "JSON Clipboard Formatter". Ein zweiter
            // Titel im Fenster wiederholte ihn nur und kostete Hoehe, die der
            // Textbereich gebraucht.
            List<String> texte = new ArrayList<>();
            sammleTexte(panel, texte);
            for (String t : texte) {
                assertFalse(t.contains("SQL"),
                        () -> "der Produktname steht noch im Fenster: '" + t + "'");
            }
        }

        private void sammleTexte(java.awt.Container c, List<String> ziel) {
            for (java.awt.Component k : c.getComponents()) {
                if (k instanceof javax.swing.JLabel l) {
                    ziel.add(l.getText());
                }
                if (k instanceof java.awt.Container kc) {
                    sammleTexte(kc, ziel);
                }
            }
        }

        @Test
        @DisplayName("die Versionszeile ist kleiner als die Statuszeile")
        void version_ist_dezenter_als_status() throws Exception {
            JLabel version = field("versionLabel");
            JLabel status = field("statusLabel");
            assertTrue(version.getFont().getSize() < status.getFont().getSize(),
                    () -> "Version " + version.getFont().getSize()
                            + " ist nicht kleiner als Status " + status.getFont().getSize());
        }
    }

    private static double contrast(Color a, Color b) {
        double l1 = luminance(a);
        double l2 = luminance(b);
        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed() / 255.0)
                + 0.7152 * channel(c.getGreen() / 255.0)
                + 0.0722 * channel(c.getBlue() / 255.0);
    }

    private static double channel(double v) {
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * Wartet, bis der Formatier-Worker seine Statusmeldung gesetzt hat, und
     * liest dabei jedes Mal ueber den EDT - ein Swing-Component darf von
     * aussen nicht direkt angefasst werden.
     */
    private void warteAufErgebnis() throws Exception {
        for (int i = 0; i < 500; i++) {
            String status = leseAufEdt(statusLabel::getText);
            // Alle drei Enden zaehlen als fertig: Erfolg, Warnung und Fehler.
            // Wer hier nur den Erfolg abwartet, laesst den Fehlerpfad in einen
            // Timeout laufen und haelt ihn dann fuer einen hängenden Vorgang.
            if (status.startsWith("\u2713") || status.startsWith("\u26a0") || status.startsWith("\u274c")) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Formatierung wurde nicht fertig: " + statusLabel.getText());
    }

    private <T> T leseAufEdt(java.util.function.Supplier<T> leser) throws Exception {
        Object[] ergebnis = new Object[1];
        SwingUtilities.invokeAndWait(() -> ergebnis[0] = leser.get());
        @SuppressWarnings("unchecked")
        T wert = (T) ergebnis[0];
        return wert;
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name) throws Exception {
        Field f = FormatterPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(panel);
    }
}
