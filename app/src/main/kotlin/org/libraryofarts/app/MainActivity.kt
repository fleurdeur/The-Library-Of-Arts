package org.libraryofarts.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import org.libraryofarts.app.ui.ArWallRoute
import org.libraryofarts.core.data.seed.SeedLearning
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.theme.LibraryOfArtsTheme
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.feature.artwork.ArtworkRoute
import org.libraryofarts.feature.collections.CollectionDetailRoute
import org.libraryofarts.feature.collections.CollectionsRoute
import org.libraryofarts.feature.explore.ExploreRoute
import org.libraryofarts.feature.games.CompareRoute
import org.libraryofarts.feature.games.GamesRoute
import org.libraryofarts.feature.games.KidsRoute
import org.libraryofarts.feature.games.MoodRoute
import org.libraryofarts.feature.games.QuizRoute
import org.libraryofarts.feature.games.ScanRoute
import org.libraryofarts.feature.home.HomeRoute
import org.libraryofarts.feature.journal.JournalRoute
import org.libraryofarts.feature.learn.ArtistRoute
import org.libraryofarts.feature.learn.FlashcardsRoute
import org.libraryofarts.feature.learn.LearnRoute
import org.libraryofarts.feature.learn.LessonRoute
import org.libraryofarts.feature.learn.TourRoute
import org.libraryofarts.feature.settings.SettingsRoute
import org.libraryofarts.feature.taste.TasteProfileRoute

object Route {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val SETTINGS = "settings"
    const val ARTWORK = "artwork/{id}"
    const val ARTIST = "artist/{name}"
    const val TASTE = "taste"
    const val JOURNAL = "journal"
    const val COLLECTIONS = "collections"
    const val COLLECTION = "collection/{id}"
    const val LEARN = "learn"
    const val TOUR = "tour/{id}"
    const val LESSON = "lesson/{id}"
    const val FLASHCARDS = "flashcards"
    const val GAMES = "games"
    const val QUIZ = "quiz/{kind}"
    const val COMPARE = "compare"
    const val MOOD = "mood"
    const val SCAN = "scan"
    const val KIDS = "kids"
    const val AR = "ar/{id}"

    fun artwork(id: String) = "artwork/$id"
    fun artist(name: String) = "artist/${URLEncoder.encode(name, "UTF-8")}"
    fun collection(id: String) = "collection/$id"
    fun tour(id: String) = "tour/$id"
    fun lesson(id: String) = "lesson/$id"
    fun quiz(kind: String) = "quiz/$kind"
    fun ar(id: String) = "ar/$id"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by settings.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            val dark = prefs.usesDarkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
            LibraryOfArtsTheme(
                darkTheme = dark,
                pureBlack = prefs.pureBlack,
                dynamicColor = prefs.dynamicColor,
                highContrast = prefs.highContrast,
                reducedMotion = prefs.reducedMotion,
                colorBlindSafe = prefs.colorBlindSafe,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    LaunchedEffect(Unit) {
                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                LibraryApp(prefs)
            }
        }
    }
}

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination(Route.HOME, "Home", Icons.Default.Home),
    TopDestination(Route.EXPLORE, "Explore", Icons.Default.Explore),
    TopDestination(Route.SETTINGS, "Settings", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryApp(prefs: UserPreferences) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTop = currentRoute in topDestinations.map { it.route }
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    var showLibrary by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Row(modifier = Modifier.fillMaxSize()) {
        if (wide && isTop) {
            LibraryRail(
                currentRoute = currentRoute,
                onSelect = { navigateTop(nav, it) },
            )
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                if (!wide && isTop) {
                    LibraryBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { navigateTop(nav, it) },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
            NavHost(
                navController = nav,
                startDestination = Route.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 920.dp),
            ) {
                composable(
                    Route.HOME,
                    deepLinks = listOf(navDeepLink { uriPattern = "libraryofarts://home" }),
                ) {
                    HomeRoute(
                        onArtworkClick = { nav.navigate(Route.artwork(it)) },
                        onOpenTasteProfile = { nav.navigate(Route.TASTE) },
                        onOpenExplore = { navigateTop(nav, Route.EXPLORE) },
                        onOpenLibrary = { showLibrary = true },
                    )
                }
                composable(
                    Route.EXPLORE,
                    deepLinks = listOf(navDeepLink { uriPattern = "libraryofarts://explore" }),
                ) {
                    ExploreRoute(
                        onArtworkClick = { nav.navigate(Route.artwork(it)) },
                        onOpenArtist = { nav.navigate(Route.artist(it)) },
                    )
                }
                composable(
                    Route.SETTINGS,
                    deepLinks = listOf(navDeepLink { uriPattern = "libraryofarts://settings" }),
                ) { SettingsRoute() }
                composable(
                    Route.ARTWORK,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "libraryofarts://artwork/{id}" },
                        navDeepLink { uriPattern = "https://libraryofarts.org/artwork/{id}" },
                    ),
                ) {
                    ArtworkRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                        onOpenAr = { nav.navigate(Route.ar(it)) },
                        onOpenArtist = { nav.navigate(Route.artist(it)) },
                    )
                }
                composable(
                    Route.ARTIST,
                    arguments = listOf(navArgument("name") { type = NavType.StringType }),
                ) { entry ->
                    val name = URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                    ArtistRoute(
                        artistName = name,
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(Route.TASTE) { TasteProfileRoute(onBack = { nav.popBackStack() }) }
                composable(
                    Route.JOURNAL,
                    deepLinks = listOf(navDeepLink { uriPattern = "libraryofarts://journal" }),
                ) {
                    JournalRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                        onOpenToday = { navigateTop(nav, Route.HOME) },
                    )
                }
                composable(Route.COLLECTIONS) {
                    CollectionsRoute(
                        onBack = { nav.popBackStack() },
                        onOpenCollection = { nav.navigate(Route.collection(it)) },
                    )
                }
                composable(
                    Route.COLLECTION,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) {
                    CollectionDetailRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(Route.LEARN) {
                    LearnRoute(
                        onBack = { nav.popBackStack() },
                        onOpenTour = { nav.navigate(Route.tour(it)) },
                        onOpenLesson = { nav.navigate(Route.lesson(it)) },
                        onOpenArtist = { nav.navigate(Route.artist(it)) },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                        onOpenFlashcards = { nav.navigate(Route.FLASHCARDS) },
                    )
                }
                composable(
                    Route.TOUR,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    TourRoute(
                        tourId = entry.arguments?.getString("id") ?: "",
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(
                    Route.LESSON,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    LessonRoute(
                        lessonId = entry.arguments?.getString("id") ?: "",
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(Route.FLASHCARDS) { FlashcardsRoute(onBack = { nav.popBackStack() }) }
                composable(Route.GAMES) {
                    GamesRoute(
                        onBack = { nav.popBackStack() },
                        onOpenQuiz = { nav.navigate(Route.quiz(it)) },
                        onOpenCompare = { nav.navigate(Route.COMPARE) },
                        onOpenMood = { nav.navigate(Route.MOOD) },
                        onOpenScan = { nav.navigate(Route.SCAN) },
                        onOpenKids = { nav.navigate(Route.KIDS) },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(
                    Route.QUIZ,
                    arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                ) { entry ->
                    QuizRoute(
                        kind = entry.arguments?.getString("kind") ?: "GENERAL",
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(Route.COMPARE) {
                    CompareRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(Route.MOOD) {
                    MoodRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(
                    Route.SCAN,
                    deepLinks = listOf(navDeepLink { uriPattern = "libraryofarts://scan" }),
                ) {
                    ScanRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(Route.KIDS) {
                    KidsRoute(
                        onBack = { nav.popBackStack() },
                        onOpenArtwork = { nav.navigate(Route.artwork(it)) },
                    )
                }
                composable(
                    Route.AR,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    ArWallRoute(
                        artworkId = entry.arguments?.getString("id") ?: "",
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            }
        }
    }

    if (showLibrary) {
        ModalBottomSheet(
            onDismissRequest = { showLibrary = false },
            sheetState = sheetState,
        ) {
            LibraryMenu(
                onNavigate = { route ->
                    showLibrary = false
                    nav.navigate(route)
                },
            )
        }
    }
}

@Composable
private fun LibraryBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            topDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(destination.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        destination.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRail(currentRoute: String?, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(94.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "GLA",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(30.dp))
        topDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(destination.route) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    destination.icon,
                    contentDescription = destination.label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    destination.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(1.dp)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun LibraryMenu(onNavigate: (String) -> Unit) {
    val entries = listOf(
        Triple("Taste profile", "Your eye, on this device", Route.TASTE),
        Triple("Journal", "Private notes and moods", Route.JOURNAL),
        Triple("Collections", "Your own exhibitions", Route.COLLECTIONS),
        Triple("Art School", "Tours, lessons, glossary", Route.LEARN),
        Triple("Looking games", "Play, compare, scan", Route.GAMES),
        Triple("Scan a painting", "Identify a work offline", Route.SCAN),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
    ) {
        Text(
            "THE READING ROOM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Your library",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            "${SeedLearning.tours.size} tours · ${SeedLearning.lessons.size} lessons · on this device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn {
            itemsIndexed(entries) { index, entry ->
                val (title, subtitle, route) = entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 24.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(30.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
            item {
                Text(
                    "No account. No ads. No tracking.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
    }
}

private fun navigateTop(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(Route.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
