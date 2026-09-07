package com.ssher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssher.data.HostProfile
import com.ssher.data.HostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HostListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HostRepository(application)

    val hosts: StateFlow<List<HostProfile>> = repository.hosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun save(profile: HostProfile) {
        viewModelScope.launch {
            repository.upsert(profile)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
