package com.songapp.ktv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songapp.ktv.KtvApp
import com.songapp.ktv.data.QueueRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel : ViewModel() {
    private val repo = KtvApp.get().repository
    private val player = KtvApp.get().player

    val items: StateFlow<List<QueueRow>> = repo.queueFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playItem(row: QueueRow) {
        viewModelScope.launch {
            repo.getById(row.songId)?.let { player.playSong(it) }
        }
    }

    fun remove(row: QueueRow) = viewModelScope.launch { repo.removeFromQueue(row.id) }
    fun clear() = viewModelScope.launch { repo.clearQueue() }
}
