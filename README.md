# PixiPet

Animaletto virtuale per Android in stile Tamaweb (Kotlin + Jetpack Compose).
Sprite, scene, nome e codice sono originali: niente asset di terzi.

## Compilare l'APK
Push su `main` (o **Actions → Build APK → Run workflow**), poi scarica l'artifact **PixiPet-debug-apk**.

## Come si gioca
Schermo di gioco in alto e menu touch scorrevole sotto (su e giu'). Tocca le voci oppure usa ◀ ▶ (cursore) e ● (conferma).
Toccando l'animaletto sullo schermo gli fai le coccole; il tasto Indietro di Android torna al menu precedente.

- **Cura:** cibo (costa monete), bagno, pulizia cacche, coccole, medicina, sonno.
- **Mappa:** parco (palla, appuntamenti), scuola (smarts), bancarella (lavoro, guadagni monete),
  genitori (solo bambino/teen, dalle 3 alle 18), vacanza (200 monete), mondo sotterraneo (10 caramelle spooky).
- **Minigiochi:** Trick or treat (prendi le caramelle, evita quelle marce), alto/basso, battuta.
- **Personalita' ed evoluzioni:** al passaggio a teen la personalita' (dolce, furbo, sportivo, goloso, burbero)
  dipende da come l'hai cresciuto e cambia aspetto. Stadi: uovo, baby, child, teen, adult, elder.
- **Immortale:** nel mondo sotterraneo, con 30 caramelle spooky, diventa angelo e non muore piu' per trascuratezza.
- **Impostazioni:** suono, colore e visibilita' del guscio, rinomina, esporta/importa salvataggio (copia negli appunti), nuovo uovo.

Il tempo scorre in tempo reale anche ad app chiusa. Valori in `Model.kt` (`Cfg`, `Pet.step`) e `Game.kt`.

## Notifiche
Anche ad app chiusa (WorkManager, controllo ogni pochi minuti, puo' ritardare col risparmio energetico):
fame, fame critica, sporco, cacche, malattia, tristezza, stanchezza, pericolo di morte, morte,
schiusa e crescita, risveglio, ritorno da genitori/vacanza e promemoria ogni 3 ore se i problemi restano.
Le notifiche si attivano/disattivano da SETUP, al primo avvio Android chiede il permesso.

## Debug
SETUP -> DEBUG: cambia stadio/eta', fa passare il tempo (SKIP), aggiunge monete e caramelle,
riempie o svuota le barre, malattia, cacche, personalita', angelo/immortale, orario genitori libero,
uccide/resuscita e invia una notifica di prova.

## Novita' (ultima versione)
- Gioco interamente in italiano.
- Quando l'animaletto dorme, la finestra mostra la notte con luna e stelle che brillano.
- Se una statistica e' al 97% o piu', l'animaletto rifiuta di ripetere quell'azione (scuote la testa e dice NO).
- Bancarella: LAVORA (5 secondi), LAVORA 10 MIN (guadagna di piu') e LAVORA 30 MIN (guadagna ancora di piu'). Durante il lavoro l'animaletto e' via; con RICHIAMA prima del tempo non si riceve nulla.
