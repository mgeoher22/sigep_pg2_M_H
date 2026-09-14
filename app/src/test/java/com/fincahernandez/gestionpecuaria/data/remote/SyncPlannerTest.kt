package com.fincahernandez.gestionpecuaria.data.remote

import org.junit.Assert.*
import org.junit.Test

class SyncPlannerTest {
    private val key=SyncKey("animales","a")
    private fun data(name:String="Vaca",state:String="SANO",stamp:Long=1)=mapOf("id" to "a","nombre" to name,"estadoSalud" to state,"creadoEn" to 1L,"actualizadoEn" to stamp)
    private fun plan(l:SyncData?,r:SyncVersion?,b:SyncVersion?=null, choices:List<SyncChoice> = emptyList())=SyncPlanner.plan(
        if(l==null)emptyMap() else mapOf(key to l),if(r==null)emptyMap() else mapOf(key to r),if(b==null)emptyMap() else mapOf(key to b),choices)
    @Test fun newLocalRowIsCreatedWithVersionZero() {
        val p=plan(data(),null);assertEquals(0L,p.uploads.single().expected);assertTrue(p.pulls.isEmpty())
    }
    @Test fun emptyDeviceReceivesCloudData() {
        val r=SyncVersion(data(),4);val p=plan(null,r);assertEquals(r,p.pulls[key]);assertTrue(p.uploads.isEmpty())
    }
    @Test fun equalLegacyCopiesEstablishBaselineWithoutUpload() {
        val r=SyncVersion(data(),4);val p=plan(data(),r);assertEquals(r,p.bases[key]);assertTrue(p.uploads.isEmpty());assertTrue(p.conflicts.isEmpty())
    }
    @Test fun unknownDifferentCopiesRequireExplicitChoice() {
        val p=plan(data("Local"),SyncVersion(data("Remoto"),2));assertEquals(1,p.conflicts.size);assertTrue(p.uploads.isEmpty())
    }
    @Test fun establishedLocalEditUsesLatestRemoteVersion() {
        val b=SyncVersion(data(),3);val p=plan(data("Nuevo"),b,b);assertEquals(3L,p.uploads.single().expected)
    }
    @Test fun unchangedLocalReceivesRemoteEdit() {
        val p=plan(data(),SyncVersion(data("Nuevo"),5),SyncVersion(data(),3));assertEquals("Nuevo",p.pulls.getValue(key).data["nombre"])
    }
    @Test fun disjointChangesMergeWithoutLosingEither() {
        val l=data("Local",stamp=4);val p=plan(l,SyncVersion(data(state="REGULAR",stamp=5),5),SyncVersion(data(),3))
        val change=p.uploads.single();assertEquals("Local",change.data["nombre"]);assertEquals("REGULAR",change.data["estadoSalud"])
        assertEquals(l,change.original);assertEquals(5L,change.data["actualizadoEn"])
    }
    @Test fun concurrentSameFieldChangesNeverUseDeviceTimeAsWinner() {
        val p=plan(data("Local",stamp=99999),SyncVersion(data("Remoto",stamp=2),5),SyncVersion(data(),3))
        assertEquals(1,p.conflicts.size);assertTrue(p.uploads.isEmpty());assertTrue(p.pulls.isEmpty())
    }
    @Test fun staleConflictChoiceDoesNotOverwriteNewerRemoteValue() {
        val old=plan(data("Local"),SyncVersion(data("Remoto"),5)).conflicts.single()
        val p=plan(data("Local"),SyncVersion(data("Más reciente"),6),choices=listOf(SyncChoice(old,true)))
        assertEquals(1,p.conflicts.size);assertTrue(p.uploads.isEmpty())
    }
    @Test fun explicitLocalChoicePreservesRemoteCreationTime() {
        val local=data("Local")+("creadoEn" to 50L);val remote=SyncVersion(data("Remoto"),5)
        val c=plan(local,remote).conflicts.single()
        val p=plan(local,remote,choices=listOf(SyncChoice(c,true)))
        assertEquals(1L,p.uploads.single().data["creadoEn"]);assertEquals(5L,p.uploads.single().expected)
    }
    @Test fun explicitCloudChoiceReplacesOnlyReviewedRecord() {
        val local=data("Local");val remote=SyncVersion(data("Remoto"),5);val c=plan(local,remote).conflicts.single()
        assertEquals(remote,plan(local,remote,choices=listOf(SyncChoice(c,false))).pulls[key])
    }
    @Test fun remoteTombstoneOnlyRemovesUnchangedLocalCopy() {
        val b=SyncVersion(data(),3);val r=SyncVersion(data(),4,true)
        assertTrue(plan(data(),r,b).pulls.getValue(key).deleted)
        assertEquals(1,plan(data("Editado"),r,b).conflicts.size)
    }
    @Test fun missingPreviouslyKnownRemoteRowIsNotRecreated() {
        val p=plan(data(),null,SyncVersion(data(),3));assertEquals(1,p.conflicts.size);assertTrue(p.uploads.isEmpty())
    }
    @Test fun missingLocalKnownRecordNeedsReviewInsteadOfSilentDeletion() {
        val b=SyncVersion(data(),3);val p=plan(null,b,b);assertEquals(1,p.conflicts.size)
    }
    @Test fun timestampOnlyDifferenceDoesNotUploadUnnecessaryEdit() {
        assertTrue(plan(data(stamp=8),SyncVersion(data(),3)).uploads.isEmpty())
    }
}
