package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun LibrarySetupScreen(onChooseFolder: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(TerminalDimensions.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsciiFrame("LOCAL LIBRARY") {
            Text("Grant read access to one music folder. Only that folder and its subfolders will be scanned.")
            BracketButton("CHOOSE MUSIC FOLDER", onChooseFolder, Modifier.padding(top = TerminalDimensions.md))
        }
    }
}
