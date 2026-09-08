package com.anothersshclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anothersshclient.data.HostProfile
import com.anothersshclient.data.HostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Survives leaving the host list so selection can be restored on return. */
sealed interface RememberedHostSelection {
    data object Fab : RememberedHostSelection
    data class Host(val id: String) : RememberedHostSelection
}

class HostListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HostRepository(application)

    /** Null until DataStore emits; empty list means no saved hosts. */
    val hosts: StateFlow<List<HostProfile>?> = repository.hosts
        .map<List<HostProfile>, List<HostProfile>?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    var rememberedSelection: RememberedHostSelection? = null
        private set

    fun rememberSelection(selection: RememberedHostSelection) {
        rememberedSelection = selection
    }

    fun save(profile: HostProfile, password: String?) {
        viewModelScope.launch {
            repository.upsert(profile, password)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
        val remembered = rememberedSelection
        if (remembered is RememberedHostSelection.Host && remembered.id == id) {
            rememberedSelection = null
        }
    }

    fun reorder(ordered: List<HostProfile>) {
        viewModelScope.launch {
            repository.reorder(ordered)
        }
    }

    suspend fun passwordFor(hostId: String): String? = repository.getPassword(hostId)
}
