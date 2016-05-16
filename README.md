# AudioVisualizationModule 

Android module for audio recording manipulation and visualization. Uses WaveFromView and fragment from : https://github.com/Semantive/waveform-android

Järgnevalt toodud juhised eeldavad, et arendaja kasutab Android Studio arenduskeskkonda:
1. Tõmbakoodihoidlastallaaudiovisuaalsemoodulilähtekood 2. Lisamoodulomaprojekti.
a. Vali menüüst File-> New -> Import Module-> Vali terve alla laetud projekti AudioVisualizationModule kaust
b. Oota kuni Gradle on sünkroniseerimis elõpetanud
3. Mooduli kasutamiseks kutsu soovitud kohas välja AudioRecordActivity
a. Activity väljakutsel tekitatakse AudioRecordFragment, mida saad oma soovi järgi disainida
b. FailisalvestamisekaustonkirjutatudstaatilisseväljaDIR_NAME
c. Salvestist sisaldava faili nimi on kirjutatud staatilisse välja FILENAME
