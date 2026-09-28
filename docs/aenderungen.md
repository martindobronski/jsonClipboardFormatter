# Was sich geändert hat

Kurz gehalten, mit dem Grund dahinter. Wer den Grund nicht braucht, liest die
fett gedruckte Zeile und lässt den Rest stehen.

## 0.1 — erste Fassung

**Das Programm liest JSON aus der Zwischenablage, macht es lesbar oder packt es auf eine Zeile und schreibt es zurück.**

Diese Fassung ist die erste. Es gibt keine Vorgängerfassung, die hier etwas
zurückblicken müsste, deshalb steht unten nicht "geändert", sondern was es gibt
und warum es so gebaut ist.

### Es nimmt JSONC und gibt striktes JSON zurück

Eingelesen wird, was in der Praxis aus Logdateien, Konfigurationen und
Editoren kommt: Kommentare (`//` und `/* */`), ein Nachlaufkomma, ein
Byte-Order-Mark am Anfang. Die Ausgabe ist davon frei. Wer eine
`tsconfig.json` formatiert, bekommt eine Datei, die jeder Parser liest, ohne
vorher die Kommentare entfernen zu müssen.

Damit das keine stillen Verluste sind, ist jeder Wert einzeln belegt:

- Zahlen bleiben, wie sie geschrieben wurden. `1.50` wird nicht zu `1.5`, ein
  `1e3` wird nicht zu `1000`. Wer eine Zahl umschreibt, verändert sie.
- Zeichenketten bleiben, wie sie geschrieben wurden, Fluchtfolgen eingeschlossen.
  `\u00e4` wird nicht zu `ä`.
- Die Reihenfolge der Schlüssel bleibt. Zwei Schlüssel mit gleichem Namen
  bleiben zwei.
- Ein Kommentar *innerhalb* eines Strings ist Text: In `"http://beispiel"` ist
  `//` kein Kommentar. Genau deshalb gibt es für Kommentare und Strings einen
  eigenen Scann und keine Regex.

Steht die Ausgabe nach dem Formatieren anders da als vorher, sagt die Statuszeile
das: einmal, wenn sich etwas geändert hat, und ein anderes Mal, wenn es schon
so da war. "War schon lesbar formatiert" ist eine Meldung wert, weil sie die
Frage beantwortet, ob die Taste etwas getan hat.

### Ein Fehler ändert nichts

Kommt kein JSON heraus, bleibt der Text stehen, in dem er war, und die Meldung
nennt Zeile und Spalte. Es wird nicht versucht, "irgendwie" zu reparieren, und
es wird nichts geraten: Ein Text, der als Datei weitergeht, muss von hier aus
nachvollziehbar bleiben. Die Meldung beginnt mit einem Kreuz und nicht mit einem
Haken, damit sie auf den ersten Blick von den Erfolgsmeldungen zu unterscheiden
ist.

### Zwei Wege zum selben Ergebnis

**JSON Formatieren** stellt ein Element je Zeile dar, mit vier Leerzeichen
Einrückung. **JSON Packen** bringt alles auf eine Zeile, für `curl`-Ausgaben
und für Dateien, die schon klein sind. Leere Objekte und leere Listen bleiben
in beiden Fällen einzeilig (`{}`, `[]`) — zwei Zeilen mit einem Leerzeichen
dazwischen wären umsonst.

Skalarwerte an der Wurzel sind gültiges JSON: `42`, `"text"` und `true`
werden genauso angenommen wie ein Objekt. Wer eine einzelne Zahl aus einem Log
kopiert, ist damit kein Sonderfall.

### Die Schranke in der Statuszeile

Formatieren, Packen und Schreiben bleiben gesperrt, solange der Text nicht als
JSON erkannt wird. Erkannt wird am ersten sinntragenden Zeichen nach Leerraum
und Kommentaren: `{`, `[`, `"`, `-`, eine Ziffer, oder exakt das Wort `true`,
`false` oder `null`.

Das ist eine Vermutung, keine Prüfung, und zwar mit Absicht: Eine volle Prüfung
müsste nach jedem Tastendruck laufen und würde genau den Text sperren, an dem
gerade getippt wird. Der Formatter bleibt der Richter — er nennt den Ort.

Weil die Wörter `true`, `false` und `null` als Wort erkannt werden, sperrt
`nein` oder `tuer` zu Recht: Es sind keine Werte, und ein Wort, das sich als
Wert verkauft, ist schlimmer als ein Wort, das abgelehnt wird.

### Was der Formatter zusichert und wie

Nach jeder Ausgabe wird sie erneut gelesen und Token für Token mit der
Eingabe verglichen. Stimmt die Zahl der Stücke nicht, oder weicht eines ab,
bricht der Vorgang ab, statt ein Ergebnis zu liefern. Das findet nicht nur einen
Fehler in der Ausgabe, sondern auch einen im Lexer — beide wären sonst erst
sichtbar, wenn jemand die Datei geöffnet hat, die dieses Programm geschrieben
hat.

Der Vergleich kostet fast nichts und läuft bei jedem Schreiben mit, nicht nur
im Test.

### Zwei Stellen, an denen nicht die kürzeste Lösung gewählt wurde

- **Kommentare werden nicht mit einer Regex gefunden.** Ein Muster wie `//.*`
  trifft auch das `//` in `"http://beispiel"` und färbt ab dort den Rest der
  Zeile. Hervorhebung und Leser zerlegen den Text deshalb selbst, mit
  denselben Regeln: Kommentare, Strings und Fluchtfolgen werden an derselben
  Stelle erkannt. Was der Leser strikt ablehnt, wird bis zum Zeilenende gefärbt
  und hört dann auf — eine unfertige Datei soll aussehen wie eine unfertige
  Datei, nicht wie ein Fehler.
- **Die Tiefenbegrenzung fehlt, obwohl sie leicht einzubauen wäre.** Man könnte
  bei einem bestimmten Nestungsgrad abbrechen und die Datei so lassen, wie sie
  ist. Dann gäbe es aber zwei Klassen von Dateien: solche, die der Formatter
  liest, und solche, die er zurückschreibt. Das ist eine schlechtere
  Eigenschaft als die hohe Speichernutzung, weil man sie von außen nicht sieht.

### Was nicht abgesichert ist

- **Tief verschachtelte Eingaben kosten quadratisch viel Speicher.** Die Ausgabe
  rückt jede Ebene ein, das wächst mit dem Quadrat der Tiefe. Gemessen: 1 000
  Ebenen ergeben 4 MB Text, 2 000 ergeben 16 MB, 4 000 ergeben 64 MB. Bei
  20 000 Ebenen sind es rund 1,6 GB, bei 100 000 bricht der Speicher ab. Das ist
  keine kaputte Stelle, sondern die Folge eines Formates, das beliebig tief sein
  darf — aber es ist eine Grenze. Prüfen und Packen sind linear, nur die Ausgabe
  ist es nicht. Es gibt deshalb **keine** Tiefenbegrenzung: eine Datei, die der
  Formatter liest, soll er auch schreiben können.
- **Bei einer halbfertigen Datei wird falsch eingefärbt, nicht gar nicht.**
  Absicht: Wer tippt, sieht eher zu viel Farbe als eine tote Fläche.
- **Sitzungszustand wird nicht gespeichert.** Kein Theme, keine
  Fensterposition, kein Verlauf. Bewusst so, aber gut zu wissen.
