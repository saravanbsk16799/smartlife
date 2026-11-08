package com.stark.smartlife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.StarkBackground
import com.stark.smartlife.ui.screens.ActivityScreen
import com.stark.smartlife.ui.screens.DeviceScreen
import com.stark.smartlife.ui.screens.HealthScreen
import com.stark.smartlife.ui.screens.HomeScreen
import com.stark.smartlife.ui.screens.ProfileScreen
import com.stark.smartlife.ui.theme.StarkTheme
import com.stark.smartlife.ui.theme.LocalHud

private data class Tab(val titleRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(R.string.tab_home, Icons.Filled.Home),
    Tab(R.string.tab_health, Icons.Filled.Favorite),
    Tab(R.string.tab_activity, Icons.Filled.DirectionsRun),
    Tab(R.string.tab_device, Icons.Filled.Watch),
    Tab(R.string.tab_profile, Icons.Filled.Person),
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val theme by vm.theme.collectAsState()
            StarkTheme(theme) { StarkRoot(vm) }
        }
    }
}

@Composable
private fun StarkRoot(vm: MainViewModel) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // Recompute "today" whenever the app returns to the foreground (e.g. after midnight).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshDay() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    StarkBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = selected == i,
                            onClick = { selected = i },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(stringResource(t.titleRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalHud.current.reactor,
                                selectedTextColor = LocalHud.current.gold,
                                indicatorColor = LocalHud.current.red.copy(alpha = 0.25f),
                            ),
                        )
                    }
                }
            },
        ) { pad ->
            androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                when (selected) {
                    0 -> HomeScreen(vm)
                    1 -> HealthScreen(vm)
                    2 -> ActivityScreen(vm)
                    3 -> DeviceScreen(vm)
                    else -> ProfileScreen(vm)
                }
            }
        }
    }
}
