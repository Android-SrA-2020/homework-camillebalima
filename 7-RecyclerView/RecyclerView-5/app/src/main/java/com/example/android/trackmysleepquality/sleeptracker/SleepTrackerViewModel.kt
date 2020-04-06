package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SleepTrackerViewModel(
        dataSource: SleepDatabaseDao,
        application: Application) : ViewModel() {


    val database = dataSource


    private var viewModelJob = Job()


    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()

    val nights = database.getAllNights()


    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }


    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }


    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }


    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }


    private var _showSnackbarEvent = MutableLiveData<Boolean?>()


    val showSnackBarEvent: LiveData<Boolean?>
        get() = _showSnackbarEvent


    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()


    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality


    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = null
    }


    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }


    private val _navigateToSleepDetail = MutableLiveData<Long>()
    val navigateToSleepDetail
        get() = _navigateToSleepDetail

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDetail.value = id
    }

    fun onSleepDetailNavigated() {
        _navigateToSleepDetail.value = null
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }


    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    private suspend fun update(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    fun onStart() {
        uiScope.launch {

            val newNight = SleepNight()

            insert(newNight)

            tonight.value = getTonightFromDatabase()
        }
    }


    fun onStop() {
        uiScope.launch {

            val oldNight = tonight.value ?: return@launch

            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)

            _navigateToSleepQuality.value = oldNight
        }
    }


    fun onClear() {
        uiScope.launch {
            clear()

            tonight.value = null

            _showSnackbarEvent.value = true
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}