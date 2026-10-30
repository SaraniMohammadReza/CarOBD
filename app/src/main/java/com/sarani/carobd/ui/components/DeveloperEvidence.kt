package com.sarani.carobd.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import com.sarani.carobd.R
import com.sarani.carobd.core.model.DtcCategory

/** Formatting only; identifiers are never translated or modified in stored evidence. */
@Composable fun DiagnosticIdentifier(value: String) {
    Text(value, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
}
@Composable fun DeveloperEvidence(value: String) {
    var expanded by remember(value) { mutableStateOf(false) }
    if (value.isNotBlank()) {
        TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.s5_details)) }
        if (expanded) DiagnosticIdentifier(value)
    }
}
@Composable fun categoryLabel(category: DtcCategory?): String = stringResource(when(category) {
    DtcCategory.STORED -> R.string.s5_stored
    DtcCategory.PENDING -> R.string.s5_pending
    DtcCategory.PERMANENT -> R.string.s5_permanent
    else -> R.string.s5_unknown
})
