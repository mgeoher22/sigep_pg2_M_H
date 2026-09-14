package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_sync_baseline", primaryKeys = ["project", "tableName", "recordId"])
data class CloudSyncBaselineEntity(val project: String, val tableName: String, val recordId: String,
    val payload: String, val version: Long, val deleted: Boolean)

@Entity(tableName = "cloud_sync_pending")
data class CloudSyncPendingEntity(@PrimaryKey val project: String, val operationId: String,
    val owner: String, val payload: String)
