package com.randos.resourceprovider.presentation.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randos.resourceprovider.ResourceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableLiveData(HomeScreenState())
    val uiState: LiveData<HomeScreenState> = _uiState

    fun getData() {
        viewModelScope.launch {
            delay(1000)
            try {
                _uiState.postValue(HomeScreenState(text = ResourceManager.Strings.test()))

            } catch (e: Exception) {
                _uiState.postValue(HomeScreenState(text = e.localizedMessage?:"---"))
            }
            Log.d("TAG", "getData: ")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("TAG", "onCleared: ")
    }
}
