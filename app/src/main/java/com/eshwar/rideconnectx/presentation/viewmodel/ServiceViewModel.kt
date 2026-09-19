package com.eshwar.rideconnectx.presentation.viewmodel

import com.eshwar.rideconnectx.data.local.OwnerScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.data.local.AppSettingsStore
import com.eshwar.rideconnectx.data.local.db.ServiceRecordEntity
import com.eshwar.rideconnectx.data.repository.ServiceRepository
import com.eshwar.rideconnectx.domain.model.DistanceUnit
import com.eshwar.rideconnectx.domain.model.ServiceStatus
import com.eshwar.rideconnectx.domain.model.UpcomingTask
import com.eshwar.rideconnectx.domain.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why a service record was refused, so the form can say so in plain words. */
sealed interface RecordError {
    /** The design's rule: a reading may not be lower than one already recorded. */
    data class OdometerTooLow(val minimumKm: Int) : RecordError
    data object DateInFuture : RecordError
    data object OdometerMissing : RecordError
}

@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val repository: ServiceRepository,
    bleRepository: BleRepository,
    settings: AppSettingsStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /** The rider's chosen units. Distances are stored in km and shown in this. */
    val distanceUnit: StateFlow<DistanceUnit> = settings.settings
        .map { it.distanceUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DistanceUnit.KM)

    val status: StateFlow<ServiceStatus> =
        repository.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceStatus())

    val records: StateFlow<List<ServiceRecordEntity>> =
        repository.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingTasks: StateFlow<List<UpcomingTask>> =
        repository.upcomingTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val remindersEnabled: StateFlow<Boolean> =
        repository.remindersEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Lowest reading a new record may carry — never below what we already know. */
    val minimumOdometerKm: StateFlow<Int> =
        repository.highestRecordedOdometerKm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Fills the tasks table on first run so the list is never blank.
        appScope.launch { repository.seedDefaultTasksIfEmpty() }

        // The screen has to work with the scooter out of range, so every valid
        // frame is cached while we do have it.
        viewModelScope.launch {
            bleRepository.telemetry.collect { t ->
                if (!t.isValid) return@collect
                repository.recordOdometer(t.odometerKm)
            }
        }
    }

    /**
     * Validates and saves. Returns null on success, or the reason it was refused.
     *
     * Runs on [appScope] because saving closes the form and can navigate away —
     * the mistake that has already cost this project three separate bugs.
     */
    fun save(
        id: Long,
        servicedAt: Long,
        centre: String,
        odometerKm: Int?,
        notes: String,
    ): RecordError? {
        if (odometerKm == null || odometerKm <= 0) return RecordError.OdometerMissing
        if (servicedAt > System.currentTimeMillis()) return RecordError.DateInFuture

        // An edit is allowed to keep its own reading, so it is compared against
        // everything except itself.
        val floor = if (id == 0L) minimumOdometerKm.value
        else records.value.filter { it.id != id }.maxOfOrNull { it.odometerKm } ?: 0
        if (odometerKm < floor) return RecordError.OdometerTooLow(floor)

        val record = ServiceRecordEntity(
            id = id,
            // Stamped with the real owner by ServiceRepository on write.
            ownerId = OwnerScope.DRAFT,
            servicedAt = servicedAt,
            centre = centre.trim().ifBlank { "Not recorded" },
            odometerKm = odometerKm,
            notes = notes.trim(),
        )
        appScope.launch {
            if (id == 0L) repository.addRecord(record) else repository.updateRecord(record)
        }
        return null
    }

    fun delete(record: ServiceRecordEntity) {
        appScope.launch { repository.deleteRecord(record) }
    }

    /** Adds a task, or edits one. Blank labels and non-positive intervals are ignored. */
    fun saveTask(id: Long, label: String, everyKm: Int?) {
        appScope.launch { repository.saveTask(id, label, everyKm ?: 0) }
    }

    fun deleteTask(id: Long) {
        appScope.launch { repository.deleteTask(id) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        appScope.launch { repository.setRemindersEnabled(enabled) }
    }
}
