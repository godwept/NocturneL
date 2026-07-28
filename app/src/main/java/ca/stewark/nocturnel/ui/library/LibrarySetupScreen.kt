package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun LibrarySetupScreen(onChooseFolder: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TerminalFrame("LOCAL LIBRARY") {
            Text("Grant NocturneL read access to one music folder. Only that folder and its subfolders will be scanned.", modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onChooseFolder) { Text("CHOOSE MUSIC FOLDER") }
        }
    }
}
