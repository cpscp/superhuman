package com.carlos.superhuman.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

// ---------------- Definições ----------------
@Serializable
data class Settings(
    val userName: String = "Carlos",
    val locationName: String = "Lisboa",
    val latitude: Double = 38.7223,
    val longitude: Double = -9.1393,
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-sonnet-5-5",
    val investFeedUrl: String = "https://raw.githubusercontent.com/cpscp/superhuman-data/main/invest/latest.json",
    val investAutoGenerate: Boolean = true,
    val investProfile: String = "Perfil moderado, horizonte 5-10 anos, investidor em Portugal (corretora europeia, ETF UCITS).",
    val beerWeeklyLimit: Int = 6,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 30,
    val weeklyReviewHour: Int = 18,
    val walletPackages: List<String> = listOf("com.google.android.apps.walletnfcrel"),
    val walletAutoConfirm: Boolean = false,
    val stravaClientId: String = "",
    val stravaClientSecret: String = "",
    val darkMode: Int = 0, // 0 = sistema, 1 = claro, 2 = escuro
    val hiddenCalendarIds: List<Long> = emptyList(),
    val calendarLabels: Map<String, String> = emptyMap(), // calendarId -> etiqueta (ex.: "Trabalho")
    val defaultCalendarId: Long = -1L,
    val onboarded: Boolean = false,
)

// ---------------- Tasks ----------------
@Serializable
enum class Priority { LOW, MEDIUM, HIGH }

@Serializable
data class SubTask(val id: String = newId(), val title: String, val done: Boolean = false)

@Serializable
data class TaskItem(
    val id: String = newId(),
    val title: String,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val priority: Priority = Priority.MEDIUM,
    val dueDay: Long? = null,          // epochDay
    val dueMinutes: Int? = null,       // minutos desde a meia-noite
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val subtasks: List<SubTask> = emptyList(),
)

// ---------------- Finanças ----------------
@Serializable
enum class ExpenseSource { MANUAL, WALLET, SUPERMERCADO, FIXA }

@Serializable
data class Expense(
    val id: String = newId(),
    val time: Long = System.currentTimeMillis(),
    val amount: Double,
    val merchant: String,
    val category: String = "Outros",
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val pending: Boolean = false,
    val note: String = "",
    val dedupKey: String = "",
)

@Serializable
data class FixedExpense(
    val id: String = newId(),
    val name: String,
    val amount: Double,
    val dayOfMonth: Int = 1,
    val category: String = "Casa",
    val active: Boolean = true,
)

// ---------------- Supermercado ----------------
@Serializable
data class SuperTrip(
    val id: String = newId(),
    val epochDay: Long,
    val store: String,
    val amount: Double,
    val expenseId: String? = null,
)

@Serializable
data class ShoppingItem(
    val id: String = newId(),
    val name: String,
    val qty: String = "",
    val aisle: String = "Geral",
    val checked: Boolean = false,
)

// ---------------- Transportes ----------------
@Serializable
data class TransportCard(
    val id: String = newId(),
    val operator: String,          // "CP" ou "Carris"
    val title: String,             // ex.: "Navegante Metropolitano"
    val number: String = "",
    val holder: String = "",
    val validUntil: String = "",
    val colorHex: String = "#1B1A19",
)

@Serializable
data class TransportRoute(
    val id: String = newId(),
    val operator: String,          // "CP" ou "Carris"
    val line: String = "",
    val from: String,
    val to: String,
    val stopId: String = "",       // id de paragem (Carris Metropolitana) para tempo real, opcional
    val departures: List<String> = emptyList(), // horários fixos "08:12"
    val url: String = "",
)

// ---------------- Invest ----------------
@Serializable
data class Holding(
    val id: String = newId(),
    val symbol: String,            // símbolo Yahoo: VUAA.L, SXR8.DE, AAPL
    val name: String = "",
    val quantity: Double,
    val avgPrice: Double,
    val currency: String = "EUR",
    val buyEpochDay: Long = java.time.LocalDate.now().toEpochDay(),
)

@Serializable
data class Quote(
    val symbol: String,
    val price: Double,
    val previousClose: Double,
    val currency: String,
    val history: List<Double> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PortfolioSnapshot(val epochDay: Long, val value: Double, val invested: Double)

@Serializable
data class InvestTip(
    val title: String = "",
    val instrument: String = "",
    val body: String = "",
    val horizon: String = "",
    val risk: String = "",
    val suggestedAmount: String = "",
    val potential: String = "",
)

@Serializable
data class NewsItem(
    val title: String = "",
    val source: String = "",
    val url: String = "",
    val summary: String = "",
    val relevance: String = "",
)

@Serializable
data class HoldingSignal(
    val symbol: String = "",
    val action: String = "MANTER",   // MANTER, REFORÇAR, REDUZIR, VENDER, REVER
    val reason: String = "",
)

@Serializable
data class InvestFeed(
    val date: String = "",
    val generatedBy: String = "",
    val marketSummary: String = "",
    val mood: String = "",           // "risk-on", "neutro", "risk-off"
    val indices: List<IndexMove> = emptyList(),
    val tips: List<InvestTip> = emptyList(),
    val news: List<NewsItem> = emptyList(),
    val signals: List<HoldingSignal> = emptyList(),
    val disclaimer: String = "Informação e análise, não aconselhamento financeiro personalizado. Decide sempre com base na tua situação.",
    val lessonTitle: String = "",
    val lessonBody: String = "",
    val beginnerNote: String = "",
)

@Serializable
data class InvestProfile(
    val set: Boolean = false,
    val oneOff: Double = 0.0,            // valor para investir já
    val monthly: Double = 0.0,           // valor por mês
    val horizonYears: Int = 10,
    val risk: Int = 1,                   // 0 cauteloso, 1 equilibrado, 2 dinâmico
    val hasEmergencyFund: Boolean = false,
    val emergencyTarget: Double = 0.0,
    val emergencySaved: Double = 0.0,
    val monthlyDay: Int = 1,
    val goal: String = "",
    val remindersOn: Boolean = true,
)

@Serializable
data class Contribution(
    val id: String = newId(),
    val epochDay: Long,
    val amount: Double,
    val symbol: String,                  // ou "FUNDO" para o fundo de emergência
)

@Serializable
data class IndexMove(val name: String = "", val change: String = "")

// ---------------- Saúde ----------------
@Serializable
data class Habit(
    val id: String = newId(),
    val name: String,
    val emoji: String = "✨",
    val targetPerWeek: Int = 7,
    val doneDays: List<Long> = emptyList(),
)

@Serializable
data class StravaAuth(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0,
    val athlete: String = "",
)

@Serializable
data class StravaActivity(
    val name: String,
    val type: String,
    val distanceKm: Double,
    val movingMin: Int,
    val startLocal: String,
    val elevation: Double = 0.0,
)

// ---------------- Notas / Casa / Objetivos ----------------
@Serializable
data class Note(
    val id: String = newId(),
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class HomeDoc(
    val id: String = newId(),
    val title: String,
    val category: String = "Geral",
    val uri: String,
    val mime: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val expiryDay: Long? = null,
)

@Serializable
data class HomeAdmin(
    val id: String = newId(),
    val title: String,
    val kind: String = "Conta",      // Conta, Manutenção, Contrato, Seguro
    val amount: Double? = null,
    val dueDay: Long? = null,
    val repeatMonths: Int = 0,
    val notes: String = "",
    val done: Boolean = false,
)

@Serializable
data class Milestone(val id: String = newId(), val title: String, val done: Boolean = false, val dueDay: Long? = null)

@Serializable
data class Goal(
    val id: String = newId(),
    val title: String,
    val area: String = "Pessoal",
    val why: String = "",
    val targetDay: Long? = null,
    val progress: Int = 0,
    val milestones: List<Milestone> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

// ---------------- Revisões ----------------
@Serializable
data class Review(
    val key: String,                 // "D-<epochDay>" ou "W-<ano>-<semana>"
    val weekly: Boolean = false,
    val mood: Int = 3,
    val wins: String = "",
    val improve: String = "",
    val next: String = "",
    val aiSummary: String = "",
    val savedAt: Long = System.currentTimeMillis(),
)

// ---------------- Assistente ----------------
@Serializable
data class ChatMsg(val role: String, val text: String, val time: Long = System.currentTimeMillis())

// ---------------- Cache para widgets ----------------
@Serializable
data class CachedEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val calendarLabel: String,
    val color: Int,
)

@Serializable
data class WeatherNow(
    val temp: Double = 0.0,
    val feels: Double = 0.0,
    val code: Int = 0,
    val wind: Double = 0.0,
    val max: Double = 0.0,
    val min: Double = 0.0,
    val rainProb: Int = 0,
    val uv: Double = 0.0,
    val hourly: List<Double> = emptyList(),
    val updatedAt: Long = 0,
)

@Serializable
data class AppData(
    val settings: Settings = Settings(),
    val tasks: List<TaskItem> = emptyList(),
    val tags: List<String> = listOf("Trabalho", "Pessoal", "Casa", "Affinity", "Saúde"),
    val budgets: Map<String, Double> = emptyMap(),          // "2026-10" -> plafond
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val categories: List<String> = listOf("Supermercado", "Restauração", "Transportes", "Casa", "Lazer", "Saúde", "Compras", "Subscrições", "Outros"),
    val superTrips: List<SuperTrip> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val stores: List<String> = listOf("Continente", "Pingo Doce", "Lidl", "Auchan", "Mercadona", "Aldi", "Minipreço"),
    val transportCards: List<TransportCard> = TransportDefaults.cards,
    val transportRoutes: List<TransportRoute> = TransportDefaults.routes,
    val holdings: List<Holding> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val snapshots: List<PortfolioSnapshot> = emptyList(),
    val investFeed: InvestFeed? = null,
    val investProfile: InvestProfile = InvestProfile(),
    val contributions: List<Contribution> = emptyList(),
    val lessonsDone: List<String> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val beers: Map<String, Int> = emptyMap(),               // epochDay (string) -> nº
    val strava: StravaAuth = StravaAuth(),
    val stravaActivities: List<StravaActivity> = emptyList(),
    val notes: List<Note> = emptyList(),
    val homeDocs: List<HomeDoc> = emptyList(),
    val homeAdmin: List<HomeAdmin> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val chat: List<ChatMsg> = emptyList(),
    val cachedEvents: List<CachedEvent> = emptyList(),
    val weather: WeatherNow = WeatherNow(),
)
