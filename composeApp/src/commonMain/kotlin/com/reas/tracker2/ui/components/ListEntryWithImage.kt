package com.reas.tracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.github.ajalt.colormath.extensions.android.composecolor.toColormathColor
import com.github.ajalt.colormath.extensions.android.composecolor.toComposeColor
import com.github.ajalt.colormath.transform.interpolate
import com.kmpalette.palette.graphics.Target
import com.reas.tracker2.ui.state
import com.reas.tracker2.ui.theme.LightColorScheme
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.crossfade.CrossfadePlugin
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val DYNAMIC_COLOR_STRENGTH = 0.2F

@Composable fun isLightTheme() = LightColorScheme.background == MaterialTheme.colorScheme.background

@Composable
fun ListEntryWithImage(
    modifier: Modifier = Modifier,
    url: suspend () -> Any? = { null },
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    dynamicColor: Boolean = false,
    alignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    val logger = remember { KotlinLogging.logger {} }
    var model by state<Any?>(null)
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            model = try {
                url()
            } catch (e: Throwable) {
                logger.warn(throwable = e) { "Couldn't retrieve image url" }
                null
            }
        }
    }

    var palette by rememberPaletteState(null)
    val color by animateColorAsState(
        if (palette == null || !dynamicColor) {
            backgroundColor
        } else {
            val background = backgroundColor.toColormathColor()
            val cover = Color(
                palette!!.getColorForTarget(
                    if (isLightTheme()) Target.LIGHT_VIBRANT else Target.DARK_MUTED,
                    backgroundColor.toArgb(),
                ),
            ).toColormathColor()
            val mixed = background.toHSL().interpolate(cover.toHSL(), DYNAMIC_COLOR_STRENGTH)
            mixed.toComposeColor()
        },
    )

    Row(
        modifier = modifier
            .background(color, RoundedCornerShape(5.dp))
            .padding(5.dp),
        verticalAlignment = alignment,
    ) {
        LandscapistImage(
            imageModel = { model },
            landscapist = koinInject(),
            component = rememberImageComponent {
                +PalettePlugin { palette = it }
                +CrossfadePlugin(500)
            },
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.0F)
                .clip(shape = RoundedCornerShape(5.dp))
                .background(color = Color.Gray),
        )
        Spacer(Modifier.width(10.dp))
        content()
    }
}
