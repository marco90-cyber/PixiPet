# PixiPet

Animaletto virtuale per Android in stile LCD (Kotlin + Jetpack Compose).
Sprite, nome e codice sono originali: niente asset Bandai.

## Come compilare l'APK

1. Crea un repo su GitHub e carica il contenuto di questa cartella.
2. Vai su **Actions → Build APK → Run workflow** (parte anche a ogni push su `main`).
3. A fine build scarica l'artifact **PixiPet-debug-apk** e installa `app-debug.apk`.

## Come si gioca

Pulsanti: **◀** e **▶** scorrono le icone, **SELECT** conferma (puoi anche toccare le icone).

| Icona | Azione |
|-------|--------|
| Forchetta | Pasto (+fame, +peso) o snack (+felicità, +peso) |
| Bimbo | Pulizia (toglie le cacche, +igiene) |
| Libro | Minigioco alto/basso (+Smarts) |
| Mazza | Minigioco baseball: SELECT quando la pallina è nella zona (+Active, -peso) |
| Lampadina | Luce: fa dormire / sveglia (l'energia si ricarica dormendo) |
| Kit medico | Cura la malattia |
| i | Info: stadio, età, generazione, peso, errori di cura |

Il tempo scorre in tempo reale, anche ad app chiusa. Stadi: uovo (2 min), baby (1 h),
child (6 h), teen (1 giorno), adult (3 giorni), elder (fino a 4 giorni), poi muore di vecchiaia.
Cuore lampeggiante in alto = l'animaletto chiede attenzione. Se muore, SELECT fa nascere un nuovo uovo.

I valori (durate, velocità di calo delle barre) sono in `Game.kt` (`Config` e `Pet.step`).
