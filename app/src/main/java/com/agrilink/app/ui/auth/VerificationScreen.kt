package com.agrilink.app.ui.auth

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrilink.app.R
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.data.api.dto.VerificationStatusDto
import com.agrilink.app.data.repo.AuthRepository
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.ui.common.Load
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.common.rememberPhotoPicker
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.AgriTopBar
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ErrorState
import com.agrilink.app.ui.components.LoadingBlock
import com.agrilink.app.ui.components.SurfaceCard
import com.agrilink.app.ui.components.VerificationTag
import com.agrilink.app.ui.theme.Organic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@StringRes
fun documentTypeRes(type: String): Int = when (type) {
    "FAYDA_ID" -> R.string.doc_fayda
    "FARM_PHOTO" -> R.string.doc_farm_photo
    "KEBELE_LETTER" -> R.string.doc_kebele
    "COOPERATIVE_LICENSE" -> R.string.doc_coop_license
    "BUSINESS_LICENSE" -> R.string.doc_business_license
    "TIN_CERTIFICATE" -> R.string.doc_tin
    "DRIVING_LICENSE" -> R.string.doc_driving_license
    "VEHICLE_REGISTRATION" -> R.string.doc_vehicle_registration
    "VEHICLE_PHOTO" -> R.string.doc_vehicle_photo
    else -> R.string.doc_other
}

data class VerificationUi(
    val status: VerificationStatusDto,
    val uploading: String? = null,
    val submitting: Boolean = false,
    val error: AppError? = null,
) {
    fun documentFor(type: String) = status.documents.firstOrNull { it.type == type }
    val allUploaded: Boolean get() = status.requiredDocuments.all { documentFor(it) != null }
    val canSubmit: Boolean get() = allUploaded && status.status in setOf("UNVERIFIED", "INFO_NEEDED", "REJECTED")
}

class VerificationViewModel(
    private val profile: ProfileRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<VerificationUi>>(Load.Loading)
    val state: StateFlow<Load<VerificationUi>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = profile.verification()) {
                is ApiResult.Ok -> _state.value = Load.Ready(VerificationUi(r.value))
                is ApiResult.Err -> _state.value = Load.Failed(r.error)
            }
        }
    }

    private fun edit(block: (VerificationUi) -> VerificationUi) =
        _state.update { if (it is Load.Ready) Load.Ready(block(it.value)) else it }

    fun upload(type: String, uri: Uri) {
        edit { it.copy(uploading = type, error = null) }
        viewModelScope.launch {
            val file = profile.uploadImage(uri, "VERIFICATION_DOCUMENT")
            if (file is ApiResult.Err) { edit { it.copy(uploading = null, error = file.error) }; return@launch }
            val doc = profile.addDocument(type, (file as ApiResult.Ok).value.id)
            if (doc is ApiResult.Err) { edit { it.copy(uploading = null, error = doc.error) }; return@launch }
            refresh()
        }
    }

    fun submit() {
        edit { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = profile.submitVerification()) {
                is ApiResult.Ok -> { auth.refreshMe(); refresh() }
                is ApiResult.Err -> edit { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

/** A4 · Verification status: what is done, what is missing, and a way to send it all for review. */
@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel,
    role: String,
    onBack: (() -> Unit)?,
    onContinue: () -> Unit,
    onAddProduce: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().background(Organic.Bg)) {
        AgriTopBar(stringResource(R.string.verification_title), onBack)
        when (val s = state) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorState(s.error, viewModel::refresh)
            is Load.Ready -> VerificationContent(s.value, role, viewModel, onContinue, onAddProduce)
        }
    }
}

@Composable
private fun VerificationContent(ui: VerificationUi, role: String, vm: VerificationViewModel, onContinue: () -> Unit, onAddProduce: (() -> Unit)?) {
    var pickingFor by remember { mutableStateOf<String?>(null) }
    val pick = rememberPhotoPicker { uri -> pickingFor?.let { vm.upload(it, uri) }; pickingFor = null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (ui.status.status == "VERIFIED") R.string.verification_done_title else R.string.verification_almost), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            VerificationTag(ui.status.status)
        }
        Text(
            stringResource(
                when (role) {
                    "FARMER" -> R.string.verification_sub_farmer
                    "DRIVER" -> R.string.verification_sub_driver
                    else -> R.string.verification_sub_buyer
                },
            ),
            style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral700,
        )
        ui.status.latestNote?.takeIf { ui.status.status in setOf("INFO_NEEDED", "REJECTED") }?.let { note ->
            SurfaceCard(color = Organic.Accent100) {
                Text(stringResource(R.string.verification_note_title), style = MaterialTheme.typography.titleSmall, color = Organic.Accent900)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = Organic.Accent900)
            }
        }

        TaskRow(Icons.Filled.PhoneAndroid, stringResource(R.string.verification_task_phone), done = true, trailing = null)
        ui.status.requiredDocuments.forEach { type ->
            val doc = ui.documentFor(type)
            val uploading = ui.uploading == type
            TaskRow(
                icon = Icons.Filled.Description, title = stringResource(documentTypeRes(type)), done = doc != null && doc.status != "REJECTED",
                subtitle = when (doc?.status) {
                    "APPROVED" -> stringResource(R.string.doc_status_approved)
                    "PENDING" -> stringResource(R.string.doc_status_uploaded)
                    "REJECTED" -> stringResource(R.string.doc_status_rejected)
                    else -> null
                },
            ) {
                if (doc?.status != "APPROVED") {
                    AgriButton(
                        stringResource(if (doc == null) R.string.action_add else R.string.action_replace), { pickingFor = type; pick() },
                        kind = if (doc == null) ButtonKind.Primary else ButtonKind.Secondary, height = 40, loading = uploading,
                    )
                }
            }
        }
        if (role == "FARMER") {
            TaskRow(Icons.Filled.Visibility, stringResource(R.string.verification_task_visit), done = false, subtitle = stringResource(R.string.verification_task_visit_sub), trailing = null)
        }
        ui.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium) }

        Spacer(Modifier.height(4.dp))
        if (ui.canSubmit) {
            AgriButton(stringResource(R.string.verification_submit), vm::submit, Modifier.fillMaxWidth(), loading = ui.submitting, height = 56)
        } else if (ui.status.status == "PENDING") {
            SurfaceCard(color = Organic.Sage100) { Text(stringResource(R.string.verification_waiting), style = MaterialTheme.typography.bodyMedium, color = Organic.Sage900) }
        }
        if (onAddProduce != null) {
            AgriButton(stringResource(R.string.verification_add_produce), onAddProduce, Modifier.fillMaxWidth(), kind = if (ui.canSubmit) ButtonKind.Secondary else ButtonKind.Primary, icon = Icons.Filled.Grass, height = 56)
        }
        AgriButton(stringResource(R.string.verification_continue), onContinue, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)
        Spacer(Modifier.height(24.dp))
    }
    LaunchedEffect(Unit) { vm.refresh() }
}

@Composable
private fun TaskRow(icon: ImageVector, title: String, done: Boolean, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Organic.Surface).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (done) Organic.Sage700 else Organic.Neutral300), contentAlignment = Alignment.Center) {
            Icon(if (done) Icons.Filled.CheckCircle else icon, null, Modifier.size(22.dp), tint = if (done) Organic.Neutral100 else Organic.Neutral800)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral700)
        }
        trailing?.invoke()
    }
}
