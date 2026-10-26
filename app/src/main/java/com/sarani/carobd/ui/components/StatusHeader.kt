package com.sarani.carobd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun StatusHeader(
    connectionState: ConnectionState,
    activeProfile: VehicleProfile?,
    batteryVoltage: Float,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusText) = when (val cs = connectionState) {
                        is ConnectionState.Connected -> NeonGreen to stringResource(R.string.status_connected)
                        is ConnectionState.Failure -> NeonRed to cs.humanMessageFa
                        is ConnectionState.Disconnected, is ConnectionState.Idle -> TextMuted to stringResource(R.string.status_disconnected)
                        is ConnectionState.Disconnecting -> NeonAmber to stringResource(R.string.status_connecting)
                        is ConnectionState.AdapterIdentifying -> NeonAmber to cs.probeStage
                        is ConnectionState.AdapterConfiguring -> NeonAmber to cs.setting
                        is ConnectionState.ProtocolDetecting -> NeonAmber to "${cs.candidateName} (${cs.attemptIndex}/${cs.totalCandidates})"
                        else -> NeonAmber to stringResource(R.string.status_connecting)
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Voltage Readout
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (batteryVoltage > 0f) String.format(java.util.Locale.US, "%.1f V", batteryVoltage) else "—",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vehicle & ECU Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeProfile?.modelNameFa ?: "—",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = activeProfile?.let { "${it.ecuName} • ${it.ecuProtocol.standard.displayName}" } ?: "—",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Action Button
                if (connectionState is ConnectionState.Connected) {
                    FilledTonalButton(
                        onClick = onDisconnectClicked,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonRed.copy(alpha = 0.2f),
                            contentColor = NeonRed
                        )
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_disconnect), fontSize = 12.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onConnectClicked,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonCyan.copy(alpha = 0.2f),
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_connect), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
