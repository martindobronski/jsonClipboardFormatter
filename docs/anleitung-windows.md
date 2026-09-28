# Anleitung für Windows

Diese Anleitung richtet sich an alle, die den JSON Clipboard Formatter auf einem
Windows-Rechner benutzen wollen. Sie setzt keine Programmierkenntnisse voraus.

> **Sie suchen die Anleitung für einen Mac?** Dann nehmen Sie
> [anleitung-macos.md](anleitung-macos.md). Dort ist Java Voraussetzung, weil es
> für den Mac kein ZIP gibt.

> ### Stand der Prüfung
>
> Diese Anleitung ist aus dem Skript `start.bat` herausgeschrieben und an jeder
> Stelle mit dem Quelltext abgeglichen. Sie ist außerdem **auf einem echten
> Windows-Rechner durchlaufen**: Ein automatischer Lauf auf `windows-latest`
> führt die vollständige Testsuite aus, ruft `start.bat -Pruefen` auf, prüft
> eine `start.local.conf` mit Leerzeichen im Pfad und mit einem Pfad, den es gar
> nicht gibt, baut das Programm ohne fertiges Jar neu und startet das Programm
> wirklich — danach wird gefragt, ob eine JVM läuft. Ein zweiter Lauf baut genau
> die Verzeichnisstruktur auf, die Sie nach dem Entpacken des ZIPs vorfinden,
> blendet anschließend `JAVA_HOME` und den Suchpfad aus und verlangt, dass die
> gestartete JVM aus dem mitgelieferten Ordner `jre/` kommt.
>
> Das ist der Grund, warum Sie für den ersten Start weder Java noch Maven
> brauchen: Es ist nicht behauptet, sondern auf einem Rechner ohne Java
> nachgewiesen worden. Wenn bei Ihnen etwas abweicht, beginnen Sie mit
> Abschnitt 8; dort steht, wie Sie den Fehlertext zu Gesicht bekommen. Der
> erste sinnvolle Test ist immer `start.bat -Pruefen`.

---

## 1. Wofür ist das Programm?

Sie markieren JSON in einem beliebigen Programm, kopieren ihn mit Strg+C in
die Zwischenablage und formatieren ihn mit einem Klick:

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
Strg+V an die gewünschte Stelle setzen können. Sie brauchen dafür **kein
Programm, das JSON kennt, und keine Internetverbindung**.

Genauso geht es umgekehrt: **JSON Packen** macht aus vielen Zeilen wieder eine
Zeile. Das ist die Form, in der viele Werkzeuge ihre Einstellungen oder
Anfragen erwarten.

## 2. Was Sie brauchen

Für den normalen Fall — Sie wollen das Programm benutzen — **brauchen Sie nichts
außer Windows.** Kein Java, kein Maven, keine Installation.

| Weg                                | Java | Maven | Internet beim Start |
| ---------------------------------- | ---- | ----- | ------------------ |
| **ZIP entpacken** (Abschnitt 3)    | —    | —     | nein               |
| Aus dem Quelltext (Abschnitt 4)    | 17+  | ja    | nein               |

Im ZIP liegt eine vollständige Java-Laufzeit bereits bei. Nach den Angaben des
Release-Texts ist das ZIP 57 MB groß, entpackt sind es rund 180 MB — fast alles
die Laufzeit, die Anwendung selbst liegt bei knapp 1 MB. Dafür startet das
Programm auf einem Rechner, auf dem noch nie Java war. Die genaue Downloadgröße
nennt der Bauvorgang, wenn er das ZIP packt.

Wollen Sie die Laufzeit nicht mitnehmen, lassen Sie den Ordner `jre/` nach dem
Entpacken einfach löschen und installieren Java selbst — dann nimmt `start.bat`
automatisch Ihr Java. Beides gleichzeitig ist unnötig; der Ordner `jre/` hat
Vorrang.

Auf einem Windows-Rechner mit ARM-Prozessor brauchen Sie die ARM-Version von
Java, nicht die für Intel. Ein ZIP gibt es derzeit nur für x64.

## 3. Der einfache Weg: ZIP entpacken und starten

1. Das ZIP herunterladen und entpacken. Dabei entsteht automatisch ein neuer
   Ordner `jsonClipboardFormatter-0.1-windows-x64`; den bitte **nicht** wieder
   löschen, sonst fehlt dem Programm sein Zuhause.
2. In diesem Ordner Doppelklick auf `start.bat`.

Was in diesem Ordner liegt, und nichts weiter:

| Pfad                                       | Wofür                                        |
| ------------------------------------------ | -------------------------------------------- |
| `start.bat`                                | das Skript, das Sie starten                 |
| `start.conf.example`                       | Vorlage, falls Sie ein festes Java wollen    |
| `jre/`                                     | die mitgelieferte Java-Laufzeit              |
| `target\JsonClipboardFormatter-0.1.jar`    | das fertige Programm                         |
| `README.md`                                | mehr über Bau und Aufbau                     |
| `docs/anleitung-windows.md`                | diese Anleitung                              |
| `docs/aenderungen.md`                      | was sich wann geändert hat                   |
| `docs/bild-hell.png`, `docs/bild-dunkel.png` | die beiden Ansichten des Programms          |

**Kein `pom.xml`, kein `src/`, kein Maven.** Im Quelltextordner stehen die
beiden ersten, im entpackten ZIP nicht — dort liegt nur, was Sie zum Benutzen
brauchen.

Danach öffnet sich das Programm. Es läuft aus einem schwarzen Fenster, das
danach im Hintergrund bleibt — Sie schließen es nicht, solange Sie das Programm
benutzen. Das ist kein Fehler, sondern das Anzeigefenster der Laufzeit.

Sollte etwas nicht stimmen, prüfen Sie zuerst in einem PowerShell-Fenster im
selben Ordner:

```powershell
start.bat -Pruefen
```

Bei einem frisch entpackten ZIP sieht das so aus:

```
Java    : C:\...\jre\bin\java.exe  (mitgeliefert)
Konfig  : keine
Maven   : nicht gefunden
Jar     : target\JsonClipboardFormatter-0.1.jar - vorhanden
Bauen   : falls Quellen neuer
Argumente: 0 an die JVM
```

Der Klammerzusatz sagt Ihnen, **woher** das Java stammt. Bei
`(mitgeliefert)` läuft alles auf der Laufzeit aus dem ZIP, und Sie können sich
sicher sein, dass keine zweite Java-Installation auf Ihrem Rechner stört.
Steht dort `(JAVA_HOME)`, `(PATH)`, `(JSONFORMATTER_JAVA)` oder
`(start.local.conf)`, wurde der Ordner `jre/` nicht gefunden und ein eigenes
Java genommen — was auch in Ordnung ist, sofern es Java 17 oder neuer ist.

`Maven   : nicht gefunden` ist beim entpackten ZIP **richtig** und kein Fehler:
Maven wird nur gebraucht, um das Programm aus dem Quelltext zu übersetzen.

`Konfig  : keine` heißt, dass es keine `start.local.conf` gibt. Das ist der
Normalfall; sie wird nur gebraucht, wenn Sie einem festen Pfad folgen möchten.

Steht eine `start.local.conf` im Ordner, nennt `Konfig  :` ihren vollen Pfad —
und darunter stehen für jeden Eintrag, der wirklich gelesen wurde, eine Zeile
`Gelesen : Java=...`, `Gelesen : Maven=...` oder `Gelesen : Maven-Jdk=...`.
Fehlt eine solche Zeile, wurde der Eintrag nicht übernommen.

Unter `Argumente:` steht, wie viele Argumente an Java durchgereicht werden. Was
durchgereicht wird, steht darunter, mit Leerstellen eingerückt.

### Ein bestimmtes Java erzwingen

Das gibt es zwei Wege, und sie unterscheiden sich in der Dauer.

**Für immer, in diesem Ordner:** Kopieren Sie die Vorlage und tragen Sie den Pfad
ein. Im ZIP liegt `start.conf.example` daneben, im Repository dieselbe Datei:

```bat
copy start.conf.example start.local.conf
notepad start.local.conf
```

```properties
java=C:\Program Files\Java\jdk-17\bin\java.exe
```

Ohne Anführungszeichen — Leerzeichen im Pfad sind in Ordnung. Alles hinter einem
`#` ist Kommentar, eine kaputte Zeile wird übersprungen. Starten Sie danach wie
gewohnt mit einem Doppelklick auf `start.bat`; `start.bat -Pruefen` zeigt Ihnen
danach `Konfig  : ...start.local.conf` und `(start.local.conf)` als Quelle.

Steht in der Datei ein Pfad, den es nicht gibt, startet das Programm trotzdem:
Das Skript prüft jeden Pfad vorher und geht dann weiter zu `JAVA_HOME` und dem
Suchpfad. Sie sehen am Klammerzusatz, welcher Eintrag wirklich benutzt wurde.

**Nur für dieses eine Fenster:** setzen Sie die Variable `JSONFORMATTER_JAVA`.
Sie gilt, bis das Fenster zu ist, und hat Vorrang vor `start.local.conf`,
`JAVA_HOME` und dem Suchpfad — damit lässt sich ein unerwünschtes Java
zuverlässig ausschließen. Gegen den Ordner `jre/` eines entpackten ZIPs hilft
sie allerdings nicht: der wird immer zuerst genommen, damit das ZIP auf sich
selbst läuft.

```powershell
$env:JSONFORMATTER_JAVA = "C:\Program Files\Java\jdk-17\bin\java.exe"
start.bat
```

Gilt nur für dieses Fenster. Für dauerhaft:

```powershell
[Environment]::SetEnvironmentVariable("JSONFORMATTER_JAVA", "C:\Program Files\Java\jdk-17\bin\java.exe", "User")
```

### Auch Maven und das JDK fürs Übersetzen festlegen

Dieselbe Datei nimmt noch zwei Einträge auf, die nur beim Übersetzen aus dem
Quelltext etwas tun:

```properties
maven=C:\Programme\apache-maven-3.9.9\bin\mvn.cmd
maven-jdk=C:\Program Files\Java\jdk-17
```

`maven` zeigt auf die `mvn.cmd`, `maven-jdk` auf den Ordner **mit** `bin`
darin, nicht auf die `java.exe`. `maven-jdk` wird nur dann gebraucht, wenn Maven
mit einem anderen Java übersetzen soll als das, mit dem die App startet — Maven
nimmt sein Java aus `JAVA_HOME`. Leer lassen heißt: `JAVA_HOME` bleibt, wie es
ist. Gebraucht wird Maven 3.6.3 oder neuer; ältere Versionen lehnen die
verwendeten Bausteine ab.

## 4. Der andere Weg: aus dem Quelltext übersetzen

Dieser Weg ist nur nötig, wenn Sie das Programm selbst verändern wollen oder
wenn Sie ohne den Download auskommen. Er braucht **Java 17 oder neuer** und
**Maven**; beides können Sie vorher prüfen.

### Schritt 1: Java prüfen

Öffnen Sie PowerShell — etwa mit **Win** tippen, „PowerShell" eingeben,
Enter — und prüfen Sie:

```powershell
java -version
```

Sieht die Antwort so oder ähnlich aus, ist alles in Ordnung:

```
openjdk version "17.0.11" 2024-04-16
OpenJDK Runtime Environment (build 17.0.11+9)
```

Kommt stattdessen ein Fehler wie *Der Befehl "java" ist nicht
erkannt* oder *'java' is not recognized*, fehlt Java oder es ist zu alt.
Installieren Sie es über [Azul Zulu](https://azul.com/downloads/)
(„JDK", Windows, x64 oder ARM64).

### Schritt 2: Das Programm holen

Wechseln Sie in einen Ordner, in dem das Programm liegen darf. Eine
PowerShell öffnet zum Beispiel in Ihrem Benutzerordner, was passt. Dann:

```powershell
git clone https://github.com/martindobronski/jsonClipboardFormatter.git
cd jsonClipboardFormatter
```

### Erst prüfen, dann übersetzen

Bevor Sie zum ersten Mal starten, prüfen Sie die Umgebung. Das ist die schnellste
Art herauszufinden, woran es liegt:

```bat
start.bat -Pruefen
```

Sie sehen dann zum Beispiel:

```
Java    : C:\Program Files\Java\jdk-17\bin\java.exe  (JAVA_HOME)
Konfig  : keine
Maven   : C:\Programme\apache-maven-3.9.9\bin\mvn.cmd
Jar     : target\JsonClipboardFormatter-0.1.jar - fehlt
Bauen   : ja
Argumente: 0 an die JVM
```

Die Zeilen bedeuten:

| Zeile        | Bedeutung                                                                                                      |
| ------------ | -------------------------------------------------------------------------------------------------------------- |
| `Java`       | gefundenes Java, im Klammerzusatz woher. „Fehler: kein Java gefunden" heißt: zurück zu Schritt 1                |
| `Konfig`     | die `start.local.conf`, falls es eine gibt — siehe „Ein bestimmtes Java erzwingen"                              |
| `Gelesen`    | die Einträge, die wirklich aus der `start.local.conf` übernommen wurden. Fehlt die Zeile, ist nichts übernommen |
| `Maven`      | gefundenes Maven oder „nicht gefunden". Fehlt es, ist das beim ersten Übersetzen ein Problem, später nicht mehr |
| `Jar`        | die übersetzte Programmdatei. „fehlt" ist beim ersten Mal normal                                                |
| `Bauen`      | ob beim Starten übersetzt werden muss                                                                           |
| `Argumente`  | was an Java durchgereicht wird                                                                                  |

Steht bei `Jar` **fehlt**, übersetzen Sie einmal:

```powershell
.\start.bat -Neu
```

Das dauert beim ersten Mal etwa eine Minute, weil dabei Dateien nachgeladen
werden.

## 5. Schritt 3: Starten

Ab jetzt genügt ein Doppelklick auf **`start.bat`** im Explorer. Oder in der
PowerShell:

```powershell
.\start.bat
```

Das Skript übersetzt vorher automatisch neu, falls es nötig ist, und startet
danach das Programm.

> **Wichtig:** Das Programm läuft im selben schwarzen Fenster, aus dem Sie es
> gestartet haben — dieses Fenster bleibt offen und im Vordergrund, das
> Programmfenster liegt dahinter. Das ist beabsichtigt: Schließen Sie das
> schwarze Fenster, beendet sich auch das Programm. Beenden Sie das Programm
> also über dessen eigenes Fenster.

### Was die Schalter können

| Befehl               | Wirkung                                                  |
| -------------------- | -------------------------------------------------------- |
| `start.bat`          | starten, übersetzt vorher bei Bedarf neu                 |
| `start.bat -Neu`     | vorher neu übersetzen, auch wenn schon alles aktuell ist |
| `start.bat -Pruefen` | nur nachsehen, ob die Umgebung passt — startet nichts    |
| `start.bat -Xmx512m` | gibt 512 MB Speicher für das Programm frei               |

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

Steht gerade keine Meldung in der Statuszeile, sehen Sie dort stattdessen den
Zähler in der Form `14 Zeilen, 200 Zeichen`; bei einem leeren Feld steht dort
`0 Zeilen, 0 Zeichen`. Er nennt dasselbe wie die Zahlen links: Bildschirmzeilen.
Ein Zeilenumbruch zählt dabei als ein Zeichen, egal ob Ihr Text ihn als `CRLF`
oder als `LF` gespeichert hat.

### Der eine wichtige Punkt: die Statuszeile

**Die Statuszeile ist das Einzige, was Ihnen sagt, ob die Knöpfe etwas tun.**

| Inhalt des Feldes        | Statuszeile                                                                                       | Knöpfe                         |
| ------------------------ | ------------------------------------------------------------------------------------------------- | ------------------------------ |
| leer                     | *(leer, stattdessen der Zähler)*                                                                   | nur *Clipboard einlesen*       |
| sieht nach JSON aus      | *(leer, stattdessen der Zähler)*                                                                   | alle frei                      |
| sieht nicht nach JSON aus | ⚠ Der Text sieht nicht nach JSON aus. Formatieren, Packen und Schreiben sind blockiert.             | nur *Clipboard einlesen*       |

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

| Knopf                       | Taste | Wirkung                                                      |
| --------------------------- | ----- | ------------------------------------------------------------ |
| **Clipboard einlesen**      | `L`   | holt den Text aus Ihrer Zwischenablage in das Feld           |
| **JSON Formatieren**        | `F`   | macht den Text im Feld lesbar, ein Element je Zeile          |
| **JSON Packen**             | `M`   | macht aus dem Text im Feld eine einzige Zeile                |
| **Ins Clipboard schreiben** | `C`   | legt den Text aus dem Feld in die Zwischenablage             |
| **Beenden**                 | `B`   | beendet das Programm; steht rechts neben den anderen Knöpfen |

Die Spalte „Taste" nennt das Kürzel aus einem Buchstaben, den die Beschriftung
unterstreicht. **Beenden** ist kein rechter Knopf, sondern ein Textlink — er
sieht deshalb anders aus als die vier Aktionen.

**Das sind zwei Schritte, nicht einer:** *JSON Formatieren* und *JSON Packen*
legen nichts in die Zwischenablage. Erst *Ins Clipboard schreiben* tut das. So
können Sie sich das Ergebnis ansehen, bevor Sie es irgendwo einfügen.

**Formatieren oder Packen?** Beides liest denselben Text und schreibt dasselbe
zurück — nur in anderer Gestalt. *Formatieren* ist für Menschen, *Packen* für
Maschinen: etwa für eine Befehlszeile, eine `.env`-Datei oder einen
Aufrufparameter.

### Normaler Arbeitsablauf

1. JSON in Ihrem Programm markieren und mit **Strg+C** kopieren
2. **Clipboard einlesen** → `✓ JSON geladen. Formatieren, Packen und Schreiben sind freigeschaltet.`
3. **JSON Formatieren** → `✓ JSON lesbar formatiert.`
4. **Ins Clipboard schreiben** → `✓ JSON erfolgreich in die Zwischenablage kopiert.`
5. In Ihr Programm zurück, **Strg+V** einfügen

War beim Formatieren nichts zu tun, meldet das Programm das ausdrücklich:
`✓ War schon lesbar formatiert.` Beim Packen entsprechend `✓ War schon
gepackt.` Erfolgsmeldungen verschwinden nach vier Sekunden von selbst. Warnungen
und Fehler bleiben stehen, bis Sie wieder etwas tun.

Steht in der Zwischenablage Text, der nicht nach JSON aussieht, meldet
**Clipboard einlesen** das: `⚠ Text geladen, sieht aber nicht nach JSON aus.
Aktionen blockiert.` Der Text steht trotzdem im Feld — Sie können ihn ansehen
und von Hand ändern. Steht dort gar nichts Lesbares, meldet das Programm
`⚠ Die Zwischenablage ist leer.` beziehungsweise `⚠ Die Zwischenablage enthält
keinen lesbaren Text.`

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

**Auch Skalarwerte sind gültig.** `42`, `"text"` und `true` sind vollständige
JSON-Dokumente und werden genauso formatiert und gepackt wie ein Objekt.

**Packen** macht daraus genau eine Zeile ohne jeden Leerraum zwischen den
Werten, mit einem abschließenden Zeilenumbruch:

```
{"name":"Wert","liste":[1,2],"leer":{},"n":null}
```

Vom Text, den **Ins Clipboard schreiben** in die Zwischenablage legt, werden
Leerzeichen und Zeilenumbrüche am Anfang und am Ende abgeschnitten. Das ist der
einzige Schritt, der den Text verändert, ohne ihn anzuzeigen. Steht im Feld
nur Leerraum, meldet der Knopf `⚠ Das Textfeld ist leer. Nichts zu kopieren.`

### Wenn im JSON etwas nicht stimmt

Statt zu raten, lässt das Programm die Eingabe stehen und sagt Ihnen, wo es
aufgehört hat:

```
❌ Zeile 3, Spalte 3: Zwei Kommas nacheinander sind zu viel. Der Text ist unverändert geblieben.
```

Der Grund steht wörtlich so im Programm, er ändert sich nicht. Die häufigsten:

| Meldung | Bedeutung |
|---|---|
| `Unerwartetes Zeichen 'E'.` | an dieser Stelle steht etwas, das JSON nicht kennt — auch dann, wenn es ein ganz normaler Buchstabe ist. Das Zeichen selbst steht in den Anführungszeichen |
| `Blockkommentar wird nicht geschlossen.` | ein `/*` wurde nie durch `*/` beendet |
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
| `Falsche Klammer: hier gehört '}' hin.` | eine geschweifte Klammer schließt ein Array, das eine eckige braucht |
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

### Zeilenumbrüche: Unix-Zeilenenden, auch auf Windows

Anders als beim Programm, aus dem Ihr Text stammt, wandern die Zeilenenden
immer nach Unix: Der Ausgabe ist ein `LF` je Umbruch, ein Wagenrücklauf wie bei
`CRLF` (`0D 0A`) wird nie geschrieben. Das gilt für **JSON Formatieren** wie
für **JSON Packen** — und damit auch für alles, was **Ins Clipboard
schreiben** in die Zwischenablage legt.

Das ist Absicht, keine Ungenauigkeit: Die Ausgabe ist strenges JSON, und das
wird auf jeder Plattform gleich geschrieben. Planen Sie darauf ein, wenn Sie
den Text in eine Datei speichern, die ein anderes Programm streng prüft.

So prüfen Sie, was in Ihrer Datei wirklich steht:

| Editor                 | Wo Sie es sehen                                                                        |
| ---------------------- | -------------------------------------------------------------------------------------- |
| **Notepad++**          | *Ansicht → Symbol anzeigen → Zeilenende anzeigen*                                      |
| **Visual Studio Code** | die Anzeige `CRLF` oder `LF` unten in der Statusleiste, nach einem Klick auf die Datei |

### Hell oder dunkel

Der Knopf oben rechts zeigt an, **wohin** Sie wechseln, nicht wo Sie sind: Im
hellen Design steht dort ein Mondzeichen mit `Dunkel`, im dunklen ein
Sonnenzeichen mit `Hell`. Ein Klick schaltet um. Die Wahl gilt für diese
Sitzung; beim nächsten Start ist es wieder hell. Das Programm merkt sich die
Einstellung absichtlich nicht — es schreibt nichts auf Ihre Festplatte.

Wechseln Sie das Design, während das Fenster vergrößert oder im echten
Vollbild ist, bleibt seine Größe unverändert. Das kostet nichts, erhält aber
den Zustand, in dem Sie das Fenster hatten.

![Oberfläche im dunklen Design](bild-dunkel.png)

## 7. Optional: Schnellzugriff über eine Tastenkombination

Wenn Sie den Formatter oft brauchen, können Sie den Start an eine Taste binden:

1. Im Explorer mit der rechten Maustaste auf `start.bat` klicken
2. **Eigenschaften** öffnen
3. Unter **Kurzbefehl** in das Feld „Verknüpfung" eine Tastenkombination eintippen,
   etwa `Strg+Alt+J`
4. Mit **Übernehmen** schließen

Ab sofort startet die Kombination das Programm.

> Der erste Start hängt davon ab, wie schnell Ihr Rechner ist — beim ersten Mal
> muss die Java-Laufzeit von der Festplatte gelesen werden. Danach geht es
> schnell.

## 8. Wenn etwas nicht klappt

### Der erste Schritt: aus einem Fenster heraus starten

Der wichtigste Rat für Windows: Starten Sie das Skript **in einem Fenster**, nicht
per Doppelklick. Dann sehen Sie jede Meldung.

In der **PowerShell**:

```powershell
cd C:\Pfad\zu\jsonClipboardFormatter
.\start.bat -Pruefen
```

Was Sie dort sehen, sagt fast immer, woran es liegt. Die Meldungen, die das
Skript ausgibt:

| Meldung                                                                       | Ursache                                                                                                     |
| ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `Fehler: kein Java gefunden. Java 17 oder neuer installieren.`                | Java fehlt oder ist zu alt — Schritt 1. Es folgt der Zusatz `Falls installiert: JAVA_HOME setzen oder Java in den PATH aufnehmen.` |
| `Fehler: Jar fehlt oder ist veraltet, aber weder mvnw.cmd noch mvn gefunden.` | Im entpackten ZIP fehlt das fertige Programm — etwa weil der Ordner `target/` gelöscht wurde. Im Quelltext: Maven fehlt, obwohl noch nicht übersetzt wurde |
| `Fehler: der Build ist fehlgeschlagen.`                                       | Beim Übersetzen ging etwas schief — die ausführliche Meldung steht darüber                                  |
| `Fehler: "target\JsonClipboardFormatter-0.1.jar" fehlt trotz Bauvorgang.`      | Der Build hat nichts erzeugt. Im Namen steckt die Versionsnummer, sie wandert mit jedem Versionswechsel mit |

Im entpackten ZIP bricht `start.bat` genau dann ab, wenn der Programmdatei weder
aus einer `pom.xml` noch aus dem mitgelieferten Jar ein Name abgeleitet werden
kann — es liegt keine `pom.xml` bei, also zählt allein das `target`-Verzeichnis.

### „Java wurde nicht gefunden", obwohl Java installiert ist

Das Skript sucht der Reihe nach im Ordner `jre/`, in der `start.local.conf`, in
der Variable `JSONFORMATTER_JAVA`, in `JAVA_HOME` und zuletzt im Suchpfad. Steht in
`start.bat -Pruefen` weder `(mitgeliefert)` noch `(JAVA_HOME)`, ist Ihr Java an
einer Stelle installiert, die der Skript nicht kennt. Setzen Sie dann
`JAVA_HOME` — oder nehmen Sie Java in den Suchpfad auf:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")
```

Danach PowerShell neu öffnen. Der Pfad muss auf den Ordner zeigen, in dem
`bin` liegt, **nicht** auf die `java.exe`.

### „mvn" wird nicht gefunden, obwohl Maven installiert ist

Das Skript sucht in dieser Reihenfolge: dem Eintrag `maven` aus der
`start.local.conf`, einer `mvnw.cmd` im selben Ordner, dann `mvn` im
Suchpfad, dann `%MAVEN_HOME%\bin\mvn.cmd`. Es sagt Ihnen im Prüfmodus, was davon
gefunden wurde:

```bat
.\start.bat -Pruefen
```

Steht dort `Maven   : nicht gefunden`, ist keiner der vier Wege sichtbar.
Prüfen Sie in dieser Reihenfolge:

1. Ist `mvn.cmd` im Suchpfad? In einer neuen PowerShell:
   
   ```powershell
   where.exe mvn
   ```
   
   Findet der Befehl nichts, hilft nur der nächste Punkt.
2. Ist `MAVEN_HOME` gesetzt und zeigt auf den Ordner **mit** `bin` darin?
   
   ```powershell
   $env:MAVEN_HOME
   ```
   
   Bei einem leeren Ergebnis hilft nur Punkt 3.
3. Tragen Sie Maven im Suchpfad ein, oder setzen Sie `MAVEN_HOME`:
   
   ```powershell
   [Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\tools\apache-maven-3.9.9", "User")
   ```

Danach PowerShell neu öffnen — die Umgebung wird erst beim Start gelesen.

> Ein Maven-Pfad mit Leerzeichen im Namen, wie `C:\Program Files\...`,
> funktioniert. Das ist auf einem echten Windows-Rechner mit einer
> `start.local.conf` geprüft worden, die genau so einen Pfad trug.

### Beim Schließen des schwarzen Fensters: „Terminate batch job (Y/N)?"

Wenn Sie das schwarze Fenster schließen, während das Programm läuft, fragt
Windows das. Antworten Sie mit **N** oder **J** — je nachdem, ob Sie das
Programm behalten wollen. Besser: beenden Sie über das Programmfenster selbst.

### „unrecognized option" beim Start

Dann steht ein Schalter an falscher Stelle. Die beiden eigenen Schalter
`-Neu` und `-Pruefen` gehören **an den Anfang**, alles andere (zum Beispiel
Speicherangaben) dahinter:

```powershell
.\start.bat -Pruefen -Xmx512m
```

### Der Knopf „JSON Formatieren" ist grau

Die Statuszeile erklärt es: Im Textfeld steht nichts, womit JSON anfangen kann.
Drücken Sie **Clipboard einlesen** und schauen Sie nach, ob die Statuszeile
umspringt. Oder tippen Sie eine geschweifte Klammer `{` in das Feld — damit ist
der Text erkannt und die Knöpfe werden frei.

### „error: could not find or load main class" oder „unable to access jarfile"

Das Programm wurde noch nicht übersetzt. Führen Sie einmal aus:

```powershell
.\start.bat -Neu
```

### Das Programmfenster öffnet sich, ist aber nicht zu sehen

Es kann hinter dem gerade benutzten Programm liegen. Starten Sie es über die
Tastenkombination aus Abschnitt 7 erneut — liegt es noch offen, kommt es nach
vorn, statt ein zweites zu öffnen.

### Windows blockiert das Skript

Kommt die Meldung eines Virusenscanners, ist das eine Fehlmeldung: `start.bat`
ist eine reine Textdatei, die Java startet. Starten Sie es in dem Fall aus dem
Explorer heraus mit **Rechtsklick → Ausführen** oder in der PowerShell, wie in
diesem Abschnitt beschrieben.

## 9. Was das Programm nicht macht

- **Kein Internet.** Es sendet nichts nach außen und speichert nichts. Den
  JSON-Text sehen nur Sie und Ihre Zwischenablage.
- **Kein Speichern.** Es legt keine Dateien an und liest keine. Auch die
  Designwahl wird nirgends notiert.
- **Kein Zurück.** Die ursprüngliche Fassung liegt nach dem Formatieren nicht
  mehr in der Zwischenablage — kopieren Sie sie vorher noch einmal, wenn Sie
  beide Fassungen brauchen.
- **Kein Raten.** Liest das Programm einen Text nicht als JSON, bleibt er
  unverändert stehen. Es ergänzt keine fehlenden Anführungszeichen und
  erfindet keine Kommata.
- **Keine Kommentare in der Ausgabe.** Was als Kommentar oder als
  Nachlaufkomma eingeht, geht verloren. Das ist kein Fehler, sondern die
  Ausgabe von strengem JSON.

## 10. Auf einen anderen Rechner umziehen

Kopieren Sie den Ordner `jsonClipboardFormatter` auf den neuen Rechner. Ist dort
noch kein Java installiert, holen Sie das zuerst nach. Einmal übersetzt läuft
das Programm ohne Maven weiter. Eine eigene `start.local.conf` wandert dabei mit
— sie enthält Ihre Pfade und gilt auf dem neuen Rechner so, wie sie hier gilt.

Die mitgelieferte Laufzeit im Ordner `jre/` wandert mit und ist auf einem
Rechner ohne Java immer die erste Wahl.

Mehr zum Programm selbst — Bauen, Aufbau, Tests, Versionierung — steht in der
[README](../README.md).
