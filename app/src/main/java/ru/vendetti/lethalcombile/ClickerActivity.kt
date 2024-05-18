package ru.vendetti.lethalcombile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onStart()
        val receivedNumber = intent.getIntExtra("EXTRA_NUMBER", 0)
        setContent { Clicker() }
    }

    @Composable
    private fun Clicker() {
        var scale by remember { mutableFloatStateOf(1f) }
        var counter by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        var tapPosition by remember { mutableStateOf<Offset?>(null) }
        val animatedOffsetY = remember { Animatable(0f) }
        val animatedAlpha = remember { Animatable(1f) }
        //val density = LocalDensity.current
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.dine),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Image(painter = painterResource(id = R.drawable.butler),
                contentDescription = null,
                modifier = Modifier
                    .size(450.dp)
                    .scale(scale)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {offset ->
                            scale = 0.9f // Уменьшение размера при нажатии
                            counter++ // Увеличиваем числовую переменную
                            tapPosition = offset // Сохраняем позицию нажатия
                            scope.launch {
                                animatedOffsetY.snapTo(0f)
                                animatedAlpha.snapTo(1f)
                                animatedOffsetY.animateTo(
                                    targetValue = -30.dp.toPx(),
                                    animationSpec = tween(durationMillis = 2000)
                                )
                                animatedAlpha.animateTo(
                                    targetValue = 0f, animationSpec = tween(durationMillis = 2000)
                                )
                                delay(2000)
                                tapPosition = null

                            }
                            tryAwaitRelease()
                            scale = 1f // Возвращение размера после отпускания
                        })
                    })
            // Отображение текста "+1" в месте нажатия, если позиция нажатия не null
            tapPosition?.let { position ->
                val offset = IntOffset(position.x.toInt(), position.y.toInt())
                Text(
                    text = "+1",
                    style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = offset.x.toFloat(),
                            translationY = offset.y.toFloat() + animatedOffsetY.value + 200,
                            alpha = animatedAlpha.value
                        )
                )
            }
        }
    }
}