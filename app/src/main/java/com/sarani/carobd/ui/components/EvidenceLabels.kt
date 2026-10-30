package com.sarani.carobd.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sarani.carobd.core.model.ReadingState
import com.sarani.carobd.R

@Composable fun readingLabel(state: ReadingState): String = stringResource(when (state) {
    ReadingState.Success -> R.string.ui_read_success
    ReadingState.NoData -> R.string.ui_read_no_data
    ReadingState.NegativeResponse -> R.string.ui_read_negative
    ReadingState.Timeout -> R.string.ui_read_timeout
    ReadingState.Malformed -> R.string.ui_read_malformed
    ReadingState.Partial -> R.string.ui_read_partial
    ReadingState.Unsupported -> R.string.ui_read_unsupported
    ReadingState.Disconnected -> R.string.ui_read_disconnected
    ReadingState.AdapterFailure -> R.string.ui_read_adapter_failure
})
@Composable fun recordingLabel(state: String): String = when (state) {
    "Recording" -> stringResource(R.string.ui_record_active)
    "Stopped" -> stringResource(R.string.ui_record_stopped)
    "Saving" -> stringResource(R.string.ui_record_saving)
    else -> stringResource(R.string.ui_record_error, state)
}
