package com.isa.cuidadocompartidomayor.data.repository
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DiaryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object{
        private const val VITAL_SIGNS_COLLECTION = "vital_signs"
        private const val MOOD_ENTRIES_COLLECTION = "mood_entries"
        private const val USERS_COLLECTION = "users"
    }

    /** Crea un nuevo registro de signos vitales */
    suspend fun createVitalSign(
        elderlyId: String,
        heartRate: Int?,
        glucose: Double?,
        glucoseMoment: GlucoseMoment?,
        temperature: Double?,
        systolicBP: Int?,
        diastolicBP: Int?,
        oxygenSaturation: Int?,
        weight: Double?,
        notes: String
    ): Result<VitalSign> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del cuidador
            val caregiverDoc = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val caregiverName = caregiverDoc.getString("name") ?: "Cuidador"

            // Obtener información del adulto mayor
            val elderlyDoc = db.collection(USERS_COLLECTION)
                .document(elderlyId)
                .get()
                .await()

            val elderlyName = elderlyDoc.getString("name") ?: "Adulto Mayor"

            // Calcular estados automáticamente
            val heartRateStatus = heartRate?.let { HeartRateStatus.fromHeartRate(it).name }
            val temperatureStatus = temperature?.let { TemperatureStatus.fromTemperature(it).name }
            val bpStatus = if (systolicBP != null && diastolicBP != null) {
                BloodPressureStatus.fromBloodPressure(systolicBP, diastolicBP).name
            } else null
            val oxygenStatus = oxygenSaturation?.let { OxygenStatus.fromOxygenSaturation(it).name }

            // Crear el objeto VitalSign
            val vitalSignId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val vitalSign = VitalSign(
                id = vitalSignId,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                caregiverId = currentUser.uid,
                caregiverName = caregiverName,
                heartRate = heartRate,
                glucose = glucose,
                glucoseMoment = glucoseMoment?.name,
                temperature = temperature,
                systolicBP = systolicBP,
                diastolicBP = diastolicBP,
                oxygenSaturation = oxygenSaturation,
                weight = weight,
                heartRateStatus = heartRateStatus,
                temperatureStatus = temperatureStatus,
                bpStatus = bpStatus,
                oxygenStatus = oxygenStatus,
                notes = notes,
                timestamp = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            // Guardar en Firestore
            db.collection(VITAL_SIGNS_COLLECTION)
                .document(vitalSignId)
                .set(vitalSign.toMap())
                .await()

            // ✅ LOG para debugging
            Log.d("DiaryRepository", """
            ✅ VitalSign guardado:
            - ID: $vitalSignId
            - Cuidador: $caregiverName (${currentUser.uid})
            - Adulto Mayor: $elderlyName ($elderlyId)
            - Timestamp: $timestamp
        """.trimIndent())

            Result.success(vitalSign)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actualizar un registro de signos vitales */
    suspend fun updateVitalSign(
        vitalSignId: String,
        heartRate: Int?,
        glucose: Double?,
        glucoseMoment: GlucoseMoment?,
        temperature: Double?,
        systolicBP: Int?,
        diastolicBP: Int?,
        oxygenSaturation: Int?,
        weight: Double?,
        notes: String
    ): Result<VitalSign>{
        return try {
            //Obtener el registro ecistente
            val existingDoc = db.collection(VITAL_SIGNS_COLLECTION)
                .document(vitalSignId)
                .get()
                .await()
            if (!existingDoc.exists()){
                return Result.failure(Exception("Registro no encontrado"))
            }

            val existing = existingDoc.toObject(VitalSign::class.java)
                ?: return Result.failure(Exception("Error al leer el registro "))

            // Obtener información del usuario actual (editor)
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val userDoc = db.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val editorName = userDoc.getString("name") ?: "Usuario"

            // Calcular nuevos estados
            val heartRateStatus = heartRate?.let { HeartRateStatus.fromHeartRate(it).name }
            val temperatureStatus = temperature?.let { TemperatureStatus.fromTemperature(it).name }
            val bpStatus = if (systolicBP != null && diastolicBP != null) {
                BloodPressureStatus.fromBloodPressure(systolicBP, diastolicBP).name
            } else null
            val oxygenStatus = oxygenSaturation?.let { OxygenStatus.fromOxygenSaturation(it).name }
            // Crear objeto actualizado
            val updatedVitalSign = existing.copy(
                heartRate = heartRate,
                glucose = glucose,
                glucoseMoment = glucoseMoment?.name,
                temperature = temperature,
                systolicBP = systolicBP,
                diastolicBP = diastolicBP,
                oxygenSaturation = oxygenSaturation,
                weight = weight,
                heartRateStatus = heartRateStatus,
                temperatureStatus = temperatureStatus,
                bpStatus = bpStatus,
                oxygenStatus = oxygenStatus,
                notes = notes,
                updatedAt = System.currentTimeMillis(),
                editedBy = currentUser.uid,
                editedByName = editorName,
                editedAt = System.currentTimeMillis()

            )

            // Actualizar en Firestore
            db.collection(VITAL_SIGNS_COLLECTION)
                .document(vitalSignId)
                .set(updatedVitalSign.toMap())
                .await()

            Result.success(updatedVitalSign)

        }catch (e: Exception){
            Result.failure(e)
        }

    }

    /** Obtiene los signos vitales de un adulto mayor (últimos N registros) */
    suspend fun getVitalSigns(elderlyId: String, limit: Int = 50): Result<List<VitalSign>> {
        return try {
            val snapshot = db.collection(VITAL_SIGNS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val vitalSigns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VitalSign::class.java)
            }

            Result.success(vitalSigns)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Obtiene los signos vitales de un adulto mayor en un rango de fechas */
    suspend fun getVitalSignsByDateRange( elderlyId: String, startDate: Long, endDate: Long ): Result<List<VitalSign>> {
        return try {
            val snapshot = db.collection(VITAL_SIGNS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val vitalSigns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VitalSign::class.java)
            }

            Result.success(vitalSigns)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Obtiene un signo vital específico por ID */
    suspend fun getVitalSignById(vitalSignId: String): Result<VitalSign> {
        return try {
            val doc = db.collection(VITAL_SIGNS_COLLECTION)
                .document(vitalSignId)
                .get()
                .await()

            val vitalSign = doc.toObject(VitalSign::class.java)
                ?: return Result.failure(Exception("Registro no encontrado"))

            Result.success(vitalSign)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Elimina un registro de signos vitales */
    suspend fun deleteVitalSign(vitalSignId: String): Result<Unit> {
        return try {
            db.collection(VITAL_SIGNS_COLLECTION)
                .document(vitalSignId)
                .delete()
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ESTADOS DE ÁNIMO ====================

    /** Crea un nuevo registro de estado de ánimo */
    suspend fun createMoodEntry(
        elderlyId: String,
        emotions: List<MoodEmotion>,
        symptoms: List<MoodSymptom>,
        energyLevel: EnergyLevel?,
        appetiteLevel: AppetiteLevel?,
        functionalCapacity: FunctionalCapacity?,
        notes: String
    ): Result<MoodEntry> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del cuidador
            val caregiverDoc = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val caregiverName = caregiverDoc.getString("name") ?: "Cuidador"

            // Obtener información del adulto mayor
            val elderlyDoc = db.collection(USERS_COLLECTION)
                .document(elderlyId)
                .get()
                .await()

            val elderlyName = elderlyDoc.getString("name") ?: "Adulto Mayor"

            // Crear el objeto MoodEntry
            val moodEntryId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val moodEntry = MoodEntry(
                id = moodEntryId,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                caregiverId = currentUser.uid,
                caregiverName = caregiverName,
                emotions = emotions.map { it.name },
                symptoms = symptoms.map { it.name },
                energyLevel = energyLevel?.name,
                appetiteLevel = appetiteLevel?.name,
                functionalCapacity = functionalCapacity?.name,
                notes = notes,
                timestamp = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            // Guardar en Firestore
            db.collection(MOOD_ENTRIES_COLLECTION)
                .document(moodEntryId)
                .set(moodEntry.toMap())
                .await()

            Result.success(moodEntry)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Actualiza un registro de estado de ánimo existente */
    suspend fun updateMoodEntry(
        moodEntryId: String,
        emotions: List<MoodEmotion>,
        symptoms: List<MoodSymptom>,
        energyLevel: EnergyLevel?,
        appetiteLevel: AppetiteLevel?,
        functionalCapacity: FunctionalCapacity?,
        notes: String
    ): Result<MoodEntry> {
        return try {
            // Obtener el registro existente
            val existingDoc = db.collection(MOOD_ENTRIES_COLLECTION)
                .document(moodEntryId)
                .get()
                .await()

            if (!existingDoc.exists()) {
                return Result.failure(Exception("Registro no encontrado"))
            }

            val existing = existingDoc.toObject(MoodEntry::class.java)
                ?: return Result.failure(Exception("Error al leer el registro"))

            // Obtener información del usuario actual (editor)
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val userDoc = db.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val editorName = userDoc.getString("name") ?: "Usuario"

            // Crear objeto actualizado
            val updatedMoodEntry = existing.copy(
                emotions = emotions.map { it.name },
                symptoms = symptoms.map { it.name },
                energyLevel = energyLevel?.name,
                appetiteLevel = appetiteLevel?.name,
                functionalCapacity = functionalCapacity?.name,
                notes = notes,
                updatedAt = System.currentTimeMillis(),
                editedBy = currentUser.uid,
                editedByName = editorName,
                editedAt = System.currentTimeMillis()
            )

            // Actualizar en Firestore
            db.collection(MOOD_ENTRIES_COLLECTION)
                .document(moodEntryId)
                .set(updatedMoodEntry.toMap())
                .await()

            Log.d("DiaryRepository", "✅ Estado de ánimo actualizado por: $editorName")
            Result.success(updatedMoodEntry)

        } catch (e: Exception) {
            Log.e("DiaryRepository", "❌ Error actualizando mood entry: ${e.message}")
            Result.failure(e)
        }
    }


    /** Obtiene los estados de ánimo de un adulto mayor (últimos N registros) */
    suspend fun getMoodEntries(elderlyId: String, limit: Int = 50): Result<List<MoodEntry>> {
        return try {
            val snapshot = db.collection(MOOD_ENTRIES_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val moodEntries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }

            Result.success(moodEntries)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Obtiene los estados de ánimo de un adulto mayor en un rango de fechas */
    suspend fun getMoodEntriesByDateRange(
        elderlyId: String,
        startDate: Long,
        endDate: Long
    ): Result<List<MoodEntry>> {
        return try {
            val snapshot = db.collection(MOOD_ENTRIES_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val moodEntries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }

            Result.success(moodEntries)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Obtiene un estado de ánimo específico por ID */
    suspend fun getMoodEntryById(moodEntryId: String): Result<MoodEntry> {
        return try {
            val doc = db.collection(MOOD_ENTRIES_COLLECTION)
                .document(moodEntryId)
                .get()
                .await()

            val moodEntry = doc.toObject(MoodEntry::class.java)
                ?: return Result.failure(Exception("Registro no encontrado"))

            Result.success(moodEntry)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Elimina un registro de estado de ánimo */
    suspend fun deleteMoodEntry(moodEntryId: String): Result<Unit> {
        return try {
            db.collection(MOOD_ENTRIES_COLLECTION)
                .document(moodEntryId)
                .delete()
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //========================================//
    //=======Cargar info por adulto=======//

    /**
     * Obtiene todos los signos vitales de todos los adultos mayores
     * del cuidador actual
     */
    suspend fun getAllVitalSignsForCaregiver(limit: Int = 50): Result<List<VitalSign>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = db.collection(VITAL_SIGNS_COLLECTION)
                .whereEqualTo("caregiverId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val vitalSigns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VitalSign::class.java)
            }

            Result.success(vitalSigns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los estados de ánimo de todos los adultos mayores
     * del cuidador actual
     */
    suspend fun getAllMoodEntriesForCaregiver(limit: Int = 50): Result<List<MoodEntry>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = db.collection(MOOD_ENTRIES_COLLECTION)
                .whereEqualTo("caregiverId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val moodEntries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }

            Result.success(moodEntries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //========================================//
    //===Para mostrar datos en dashboard===//
    /**
     * Obtiene el último registro de signos vitales de un adulto mayor
     */
    suspend fun getLatestVitalSign(elderlyId: String): Result<VitalSign?> {
        return try {
            val snapshot = db.collection(VITAL_SIGNS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val vitalSign = snapshot.documents.firstOrNull()?.toObject(VitalSign::class.java)
            Result.success(vitalSign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el último registro de estado de ánimo de un adulto mayor
     */
    suspend fun getLatestMoodEntry(elderlyId: String): Result<MoodEntry?> {
        return try {
            val snapshot = db.collection(MOOD_ENTRIES_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val moodEntry = snapshot.documents.firstOrNull()?.toObject(MoodEntry::class.java)
            Result.success(moodEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

      //========================================//
     //===       Mostrar estadisticas       ===//
    //========================================//
    /**
     * Calcula estadísticas de signos vitales para un período específico
     */
    suspend fun getVitalSignsStatistics(
        elderlyId: String,
        startDate: Long,
        endDate: Long
    ): Result<VitalSignsStatistics> {
        return try {
            val vitalSigns = getVitalSignsByDateRange(elderlyId, startDate, endDate)
                .getOrNull() ?: emptyList()

            val stats = VitalSignsStatistics(
                averageHeartRate = vitalSigns.mapNotNull { it.heartRate }.average().takeIf { !it.isNaN() },
                averageGlucose = vitalSigns.mapNotNull { it.glucose }.average().takeIf { !it.isNaN() },
                averageTemperature = vitalSigns.mapNotNull { it.temperature }.average().takeIf { !it.isNaN() },
                averageSystolicBP = vitalSigns.mapNotNull { it.systolicBP }.average().takeIf { !it.isNaN() }?.toInt(),
                averageDiastolicBP = vitalSigns.mapNotNull { it.diastolicBP }.average().takeIf { !it.isNaN() }?.toInt(),
                averageOxygenSaturation = vitalSigns.mapNotNull { it.oxygenSaturation }.average().takeIf { !it.isNaN() }?.toInt(),
                averageWeight = vitalSigns.mapNotNull { it.weight }.average().takeIf { !it.isNaN() },
                totalRecords = vitalSigns.size
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene TODOS los signos vitales de un adulto mayor (sin límite)
     * Útil para gráficas y estadísticas
     */
    suspend fun getVitalSignsByElderlyId(elderlyId: String): Result<List<VitalSign>> {
        return try {
            val snapshot = db.collection(VITAL_SIGNS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.ASCENDING) // Orden ascendente para gráficas
                .get()
                .await()

            val vitalSigns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VitalSign::class.java)
            }

            Log.d("DiaryRepository", "✅ ${vitalSigns.size} signos vitales obtenidos")
            Result.success(vitalSigns)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "❌ Error obteniendo signos vitales: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Obtiene TODOS los estados de ánimo de un adulto mayor (sin límite)
     * Útil para gráficas y estadísticas
     */
    suspend fun getMoodEntriesByElderlyId(elderlyId: String): Result<List<MoodEntry>> {
        return try {
            val snapshot = db.collection(MOOD_ENTRIES_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.ASCENDING) // Orden ascendente para gráficas
                .get()
                .await()

            val moodEntries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }

            Log.d("DiaryRepository", "✅ ${moodEntries.size} estados de ánimo obtenidos")
            Result.success(moodEntries)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "❌ Error obteniendo estados de ánimo: ${e.message}")
            Result.failure(e)
        }
    }

    // Data class para las estadísticas
    data class VitalSignsStatistics(
        val averageHeartRate: Double?,
        val averageGlucose: Double?,
        val averageTemperature: Double?,
        val averageSystolicBP: Int?,
        val averageDiastolicBP: Int?,
        val averageOxygenSaturation: Int?,
        val averageWeight: Double?,
        val totalRecords: Int
    )
}