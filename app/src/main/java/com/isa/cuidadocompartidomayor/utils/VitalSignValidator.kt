package com.isa.cuidadocompartidomayor.utils

/*utilitaria para validar signos vitales*/
object VitalSignValidator {
    //rangos para cada signo vital
    private const val MIN_HEART_RATE = 30
    private const val MAX_HEART_RATE = 250

    private const val MIN_GLUCOSE = 20.0
    private const val MAX_GLUCOSE = 600.0

    private const val MIN_TEMPERATURE = 30.0
    private const val MAX_TEMPERATURE = 43.0

    private const val MIN_SYSTOLIC_BP = 30
    private const val MAX_SYSTOLIC_BP = 250

    private const val MIN_DIASTOLIC_BP = 30
    private const val MAX_DIASTOLIC_BP = 150

    private const val MIN_OXYGEN_SATURATION = 50
    private const val MAX_OXYGEN_SATURATION = 100

    private const val MIN_WEIGHT = 20.0
    private const val MAX_WEIGHT = 300.0

    /**
     * Valida la frecuencia cardíaca
     */
    fun validateHeartRate(heartRate: Int?): ValidationResult {
        if (heartRate == null) return ValidationResult.Valid

        return when {
            heartRate < MIN_HEART_RATE -> ValidationResult.Invalid("La frecuencia cardíaca no puede ser menor a $MIN_HEART_RATE LPM")
            heartRate > MAX_HEART_RATE -> ValidationResult.Invalid("La frecuencia cardíaca no puede ser mayor a $MAX_HEART_RATE LPM")
            else -> ValidationResult.Valid
        }
    }
    /**
     * Valida el nivel de glucosa
     */
    fun validateGlucose(glucose: Double?): ValidationResult {
        if (glucose == null) return ValidationResult.Valid

        return when {
            glucose < MIN_GLUCOSE -> ValidationResult.Invalid("La glucosa no puede ser menor a $MIN_GLUCOSE mg/dL")
            glucose > MAX_GLUCOSE -> ValidationResult.Invalid("La glucosa no puede ser mayor a $MAX_GLUCOSE mg/dL")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida la temperatura
     */
    fun validateTemperature(temperature: Double?): ValidationResult {
        if (temperature == null) return ValidationResult.Valid

        return when {
            temperature < MIN_TEMPERATURE -> ValidationResult.Invalid("La temperatura no puede ser menor a $MIN_TEMPERATURE °C")
            temperature > MAX_TEMPERATURE -> ValidationResult.Invalid("La temperatura no puede ser mayor a $MAX_TEMPERATURE °C")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida la presión arterial
     */
    fun validateBloodPressure(systolic: Int?, diastolic: Int?): ValidationResult {
        // Si ambos son nulos, es válido
        if (systolic == null && diastolic == null) return ValidationResult.Valid

        // Si uno está lleno y el otro vacío, es inválido
        if (systolic == null || diastolic == null) {
            return ValidationResult.Invalid("Debe ingresar tanto la presión sistólica como la diastólica")
        }

        return when {
            systolic < MIN_SYSTOLIC_BP -> ValidationResult.Invalid("La presión sistólica no puede ser menor a $MIN_SYSTOLIC_BP mmHg")
            systolic > MAX_SYSTOLIC_BP -> ValidationResult.Invalid("La presión sistólica no puede ser mayor a $MAX_SYSTOLIC_BP mmHg")
            diastolic < MIN_DIASTOLIC_BP -> ValidationResult.Invalid("La presión diastólica no puede ser menor a $MIN_DIASTOLIC_BP mmHg")
            diastolic > MAX_DIASTOLIC_BP -> ValidationResult.Invalid("La presión diastólica no puede ser mayor a $MAX_DIASTOLIC_BP mmHg")
            systolic <= diastolic -> ValidationResult.Invalid("La presión sistólica debe ser mayor que la diastólica")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida la saturación de oxígeno
     */
    fun validateOxygenSaturation(saturation: Int?): ValidationResult {
        if (saturation == null) return ValidationResult.Valid

        return when {
            saturation < MIN_OXYGEN_SATURATION -> ValidationResult.Invalid("La saturación no puede ser menor a $MIN_OXYGEN_SATURATION%")
            saturation > MAX_OXYGEN_SATURATION -> ValidationResult.Invalid("La saturación no puede ser mayor a $MAX_OXYGEN_SATURATION%")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida el peso
     */
    fun validateWeight(weight: Double?): ValidationResult {
        if (weight == null) return ValidationResult.Valid

        return when {
            weight < MIN_WEIGHT -> ValidationResult.Invalid("El peso no puede ser menor a $MIN_WEIGHT Kg")
            weight > MAX_WEIGHT -> ValidationResult.Invalid("El peso no puede ser mayor a $MAX_WEIGHT Kg")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida que al menos un signo vital esté presente
     */
    fun validateAtLeastOneSign(
        heartRate: Int?,
        glucose: Double?,
        temperature: Double?,
        systolicBP: Int?,
        diastolicBP: Int?,
        oxygenSaturation: Int?,
        weight: Double?
    ): ValidationResult {
        val hasAnySign = heartRate != null || glucose != null || temperature != null ||
                (systolicBP != null && diastolicBP != null) || oxygenSaturation != null || weight != null

        return if (hasAnySign) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Debe registrar al menos un signo vital")
        }
    }

    /** Clase sellada para resultados de validación */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()

        fun isValid(): Boolean = this is Valid
        fun getErrorMessage(): String? = if (this is Invalid) message else null
    }


}