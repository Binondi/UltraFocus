package devs.org.ultrafocus.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import devs.org.ultrafocus.R
import devs.org.ultrafocus.adapters.SelectAppsAdapter
import devs.org.ultrafocus.database.AppDatabase
import devs.org.ultrafocus.databinding.ActivitySelectAppBinding
import devs.org.ultrafocus.model.AppInfo
import devs.org.ultrafocus.repository.AppRepository
import devs.org.ultrafocus.viewModel.MainViewModel
import devs.org.ultrafocus.viewModel.factory.MainModelFactory
import kotlinx.coroutines.launch

class SelectAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAppBinding
    private lateinit var adapter: SelectAppsAdapter
    private lateinit var viewModel: MainViewModel
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySelectAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database)
        val factory = MainModelFactory(repository)
        viewModel = factory.create(MainViewModel::class.java)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadInstalledApps()
        clickListeners()

    }

    private fun clickListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId){
                R.id.done -> {
                    adapter.onItemSelected { apps ->
                        if(apps.isEmpty()){
                            Toast.makeText(
                                this,
                                "No Apps Selected",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@onItemSelected
                        }
                        lifecycleScope.launch {
                            apps.forEach { appInfo ->
                                viewModel.blockApp(appInfo)
                            }
                            finish()
                        }

                    }
                }
                else -> {}
            }
            return@setOnMenuItemClickListener true
        }
        binding.swipeLayout.setOnRefreshListener {
            loadInstalledApps()
        }
    }

    private fun loadInstalledApps() {
        handler.post{
            lifecycleScope.launch {
                val appList = viewModel.listAllApps(this@SelectAppActivity)
                val blockedApps = viewModel.getBlockedApps()
                setAdapter(appList, blockedApps)
            }
        }
    }

    private fun setAdapter(appList: List<AppInfo>, blockedApps: List<AppInfo>) {
        adapter = SelectAppsAdapter(this, appList, blockedApps)
        adapter.setOnAppDeselectedListener { appInfo ->
            lifecycleScope.launch {
                viewModel.removeBlockedApp(appInfo)
            }
        }
        binding.apply {
            loading.visibility = View.GONE
            swipeLayout.visibility = View.VISIBLE
            recyclerView.adapter = adapter
            swipeLayout.isRefreshing = false
        }
    }
}