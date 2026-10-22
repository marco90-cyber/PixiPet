# PixiPet

Animaletto virtuale per Android in stile Tamaweb (Kotlin + Jetpack Compose).
Sprite, scene, nome e codice sono originali: niente asset di terzi.

## Compilare l'APK
Push su `main` (o **Actions → Build APK → Run workflow**), poi scarica l'artifact **PixiPet-debug-apk**.

## Come si gioca
Tocca lo schermo (o premi un pulsante) per aprire il menu. ◀ ▶ spostano il cursore, ● conferma.
Si può anche toccare direttamente le voci.

- **Cura:** cibo (costa monete), bagno, pulizia cacche, coccole, medicina, sonno.
- **Mappa:** parco (palla, appuntamenti), scuola (smarts), bancarella (lavoro, guadagni monete),
  genitori (solo bambino/teen, dalle 3 alle 18), vacanza (200 monete), mondo sotterraneo (10 caramelle spooky).
- **Minigiochi:** Trick or treat (prendi le caramelle, evita quelle marce), alto/basso, battuta.
- **Personalita' ed evoluzioni:** al passaggio a teen la personalita' (dolce, furbo, sportivo, goloso, burbero)
  dipende da come l'hai cresciuto e cambia aspetto. Stadi: uovo, baby, child, teen, adult, elder.
- **Immortale:** nel mondo sotterraneo, con 30 caramelle spooky, diventa angelo e non muore piu' per trascuratezza.
- **Impostazioni:** suono, colore e visibilita' del guscio, rinomina, esporta/importa salvataggio (copia negli appunti), nuovo uovo.

Il tempo scorre in tempo reale anche ad app chiusa. Valori in `Model.kt` (`Cfg`, `Pet.step`) e `Game.kt`.
