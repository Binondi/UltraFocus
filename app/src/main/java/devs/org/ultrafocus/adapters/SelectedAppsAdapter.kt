package devs.org.ultrafocus.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import devs.org.ultrafocus.databinding.SelectedAppItemBinding
import devs.org.ultrafocus.model.AppInfo

class SelectedAppsAdapter(
    private val context: Context,
    private val list: MutableList<AppInfo>
) :
    RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder>() {

    private var onItemRemovedCallback: ((AppInfo) -> Unit)? = null
    private var onSetTimePeriodCallback: ((AppInfo) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = SelectedAppItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val appInfo = list[position]
        holder.bind(appInfo, position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: SelectedAppItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppInfo, position: Int) {
            Glide.with(context)
                .load(appInfo.icon)
                .into(binding.icon)
            binding.label.text = appInfo.appName
            val from = appInfo.fromTime?.let { formatTo12Hour(it) } ?: "--"
            val to = appInfo.toTime?.let { formatTo12Hour(it) } ?: "--"
            val repeat = appInfo.repeatMode ?: "--"
            binding.timePeriodInfo.text = "Time: $from - $to | Repeat: $repeat"
            binding.delete.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < list.size) {
                    val removedApp = list[pos]
                    list.removeAt(pos)
                    notifyItemRemoved(pos)
                    onItemRemovedCallback?.invoke(removedApp)
                }
            }
            binding.btnSetTimePeriod.setOnClickListener {
                onSetTimePeriodCallback?.invoke(appInfo)
            }
        }
    }

    fun setOnItemRemovedListener(callback: (AppInfo) -> Unit) {
        this.onItemRemovedCallback = callback
    }

    fun setOnSetTimePeriodListener(callback: (AppInfo) -> Unit) {
        this.onSetTimePeriodCallback = callback
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
}