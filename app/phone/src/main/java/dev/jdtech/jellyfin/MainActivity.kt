package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.databinding.ActivityMainBinding
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var navController: NavController

    @OptIn(NavigationUiSaveStateControl::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (appPreferences.amoledTheme) {
            setTheme(CoreR.style.Theme_FindroidAMOLED)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.app_navigation)

        checkServersEmpty(graph) {
            navController.setGraph(graph, intent.extras)
        }
        checkUser(graph) {
            navController.setGraph(graph, intent.extras)
        }

        val navView: NavigationBarView = binding.navView as NavigationBarView

        setSupportActionBar(binding.mainToolbar)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.mediaFragment,
                R.id.favoriteFragment,
                R.id.downloadFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        // navView.setupWithNavController(navController)
        // Don't save the state of other main navigation items, only this experimental function allows turning off this behavior
        NavigationUI.setupWithNavController(navView, navController, false)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility = when (destination.id) {
                R.id.twoPaneSettingsFragment, R.id.serverSelectFragment, R.id.addServerFragment, R.id.loginFragment, com.mikepenz.aboutlibraries.R.id.about_libraries_dest, R.id.usersFragment, R.id.serverAddressesFragment -> View.GONE
                else -> View.VISIBLE
            }
            if (destination.id == com.mikepenz.aboutlibraries.R.id.about_libraries_dest) binding.mainToolbar.title =
                getString(CoreR.string.app_info)
        }

        loadDownloadLocation(applicationContext)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    private fun checkServersEmpty(graph: NavGraph, onServersEmpty: () -> Unit = {}) {
        if (!viewModel.startDestinationChanged) {
            val nServers = database.getServersCount()
            if (nServers < 1) {
                graph.setStartDestination(R.id.addServerFragment)
                viewModel.startDestinationChanged = true
                onServersEmpty()
            }
        }
    }

    private fun checkUser(graph: NavGraph, onNoUser: () -> Unit = {}) {
        if (!viewModel.startDestinationChanged) {
            appPreferences.currentServer?.let {
                val currentUser = database.getServerCurrentUser(it)
                if (currentUser == null) {
                    graph.setStartDestination(R.id.loginFragment)
                    viewModel.startDestinationChanged = true
                    onNoUser()
                }
            }
        }
    }
}
