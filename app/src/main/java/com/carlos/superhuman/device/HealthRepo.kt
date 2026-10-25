package com.carlos.superhuman.device

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

data class HealthSnapshot(
    val stepsToday: Long = 0,
    val distanceTodayKm: Double = 0.0,
    val steps7: List<Long> = emptyList(),
    val sleepLastNightMin: Long = 0,
    val sleep7: List<Long> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

data class Workout(val title: String, val type: Int, val start: Instant, val minutes: Long)

/** Health Connect: lê passos, sono e treinos (o Strava e o Google Fit escrevem aqui). */
object HealthRepo {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun available(ctx: Context): Boolean =
        HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE

    suspend fun granted(ctx: Context): Boolean = runCatching {
        HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions().containsAll(permissions)
    }.getOrDefault(false)

    suspend fun read(ctx: Context): HealthSnapshot? = runCatching {
        val client = HealthConnectClient.getOrCreate(ctx)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startToday = today.atStartOfDay(zone).toInstant()
        val now = Instant.now()

        val agg = client.aggregate(
            AggregateRequest(setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL), TimeRangeFilter.between(startToday, now))
        )
        val steps = agg[StepsRecord.COUNT_TOTAL] ?: 0L
        val dist = agg[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0

        val weekly = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            )
        )
        val steps7 = weekly.map { it.result[StepsRecord.COUNT_TOTAL] ?: 0L }

        val sleeps = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(today.minusDays(7).atStartOfDay(zone).toInstant(), now))
        ).records
        val sleepByNight = (0..6).map { back ->
            val wakeDay = today.minusDays((6 - back).toLong())
            sleeps.filter { it.endTime.atZone(zone).toLocalDate() == wakeDay }
                .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        }

        val ex = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(today.minusDays(14).atStartOfDay(zone).toInstant(), now))
        ).records.sortedByDescending { it.startTime }.take(10).map {
            Workout(it.title ?: "Treino", it.exerciseType, it.startTime, Duration.between(it.startTime, it.endTime).toMinutes())
        }

        HealthSnapshot(steps, dist, steps7, sleepByNight.lastOrNull() ?: 0L, sleepByNight, ex)
    }.getOrNull()

    fun exerciseName(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Corrida"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Caminhada"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Ciclismo"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Natação"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Força"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Caminhada na natureza"
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Futebol"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        else -> "Treino"
    }
}
