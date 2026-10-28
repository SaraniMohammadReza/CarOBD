package com.sarani.carobd.ui.screens.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.TextMuted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionCenterScreen(
    viewModel: ConnectionCenterViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackBar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConnectionCenterEffect.ShowMessage -> {
                    scope.launch { snackBar.showSnackbar(effect.text) }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_connection_center)) },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonCyan)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(ConnectionCenterIntent.CreateNewProfile) }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCardBg, titleContentColor = NeonCyan)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(ConnectionCenterIntent.CreateNewProfile) },
                containerColor = NeonCyan
            ) { Icon(Icons.Default.Wifi, contentDescription = null, tint = DarkBg) }
        },
        snackbarHost = { SnackbarHost(snackBar) }
    ) { inner ->
        if (state.profiles.isEmpty()) {
            EmptyProfiles(inner)
        } else {
            ProfileList(
                modifier = Modifier.padding(inner),
                state = state,
                onSelect = { viewModel.onIntent(ConnectionCenterIntent.Select(it)) },
                onConnect = { viewModel.onIntent(ConnectionCenterIntent.ConnectSpecific(it)) },
                onDelete = { viewModel.onIntent(ConnectionCenterIntent.Delete(it)) },
                onTest = { viewModel.onIntent(ConnectionCenterIntent.TestConnection(it)) },
            )
        }
    }
}

@Composable
private fun EmptyProfiles(inner: PaddingValues) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(inner),
        contentAlignment = Alignment.Center
    ) {
        Text("No connection profiles yet.\nTap + to create one.", color = TextMuted, fontSize = 14.sp)
    }
}

@Composable
private fun ProfileList(
    modifier: Modifier,
    state: ConnectionCenterState,
    onSelect: (String) -> Unit,
    onConnect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (String) -> Unit,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        state.profiles.forEach { profile ->
            val selected = profile.id == state.selectedProfileId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(if (selected) NeonCyan.copy(0.12f) else DarkCardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .clickable { onSelect(profile.id) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(if (selected) NeonCyan else TextMuted, RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            profile.name,
                            color = if (selected) NeonCyan else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                    Row {
                        if (state.isConnected) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        } else {
                            IconButton(onClick = { onConnect(profile.id) }) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonCyan)
                            }
                        }
                        IconButton(onClick = { onTest(profile.id) }) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = TextMuted)
                        }
                        IconButton(onClick = { onDelete(profile.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted)
                        }
                    }
                }
                Text("${profile.transportType} · ${profile.target}", color = TextMuted, fontSize = 12.sp)
                Text("Protocol: ${profile.protocol}", color = TextMuted, fontSize = 12.sp)
                profile.lastSuccessfulAt?.let { ts ->
                    Text(
                        "Last connected: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(ts))}",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
@Suppress("UnusedMaterialScaffoldPaddingParameter", "unused")
private fun ProfileDetailSheet(
    modifier: Modifier,
    profile: ProfileSummary?,
    onDismiss: () -> Unit,
) {
    if (profile == null) return
    var newName by rememberSaveable { mutableStateOf(profile.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile: ${profile.name}", color = NeonCyan) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Text("Transport: ${profile.transportType} (${profile.target})", color = TextMuted)
                Text("Protocol: ${profile.protocol}", color = TextMuted)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = DarkCardBg,
        titleContentColor = NeonCyan
    )
}
