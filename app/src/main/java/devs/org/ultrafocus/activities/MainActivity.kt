package devs.org.ultrafocus.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import devs.org.ultrafocus.R
import devs.org.ultrafocus.adapters.SelectedAppsAdapter
import devs.org.ultrafocus.database.AppDatabase
import devs.org.ultrafocus.databinding.ActivityMainBinding
import devs.org.ultrafocus.model.AppInfo
import devs.org.ultrafocus.repository.AppRepository
import devs.org.ultrafocus.viewModel.MainViewModel
import devs.org.ultrafocus.viewModel.factory.MainModelFactory
import kotlinx.coroutines.launch
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import android.app.TimePickerDialog
import android.app.AlertDialog
import android.content.ComponentName
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TimePicker
import androidx.core.app.ActivityOptionsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import devs.org.ultrafocus.services.BlockerAccessibilityService
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var handler = Handler(Looper.getMainLooper())
    private var list = mutableListOf<AppInfo>()
    private lateinit var options: ActivityOptionsCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        options = ActivityOptionsCompat.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database)
        val factory = MainModelFactory(repository)
        viewModel = factory.create(MainViewModel::class.java)
        clickListeners()
        loadSelectedApps()
        updateFocusSwitchState()
    }

    override fun onResume() {
        super.onResume()
        loadSelectedApps()
        updateFocusSwitchState()
    }

    private fun updateFocusSwitchState() {
        binding.focusSwitch.isChecked = isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "${packageName}/devs.org.ultrafocus.services.BlockerAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return !TextUtils.isEmpty(enabledServices) && enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }



    private fun openAccessibilityServiceScreen(cls: Class<*>) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            val componentName = ComponentName(this, cls)
            intent.putExtra(":settings:fragment_args_key", componentName.flattenToString())
            val bundle = Bundle()
            bundle.putString(":settings:fragment_args_key", componentName.flattenToString())
            intent.putExtra(":settings:show_fragment_args", bundle)
            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to general Accessibility Settings
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun loadSelectedApps() {
        handler.post {
            lifecycleScope.launch {
                list.clear()
                list = viewModel.getBlockedApps()
                setAdapter()
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapter() {
        val adapter = SelectedAppsAdapter(this, list)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.setOnItemRemovedListener { appInfo ->
            lifecycleScope.launch {
                if (appInfo != null) viewModel.removeBlockedApp(appInfo)
            }
        }
        adapter.setOnSetTimePeriodListener { appInfo ->
            showTimePeriodDialogForApp(appInfo)
        }
    }

    private fun clickListeners() {
        binding.btnAddApps.setOnClickListener {
            startActivity(Intent(this, SelectAppActivity::class.java))
        }
        binding.focusSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isAccessibilityServiceEnabled()) {
                    showMaterialDialog()
                }
            } else {
                Toast.makeText(this, "To disable, turn off UltraFocus in Accessibility Settings.", Toast.LENGTH_LONG).show()
                binding.focusSwitch.isChecked = isAccessibilityServiceEnabled()
            }
        }
        binding.btnAddTimePeriod.setOnClickListener {
            showTimePeriodDialog()
        }
    }

    private fun showTimePeriodDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_period, null)
        val fromTimeBtn = dialogView.findViewById<View>(R.id.btnFromTime)
        val toTimeBtn = dialogView.findViewById<View>(R.id.btnToTime)
        val repeatSpinner = dialogView.findViewById<Spinner>(R.id.spinnerRepeat)
        var fromTime: String? = null
        var toTime: String? = null
        val repeatModes = arrayOf("Daily", "Weekly", "Monthly")
        repeatSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatModes)

        fromTimeBtn.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
                fromTime = String.format("%02d:%02d", hourOfDay, minute)
                (fromTimeBtn as? android.widget.Button)?.text = formatTo12Hour(fromTime!!)
            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), false).show()
        }
        toTimeBtn.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
                toTime = String.format("%02d:%02d", hourOfDay, minute)
                (toTimeBtn as? android.widget.Button)?.text = formatTo12Hour(toTime!!)
            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), false).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Time Period")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val repeatMode = repeatSpinner.selectedItem.toString().uppercase()
                lifecycleScope.launch {
                    viewModel.updateBlockedAppsTimePeriod(fromTime, toTime, repeatMode)
                    Toast.makeText(this@MainActivity, "Time period set for blocked apps!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePeriodDialogForApp(appInfo: AppInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_period, null)
        val fromTimeBtn = dialogView.findViewById<View>(R.id.btnFromTime)
        val toTimeBtn = dialogView.findViewById<View>(R.id.btnToTime)
        val repeatSpinner = dialogView.findViewById<Spinner>(R.id.spinnerRepeat)
        var fromTime: String? = appInfo.fromTime
        var toTime: String? = appInfo.toTime
        val repeatModes = arrayOf("Daily", "Weekly", "Monthly")
        repeatSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatModes)
        if (appInfo.repeatMode != null) {
            val idx = repeatModes.indexOfFirst { it.equals(appInfo.repeatMode, ignoreCase = true) }
            if (idx >= 0) repeatSpinner.setSelection(idx)
        }
        (fromTimeBtn as? android.widget.Button)?.text = fromTime ?: "From Time"
        (toTimeBtn as? android.widget.Button)?.text = toTime ?: "To Time"
        fromTimeBtn.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
                fromTime = String.format("%02d:%02d", hourOfDay, minute)
                (fromTimeBtn as? android.widget.Button)?.text = formatTo12Hour(fromTime!!)
            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), false).show()
        }
        toTimeBtn.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            TimePickerDialog(this, { _: TimePicker, hourOfDay: Int, minute: Int ->
                toTime = String.format("%02d:%02d", hourOfDay, minute)
                (toTimeBtn as? android.widget.Button)?.text = formatTo12Hour(toTime!!)
            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), false).show()
        }
        AlertDialog.Builder(this)
            .setTitle("Set Time Period for ${appInfo.appName}")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val repeatMode = repeatSpinner.selectedItem.toString().uppercase()
                lifecycleScope.launch {
                    viewModel.updateSingleAppTimePeriod(appInfo, fromTime, toTime, repeatMode)
                    Toast.makeText(this@MainActivity, "Time period set for ${appInfo.appName}!", Toast.LENGTH_LONG).show()
                    delay(500)
                    loadSelectedApps()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatTo12Hour(time: String): String {
        return try {
            val sdf24 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val date = sdf24.parse(time)
            if (date != null) sdf12.format(date) else time
        } catch (e: Exception) {
            time
        }
    }

    private fun showMaterialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.enable_accessibility_service))
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton(getString(R.string.grant)){ _, _->
                openAccessibilityServiceScreen(BlockerAccessibilityService::class.java)
            }
            .setNegativeButton(getString(R.string.cancel)){ dialog, _ ->null}
            .create().show()
    }
}


