package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val favoriteSections: List<FavoriteSection>) : UiState()
        object Loading : UiState()
        data class Error(val message: String?) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getFavoriteItems()

                if (items.isEmpty()) {
                    uiState.emit(UiState.Normal(emptyList()))
                    return@launch
                }

                val favoriteSections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.type == "Movie" }).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Shows",
                        items.filter { it.type == "Series" }).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.type == "Episode" }).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                }

                uiState.emit(UiState.Normal(favoriteSections))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e.message))
            }
        }
    }
}