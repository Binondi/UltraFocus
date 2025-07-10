package devs.org.ultrafocus.viewModel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import devs.org.ultrafocus.repository.AppRepository
import devs.org.ultrafocus.viewModel.MainViewModel

class MainModelFactory(val repository: AppRepository) :
    ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
}