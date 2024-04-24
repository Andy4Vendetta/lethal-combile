@file:Suppress("SpellCheckingInspection")

package ru.vendetti.lethalcombile

import android.os.Bundle
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vendetti.lethalcombile.ui.theme.LethalCombileTheme
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBack
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBorder
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalRed
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalText
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalTextDark
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LoginAppScreen() }
    }
}
@Composable
fun LoginAppScreen(){
    var errorText by remember { mutableStateOf("")}
    var text by remember { mutableStateOf("")}  // Состояние для хранения введённого текста
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        keyboardController?.show()
    }
    LethalCombileTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = LethalTerminalBack) {
            Box(
                modifier = Modifier
                    .border(30.dp, LethalTerminalBorder)
                    .padding(30.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text("[WELCOME]\n", color = LethalTerminalText, fontSize = 25.sp)
                    HorizontalDivider(color = LethalTerminalText)
                    Text("\nRegister or login before starting the game. You can do it by typing:", color = LethalTerminalText, fontSize = 20.sp)
                    Text("register <emal> <password> <password>\n", color = LethalTerminalTextDark, fontSize = 16.sp)
                    Text("login <email> <password>\n", color = LethalTerminalTextDark, fontSize = 16.sp)
                    HorizontalDivider(color = LethalTerminalText)
                    Text("\nAll available commands:", color = LethalTerminalText, fontSize = 20.sp)
                    Text("register\nlogin\nexit", color = LethalTerminalTextDark, fontSize = 16.sp)
                    Text("\nYou can type your command right below:", color = LethalTerminalText, fontSize = 20.sp)
                    Text(errorText, color = LethalTerminalRed, fontSize = 20.sp)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = LethalTerminalText, fontSize = 20.sp),
                        cursorBrush = SolidColor(LethalTerminalText),
                        modifier = Modifier
                            .padding(top = 30.dp)
                            .fillMaxSize(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            executeCommand(text) { newErrorText ->
                                errorText = newErrorText
                            }
                        })
                    )
                }
            }
        }
    }
}
fun executeCommand(
    text: String,
    setErrorText: (String) -> Unit
) {
    when (text.trim().split(" ")[0]) {
        "exit" -> exitProcess(-1)
        "login" -> checkLogin(text, setErrorText)
        "register" -> checkRegister(text, setErrorText)
        else -> {
            setErrorText("This command doesn't exist!")
        }
    }
}
fun checkLogin(text: String, setErrorText: (String) -> Unit) {
    val parts = text.split(" ")
    if(parts.size == 2){
        if(parts[1] == "aboba"){
            setErrorText("SUS")
        } else {
            setErrorText("Incorrect password")
        }
    } else {
        setErrorText("Wrong syntax")
    }
}
fun checkRegister(text: String, setErrorText: (String) -> Unit){
    val parts = text.split(" ")
    if(parts.size == 3){
        if(parts[1] == "hmmm" && parts[1] == parts[2]){
            setErrorText("ladno")
        } else {
            setErrorText("Incorrect password")
        }
    } else {
        setErrorText("Wrong syntax")
    }
}