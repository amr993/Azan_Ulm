package com.ulm.azan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulm.azan.R

/** Emerald gradient header with a faint star motif, crescent, and Arabic title. */
@Composable
fun BrandHeader(
    title: String,
    subtitle: String? = null,
    arabic: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(Brand.headerBrush)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_geo_star),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(170.dp)
                .offset(x = 52.dp, y = (-44).dp),
            alpha = 0.12f,
            colorFilter = ColorFilter.tint(Brand.GoldLight)
        )
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_crescent_star),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    colorFilter = ColorFilter.tint(Brand.GoldLight)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (arabic != null) {
                        Text(
                            arabic,
                            color = Brand.GoldLight,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(14.dp))
            OrnamentalDivider(onDark = true)
        }
    }
}

/** A thin gold rule with a central 8-point star. */
@Composable
fun OrnamentalDivider(onDark: Boolean = false, modifier: Modifier = Modifier) {
    val c = if (onDark) Brand.GoldLight else Brand.Gold
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(c.copy(alpha = 0.45f)))
        Image(
            painter = painterResource(R.drawable.ic_geo_star),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 8.dp).size(12.dp),
            colorFilter = ColorFilter.tint(c)
        )
        Box(Modifier.weight(1f).height(1.dp).background(c.copy(alpha = 0.45f)))
    }
}

/** Small star bullet used in lists. */
@Composable
fun StarBullet(sizeDp: Int = 10, tint: Color = Brand.Gold) {
    Image(
        painter = painterResource(R.drawable.ic_geo_star),
        contentDescription = null,
        modifier = Modifier.size(sizeDp.dp),
        colorFilter = ColorFilter.tint(tint)
    )
}
