package xyz.siwane.shizucorefetch.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

@Composable
fun ShizukuHubScreen(modifier: Modifier = Modifier, mainViewModel: MainViewModel? = null) {
    val context = LocalContext.current

    val isRunning by (mainViewModel?.isShizukuRunning?.collectAsState() ?: remember { mutableStateOf(false) })
    val isGranted by (mainViewModel?.isShizukuGranted?.collectAsState() ?: remember { mutableStateOf(false) })
    val binderVersion by (mainViewModel?.shizukuBinderVersion?.collectAsState() ?: remember { mutableStateOf(-1) })
    val isSilentInstallEnabled by (mainViewModel?.isSilentInstallEnabled?.collectAsState() ?: remember { mutableStateOf(true) })

    val canUseSilentInstall = isRunning && isGranted

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // 1. بطاقة حالة شيزوكو الرئيسية
        item {
            val statusBgColor = if (isRunning) {
                if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            }

            val statusContentColor = if (isRunning) {
                if (isGranted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = statusBgColor),
                border = BorderStroke(1.dp, statusContentColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = if (isRunning && isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRunning) stringResource(id = R.string.hub_status_running) else stringResource(id = R.string.hub_status_stopped),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusContentColor
                            )
                        }
                        IconButton(onClick = { mainViewModel?.refreshShizukuState() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh, 
                                contentDescription = stringResource(id = R.string.refresh),
                                tint = statusContentColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoBadge(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.hub_permission_label),
                            value = if (isGranted) stringResource(id = R.string.shizuku_granted) else stringResource(id = R.string.shizuku_needed)
                        )
                        InfoBadge(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.hub_binder_label),
                            value = if (binderVersion > 0) "v$binderVersion" else "---"
                        )
                    }

                    // تنبيه ذكي للإصدارات القديمة عبر Strings
                    AnimatedVisibility(visible = binderVersion in 1..10) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.hub_binder_root_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { mainViewModel?.requestShizukuPermission() },
                        enabled = isRunning && !isGranted,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusContentColor,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = when {
                                !isRunning -> stringResource(id = R.string.hub_status_stopped)
                                isGranted -> stringResource(id = R.string.shizuku_granted)
                                else -> stringResource(id = R.string.hub_action_check_permission)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. بطاقة إعدادات التثبيت الصامت الذكية
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (canUseSilentInstall) 1f else 0.5f)
                    .clickable(enabled = canUseSilentInstall) { 
                        mainViewModel?.updateSilentInstallEnabled(!isSilentInstallEnabled) 
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canUseSilentInstall) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.hub_silent_install_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSilentInstallEnabled && canUseSilentInstall) 
                                    stringResource(id = R.string.hub_silent_install_enabled) 
                                else if (!canUseSilentInstall)
                                    stringResource(id = R.string.hub_silent_install_requires_shizuku)
                                else
                                    stringResource(id = R.string.hub_silent_install_disabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSilentInstallEnabled && canUseSilentInstall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSilentInstallEnabled && canUseSilentInstall,
                            onCheckedChange = { mainViewModel?.updateSilentInstallEnabled(it) },
                            enabled = canUseSilentInstall,
                            colors = SwitchDefaults.colors(
                                disabledCheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        // 3. تنبيه الصلاحيات وإعدادات التطبيق
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.hub_warning_permissions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.hub_action_open_settings),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. الدليل الشامل لتفعيل شيزوكو (بالكامل عبر Strings)
        item {
            var expandedGuide by remember { mutableStateOf<Int?>(null) }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.hub_guide_main_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // طريقة 1: الوايرلس
                    ExpandableGuideSection(
                        title = stringResource(id = R.string.hub_guide_wireless_title),
                        icon = Icons.Default.Wifi,
                        isExpanded = expandedGuide == 1,
                        onClick = { expandedGuide = if (expandedGuide == 1) null else 1 }
                    ) {
                        GuideStep(number = "1", text = stringResource(id = R.string.hub_guide_wireless_step1))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "2", text = stringResource(id = R.string.hub_guide_wireless_step2))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "3", text = stringResource(id = R.string.hub_guide_wireless_step3))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // طريقة 2: الكمبيوتر
                    ExpandableGuideSection(
                        title = stringResource(id = R.string.hub_guide_pc_title),
                        icon = Icons.Default.Cable,
                        isExpanded = expandedGuide == 2,
                        onClick = { expandedGuide = if (expandedGuide == 2) null else 2 }
                    ) {
                        GuideStep(number = "1", text = stringResource(id = R.string.hub_guide_pc_step1))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "2", text = stringResource(id = R.string.hub_guide_pc_step2))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "3", text = stringResource(id = R.string.hub_guide_pc_step3))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // طريقة 3: الروت
                    ExpandableGuideSection(
                        title = stringResource(id = R.string.hub_guide_root_title),
                        icon = Icons.Default.DeveloperMode,
                        isExpanded = expandedGuide == 3,
                        onClick = { expandedGuide = if (expandedGuide == 3) null else 3 }
                    ) {
                        GuideStep(number = "1", text = stringResource(id = R.string.hub_guide_root_step1))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "2", text = stringResource(id = R.string.hub_guide_root_step2))
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStep(number = "3", text = stringResource(id = R.string.hub_guide_root_step3))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ExpandableGuideSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun GuideStep(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
        )
    }
}

@Composable
private fun InfoBadge(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
