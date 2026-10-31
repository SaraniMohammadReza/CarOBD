package com.sarani.carobd.ui.screens.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.data.transport.SimulatorScenario
import kotlinx.coroutines.launch
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorSelectorScreen(
    viewModel: SimulatorSelectorViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val canConnect = state.connectionState is com.sarani.carobd.core.model.ConnectionState.Idle ||
        state.connectionState is com.sarani.carobd.core.model.ConnectionState.Disconnected ||
        state.connectionState is com.sarani.carobd.core.model.ConnectionState.Failure
    val scope = rememberCoroutineScope()
    val snackBar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { e ->
            when (e) {
                is SimulatorSelectorEffect.Message ->
                    scope.launch { snackBar.showSnackbar(e.text) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_simulator), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.ui_back), tint = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCardBg),
            )
        },
        snackbarHost = { SnackbarHost(snackBar) }
    ) { inner: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Educational Explanation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.simulator_banner_title),
                        color = NeonAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.simulator_banner_desc),
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Connection Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.ui_connection_state),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    when (val connection = state.connectionState) {
                        is com.sarani.carobd.core.model.ConnectionState.Connected -> {
                            Text(
                                text = stringResource(if (connection.isSimulator) R.string.s5_connected_sim else R.string.ui_connection_state),
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { viewModel.onIntent(SimulatorSelectorIntent.Disconnect) },
                                colors = ButtonDefaults.buttonColors(containerColor = com.sarani.carobd.ui.theme.NeonRed.copy(alpha = 0.2f), contentColor = com.sarani.carobd.ui.theme.NeonRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.btn_disconnect), fontWeight = FontWeight.Bold)
                            }
                        }
                        is com.sarani.carobd.core.model.ConnectionState.Failure -> {
                            Text(com.sarani.carobd.ui.components.connectionIssueLabel(connection.issue), color = com.sarani.carobd.ui.theme.NeonRed)
                            com.sarani.carobd.ui.components.DeveloperEvidence(connection.technicalMessage)
                        }
                        com.sarani.carobd.core.model.ConnectionState.Idle, com.sarani.carobd.core.model.ConnectionState.Disconnected -> {
                            Text(stringResource(R.string.s5_disconnected), color = TextMuted, fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.onIntent(SimulatorSelectorIntent.Connect) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_connect), color = DarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeonAmber)
                            Text(stringResource(R.string.s5_connection_progress), color = NeonAmber, fontSize = 11.sp)
                            androidx.compose.material3.TextButton(onClick = { viewModel.onIntent(SimulatorSelectorIntent.Disconnect) }) {
                                Text(stringResource(R.string.ui_cancel), color = com.sarani.carobd.ui.theme.NeonRed)
                            }
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.simulator_scenario_picker_hint),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            state.scenarios.forEach { scenario ->
                val selected = scenario.id == state.selectedScenarioId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (selected) NeonCyan.copy(alpha = 0.6f) else DarkCardBorder,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    ScenarioRow(scenario, selected) {
                        viewModel.onIntent(SimulatorSelectorIntent.SelectScenario(scenario.id))
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ScenarioRow(
    scenario: SimulatorScenario,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan),
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(
                if (androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "fa") scenario.labelFa else scenario.labelEn,
                color = if (selected) NeonCyan else com.sarani.carobd.ui.theme.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            com.sarani.carobd.ui.components.DiagnosticIdentifier(scenario.id)
        }
    }
}
