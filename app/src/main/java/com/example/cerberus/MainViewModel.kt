package com.example.cerberus

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cerberus.data.AppInfoCache
import com.example.cerberus.data.LockedAppsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _lockedAppsCount = MutableLiveData<Int>()
    val lockedAppsCount: LiveData<Int> = _lockedAppsCount

    fun preloadAppInfo(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            AppInfoCache.preloadAll(context)
        }
    }

    fun updateLockedAppsCount(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = LockedAppsCache.getLockedApps(context).size
            _lockedAppsCount.postValue(count)
        }
    }
}