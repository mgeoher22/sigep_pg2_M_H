package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.fincahernandez.gestionpecuaria.data.transfer.BulkDataImportRepository
import com.fincahernandez.gestionpecuaria.data.transfer.BulkExportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportPreview
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportMode
import com.fincahernandez.gestionpecuaria.data.transfer.DataTransferModule

/** Expone el intercambio masivo sin entregar la base de datos directamente a la vista. */
class BulkDataImportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BulkDataImportRepository(application)

    suspend fun export(uri: Uri, modules: Set<DataTransferModule>): BulkExportResult =
        repository.export(uri, modules)

    suspend fun inspect(
        uri: Uri,
        modules: Set<DataTransferModule>,
        mode: BulkImportMode
    ): BulkImportPreview = repository.inspect(uri, modules, mode)

    suspend fun import(
        uri: Uri,
        modules: Set<DataTransferModule>,
        mode: BulkImportMode
    ): BulkImportResult = repository.import(uri, modules, mode)
}
