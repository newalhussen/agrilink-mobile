package com.agrilink.app.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrilink.app.R
import com.agrilink.app.core.Phone
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.common.rememberSpeaker
import com.agrilink.app.ui.components.AgriButton
import com.agrilink.app.ui.components.ButtonKind
import com.agrilink.app.ui.components.ChoiceCard
import com.agrilink.app.ui.components.IconAction
import com.agrilink.app.ui.components.OtpInput
import com.agrilink.app.ui.components.PillField
import com.agrilink.app.ui.components.Tag
import com.agrilink.app.ui.components.Tone
import com.agrilink.app.ui.theme.Organic

private data class LanguageOption(val code: String, val nativeName: String, val continueLabel: String, val speakText: String)

private val languageOptions = listOf(
    LanguageOption("om", "Afaan Oromoo", "Itti fufi", "Afaan Oromoo filadhu"),
    LanguageOption("am", "አማርኛ", "ቀጥል", "አማርኛ ይምረጡ"),
    LanguageOption("en", "English", "Continue", "Choose English"),
)

/** A1 · Language. Each option is written in its own script and can be read aloud; the choice is stored before any network call. */
@Composable
fun LanguageScreen(initial: String?, onContinue: (String) -> Unit, onBack: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(initial ?: "am") }
    val speaker = rememberSpeaker()
    val option = languageOptions.first { it.code == selected }
    Column(modifier.fillMaxSize().background(Organic.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
        if (onBack != null) IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        Spacer(Modifier.height(if (onBack != null) 8.dp else 32.dp))
        Text("Choose your language", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp))
        Text("Afaan filadhu · ቋንቋ ይምረጡ", style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral700, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            languageOptions.forEach { lang ->
                ChoiceCard(
                    title = lang.nativeName,
                    selected = lang.code == selected,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    trailing = if (lang.code == selected) null else ({
                        IconAction(Icons.Filled.VolumeUp, stringResource(R.string.read_aloud), { speaker.speak(lang.speakText, lang.code) }, tint = Organic.Neutral700)
                    }),
                    onClick = { selected = lang.code; speaker.speak(lang.speakText, lang.code) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        AgriButton(option.continueLabel, { onContinue(selected) }, Modifier.fillMaxWidth(), height = 56)
    }
}

/** A2 · Role. */
@Composable
fun RoleScreen(onChosen: (String) -> Unit, onSignIn: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf("FARMER") }
    val context = LocalContext.current
    val speaker = rememberSpeaker()
    val readAloud = stringResource(R.string.role_title)
    Column(modifier.fillMaxSize().background(Organic.Bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(Organic.Neutral100).clickable { speaker.speak(readAloud, com.agrilink.app.ui.common.LocaleHelper.current()) }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeUp, null, Modifier.size(16.dp), tint = Organic.Neutral800)
                Text(stringResource(R.string.read_aloud), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Text(stringResource(R.string.role_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp), modifier = Modifier.padding(top = 20.dp, bottom = 16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RoleCard("FARMER", Icons.Filled.Grass, R.string.role_farmer_title, R.string.role_farmer_sub, selected, Organic.Sage700, Organic.Bg, Organic.Sage200, Organic.Sage600) { selected = "FARMER" }
            RoleCard("BUYER", Icons.Filled.Storefront, R.string.role_buyer_title, R.string.role_buyer_sub, selected, Organic.Accent300, Organic.Accent900, Organic.Accent200, Organic.Accent) { selected = "BUYER" }
            RoleCard("DRIVER", Icons.Filled.LocalShipping, R.string.role_driver_title, R.string.role_driver_sub, selected, Organic.Neutral300, Organic.Neutral900, Organic.Neutral200, Organic.Neutral700) { selected = "DRIVER" }
        }
        Row(
            Modifier.padding(top = 22.dp).clickable { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:8844"))) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Phone, null, Modifier.size(18.dp), tint = Organic.Accent700)
            Text(stringResource(R.string.role_help), style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral800)
        }
        Spacer(Modifier.height(24.dp))
        AgriButton(stringResource(R.string.action_continue), { onChosen(selected) }, Modifier.fillMaxWidth(), height = 56)
        AgriButton(stringResource(R.string.role_have_account), onSignIn, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)
    }
}

@Composable
private fun RoleCard(
    role: String, icon: ImageVector, title: Int, subtitle: Int, selected: String,
    circle: Color, circleContent: Color, selectedFill: Color, accent: Color, onClick: () -> Unit,
) {
    ChoiceCard(
        title = stringResource(title), subtitle = stringResource(subtitle), selected = selected == role,
        accent = accent, selectedFill = selectedFill, onClick = onClick,
        leading = {
            Box(Modifier.size(64.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(30.dp), tint = circleContent)
            }
        },
    )
}

/** Sign-up (name + phone) or sign-in (phone + code, or password). */
@Composable
fun PhoneScreen(
    viewModel: AuthViewModel,
    login: Boolean,
    onBack: () -> Unit,
    onCodeSent: () -> Unit,
    onSignedIn: () -> Unit,
    onSwitchMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(login) { viewModel.startFlow(login) }
    Column(
        modifier.fillMaxSize().background(Organic.Bg).statusBarsPadding().imePadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        Text(
            stringResource(if (login) R.string.phone_title_login else R.string.phone_title_signup),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
        )
        Text(
            stringResource(if (login) R.string.phone_sub_login else R.string.phone_sub_signup),
            style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral700,
        )
        if (!login) PillField(state.fullName, viewModel::setName, stringResource(R.string.phone_name))
        PillField(
            value = state.phone, onValueChange = viewModel::setPhone, label = stringResource(R.string.phone_number),
            placeholder = "911 234 567", keyboardType = KeyboardType.Phone, prefix = "+251  ",
            leadingIcon = Icons.Filled.Phone,
        )
        if (login && state.usePassword) {
            PillField(
                value = state.password, onValueChange = viewModel::setPassword, label = stringResource(R.string.phone_password),
                keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation(), leadingIcon = Icons.Filled.Lock,
            )
        }
        state.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) }
        AgriButton(
            text = stringResource(if (login && state.usePassword) R.string.phone_sign_in else R.string.phone_send_code),
            onClick = { viewModel.submitPhone(onCodeSent, onSignedIn) },
            modifier = Modifier.fillMaxWidth(), loading = state.busy, enabled = state.canSubmitPhone, height = 56,
        )
        if (login) {
            AgriButton(
                stringResource(if (state.usePassword) R.string.phone_use_code else R.string.phone_use_password),
                viewModel::togglePassword, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost,
            )
        }
        AgriButton(stringResource(if (login) R.string.phone_new_here else R.string.phone_have_account), onSwitchMode, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)
    }
}

/** A3 · Phone code. */
@Composable
fun OtpScreen(viewModel: AuthViewModel, onBack: () -> Unit, onDone: (newAccount: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.code) { if (state.code.length == 6) viewModel.verify(onDone) }
    Column(
        modifier.fillMaxSize().background(Organic.Bg).statusBarsPadding().imePadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        Text(stringResource(R.string.otp_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp))
        Text(stringResource(R.string.otp_sent_to, Phone.mask(state.normalizedPhone)), style = MaterialTheme.typography.bodyLarge, color = Organic.Neutral700)
        OtpInput(state.code, viewModel::setCode, error = state.error != null)
        state.error?.let { Text(it.errorText(), color = Organic.Accent700, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) }
        state.devOtp?.let { dev ->
            Row(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(Organic.Sage100).clickable { viewModel.setCode(dev) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Tag(stringResource(R.string.otp_dev_tag), Tone.Sage)
                Text(stringResource(R.string.otp_dev_hint, dev), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            }
        }
        val resend = if (state.resendSeconds > 0) {
            stringResource(R.string.otp_resend_in, "%d:%02d".format(state.resendSeconds / 60, state.resendSeconds % 60))
        } else stringResource(R.string.otp_resend)
        AgriButton(resend, viewModel::resend, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, enabled = state.resendSeconds == 0 && !state.busy)
        AgriButton(stringResource(R.string.otp_verify), { viewModel.verify(onDone) }, Modifier.fillMaxWidth(), loading = state.busy, enabled = state.code.length == 6, height = 56)
    }
}
