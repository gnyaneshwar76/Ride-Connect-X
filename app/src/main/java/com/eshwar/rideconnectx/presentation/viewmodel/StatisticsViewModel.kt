package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.data.local.db.RideBucket
import com.eshwar.rideconnectx.data.local.db.RideEntity
import com.eshwar.rideconnectx.data.local.db.RideTotals
import com.eshwar.rideconnectx.data.repository.RideRepository
import com.eshwar.rideconnectx.data.repository.StatsPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repo: RideRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(StatsPeriod.WEEK)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    // Each stream re-subscribes when the filter changes.
    val totals: StateFlow<RideTotals> = _period
        .flatMapLatest { repo.observeTotals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideTotals(0, 0, 0L, 0.0))

    val rides: StateFlow<List<RideEntity>> = _period
        .flatMapLatest { repo.observeRides(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val buckets: StateFlow<List<RideBucket>> = _period
        .flatMapLatest { repo.observeBuckets(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPeriod(p: StatsPeriod) { _period.value = p }
}
