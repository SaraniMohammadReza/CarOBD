package com.sarani.carobd.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DtcSeverity
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.onIntent(DiagnosticsIntent.ScanCodes) },
                    enabled = !state.isScanning && !state.isClearing,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.dtc_scan_button), color = DarkBg, fontWeight = FontWeight.Bold)
                }

                if (state.troubleCodes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.onIntent(DiagnosticsIntent.RequestClearCodes) },
                        enabled = !state.isScanning && !state.isClearing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.dtc_clear_button), color = NeonRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last message status banner
            state.lastScanMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(text = msg, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Results List
            if (state.troubleCodes.isEmpty()) {
                // Truthful empty state (§28): a green "no codes" confirmation is shown
                // ONLY after a completed, successful scan that returned zero codes.
                // Idle and failed scans must NOT look successful.
                val noCodesConfirmed = state.scanCompletedSuccessfully
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (noCodesConfirmed) NeonGreen else TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (noCodesConfirmed) stringResource(R.string.dtc_no_codes) else "در انتظار اسکن یا اتصال برقرار نشده",
                            color = if (noCodesConfirmed) TextPrimary else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "برای اسکن حافظه خطاهای ایسیو دکمه بالا را لمس کنید",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.troubleCodes, key = { "${it.responderAddressHex}-${it.category.name}-${it.code}" }) { dtc ->
                        DtcCard(dtc = dtc)
                    }
                }
            }
        }

        // Confirmation Dialog for Clearing DTCs
        if (state.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(DiagnosticsIntent.DismissConfirmDialog) },
                title = { Text(stringResource(R.string.dtc_clear_confirm_title), color = TextPrimary) },
                text = { Text(stringResource(R.string.dtc_clear_confirm_msg), color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onIntent(DiagnosticsIntent.ConfirmClearCodes) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        Text(stringResource(R.string.confirm), color = DarkBg)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(DiagnosticsIntent.DismissConfirmDialog) }) {
                        Text(stringResource(R.string.cancel), color = TextMuted)
                    }
                },
                containerColor = DarkCardBg
            )
        }
    }
}

@Composable
fun DtcCard(dtc: DiagnosticCode) {
    val severityColor = when (dtc.severity) {
        DtcSeverity.CRITICAL -> NeonRed
        DtcSeverity.MODERATE -> NeonAmber
        DtcSeverity.INFORMATIONAL -> NeonCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(14.dp))
            .border(1.dp, severityColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dtc.code,
                        color = severityColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dtc.system.name,
                        color = severityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Persian Title
            Text(
                text = dtc.titleFa,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // English Title
            Text(
                text = dtc.titleEn,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )

            // §7 / #15: source ECU + category identity (never guessed; never absent).
            Text(
                text = "رد: ${dtc.responderAddressHex} • ${dtc.category.name.lowercase()}",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Persian Explanation and Troubleshooting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = dtc.descriptionFa,
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
