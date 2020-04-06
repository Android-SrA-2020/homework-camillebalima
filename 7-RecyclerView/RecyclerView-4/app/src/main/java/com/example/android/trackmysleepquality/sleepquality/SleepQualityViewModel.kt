package com.example.android.trackmysleepquality.sleepquality

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SleepQualityViewModel(
        private val sleepNightKey: Long = 0L,
        dataSource: SleepDatabaseDao) : ViewModel() {


    val database = dataSource


    private val viewModelJob = Job()


    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _navigateToSleepTracker = MutableLiveData<Boolean?>()


    val navigateToSleepTracker: LiveData<Boolean?>
        get() = _navigateToSleepTracker


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


    fun doneNavigating() {
        _navigateToSleepTracker.value = null
    }


    fun onSetSleepQuality(quality: Int) {
        uiScope.launch {

            withContext(Dispatchers.IO) {
                val tonight = database.get(sleepNightKey)
                tonight.sleepQuality = quality
                database.update(tonight)
            }

            _navigateToSleepTracker.value = true
        }
    }
}
