package com.leeam.cryptowidget.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leeam.cryptowidget.ui.theme.*

@Composable
fun CryptoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String) =
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)

@Composable
fun CryptoFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, fontSize = 12.sp) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LocalThemeColors.current.accent,
            selectedLabelColor     = BgDark,
            containerColor         = Surface,
            labelColor             = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor         = CardBorder,
            selectedBorderColor = LocalThemeColors.current.accent
        )
    )
}

@Composable
fun CryptoOutlinedButton(label: String, onClick: () -> Unit) =
    OutlinedButton(
        onClick = onClick,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = LocalThemeColors.current.accent),
        border  = BorderStroke(1.dp, LocalThemeColors.current.accent),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 11.sp) }

@Composable
fun cryptoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = LocalThemeColors.current.accent,
    unfocusedBorderColor = CardBorder,
    cursorColor          = LocalThemeColors.current.accent,
    focusedLabelColor    = LocalThemeColors.current.accent
)

/** A full-width save button in a gradient footer, used by multiple settings screens. */
@Composable
fun SaveButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.Color.Transparent, BgDark.copy(alpha = 0.97f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Button(
            onClick  = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = LocalThemeColors.current.accent),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(label, color = BgDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
