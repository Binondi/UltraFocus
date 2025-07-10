package devs.org.ultrafocus.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import devs.org.ultrafocus.databinding.SelectAppsItemBinding
import devs.org.ultrafocus.model.AppInfo

class SelectAppsAdapter(
    private val context: Context,
    private val list: List<AppInfo>,
    private val initiallySelected: List<AppInfo> = emptyList()
) :
    RecyclerView.Adapter<SelectAppsAdapter.ViewHolder>() {

    private val selectedAppsList = mutableListOf<AppInfo>()
    private var onAppDeselected: ((AppInfo) -> Unit)? = null

    init {
        selectedAppsList.addAll(initiallySelected)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = SelectAppsItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val appInfo = list[position]
        val isChecked = selectedAppsList.any { it.packageName == appInfo.packageName }
        holder.bind(appInfo, isChecked)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: SelectAppsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppInfo, isChecked: Boolean) {
            Glide.with(context)
                .load(appInfo.icon)
                .into(binding.icon)
            binding.label.text = appInfo.appName
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.isChecked = isChecked
            binding.checkbox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (!selectedAppsList.any { it.packageName == appInfo.packageName }) {
                        selectedAppsList.add(appInfo)
                    }
                } else {
                    selectedAppsList.removeAll { it.packageName == appInfo.packageName }
                    onAppDeselected?.invoke(appInfo)
                }
            }
        }
    }

    fun onItemSelected(onItemSelected:(MutableList<AppInfo>) -> Unit){
       onItemSelected(selectedAppsList)
    }

    fun setOnAppDeselectedListener(callback: (AppInfo) -> Unit) {
        this.onAppDeselected = callback
    }
}