package xyz.siwane.shizucorefetch.ui

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.ui.screens.ApkManagerScreen
import xyz.siwane.shizucorefetch.ui.screens.AppDetailsScreen
import xyz.siwane.shizucorefetch.ui.screens.DummyApp
import xyz.siwane.shizucorefetch.ui.screens.GitHubLoginWebViewScreen
import xyz.siwane.shizucorefetch.ui.screens.LibraryScreen
import xyz.siwane.shizucorefetch.ui.screens.MyAccountScreen
import xyz.siwane.shizucorefetch.ui.screens.SettingsScreen
import xyz.siwane.shizucorefetch.ui.screens.ShizukuHubScreen
import xyz.siwane.shizucorefetch.ui.screens.StoreScreen
import xyz.siwane.shizucorefetch.viewmodel.AuthUiState
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    // appDetailsStack: مكدس تنقل حقيقي لتفاصيل التطبيق (بدل selectedApp متغير
    // وحيد). قبل هادشي: فتح تطبيق آخر (من "تطبيقات أخرى للمطور" جوا تفاصيل
    // تطبيق، أو من المكتبة) كان كيبدّل نفس المتغير الوحيد بلا أي تذكر للمستوى
    // السابق، فالرجوع للخلف كان كيمسح كلشي ويرجع مباشرة لجذر المتجر — دابا كل
    // فتح تطبيق كيتزاد (push) فوق المكدس، والرجوع كيسحب (pop) آخر واحد فقط.
    val appDetailsStack = remember { androidx.compose.runtime.mutableStateListOf<DummyApp>() }
    val selectedApp: DummyApp? = appDetailsStack.lastOrNull()
    fun pushAppDetails(app: DummyApp) {
        if (appDetailsStack.lastOrNull()?.appId != app.appId) {
            appDetailsStack.add(app)
        }
    }
    fun popAppDetails() {
        if (appDetailsStack.isNotEmpty()) {
            appDetailsStack.removeAt(appDetailsStack.lastIndex)
        }
    }
    // listState ديال قائمة المتجر الرئيسية محقونة هنا (مستوى MainScreen اللي
    // ماكيخرجش من composition أبدًا)، ماشي محلية جوه StoreScreen — هادشي كيخلي
    // موضع السكرول محفوظ لما تفتح تفاصيل تطبيق وترجع، أو تبدّل بين التبويبات
    // وترجع لتبويب المتجر.
    val storeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var showMyAccount by remember { mutableStateOf(false) }
    var showApkManager by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }
    var pendingAuthorizeUrl by remember { mutableStateOf("") }
    var showInfoSheet by remember { mutableStateOf(false) }

    val isGithubLoggedIn by mainViewModel.isGithubLoggedIn.collectAsState()
    val isOnline by mainViewModel.isOnline.collectAsState()
    val githubUsername by mainViewModel.githubUsername.collectAsState()
    val githubAvatarUrl by mainViewModel.githubAvatarUrl.collectAsState()
    val authUiState by mainViewModel.authUiState.collectAsState()
    val bookmarkedApps by mainViewModel.bookmarkedApps.collectAsState()

    val featuredApps by mainViewModel.featuredStoreApps.collectAsState()
    val storeApps by mainViewModel.filteredStoreApps.collectAsState()
    val isLoadingStore by mainViewModel.isLoadingStore.collectAsState()
    val storeLoadError by mainViewModel.storeLoadError.collectAsState()
    val selectedCategory by mainViewModel.selectedCategory.collectAsState()
    val availableCategories by mainViewModel.availableCategories.collectAsState()
    val isShizukuGranted by mainViewModel.isShizukuGranted.collectAsState()
    val isSilentInstallEnabled by mainViewModel.isSilentInstallEnabled.collectAsState()
    
    val languageCode by mainViewModel.languageCode.collectAsState(initial = "en")
    
    // التقاط أحداث الـ Shortcuts
    val shortcutAction by mainViewModel.shortcutAction.collectAsState()

    val currentContext = LocalContext.current
    val currentConfiguration = LocalConfiguration.current
    val currentLayoutDirection = LocalLayoutDirection.current
    val currentDensity = LocalDensity.current 

    LaunchedEffect(authUiState, isGithubLoggedIn) {
        val state = authUiState
        if (state is AuthUiState.Error) {
            Toast.makeText(currentContext, state.message, Toast.LENGTH_LONG).show()
        }
        if (isGithubLoggedIn && (showWebViewLogin || showAuthSheet)) {
            showWebViewLogin = false
            showAuthSheet = false
        }
    }
    
    // الاستجابة للاختصارات (Shortcuts) وتغيير الشاشة
    LaunchedEffect(shortcutAction) {
        when (shortcutAction) {
            "ACTION_OPEN_LIBRARY" -> {
                selectedTab = 2
                showApkManager = false
                appDetailsStack.clear()
                showMyAccount = false
                mainViewModel.consumeShortcutAction()
            }
            "ACTION_OPEN_APK_MANAGER" -> {
                selectedTab = 2
                showApkManager = true
                appDetailsStack.clear()
                showMyAccount = false
                mainViewModel.consumeShortcutAction()
            }
            "ACTION_OPEN_SETTINGS" -> {
                selectedTab = 3
                showApkManager = false
                appDetailsStack.clear()
                showMyAccount = false
                mainViewModel.consumeShortcutAction()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                val isStoreTab = selectedTab == 0
                // ماشي مربوطة بـ selectedTab (appDetailsStack كتبان فوق أي تبويب كان
                // أصل التطبيق منه: المتجر أو المكتبة)
                val isAppDetails = appDetailsStack.isNotEmpty()
                val showSearch = isStoreTab && !isAppDetails && !showMyAccount
                
                HomeHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        mainViewModel.updateStoreSearchQuery(it)
                    },
                    isLoggedIn = isGithubLoggedIn,
                    username = githubUsername,
                    avatarUrl = githubAvatarUrl,
                    onProfileClick = { showAuthSheet = true },
                    onInfoClick = { showInfoSheet = true },
                    languageCode = languageCode,
                    showSearchBar = showSearch,
                    isOnline = isOnline
                )
            },
            bottomBar = {
                CustomBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        appDetailsStack.clear()
                        showMyAccount = false
                        showApkManager = false
                    }
                )
            }
        ) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding).fillMaxSize()
            
            if (showMyAccount) {
                val githubName by mainViewModel.githubName.collectAsState()
                val githubEmail by mainViewModel.githubEmail.collectAsState()
                val githubBio by mainViewModel.githubBio.collectAsState()
                val githubProfileUrl by mainViewModel.githubProfileUrl.collectAsState()
                val userRepos by mainViewModel.userRepos.collectAsState()
                val githubFollowers by mainViewModel.githubFollowers.collectAsState(initial = 0)
                val githubFollowing by mainViewModel.githubFollowing.collectAsState(initial = 0)
                
                MyAccountScreen(
                    username = githubUsername,
                    displayName = githubName,
                    avatarUrl = githubAvatarUrl,
                    email = githubEmail,
                    bio = githubBio,
                    profileUrl = githubProfileUrl,
                    publicRepos = userRepos.size,
                    followers = githubFollowers,
                    following = githubFollowing,
                    repos = userRepos,
                    onBackClick = { showMyAccount = false },
                    modifier = screenModifier
                )
            } else if (showApkManager) {
                ApkManagerScreen(
                    onBackClick = { showApkManager = false },
                    modifier = screenModifier
                )
            } else if (appDetailsStack.isNotEmpty()) {
                AppDetailsScreen(
                    app = selectedApp!!,
                    isBookmarked = bookmarkedApps.contains(selectedApp!!.name),
                    onBookmarkToggle = { mainViewModel.toggleBookmark(selectedApp!!.name) },
                    onBackClick = { popAppDetails() },
                    onAppClick = { newApp -> pushAppDetails(newApp) },
                    onNavigateToLibrary = { 
                        selectedTab = 2 
                        appDetailsStack.clear() 
                        showApkManager = true
                    },
                    mainViewModel = mainViewModel,
                    modifier = screenModifier
                )
            } else {
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 250),
                    modifier = screenModifier,
                    label = "Tab_Transition"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> {
                            LaunchedEffect(Unit) { mainViewModel.loadStoreApps() }
                            StoreScreen(
                                modifier = Modifier.fillMaxSize(),
                                featuredApps = featuredApps,
                                apps = storeApps,
                                isLoading = isLoadingStore,
                                loadError = storeLoadError,
                                selectedCategory = selectedCategory,
                                availableCategories = availableCategories,
                                isShizukuGranted = isShizukuGranted,
                                isSilentInstallEnabled = isSilentInstallEnabled,
                                isSearchActive = searchQuery.isNotBlank(), // 👈 تم التمرير هنا
                                onCategorySelect = { mainViewModel.selectStoreCategory(it) },
                                onRetry = { mainViewModel.loadStoreApps(force = true) },
                                onAppClick = { pushAppDetails(it) },
                                listState = storeListState,
                                mainViewModel = mainViewModel
                            )
                        }
                        1 -> ShizukuHubScreen(modifier = Modifier.fillMaxSize(), mainViewModel = mainViewModel)
                        2 -> LibraryScreen(
                                mainViewModel = mainViewModel,
                                onOpenApkManager = { showApkManager = true },
                                onAppClick = { pushAppDetails(it) },
                                modifier = Modifier.fillMaxSize()
                             )
                        3 -> SettingsScreen(mainViewModel = mainViewModel, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        if (showWebViewLogin) {
            CompositionLocalProvider(
                LocalContext provides currentContext,
                LocalConfiguration provides currentConfiguration,
                LocalLayoutDirection provides currentLayoutDirection,
                LocalDensity provides currentDensity
            ) {
                GitHubLoginWebViewScreen(
                    authorizeUrl = pendingAuthorizeUrl,
                    expectedState = mainViewModel.currentPendingState(),
                    onAuthCode = { code -> mainViewModel.completeGithubOAuth(code) },
                    onCancel = {
                        mainViewModel.cancelGithubOAuth()
                        showWebViewLogin = false
                    },
                    onError = { message ->
                        Toast.makeText(currentContext, message, Toast.LENGTH_LONG).show()
                        showWebViewLogin = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showAuthSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAuthSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                CompositionLocalProvider(
                    LocalContext provides currentContext,
                    LocalConfiguration provides currentConfiguration,
                    LocalLayoutDirection provides currentLayoutDirection,
                    LocalDensity provides currentDensity
                ) {
                    var tokenInput by remember { mutableStateOf("") }
                    val scrollState = rememberScrollState()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isGithubLoggedIn) {
                            Text(
                                text = stringResource(id = R.string.github_logged_in_as) + " @$githubUsername",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
                            )
                            Text(
                                text = stringResource(id = R.string.account_details_brief),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
                            )

                            if (showMyAccount) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.already_on_account),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        showAuthSheet = false 
                                        showMyAccount = true 
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 56.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(id = R.string.btn_my_account), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { 
                                    mainViewModel.logoutGithub()
                                    showAuthSheet = false 
                                    showMyAccount = false
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(id = R.string.github_logout), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                        } else {
                            Text(
                                text = stringResource(id = R.string.github_sheet_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
                            )
                            Text(
                                text = stringResource(id = R.string.github_sheet_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
                            )

                            Button(
                                onClick = {
                                    pendingAuthorizeUrl = mainViewModel.startGithubOAuth()
                                    showWebViewLogin = true
                                    showAuthSheet = false
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                            ) {
                                Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_github_v), contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(id = R.string.github_oauth_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Text(stringResource(id = R.string.github_or_divider), modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
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
                            
                            OutlinedButton(
                                onClick = {
                                    if (tokenInput.isNotBlank()) {
                                        mainViewModel.loginWithPersonalToken(tokenInput.trim())
                                    }
                                },
                                enabled = authUiState !is AuthUiState.Loading,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                            ) {
                                if (authUiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(id = R.string.github_token_btn), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ورقة المعلومات السفلية (Bottom Sheet Info) بتصميم عصري يتبع لغة التطبيق وتخصيص الواجهة
        if (showInfoSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val localizedContext = remember(languageCode) {
                val locale = Locale(languageCode)
                val config = Configuration(currentContext.resources.configuration)
                config.setLocale(locale)
                currentContext.createConfigurationContext(config)
            }
            
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides currentConfiguration,
                    LocalLayoutDirection provides currentLayoutDirection,
                    LocalDensity provides currentDensity
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.github_explanation_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Start)
                        )
                        Text(
                            text = localizedContext.getString(R.string.github_explanation_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
                        )
                        Button(
                            onClick = { showInfoSheet = false },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                text = localizedContext.getString(R.string.github_explanation_agree),
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
fun CustomBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val navItems = listOf(
        Triple(stringResource(id = R.string.nav_store), Icons.Default.Storefront, Icons.Outlined.Storefront),
        Triple(stringResource(id = R.string.nav_shizuku), Icons.Default.Security, Icons.Outlined.Security),
        Triple(stringResource(id = R.string.nav_library), Icons.Default.Bookmark, Icons.Outlined.BookmarkBorder),
        Triple(stringResource(id = R.string.nav_settings), Icons.Default.Settings, Icons.Outlined.Settings)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp, 
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), 
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 2.2f else 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "weight"
            )

            val interactionSource = remember { MutableInteractionSource() }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, 
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = isSelected && index >= 2,
                            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(tween(200)),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(tween(100))
                        ) {
                            Text(
                                text = item.first,
                                color = contentColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) item.second else item.third,
                            contentDescription = item.first,
                            tint = contentColor,
                            modifier = Modifier.size(26.dp)
                        )

                        AnimatedVisibility(
                            visible = isSelected && index < 2,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(tween(200)),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(tween(100))
                        ) {
                            Text(
                                text = item.first,
                                color = contentColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoggedIn: Boolean,
    username: String,
    avatarUrl: String = "",
    onProfileClick: () -> Unit,
    onInfoClick: () -> Unit,
    languageCode: String,
    showSearchBar: Boolean = true,
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localizedContext = remember(languageCode) {
        val locale = Locale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val isDark = isSystemInDarkTheme()
    val searchBorderColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp, 
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), 
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // الجزء الأيسر: الأيقونة + الاسم + شارة Online
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = localizedContext.getString(R.string.store_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // شارة Online/Offline: كتعكس حالة الشبكة الحقيقية دابا (كانت
                    // نص ثابت "Online" ديمًا، بلا أي فحص فعلي للاتصال).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) // تم التعديل هنا
                            .padding(horizontal = 12.dp, vertical = 6.dp) 
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = localizedContext.getString(if (isOnline) R.string.online_status else R.string.offline_status),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // الجزء الأيمن: زر المعلومات + صورة الملف الشخصي
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    // أيقونة الحساب بتعديل لون الخلفية في حالة عدم تسجيل الدخول لتصبح واضحة
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) // تم التعديل هنا لتباين أفضل
                            )
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn && avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(34.dp).clip(CircleShape)
                            )
                        } else if (isLoggedIn && username.isNotEmpty()) {
                            Text(
                                text = username.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            if (showSearchBar) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(text = localizedContext.getString(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = searchBorderColor.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
