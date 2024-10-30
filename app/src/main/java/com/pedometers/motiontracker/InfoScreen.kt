package com.pedometers.motiontracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedometers.motiontracker.navigation.NavigationDestination

object InfoScreen : NavigationDestination {
    override val route: String = "info"
    override val titleRes: String = "Come usare l'app"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    navigateUp: () -> Unit
) {


    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            MotionTrackerAppBar(
                title = InfoScreen.titleRes,
                canNavigateBack = true,
                navigateUp = navigateUp
            )
        }) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),

            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            HowToUse()
        }
    }
}


@Composable
fun HowToUse() {
    Text(
        text = "Benvenuto nell'app di Motion Tracking!",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Questa applicazione Android raccoglie e registra in tempo reale i dati dai sensori del dispositivo, tra cui Accelerometro, Giroscopio e Magnetometro, oltre che dal sensore Movesense.",
        fontSize = 16.sp,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(16.dp))

    StepText(
        stepNumber = "1",
        title = "Concedi le Autorizzazioni",
        description = "L'app necessita dei permessi di accesso ai dati di attività del dispositivo e ai sensori esterni. Concedi le autorizzazioni richieste all'avvio per garantire il corretto funzionamento."
    )

    StepText(
        stepNumber = "2",
        title = "Imposta i Parametri Utente",
        description = "Inserisci informazioni come età, altezza, peso e seleziona l'attività e la posizione del telefono, così da personalizzare i dati registrati in base al tuo profilo."
    )

    StepText(
        stepNumber = "3",
        title = "Avvia la Registrazione",
        description = "Premi il pulsante \"Start Listening\" per avviare la registrazione dei dati dai sensori. Durante la registrazione ricorda di effettuare esattamente 50 passi, poiché questo conteggio ti sarà necessario per il passaggio successivo."
    )

    StepText(
        stepNumber = "4",
        title = "Inserisci il Numero di Passi",
        description = "Raggiunti i 50 passi compila il campo."
    )

    StepText(
        stepNumber = "5",
        title = "Invia i Dati",
        description = "Dopo aver inserito i dati necessari, premi 'Invia' per salvare le informazioni su un database remoto. In caso contrario, seleziona 'Annulla' per terminare senza salvare."
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Ora sei pronto per usare l'app e iniziare a raccogliere dati di movimento!",
        fontSize = 16.sp,
        color = Color.Gray
    )
}

@Composable
fun StepText(stepNumber: String, title: String, description: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Passo $stepNumber: $title",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}