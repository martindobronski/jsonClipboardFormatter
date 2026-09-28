# Anleitung für macOS

Diese Anleitung richtet sich an alle, die den JSON Clipboard Formatter auf einem
Mac benutzen wollen. Sie setzt keine Programmierkenntnisse voraus — nur ein
Terminal, in das Sie einen Befehl eintippen können.

> **Sie suchen die Anleitung für Windows?** Dann nehmen Sie
> [anleitung-windows.md](anleitung-windows.md). Dort gibt es ein fertiges ZIP,
> in dem die Java-Laufzeit bereits enthalten ist — auf dem Mac ist Java
> Voraussetzung, weil es dafür kein ZIP gibt.

> ### Falls bei Ihnen eine mitgelieferte Laufzeit liegt
>
> `start.sh` nimmt, falls vorhanden, ein Java aus einem Ordner `jre/` neben
> dem Skript — auf dem Mac liegt normalerweise keiner, dann greift Ihr
> installiertes Java. `start.sh -Pruefen` sagt Ihnen in jedem Fall, welches
> Java verwendet wird und woher es stammt. Näheres dazu steht im README.

---

## 1. Wofür ist das Programm?

Sie markieren JSON in einem beliebigen Programm, kopieren ihn mit Cmd+C in die
Zwischenablage und formatieren ihn mit einem Klick:

```
{"name":"Wert","liste":[1,2],"leer":{},"n":null}
```

wird zu:

```json
{
    "name": "Wert",
    "liste": [
        1,
        2
    ],
    "leer": {},
    "n": null
}
```

Das Ergebnis liegt danach wieder in der Zwischenablage, sodass Sie es mit
Cmd+V an die gewünschte Stelle setzen können. Sie brauchen dafür **kein
Programm, das JSON kennt, und keine Internetverbindung**.

Genauso geht es umgekehrt: **JSON Packen** macht aus vielen Zeilen wieder eine
Zeile — das ist die Form, in der viele Werkzeuge ihre Einstellungen oder
Anfragen erwarten.

## 2. Was Sie brauchen

| | |
|---|---|
| **Java 17 oder neuer** | zwingend — das Programm ist in Java geschrieben |
| **Maven** | nur nötig, wenn Sie das Programm **selbst übersetzen** wollen; zum Benutzen nicht nötig |

Maven können Sie sich sparen: Wenn das Programm einmal übersetzt wurde, startet
es auch ohne Maven. `start.sh` baut nur, wenn die Programmdatei fehlt oder eine
Quelldatei neuer ist als sie; ist beides nicht der Fall, wird Maven gar nicht
angesprochen. Mit `Maven   : nicht gefunden` neben einem vorhandenen Jar ist
das der Fall.

## 3. Schritt 1: Java prüfen

Öffnen Sie das Terminal über **Finder → Programme → Dienstprogramme →
Terminal** (oder Cmd+Leertaste, „Terminal" eintippen, Enter) und geben Sie ein:

```sh
java -version
```

Sieht die Antwort so oder ähnlich aus, ist alles in Ordnung:

```
openjdk version "27" 2026-09-15
OpenJDK Runtime Environment (build 27+35-2325)
```

Steht dort `zsh: command not found: java` oder eine Versionszahl unter 17, fehlt
Java oder ist zu alt. Installieren Sie es über [Azul Zulu](https://azul.com/downloads/)
(„JDK", macOS, Apple Silicon oder Intel) und starten Sie das Terminal neu.

## 4. Schritt 2: Das Programm holen

Wechseln Sie in einen Ordner, in dem das Programm liegen darf, und holen Sie
es mit Git:

```sh
git clone https://github.com/martindobronski/jsonClipboardFormatter.git
cd jsonClipboardFormatter
```

Danach übersetzen Sie es einmal. Das dauert beim ersten Mal etwa eine Minute
und lädt einige Dateien nach:

```sh
./start.sh -Neu
```

`start.sh` schreibt vorher `Bauen ...` und bleibt danach still, wenn alles
klappt — der Erfolg ist das Fenster, das sich öffnet. Das Ergebnis liegt als
`target/JsonClipboardFormatter-0.1.jar` im Ordner `target`. Geht beim Übersetzen
etwas schief, schreibt Maven seinen Grund in dasselbe Terminalfenster.

## 5. Schritt 3: Programm starten

Ab jetzt genügt immer dieser eine Befehl:

```sh
./start.sh
```

Das Fenster **JSON Clipboard Formatter** öffnet sich. Das Skript baut vorher
automatisch neu, falls Sie oder jemand an den Quellen etwas geändert hat — Sie
müssen das also nicht selbst entscheiden.

Das Programm hat keine Menüleiste: Bedienen können Sie es nur über die Knöpfe.
Beenden geht über **Beenden** unten rechts oder über das Schließen des Fensters.

### Ohne Terminal: Start per Doppelklick

Wenn Sie lieber nicht jedes Mal einen Befehl eintippen, machen Sie sich eine
Doppelklick-Datei. Die ist lediglich eine Kopie desselben Skripts unter einem
Namen, den der Finder per Doppelklick öffnet:

```sh
cp start.sh start.command
chmod +x start.command
```

Ab jetzt startet ein **Doppelklick auf `start.command` im Finder** das Programm.

Geprüft ist, dass die umbenannte Datei genauso läuft wie `start.sh` — mit
`./start.command -Pruefen` liefert sie dieselbe Ausgabe. Der Klick selbst im
Finder konnte hier nicht per Maus ausgeführt werden; er ruft denselben Befehl
auf, den Sie in Schritt 3 getippt haben. Sollte der Finder beim ersten Versuch
meckern, nehmen Sie den Weg über das Terminal.

### Was die Schalter können

| Befehl | Wirkung |
|---|---|
| `./start.sh` | starten, baut vorher bei Bedarf neu |
| `./start.sh -Neu` | vorher neu übersetzen, auch wenn schon alles aktuell ist |
| `./start.sh -Pruefen` | nur nachsehen, ob die Umgebung passt — startet nichts |
| `./start.sh -Xmx512m` | gibt 512 MB Speicher für das Programm frei |

Jeder Schalter, der nicht `-Neu` oder `-Pruefen` ist, geht unverändert an Java
durch. Die beiden eigenen gehören an den Anfang:

```sh
./start.sh -Pruefen -Xmx512m
```

### Wenn Java oder Maven an einer ungewöhnlichen Stelle liegen

Fast immer findet `start.sh` von selbst das richtige Java. Die Reihenfolge ist
diese: ein mitgeliefertes `jre/` neben dem Skript, der Eintrag `java` aus der
Konfigurationsdatei, die Variable `JSONFORMATTER_JAVA`, `JAVA_HOME` und zuletzt
der Suchpfad. Fehlt auch der, kommt noch `/usr/libexec/java_home` zum Zug, wenn
macOS ein JDK kennt, das nicht im Suchpfad steht.

Liegt Ihr Java an einer Stelle, die auf dieser Liste nicht vorkommt, tragen Sie
den Pfad in `start.local.conf` ein — eine Datei neben `start.sh`, die **nicht**
mit Git verwaltet wird, weil sie die Pfade Ihres Rechners enthält:

```sh
cp start.conf.example start.local.conf
```

```
java=/usr/local/opt/openjdk@17/bin/java
maven=/usr/local/bin/mvn
maven-jdk=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

Anführungszeichen sind nicht nötig, Leerzeichen im Pfad sind in Ordnung, alles
hinter einem `#` ist Kommentar, und eine Zeile, die kaputt ist, wird
übersprungen. `maven` zeigt auf das Programm `mvn`, `maven-jdk` auf den Ordner
**mit** `bin` darin. `maven-jdk` brauchen Sie nur, wenn Maven mit einem anderen
Java übersetzen soll als dem, mit dem die Programm startet; leer lassen heißt,
`JAVA_HOME` bleibt unangetastet.

Ein Eintrag, dessen Pfad es nicht gibt, ist kein Problem: `start.sh` prüft
jeden Pfad mit `java -version` und geht dann weiter. `./start.sh -Pruefen` zeigt
Ihnen, welcher Eintrag wirklich benutzt wurde — steht dort `Konfig  : ...` und
daneben `(start.local.conf)`, ist es Ihrer.

## 6. Bedienung

![Oberfläche im hellen Design](bild-hell.png)

So sieht das Programm beim Start aus. Das dunkle Design erreichen Sie über den
Knopf oben rechts.

Das Fenster trägt den Titel **JSON Clipboard Formatter**, rechts oben steht der
Knopf zum Umschalten zwischen dunkel und hell. Darunter das große Textfeld mit
den Zeilenzahlen links. Ganz unten eine Zeile mit den Knöpfen und darunter
noch einmal die Statuszeile; unten rechts steht die Versionszeile, zum Beispiel
`Version 0.1 vom 28.09.2026`.

### Die Zahlen links im Textfeld

Links im Textfeld stehen Zahlen. Sie zählen die **Zeilen, die Sie gerade sehen**,
nicht die Absätze im Text: Das Feld ist 84 Zeichen breit, und eine längere Zeile
bricht weich um. Bekommt eine Zeile davon zwei Bildschirmzeilen, stehen dort auch
zwei Zahlen.

Das ist der Grund, warum die Zahlen hilfreich sind: Jede Zahl gehört zu genau der
Zeile, die daneben steht — beim Tippen, beim Formatieren und beim Blättern.

Steht gerade keine Meldung in der Statuszeile, sehen Sie dort den Zähler, zum
Beispiel `14 Zeilen, 200 Zeichen` bei einer `package.json` mit 13 Zeilen. Er
nennt dasselbe wie die Zahlen links:
Bildschirmzeilen. Ein Zeilenumbruch zählt dabei als ein Zeichen, egal ob Ihr
Text ihn als `CRLF` oder als `LF` gespeichert hat.

### Der eine wichtige Punkt: die Statuszeile

**Die Statuszeile ist das Einzige, was Ihnen sagt, ob die Knöpfe etwas tun.**

| Inhalt des Feldes | Statuszeile | Knöpfe |
|---|---|---|
| leer | *(leer, stattdessen der Zähler)* | nur *Clipboard einlesen* |
| sieht nach JSON aus | *(leer, stattdessen der Zähler)* | alle frei |
| sieht nicht nach JSON aus | ⚠ Der Text sieht nicht nach JSON aus. Formatieren, Packen und Schreiben sind blockiert. | nur *Clipboard einlesen* |

Genau in diesem letzten Fall sind **JSON Formatieren**, **JSON Packen** und
**Ins Clipboard schreiben** grau. Das ist Absicht: So überschreiben Sie nicht
versehentlich einen normalen Text aus Ihrer Zwischenablage mit JSON, das Sie gar
nicht erwartet haben. **Clipboard einlesen** bleibt immer anklickbar — Sie
können also jederzeit nachsehen, was in der Zwischenablage steht.

Wann ein Text als JSON gilt, entscheidet das Programm an **einem** Zeichen: dem
ersten, das weder Leerraum noch Kommentar ist. Es zählt zu JSON, wenn dort

- eine geschweifte Klammer `{` steht,
- eine eckige Klammer `[` steht,
- ein Anführungszeichen `"` steht,
- ein Minuszeichen `-` steht,
- eine Ziffer `0` bis `9` steht,
- oder exakt das Wort `true`, `false` oder `null` beginnt.

Alles andere sperrt. Eine Einkaufsliste sperrt ebenso wie ein HTML-Schnipsel,
weil dort ein Buchstabe steht, mit dem JSON nicht anfangen kann.

Wichtig ist auch: Das ist eine **Vermutung, keine Prüfung**. Der eigentliche
Richter ist das Formatieren selbst — es liest den ganzen Text und meldet, was
darin nicht stimmt (siehe unten).

### Die vier Knöpfe

| Knopf | Taste | Wirkung |
|---|---|---|
| **Clipboard einlesen** | `L` | holt den Text aus Ihrer Zwischenablage in das Feld |
| **JSON Formatieren** | `F` | macht den Text im Feld lesbar, ein Element je Zeile |
| **JSON Packen** | `M` | macht aus dem Text im Feld eine einzige Zeile |
| **Ins Clipboard schreiben** | `C` | legt den Text aus dem Feld in die Zwischenablage |
| **Beenden** | `B` | beendet das Programm; steht rechts neben den anderen Knöpfen |

Die Spalte „Taste" nennt das Kürzel aus einem Buchstaben, mit dem sich der Knopf
auch ohne Maus auslösen lässt. **Beenden** ist kein rechter Knopf, sondern ein
Textlink — er sieht deshalb anders aus als die vier Aktionen.

**Das sind zwei Schritte, nicht einer:** *JSON Formatieren* und *JSON Packen*
legen nichts in die Zwischenablage. Erst *Ins Clipboard schreiben* tut das. So
können Sie sich das Ergebnis ansehen, bevor Sie es irgendwo einfügen.

**Formatieren oder Packen?** Beides liest denselben Text und schreibt dasselbe
zurück — nur in anderer Gestalt. *Formatieren* ist für Menschen, *Packen* für
Maschinen: etwa für eine Befehlszeile, eine `.env`-Datei oder einen
Aufrufparameter.

### Normaler Arbeitsablauf

1. JSON in Ihrem Programm markieren und mit **Cmd+C** kopieren
2. **Clipboard einlesen** → `✓ JSON geladen. Formatieren, Packen und Schreiben sind freigeschaltet.`
3. **JSON Formatieren** → `✓ JSON lesbar formatiert.`
4. **Ins Clipboard schreiben** → `✓ JSON erfolgreich in die Zwischenablage kopiert.`
5. In Ihr Programm zurück, **Cmd+V** einfügen

War beim Formatieren nichts zu tun, meldet das Programm das ausdrücklich:
`✓ War schon lesbar formatiert.` Beim Packen entsprechend `✓ War schon
gepackt.` Erfolgsmeldungen verschwinden nach vier Sekunden von selbst. Warnungen
und Fehler bleiben stehen, bis Sie wieder etwas tun.

Steht in der Zwischenablage Text, der nicht nach JSON aussieht, meldet
**Clipboard einlesen** das: `⚠ Text geladen, sieht aber nicht nach JSON aus.
Aktionen blockiert.` Der Text steht trotzdem im Feld — Sie können ihn ansehen
und von Hand ändern.

### Was beim Formatieren aus Ihrem Text wird

Alles, was JSON-Programme ausgeben, ist streng, aber nicht immer schön. Das
Programm macht daraus eine lesbare Form:

| Regel | |
|---|---|
| Einrückung | vier Leerzeichen je Ebene |
| Elemente | jedes Element steht auf seiner eigenen Zeile |
| Leere Objekte und Listen | bleiben einzeilig: `{}` und `[]` |
| Kommentare | fallen weg, sowohl `// ...` als auch `/* ... */` |
| Nachlaufkommas | fallen weg |
| Ausgabe | ist immer strenges JSON — ohne Kommentare, ohne Nachlaufkommas |
| Zahlen | bleiben wörtlich erhalten, es wird nichts umgerechnet oder gerundet |
| Schlüssel | Reihenfolge und Schreibweise bleiben, auch doppelte Schlüssel |
| Zeilenenden | sind immer Unix-Zeilenenden, ein `LF` je Umbruch |
| Schluss | genau ein Zeilenumbruch am Ende |

Vier Leerzeichen, ein Element je Zeile, leere Container einzeilig, ein
abschließender Zeilenumbruch — mehr verspricht **JSON Formatieren** nicht.

**Zwei Dinge verschwinden also, ohne dass es Sie vorher gewarnt hat:**
Kommentare und Nachlaufkommas. Beides dulden manche Werkzeuge (Visual Studio
Code, Eclipse, `tsconfig.json`), in JSON selbst ist es nicht erlaubt. Ein `//`
im Text schützt Ihren Code also nicht davor: Alles dahinter bis zum Zeilenende
gilt als Kommentar und fällt beim Formatieren weg.

**Zeilenenden wandern mit.** Kommt Ihr Text aus einem Programm mit
Windows-Zeilenenden (`CRLF`), kommen Unix-Zeilenenden (`LF`) zurück. Das ist
Absicht, keine Ungenauigkeit.

**Auch Skalarwerte sind gültig.** `42`, `"text"` und `true` sind vollständige
JSON-Dokumente und werden genauso formatiert und gepackt wie ein Objekt.

**Packen** macht daraus genau eine Zeile ohne jeden Leerraum zwischen den Werten,
mit einem abschließenden Zeilenumbruch:

```
{"name":"Wert","liste":[1,2],"leer":{},"n":null}
```

Vom Text, den **Ins Clipboard schreiben** in die Zwischenablage legt, werden
Leerzeichen und Zeilenumbrüche am Anfang und am Ende abgeschnitten. Das ist der
einzige Schritt, der den Text verändert, ohne ihn anzuzeigen.

### Wenn im JSON etwas nicht stimmt

Statt zu raten, lässt das Programm die Eingabe stehen und sagt Ihnen, wo es
aufgehört hat:

```
❌ Zeile 3, Spalte 5: Zwei Kommas nacheinander sind zu viel. Der Text ist unverändert geblieben.
```

Der Grund steht wörtlich so im Programm, er ändert sich nicht. Die häufigsten:

| Meldung | Bedeutung |
|---|---|
| `Unerwartetes Zeichen 'E'.` | an dieser Stelle steht etwas, das JSON nicht kennt — auch dann, wenn es ein ganz normaler Buchstabe ist. Das Zeichen selbst steht in den Anführungszeichen |
| `String wird nicht geschlossen.` | ein Anführungszeichen wurde nicht geschlossen |
| `Unvollständige \u-Folge: vier Hexadezimalzeichen sind nötig.` | hinter `\u` stehen weniger als vier Hexadezimalzeichen |
| `Unbekannte Fluchtfolge \q. Erlaubt sind \" \\ \/ \b \f \n \r \t und \u mit vier Hexadezimalzeichen.` | der Backslash führt zu etwas, das es nicht gibt |
| `Unerlaubtes Zeichen 0x9 im String. Es gehört als \u-Folge hinein.` | ein Zeichen im Text, das da nicht hingehört — ein Tabulator etwa, im Programm als `0x9` bezeichnet |
| `Zahlen dürfen keine führende Null haben.` | eine Zahl wie `01` — geht durch viele Parser ohne Meldung hindurch |
| `Nach dem Minuszeichen muss eine Ziffer kommen.` | nach `-` folgt keine Ziffer |
| `Nach dem Punkt muss mindestens eine Ziffer kommen.` | die Zahl endet mit `.` |
| `Nach 'e' muss mindestens eine Ziffer kommen.` | der Exponent einer Zahl ist unvollständig |
| `Hier wird ein Wert erwartet, zum Beispiel true, false oder null.` | `true` ist abgekürzt, oder es steht etwas falsches da |
| `Hier wird ein Schlüssel und dann ein Doppelpunkt erwartet.` | der Doppelpunkt steht an einer Stelle, an der keiner hingehört |
| `Hier wird ein Doppelpunkt erwartet.` | nach einem Schlüssel fehlt der Doppelpunkt |
| `Hier wird ein Komma oder ']' erwartet.` | in einer Liste fehlt ein Komma oder ein Schlusszeichen |
| `Falsche Klammer: hier gehört '}' hin.` | eine runde Klammer schließt ein Objekt, das eine geschweifte braucht |
| `Zwei Kommas nacheinander sind zu viel.` | ein Komma zu viel |
| `Nach dem Wert darf nichts mehr kommen.` | hinter dem JSON steht noch etwas anderes |
| `Ein Objekt wird nicht geschlossen. Es fehlt ein '}'.` | eine geschweifte Klammer fehlt; die Meldung nennt die Stelle, an der sie geöffnet wurde |
| `Ein Array wird nicht geschlossen. Es fehlt ein ']'.` | eine eckige Klammer fehlt |
| `Der Text enthält keinen JSON-Wert.` | der Text besteht nur aus Leerraum und Kommentaren |
| `Nach dem Doppelpunkt fehlt ein Wert.` | der letzte Eintrag ist unvollständig |

**Nichts wird korrigiert und nichts geraten.** Sie bekommen Ihre Eing zurück,
unverändert, und eine Zeile mit der Stelle, an der nachzusehen ist. Das ist
Absicht: Der Text landet als Datei, Anfrage oder Antwort irgendwo, und niemand
könnte hinterher sagen, was sich geändert hat.

Prüfen Sie vor dem Formatieren die **Statuszeile**. Steht dort die Warnung, dass
der Text nicht nach JSON aussieht, sind die drei Knöpfe grau — das ist die
Antwort, kein Fehler. Steht dort nichts, ist der Text erkannt und die Knöpfe
sind frei.

### Hell oder dunkel

Der Knopf oben rechts zeigt an, **wohin** Sie wechseln, nicht wo Sie sind: Im
hellen Design steht dort ein Mondzeichen mit `Dunkel`, im dunklen ein
Sonnenzeichen mit `Hell`. Ein Klick schaltet um. Die Wahl gilt für diese
Sitzung; beim nächsten Start ist es wieder hell. Das Programm merkt sich die
Einstellung absichtlich nicht — es schreibt nichts auf Ihre Festplatte.

![Oberfläche im dunklen Design](bild-dunkel.png)

## 7. Optional: ein Hotkey für den Formatter

Wenn Sie den Formatter oft brauchen, richtet Raycast einen Tastekurzbefehl ein,
mit dem Sie das Fenster jederzeit aufrufen. Voraussetzung ist
[Raycast](https://www.raycast.com/) (kostenlos).

1. Öffnen Sie Raycast und drücken Sie **Cmd + Komma**
2. Gehen Sie auf **Script Commands** und dann auf **Add Script Directory**
3. Wählen Sie den Ordner `raycast` im Programmordner `jsonClipboardFormatter`
4. Drücken Sie **Alt + Leertaste**, tippen Sie **JSON Formatter** und drücken
   Sie **Cmd + K**
5. Wählen Sie **Configure Command** und dann **Record Hotkey**
6. Drücken Sie die Taste, die Sie wollen

Ab jetzt genügt Ihre gewählte Taste, um das Programm zu öffnen. Läuft es schon,
holt der Hotkey das vorhandene Fenster nach vorn, statt ein zweites zu
öffnen — zwei Fenster würden beide dieselbe Zwischenablage bearbeiten.

Damit das Vordergrundholen klappt, braucht Raycast die Berechtigung
**Steuerung von Computer** (englisch *Accessibility*). Steht sie nicht in den
Systemeinstellungen, bleibt das Fenster hinten, und der Aufruf endet trotzdem
ohne jede Meldung.

Schlägt der Start fehl, legt Raycast eine Benachrichtigung mit dem Wortlaut
`start.sh ist fehlgeschlagen - im Terminal mit -Pruefen pruefen.` an. Dann
starten Sie das Programm einmal im Terminal, wie in Abschnitt 8 beschrieben —
dort sehen Sie, woran es liegt.

## 8. Wenn etwas nicht klappt

### Der Knopf „JSON Formatieren" ist grau

Die Statuszeile erklärt es: Im Textfeld steht nichts, womit JSON anfangen kann —
kein `{`, kein `[`, kein `"`, kein `-`, keine Ziffer und auch nicht das Wort
`true`, `false` oder `null`. Drücken Sie **Clipboard einlesen** und schauen Sie
nach, ob die Statuszeile umspringt. Oder tippen Sie eine geschweifte Klammer `{`
in das Feld — damit ist der Text erkannt und die Knöpfe werden frei.

### „error: could not find or load main class" oder „unable to access jarfile"

Das Programm wurde noch nicht übersetzt. Führen Sie einmal aus:

```sh
./start.sh -Neu
```

### Das Skript bricht mit „Fehler: …" ab

| Meldung | Ursache |
|---|---|
| `Fehler: kein Java gefunden. Java 17 oder neuer installieren.` | Java fehlt oder ist zu alt — zurück zu Schritt 1 |
| `Fehler: weder ./mvnw noch mvn gefunden - Jar kann nicht gebaut werden.` | Es muss übersetzt werden, aber es ist kein Maven da |
| `Fehler: target/JsonClipboardFormatter-0.1.jar fehlt trotz Bauvorgang.` | Der Build hat nichts erzeugt. Im Namen steckt die Versionsnummer, sie wandert bei jedem Versionswechsel mit |

### „mvn: command not found" beim Bauen

Sie brauchen Maven nur zum Übersetzen. Installieren Sie es mit
[Homebrew](https://brew.sh/): `brew install maven`. Zum reinen Benutzen können
Sie Maven auch wieder deinstallieren, sobald einmal gebaut wurde.

### „./start.sh: Permission denied"

Einmalig die Ausführungsrechte setzen:

```sh
chmod +x start.sh
```

### Der Finder lässt `start.command` nicht öffnen

Das ist die macOS-Sperre für Dateien aus dem Internet. Starten Sie das Programm
in diesem Fall über das Terminal. Alternativ im Finder: Rechtsklick auf
`start.command` → **Öffnen** → im Dialog erneut **Öffnen**.

### Das Fenster öffnet sich, ist aber nicht zu sehen

Es kann hinter dem gerade benutzten Programm liegen. Klicken Sie im Dock auf das
Symbol des Programms, oder richten Sie den Raycast-Hotkey aus Abschnitt 7 ein.

### Was funktioniert, wenn Sie mehr wissen wollen

`./start.sh -Pruefen` sagt Ihnen, welchen Java- und welchen Maven-Pfad das
Programm findet, ob die Programmdatei vorhanden ist und wie viele Argumente an
Java durchgereicht werden. Auf einem Mac mit installiertem Java sieht das so
aus:

```
Java    : /usr/bin/java  (PATH)
Konfig  : keine
Maven   : mvn
Jar     : target/JsonClipboardFormatter-0.1.jar (vorhanden)
Bauen   : falls Quellen neuer
Argumente: 0 an die JVM
```

| Zeile | Bedeutung |
|---|---|
| `Java` | das gefundene Java und, im Klammerzusatz, woher es stammt. Gesucht wird in dieser Reihenfolge: `mitgeliefert`, `start.local.conf`, `JSONFORMATTER_JAVA`, `JAVA_HOME`, `PATH`, `java_home` |
| `Konfig` | die `start.local.conf`, falls es eine gibt — siehe Abschnitt 5 |
| `Maven` | gefundenes Maven oder `nicht gefunden`. Fehlt es, ist das beim ersten Übersetzen ein Problem, später nicht mehr |
| `Jar` | die übersetzte Programmdatei. `fehlt` ist beim ersten Mal normal |
| `Bauen` | ob beim Starten übersetzt werden muss |
| `Argumente` | was an Java durchgereicht wird |

Steht bei `Jar` **fehlt**, übersetzen Sie einmal mit `./start.sh -Neu`.

## 9. Was das Programm nicht macht

- **Kein Internet.** Es sendet nichts nach außen und speichert nichts. Den
  JSON-Text sehen nur Sie und Ihre Zwischenablage.
- **Kein Speichern.** Es legt keine Dateien an und liest keine. Auch die
  Designwahl und die Fensterposition werden nirgends notiert.
- **Kein Zurück.** Die ursprüngliche Fassung liegt nach dem Formatieren nicht
  mehr in der Zwischenablage — kopieren Sie sie vorher noch einmal, wenn Sie
  beide Fassungen brauchen. Auch im Textfeld gibt es kein Rückgängig.
- **Kein Raten.** Liest das Programm einen Text nicht als JSON, bleibt er
  unverändert stehen. Es ergänzt keine fehlenden Anführungszeichen und
  erfindet keine Kommata.
- **Keine Kommentare in der Ausgabe.** Was als Kommentar oder als
  Nachlaufkomma eingeht, geht verloren. Das ist kein Fehler, sondern die
  Ausgabe von strengem JSON.

## 10. Wenn Sie den Mac wechseln

Kopieren Sie den Ordner `jsonClipboardFormatter` auf den neuen Rechner. Ist dort
noch kein Java installiert, holen Sie das zuerst nach. Einmal übersetzt läuft
das Programm ohne Maven weiter. Eine eigene `start.local.conf` wandert dabei mit
— sie enthält Ihre Pfade und gilt auf dem neuen Rechner so, wie sie hier gilt.

Mehr zum Programm selbst — Bauen, Aufbau, Tests, Versionierung — steht in der
[README](../README.md).
