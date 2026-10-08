package com.fincahernandez.gestionpecuaria.data.remote

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Test

class AutoSyncLoopTest {
    private fun loop(sync:suspend()->SyncResult,report:(AutoSyncStatus)->Unit={}) = AutoSyncLoop(
        sync=sync,report=report,settleMs=10,pollMs=10000,retryMs=40,maxRetryMs=5000)
    private suspend fun until(condition:()->Boolean) { withTimeout(3000) {while(!condition())delay(2)} }

    @Test fun startsWithoutAnyLocalEditAndStopsOnCancellation()=runBlocking {
        var calls=0;val statuses=mutableListOf<AutoSyncStatus>()
        val job=launch {loop({calls++;SyncResult(0,0,0)},statuses::add).run(emptyFlow())}
        until {statuses.any {it.phase==AutoSyncPhase.COMPLETE}}
        job.cancelAndJoin();delay(30)
        assertEquals(1,calls);assertEquals(AutoSyncPhase.RUNNING,statuses.first().phase)
    }
    @Test fun burstsOfEditsAreCoalescedIntoOneRun()=runBlocking {
        val changes=MutableSharedFlow<Unit>(extraBufferCapacity=100);var calls=0
        val job=launch {loop({calls++;SyncResult(0,0,0)}).run(changes)}
        until {calls==1};delay(20)
        repeat(50){changes.emit(Unit)}
        until {calls==2};delay(50)
        job.cancelAndJoin();assertEquals(2,calls)
    }
    @Test fun editDuringUploadSchedulesAnotherRunButNeverOverlaps()=runBlocking {
        val changes=MutableSharedFlow<Unit>(extraBufferCapacity=10)
        val gate=CompletableDeferred<Unit>();var calls=0;var active=0;var maxActive=0
        val job=launch {loop({
            calls++;active++;maxActive=maxOf(maxActive,active)
            if(calls==1)gate.await()
            active--;SyncResult(1,0,0)
        }).run(changes)}
        until {calls==1};changes.emit(Unit);gate.complete(Unit)
        until {calls==2};job.cancelAndJoin();assertEquals(1,maxActive)
    }
    @Test fun temporaryNetworkErrorRetriesAndCanRecover()=runBlocking {
        var calls=0;val statuses=mutableListOf<AutoSyncStatus>()
        val job=launch {loop({if(++calls==1)throw IOException("network");SyncResult(1,0,0)},statuses::add).run(emptyFlow())}
        until {statuses.any {it.phase==AutoSyncPhase.COMPLETE}}
        job.cancelAndJoin();assertEquals(2,calls)
        assertTrue(statuses.any {it.phase==AutoSyncPhase.RETRY && it.detail=="network"})
    }
    @Test fun dirtyEventsDoNotBypassErrorBackoff()=runBlocking {
        var calls=0;val changes=MutableSharedFlow<Unit>(extraBufferCapacity=100)
        val runner=AutoSyncLoop(sync={calls++;throw IOException("network")},report={},settleMs=5,pollMs=1000,retryMs=200,maxRetryMs=1000)
        val job=launch {runner.run(changes)}
        until {calls==1};repeat(20){changes.emit(Unit)};delay(70)
        assertEquals(1,calls);job.cancelAndJoin()
    }
    @Test fun conflictsWaitForReviewWithoutAutomaticallyChoosingAWinner()=runBlocking {
        var calls=0;val statuses=mutableListOf<AutoSyncStatus>()
        val conflict=SyncConflict(SyncKey("animales","a"),emptyMap(),null,"Revisar")
        val job=launch {loop({calls++;SyncResult(0,0,0,listOf(conflict))},statuses::add).run(emptyFlow())}
        until {statuses.any {it.phase==AutoSyncPhase.REVIEW}};delay(50)
        job.cancelAndJoin();assertEquals(1,calls)
    }
    @Test fun unsentChangesCauseFollowupWithoutWaitingForPeriodicPoll()=runBlocking {
        var calls=0;val statuses=mutableListOf<AutoSyncStatus>()
        val job=launch {loop({calls++;SyncResult(1,0,if(calls==1)1 else 0)},statuses::add).run(emptyFlow())}
        until {statuses.any {it.phase==AutoSyncPhase.COMPLETE}}
        job.cancelAndJoin();assertEquals(2,calls);assertTrue(statuses.any {it.phase==AutoSyncPhase.PENDING})
    }
    @Test fun idleLoopPeriodicallyReceivesOtherDeviceChanges()=runBlocking {
        var calls=0
        val job=launch {AutoSyncLoop(sync={calls++;SyncResult(0,1,0)},report={},settleMs=5,pollMs=30).run(emptyFlow())}
        until {calls>=2};job.cancelAndJoin();assertTrue(calls>=2)
    }
    @Test fun cancellationDuringRequestDoesNotReportFailureOrRetry()=runBlocking {
        var calls=0;val statuses=mutableListOf<AutoSyncStatus>()
        val job=launch {loop({calls++;awaitCancellation()},statuses::add).run(emptyFlow())}
        until {calls==1};job.cancelAndJoin();delay(30)
        assertEquals(1,calls);assertFalse(statuses.any {it.phase==AutoSyncPhase.RETRY})
    }
}
