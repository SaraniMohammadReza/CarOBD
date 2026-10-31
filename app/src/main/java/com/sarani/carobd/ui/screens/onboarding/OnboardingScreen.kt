package com.sarani.carobd.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.ui.screens.owned.*

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var addVehicle by rememberSaveable { mutableStateOf(false) }
    val id = rememberSaveable { java.util.UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf("") }
    var make by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var engine by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    val draft = OwnedVehicleDraft(id=id, displayName=name, manufacturer=make, model=model, engineCode=engine, odometer=odometer)
    val parsed = draft.parse()
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={ viewModel.complete(null) }, enabled=!busy, modifier=Modifier.testTag("onboarding_skip")) {
            Text(stringResource(R.string.onboarding_btn_skip))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(step) {
                0 -> {
                    Text(stringResource(R.string.onboarding_welcome_title), style=MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.ui_onboarding_limits))
                    Text(stringResource(R.string.onboarding_safety_note))
                }
                1 -> {
                    Text(stringResource(R.string.onboarding_step_vehicle), style=MaterialTheme.typography.headlineSmall)
                    Row(Modifier.fillMaxWidth().toggleable(value=addVehicle,onValueChange={addVehicle=it}).testTag("onboarding_add_vehicle")) { Checkbox(addVehicle,null); Text(stringResource(R.string.onboarding_add_owned_vehicle)) }
                    if(addVehicle) {
                        OutlinedTextField(name,{name=it}, label={Text(stringResource(R.string.my_vehicle_name))}, singleLine=true, modifier=Modifier.fillMaxWidth().testTag("onboarding_name"))
                        OutlinedTextField(make,{make=it}, label={Text(stringResource(R.string.my_vehicle_manufacturer))}, singleLine=true, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(model,{model=it}, label={Text(stringResource(R.string.my_vehicle_model))}, singleLine=true, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(engine,{engine=it}, label={Text(stringResource(R.string.my_vehicle_engine_code))}, singleLine=true, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(odometer,{odometer=it}, label={Text(stringResource(R.string.my_vehicle_odometer))}, singleLine=true, modifier=Modifier.fillMaxWidth().testTag("onboarding_odometer"))
                        if(parsed.error == OwnedVehicleEditorError.ODOMETER_INVALID) Text(stringResource(R.string.my_vehicle_error_odometer), color=MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Text(stringResource(R.string.onboarding_step_safety), style=MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.onboarding_safety_note))
                    Text(stringResource(R.string.vehicle_exact_match_required))
                    Text(stringResource(R.string.connection_safety_desc))
                }
            }
            if(error != null) Text(stringResource(R.string.my_vehicle_error_save), color=MaterialTheme.colorScheme.error)
        }
        if(step > 0) TextButton(onClick={step--}, enabled=!busy) { Text(stringResource(R.string.onboarding_back)) }
        Button(onClick={if(step<2) step++ else viewModel.complete(if(addVehicle) draft else null)},
            enabled=!busy && !(step==1 && addVehicle && parsed.error!=null), modifier=Modifier.fillMaxWidth().testTag(if(step<2) "onboarding_next" else "onboarding_finish")) {
            Text(stringResource(if(step<2) R.string.onboarding_btn_next else R.string.onboarding_btn_get_started))
        }
    }
}
