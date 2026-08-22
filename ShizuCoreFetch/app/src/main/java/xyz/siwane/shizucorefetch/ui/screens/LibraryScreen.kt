package xyz.siwane.shizucorefetch.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.data.InstallUiState
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    mainViewModel: MainViewModel,
    onOpenApkManager: () -> Unit,
    onAppClick: (DummyApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarkedNames by mainViewModel.bookmarkedApps.collectAsState()
    val allApps by mainViewModel.allStoreApps.collectAsState()

    LaunchedEffect(Unit) {
        if (allApps.isEmpty()) mainViewModel.loadStoreApps()
    }

    // installStates هو نفس المصدر الموحّد اللي كتقرا منه StoreScreen
    // وAppDetailsScreen (مطابقة بالحزمة أو بالاسم، فحص حقيقي لـ PackageManager) —
    // بلا نسخة ثالثة مكررة من نفس منطق المقارنة كيفما كان قبل هنا.
    val installStatesMap by mainViewModel.installStates.collectAsState()

    val installedApps = remember(allApps, installStatesMap) {
        allApps.filter { app -> installStatesMap[app.appId] is InstallUiState.Open }
    }

    val updateApps = remember(allApps, installStatesMap) {
        allApps.filter { app -> installStatesMap[app.appId] is InstallUiState.Update }
    }

    val bookmarkedAppsList = allApps.filter { bookmarkedNames.contains(it.name) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        // 1. مدير ملفات APK
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApkManager() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.apk_manager_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.apk_manager_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 2. التطبيقات التي تتطلب تحديث
        if (updateApps.isNotEmpty()) {
            item {
                SectionTitle(title = stringResource(id = R.string.lib_updates_title))
            }
            items(updateApps) { app ->
                AppListItem(app = app, mainViewModel = mainViewModel, onClick = { onAppClick(app) })
            }
        }

        // 3. التطبيقات المثبتة
        if (installedApps.isNotEmpty()) {
            item {
                SectionTitle(title = stringResource(id = R.string.lib_installed_title))
            }
            items(installedApps) { app ->
                AppListItem(app = app, mainViewModel = mainViewModel, onClick = { onAppClick(app) })
            }
        }

        // 4. المفضلة
        item {
            SectionTitle(title = stringResource(id = R.string.lib_bookmarks_title))
        }
        
        if (bookmarkedAppsList.isNotEmpty()) {
            items(bookmarkedAppsList) { app ->
                AppListItem(app = app, mainViewModel = mainViewModel, onClick = { onAppClick(app) })
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.library_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
