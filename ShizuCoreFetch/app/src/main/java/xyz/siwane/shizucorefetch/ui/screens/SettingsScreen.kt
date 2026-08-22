package xyz.siwane.shizucorefetch.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

data class PresetThemeColor(val nameRes: Int, val color: Color)

val presetColors = listOf(
    PresetThemeColor(R.string.color_amethyst, Color(0xFF9B51E0)),
    PresetThemeColor(R.string.color_emerald, Color(0xFF2ECC71)),
    PresetThemeColor(R.string.color_sapphire, Color(0xFF4A90E2)),
    PresetThemeColor(R.string.color_siwane, Color(0xFF14B8A6)),
    PresetThemeColor(R.string.color_crimson, Color(0xFFE74C3C)),
    PresetThemeColor(R.string.color_sunset, Color(0xFFF39C12))
)

val supportedLanguages = listOf(
    "ar" to "العربية",
    "en" to "English",
    "fr" to "Français",
    "es" to "Español",
    "pt" to "Português",
    "ru" to "Русский",
    "hi" to "हिन्दी",
    "ja" to "日本語",
    "zh" to "中文",
    "tr" to "Türkçe",
    "cs" to "Čeština"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTheme by mainViewModel.themeMode.collectAsState()
    val selectedColorIndex by mainViewModel.themeColorIndex.collectAsState()
    val isDynamicColor by mainViewModel.isDynamicColor.collectAsState()
    val useCustomColor by mainViewModel.useCustomColor.collectAsState()
    val customHexColor by mainViewModel.customHexColor.collectAsState()
    val uiScale by mainViewModel.uiScale.collectAsState()
    val currentLanguageCode by mainViewModel.languageCode.collectAsState()
    val isGithubLoggedIn by mainViewModel.isGithubLoggedIn.collectAsState()
    val githubUsername by mainViewModel.githubUsername.collectAsState()
    val isTokenActive by mainViewModel.isTokenActive.collectAsState()
    
    // مراقب ذكي: يُحدث الحصة تلقائياً بعد تسجيل الدخول مع تأخير بسيط لضمان استقرار التوكن في النظام
    LaunchedEffect(isGithubLoggedIn) {
        kotlinx.coroutines.delay(500) // تأخير نصف ثانية
        mainViewModel.refreshRateLimit()
    }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCustomHexSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val standardCardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)

    val currentContext = LocalContext.current
    val currentConfiguration = LocalConfiguration.current
    val currentLayoutDirection = LocalLayoutDirection.current
    val currentDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = standardCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageSheet = true }
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(id = R.string.settings_language),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val languageDisplayText = supportedLanguages.find { it.first == currentLanguageCode }?.second ?: "Language"
                            Text(
                                text = languageDisplayText,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = standardCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_appearance),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(id = R.string.theme_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val options = listOf(
                                "Dark" to stringResource(R.string.theme_dark),
                                "Light" to stringResource(R.string.theme_light),
                                "System" to stringResource(R.string.theme_system)
                            )
                            
                            options.forEachIndexed { index, (id, label) ->
                                val isSelected = selectedTheme == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = if (index == 0) 50.dp else 0.dp,
                                                bottomStart = if (index == 0) 50.dp else 0.dp,
                                                topEnd = if (index == options.size - 1) 50.dp else 0.dp,
                                                bottomEnd = if (index == options.size - 1) 50.dp else 0.dp
                                            )
                                        )
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { mainViewModel.updateThemeMode(id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < options.size - 1) {
                                    Box(modifier = Modifier.width(1.dp).fillMaxSize().background(MaterialTheme.colorScheme.outline))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(id = R.string.main_theme_color_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            presetColors.chunked(3).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowColors.forEach { preset ->
                                        val index = presetColors.indexOf(preset)
                                        val isSelected = selectedColorIndex == index && !isDynamicColor && !useCustomColor
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(preset.color.copy(alpha = if (isSelected) 0.2f else 0.05f))
                                                .clickable { mainViewModel.updateThemeColorIndex(index) }
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) preset.color else Color.Transparent,
                                                    shape = RoundedCornerShape(24.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = stringResource(id = preset.nameRes),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(preset.color))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showCustomHexSheet = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useCustomColor && !isDynamicColor) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (useCustomColor && !isDynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (useCustomColor && !isDynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FormatColorFill, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.btn_custom_hex),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = { mainViewModel.updateDynamicColor(true) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDynamicColor) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isDynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ColorLens, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.btn_dynamic_m3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(id = R.string.ui_scale_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.ui_scale_desc_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(uiScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                CustomThickSlider(
                                    value = uiScale,
                                    onValueChange = { mainViewModel.updateUiScale(it) },
                                    valueRange = 0.7f..1.1f,
                                    steps = 7,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = standardCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.github_auth_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTokenActive) Color(0xFF2ECC71).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isTokenActive) stringResource(id = R.string.token_status_active) else stringResource(id = R.string.token_status_inactive),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTokenActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isTokenActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.github_token_status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.github_rate_limit), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { mainViewModel.logoutGithub() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(stringResource(id = R.string.btn_delete_token), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            var inlineTokenInput by remember { mutableStateOf("") }
                            Text(
                                text = stringResource(id = R.string.github_auth_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = inlineTokenInput,
                                onValueChange = { inlineTokenInput = it },
                                label = { Text(stringResource(id = R.string.github_token_label)) },
                                placeholder = { Text(stringResource(id = R.string.github_token_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (inlineTokenInput.isNotBlank()) {
                                        mainViewModel.loginWithPersonalToken(inlineTokenInput.trim())
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(stringResource(id = R.string.github_token_btn), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(id = R.string.proxy_info_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = stringResource(id = R.string.proxy_info_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item {
                val workerEndpoint by mainViewModel.workerEndpoint.collectAsState()
                val workerStatus by mainViewModel.workerStatus.collectAsState()

                LaunchedEffect(Unit) { mainViewModel.testWorkerConnection() }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = standardCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.settings_gas_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { mainViewModel.testWorkerConnection() }) {
                                if (workerStatus is xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.settings_gas_endpoint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = workerEndpoint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val status = workerStatus
                                val (badgeColor, badgeText) = when (status) {
                                    is xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Online ->
                                        MaterialTheme.colorScheme.primary to stringResource(id = R.string.settings_gas_active) + " • ${status.latencyMs}ms"
                                    is xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Offline ->
                                        MaterialTheme.colorScheme.error to stringResource(id = R.string.settings_gas_inactive)
                                    is xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Checking ->
                                        MaterialTheme.colorScheme.onSurfaceVariant to stringResource(id = R.string.settings_gas_checking)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(id = R.string.settings_gas_checking)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                        if (workerStatus is xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Offline) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (workerStatus as xyz.siwane.shizucorefetch.viewmodel.MainViewModel.WorkerStatus.Offline).reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val cal = remember { java.util.Calendar.getInstance() }
                        val currentMinute = cal.get(java.util.Calendar.MINUTE)
                        val timeAllowed = (currentMinute in 5..10) || (currentMinute in 35..40)
                        val nextWindow = if (currentMinute < 5) "05" else if (currentMinute < 35) "35" else "05"
                        
                        var devClickCount by remember { mutableIntStateOf(0) }
                        var forceEnableCacheBtn by remember { mutableStateOf(false) }
                        
                        val canClearCache = timeAllowed || forceEnableCacheBtn
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (!forceEnableCacheBtn) {
                                            devClickCount++
                                            if (devClickCount >= 7) {
                                                forceEnableCacheBtn = true
                                            }
                                        }
                                    }
                            ) {
                                Text(text = stringResource(id = R.string.settings_clear_cache), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (forceEnableCacheBtn) "وضع المطور: التنظيف متاح دائماً" else if (timeAllowed) stringResource(id = R.string.settings_clear_cache_ready) else stringResource(id = R.string.settings_clear_cache_wait, nextWindow), 
                                    style = MaterialTheme.typography.labelMedium, 
                                    color = if (canClearCache) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { 
                                    if (canClearCache) {
                                        try {
                                            currentContext.cacheDir.deleteRecursively()
                                            java.io.File(currentContext.filesDir, "store_smart_cache.json").delete()
                                            java.io.File(currentContext.filesDir, "rich_metadata_cache.json").delete()
                                            android.widget.Toast.makeText(currentContext, "تم حذف الملفات! اسحب شاشة المتجر لتحديث البيانات.", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {}
                                    }
                                },
                                enabled = canClearCache,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canClearCache) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (canClearCache) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                val rateLimitInfo by mainViewModel.rateLimitInfo.collectAsState()
                val isLoadingRateLimit by mainViewModel.isLoadingRateLimit.collectAsState()


                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = standardCardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.settings_quota_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { mainViewModel.refreshRateLimit() }) {
                                if (isLoadingRateLimit) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        val info = rateLimitInfo
                        if (info == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.settings_quota_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            val usedFraction = if (info.limit > 0) info.used.toFloat() / info.limit.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { usedFraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (usedFraction > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(id = R.string.settings_quota_used, info.used, info.limit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_quota_remaining, info.remaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val resetDate = remember(info.resetEpochSeconds) {
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(info.resetEpochSeconds * 1000))
                            }
                            Text(
                                text = stringResource(id = R.string.settings_quota_reset, resetDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = R.string.for_developers_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.for_developers_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(stringResource(id = R.string.topic_shizuku), fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            )
                            SuggestionChip(
                                onClick = { },
                                label = { Text(stringResource(id = R.string.topic_shizucorefetch), fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.dev_support_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                    
                    PremiumPillButton(text = stringResource(id = R.string.dev_name), icon = Icons.Default.Person) {
                        uriHandler.openUri("https://jamal.elhizazi.me")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumPillButton(text = stringResource(id = R.string.dev_website), icon = Icons.Default.Public) {
                        uriHandler.openUri("https://www.siwane.xyz")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumPillButton(text = stringResource(id = R.string.dev_github), icon = ImageVector.vectorResource(id = R.drawable.ic_github_v)) {
                        uriHandler.openUri("https://github.com/elhizazi1")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumPillButton(text = stringResource(id = R.string.dev_email), icon = Icons.Default.Email, isOutlined = true) {
                        uriHandler.openUri("mailto:jamal@elhizazi.me")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumPillButton(text = stringResource(id = R.string.dev_paypal), icon = Icons.Default.Favorite, isOutlined = false) {
                        uriHandler.openUri("https://paypal.me/Elhizazi")
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.about_app_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                    
                    PremiumPillButton(text = stringResource(id = R.string.about_privacy), icon = Icons.Default.Security) {
                        uriHandler.openUri("https://shizucorefetch.siwane.xyz/privacy")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumPillButton(text = stringResource(id = R.string.about_terms), icon = Icons.Default.Article) {
                        uriHandler.openUri("https://shizucorefetch.siwane.xyz/terms")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val shareSubject = stringResource(id = R.string.share_subject)
                    val shareText = stringResource(id = R.string.share_text)
                    val shareChooserTitle = stringResource(id = R.string.share_chooser_title)
                    PremiumPillButton(text = stringResource(id = R.string.about_share), icon = Icons.Default.Share) { 
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject)
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        
                        val chooserIntent = android.content.Intent.createChooser(shareIntent, shareChooserTitle).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        currentContext.startActivity(chooserIntent)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var isCheckingUpdate by remember { mutableStateOf(false) }
                    PremiumPillButton(
                        text = if (isCheckingUpdate) stringResource(id = R.string.settings_gas_checking) else stringResource(id = R.string.about_update), 
                        icon = Icons.Default.Sync, 
                        isOutlined = true
                    ) {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            mainViewModel.checkForUpdates { message, url ->
                                isCheckingUpdate = false
                                android.widget.Toast.makeText(currentContext, message, android.widget.Toast.LENGTH_LONG).show()
                                if (url != null) {
                                    uriHandler.openUri(url)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // كنقراو رقم الإصدار حقيقةً من PackageManager (نفس المصدر اللي
                    // كيقرا منه النظام ونتائج adb shell dumpsys)، بلا ما نعتمدو
                    // على نص ثابت فـ strings.xml كان خاصو يتحدّث يدويًا فمكانين
                    // منفصلين (build.gradle.kts + strings.xml) — قبل هادشي، رفع
                    // versionName فـ build.gradle كان كيبان فـ APK الحقيقي وفـ
                    // Play/adb، لكن هاد النص هنا كان يبقى ثابت على القيمة القديمة
                    // لأنه ماشي مربوط بيه أصلاً.
                    val versionName = remember {
                        try {
                            currentContext.packageManager
                                .getPackageInfo(currentContext.packageName, 0)
                                .versionName ?: ""
                        } catch (_: Exception) {
                            ""
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentContext.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                CompositionLocalProvider(
                    LocalContext provides currentContext,
                    LocalConfiguration provides currentConfiguration,
                    LocalLayoutDirection provides currentLayoutDirection,
                    LocalDensity provides currentDensity
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_language),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        supportedLanguages.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        mainViewModel.updateLanguage(code)
                                        showLanguageSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (currentLanguageCode == code) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentLanguageCode == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = currentLanguageCode == code,
                                    onClick = {
                                        mainViewModel.updateLanguage(code)
                                        showLanguageSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCustomHexSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCustomHexSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                CompositionLocalProvider(
                    LocalContext provides currentContext,
                    LocalConfiguration provides currentConfiguration,
                    LocalLayoutDirection provides currentLayoutDirection,
                    LocalDensity provides currentDensity
                ) {
                    var tempHexInput by remember { mutableStateOf(if (useCustomColor) customHexColor.replace("#", "") else "14B8A6") }
                    var previewColor by remember { mutableStateOf(Color.Transparent) }
                    var isHexValid by remember { mutableStateOf(true) }

                    try {
                        val formattedHex = if (tempHexInput.startsWith("#")) tempHexInput else "#$tempHexInput"
                        previewColor = Color(android.graphics.Color.parseColor(formattedHex))
                        isHexValid = true
                    } catch (e: Exception) {
                        isHexValid = false
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.custom_hex_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Start)
                        )
                        
                        Text(
                            text = stringResource(id = R.string.color_picker_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
                        )

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val radius = size.width / 2f
                                        val dx = change.position.x - center.x
                                        val dy = change.position.y - center.y
                                        val dist = minOf(sqrt(dx * dx + dy * dy), radius)
                                        var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180 / PI).toFloat()
                                        if (angle < 0) angle += 360f
                                        val hsv = floatArrayOf(angle, dist / radius, 1f)
                                        val c = android.graphics.Color.HSVToColor(hsv)
                                        tempHexInput = String.format("%06X", 0xFFFFFF and c)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val radius = size.width / 2f
                                        val dx = offset.x - center.x
                                        val dy = offset.y - center.y
                                        val dist = minOf(sqrt(dx * dx + dy * dy), radius)
                                        var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180 / PI).toFloat()
                                        if (angle < 0) angle += 360f
                                        val hsv = floatArrayOf(angle, dist / radius, 1f)
                                        val c = android.graphics.Color.HSVToColor(hsv)
                                        tempHexInput = String.format("%06X", 0xFFFFFF and c)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent)))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isHexValid) previewColor else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            OutlinedTextField(
                                value = tempHexInput,
                                onValueChange = { tempHexInput = it.take(6).uppercase() },
                                label = { Text(stringResource(id = R.string.custom_hex_label)) },
                                isError = !isHexValid,
                                singleLine = true,
                                prefix = { Text("#") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (isHexValid) {
                                    val finalHex = if (tempHexInput.startsWith("#")) tempHexInput else "#$tempHexInput"
                                    mainViewModel.updateCustomHexColor(finalHex)
                                    showCustomHexSheet = false
                                }
                            },
                            enabled = isHexValid,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.custom_hex_apply),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPillButton(
    text: String,
    icon: ImageVector,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    
    val contentColor = MaterialTheme.colorScheme.onSurface
    
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = contentColor
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomThickSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val totalSteps = steps + 2
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val tickActiveColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    val tickInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .pointerInput(isRtl) {
                detectTapGestures { offset ->
                    val rawFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val newFraction = if (isRtl) 1f - rawFraction else rawFraction
                    val stepFraction = Math.round(newFraction * (totalSteps - 1)) / (totalSteps - 1).toFloat()
                    onValueChange(valueRange.start + stepFraction * range)
                }
            }
            .pointerInput(isRtl) {
                detectHorizontalDragGestures { change, _ ->
                    val rawFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newFraction = if (isRtl) 1f - rawFraction else rawFraction
                    val stepFraction = Math.round(newFraction * (totalSteps - 1)) / (totalSteps - 1).toFloat()
                    onValueChange(valueRange.start + stepFraction * range)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
        ) {
            val trackHeight = 14.dp.toPx()
            val thumbWidth = 6.dp.toPx()
            val thumbHeight = 36.dp.toPx()
            val cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
            
            val trackStartY = (size.height - trackHeight) / 2
            val activeWidth = size.width * fraction
            
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, trackStartY),
                size = Size(size.width, trackHeight),
                cornerRadius = cornerRadius
            )
            
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(0f, trackStartY),
                size = Size(activeWidth, trackHeight),
                cornerRadius = cornerRadius
            )
            
            val stepSpacing = size.width / (totalSteps - 1)
            for (i in 0 until totalSteps) {
                val dotX = i * stepSpacing
                val safeDotX = dotX.coerceIn(trackHeight / 2, size.width - trackHeight / 2)
                val isActive = dotX <= activeWidth
                drawCircle(
                    color = if (isActive) tickActiveColor else tickInactiveColor,
                    radius = 3.dp.toPx(),
                    center = Offset(safeDotX, size.height / 2)
                )
            }
            
            val thumbX = (size.width * fraction).coerceIn(thumbWidth / 2, size.width - thumbWidth / 2)
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(thumbX - thumbWidth / 2, (size.height - thumbHeight) / 2),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2, thumbWidth / 2)
            )
        }
    }
}
