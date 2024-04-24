package ru.vendetti.lethalcombile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ru.vendetti.lethalcombile.ui.theme.LethalCombileTheme
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBack
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBorder
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalText
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalTextDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = LethalTerminalBorder.toArgb()
        setContent { LoginAppScreen() }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginAppScreen(){
    LethalCombileTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = LethalTerminalBack) {
            Box(
                modifier = Modifier
                    .border(30.dp, LethalTerminalBorder)
                    .padding(30.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(30.dp)
                ) {
                    var text by remember { mutableStateOf("")}  // Состояние для хранения введённого текста
                    val keyboardController = LocalSoftwareKeyboardController.current
                    Text("[WELCOME]\n", color = LethalTerminalText, fontSize = 32.sp)
                    Divider(color = LethalTerminalText)
                    Text("\nPls register or login before starting the game.\nYou can do it by typing:\n", color = LethalTerminalText, fontSize = 20.sp)
                    Text("register <nickname>\n> Enter password: <password>\n> Repeat password: <password>\n", color = LethalTerminalTextDark, fontSize = 16.sp)
                    Text("login <nickname>\n> Enter password: <password>\n", color = LethalTerminalTextDark, fontSize = 16.sp)
                    Divider(color = LethalTerminalText)
                    Text("\nYou can type your command right below:", color = LethalTerminalText, fontSize = 20.sp)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = LethalTerminalText),
                        cursorBrush = SolidColor(LethalTerminalText),
                        modifier = Modifier
                            .padding(top = 30.dp)
                            .fillMaxSize(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            //keyboardController?.hide()
                            if (text.trim().isNotEmpty()) {
                                println("Пользователь ввёл: $text") // Обработайте введенные данные
                            }
                        })
                    )
                    LaunchedEffect(Unit) {
                        keyboardController?.show()
                    }
                }
            }
        }
    }
}