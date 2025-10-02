package dev.solora.dashboard

import dev.solora.data.FirebaseQuote

/**
 * Dashboard statistics and analytics data
 */
data class DashboardData(
    val totalQuotes: Int = 0,
    val averageSystemSize: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val averageMonthlySavings: Double = 0.0,
    val systemSizeDistribution: SystemSizeDistribution = SystemSizeDistribution(),
    val monthlyPerformance: MonthlyPerformance = MonthlyPerformance(),
    val topLocations: List<LocationStats> = emptyList()
)

data class SystemSizeDistribution(
    val size0to3kw: Int = 0,
    val size3to6kw: Int = 0,
    val size6to10kw: Int = 0,
    val size10kwPlus: Int = 0
)

data class MonthlyPerformance(
    val quotesThisMonth: Int = 0,
    val quotesLastMonth: Int = 0,
    val growthPercentage: Double = 0.0
)

data class LocationStats(
    val location: String,
    val count: Int,
    val averageSystemSize: Double
)

/**
 * Calculate dashboard data from quotes
 */
fun calculateDashboardData(quotes: List<FirebaseQuote>): DashboardData {
    if (quotes.isEmpty()) {
        return DashboardData()
    }

    // Basic statistics
    val totalQuotes = quotes.size
    val averageSystemSize = quotes.mapNotNull { it.systemKwp }.average()
    val totalRevenue = quotes.sumOf { it.systemKwp * 15000.0 } // Estimate R15k per kW
    val averageMonthlySavings = quotes.mapNotNull { it.monthlySavings }.average()

    // System size distribution
    val systemSizeDistribution = SystemSizeDistribution(
        size0to3kw = quotes.count { (it.systemKwp ?: 0.0) in 0.0..3.0 },
        size3to6kw = quotes.count { (it.systemKwp ?: 0.0) in 3.1..6.0 },
        size6to10kw = quotes.count { (it.systemKwp ?: 0.0) in 6.1..10.0 },
        size10kwPlus = quotes.count { (it.systemKwp ?: 0.0) > 10.0 }
    )

    // Monthly performance (simplified - using creation dates)
    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
    val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear

    val quotesThisMonth = quotes.count { quote ->
        quote.createdAt?.toDate()?.let { date ->
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.get(java.util.Calendar.MONTH) == currentMonth && 
            cal.get(java.util.Calendar.YEAR) == currentYear
        } ?: false
    }

    val quotesLastMonth = quotes.count { quote ->
        quote.createdAt?.toDate()?.let { date ->
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.get(java.util.Calendar.MONTH) == lastMonth && 
            cal.get(java.util.Calendar.YEAR) == lastMonthYear
        } ?: false
    }

    val growthPercentage = if (quotesLastMonth > 0) {
        ((quotesThisMonth - quotesLastMonth).toDouble() / quotesLastMonth) * 100
    } else {
        if (quotesThisMonth > 0) 100.0 else 0.0
    }

    val monthlyPerformance = MonthlyPerformance(
        quotesThisMonth = quotesThisMonth,
        quotesLastMonth = quotesLastMonth,
        growthPercentage = growthPercentage
    )

    // Top locations
    val locationGroups = quotes.groupBy { it.address }
    val topLocations = locationGroups.map { (location, locationQuotes) ->
        LocationStats(
            location = location,
            count = locationQuotes.size,
            averageSystemSize = locationQuotes.mapNotNull { it.systemKwp }.average()
        )
    }.sortedByDescending { it.count }.take(5)

    return DashboardData(
        totalQuotes = totalQuotes,
        averageSystemSize = averageSystemSize,
        totalRevenue = totalRevenue,
        averageMonthlySavings = averageMonthlySavings,
        systemSizeDistribution = systemSizeDistribution,
        monthlyPerformance = monthlyPerformance,
        topLocations = topLocations
    )
}
