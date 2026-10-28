package com.aistudio.trueidle

import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.StableNumberText
import com.aistudio.trueidle.core.TierConfig
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Benchmark harness for the tier engine, the number formatter and the dirty-state gate.
 *
 * Run with:
 * ```
 * ./gradlew :app:testDebugUnitTest --tests "*EngineBenchmark*" -Dtrueidle.benchmark=true
 * ```
 * Without `-Dtrueidle.benchmark=true` the test is skipped, so ordinary CI runs do not depend on the
 * speed of the machine running them.
 *
 * Methodology (see the class docs of [Stat] for the reporting rules):
 * - warmup batches first, then [BATCHES] measured batches; median and p95 are per-operation times
 *   derived from each batch's total, which keeps timer resolution irrelevant for sub-microsecond
 *   operations,
 * - every batch returns a checksum that is consumed, so nothing can be optimised away,
 * - setup (building inputs, loading the tier table) is outside the timed section,
 * - cache state is reset before cache-sensitive cases and reported explicitly,
 * - hit and miss paths are measured separately.
 *
 * This runs on the JVM (unit-test runtime). It is **not** an ART/device measurement: numbers here
 * show relative improvement and absolute JVM cost, and the report says so.
 */
class EngineBenchmark {

    @Test
    fun runBenchmarksWhenEnabled() {
        assumeTrue(
            "benchmarks are skipped unless -Dtrueidle.benchmark=true is passed",
            System.getProperty("trueidle.benchmark") == "true"
        )
        EngineBenchmarkReport.run()
    }
}

/** Statistics for one benchmark case. */
data class Stat(
    val name: String,
    val medianNs: Double,
    val p95Ns: Double,
    val minNs: Double,
    val maxNs: Double,
    val allocBytesPerOp: Double,
    val opsPerBatch: Int,
    val note: String = ""
)

object EngineBenchmarkReport {

    private const val WARMUP_BATCHES = 8
    private const val BATCHES = 21

    private val tierIds = arrayOf(
        "F-", "D", "A+", "S", "SSR", "UR", "EX+", "Transcendent EX", "Cosmic++", "Omega EX",
        "Zenith", "Apotheosis+++", "Infinite EX", "not-a-tier", ""
    )

    private val representativeLongs: LongArray = buildRepresentativeLongs()
    private val representativeDoubles: DoubleArray = buildRepresentativeDoubles()

    @JvmStatic
    fun run() {
        val results = ArrayList<Stat>()

        val tierInitNs = measureClassInit("com.aistudio.trueidle.core.TierConfig")
        val baselineInitNs = measureClassInit("com.aistudio.trueidle.core.TierInfo")
        results += Stat(
            name = "TierConfig: class load + <clinit> (isolated loader)",
            medianNs = tierInitNs,
            p95Ns = tierInitNs,
            minNs = tierInitNs,
            maxNs = tierInitNs,
            allocBytesPerOp = -1.0,
            opsPerBatch = 1,
            note = "JVM only; ART resolves classes from DEX and its cost is not measured here"
        )
        results += Stat(
            name = "TierConfig: baseline class load (empty <clinit>)",
            medianNs = baselineInitNs,
            p95Ns = baselineInitNs,
            minNs = baselineInitNs,
            maxNs = baselineInitNs,
            allocBytesPerOp = -1.0,
            opsPerBatch = 1,
            note = "same loader path, no table to build: the difference is the table build (rough)"
        )
        results += Stat(
            name = "TierConfig: table build (difference, rough)",
            medianNs = (tierInitNs - baselineInitNs).coerceAtLeast(0.0),
            p95Ns = (tierInitNs - baselineInitNs).coerceAtLeast(0.0),
            minNs = (tierInitNs - baselineInitNs).coerceAtLeast(0.0),
            maxNs = (tierInitNs - baselineInitNs).coerceAtLeast(0.0),
            allocBytesPerOp = -1.0,
            opsPerBatch = 1,
            note = "109 hex parses + 109-entry rank map + 2 IntArrays"
        )

        // ---------------------------------------------------------------- tier engine
        results += bench("Tier lookup, known id", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) sink += TierConfig.rankOf(tierIds[i and 13])
            sink.toLong()
        }
        results += bench("Tier lookup, unknown id (fallback)", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) sink += TierConfig.rankOf("definitely-not-a-tier")
            sink.toLong()
        }
        results += bench("Rank comparison", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) sink += TierConfig.compareRanks(tierIds[i and 7], tierIds[(i + 1) and 7])
            sink.toLong()
        }
        results += bench("Colour access", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) sink += TierConfig.colorOf(tierIds[i and 13])
            sink.toLong()
        }
        results += bench("100,000 mixed tier lookups", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) {
                sink += TierConfig.rankOf(tierIds[i % tierIds.size]) +
                    TierConfig.colorOf(tierIds[(i * 7) % tierIds.size]) +
                    TierConfig.compareRanks(tierIds[i % tierIds.size], tierIds[(i + 3) % tierIds.size])
            }
            sink.toLong()
        }

        // ------------------------------------------------------------------ formatter
        NumberFormatter.clearCache()
        results += bench("Format integer < 1000 (gold)", 100_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) sink += NumberFormatter.short(representativeLongs[i % 64]).length
            sink
        }
        results += bench("Format fraction < 1000 (HP)", 100_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) sink += NumberFormatter.short(representativeDoubles[i % 64]).length
            sink
        }
        results += benchCacheMiss("Format suffix path, cold cache", representativeDoubles)
        results += benchCacheHit("Format suffix path, warm cache (hit)", 1234.5678)
        results += benchCacheMiss("Format very large value (scientific)", doubleArrayOf(1.23e100, 4.5e250, 1e300))
        results += bench("Format near every notation threshold", 62_500) { ops ->
            var sink = 0L
            for (i in 0 until ops) sink += NumberFormatter.short(THRESHOLD_VALUES[i % THRESHOLD_VALUES.size]).length
            sink
        }
        results += bench("Alphabetic suffix conversion (1..10 characters)", 100_000) { ops ->
            var sink = 0
            for (i in 0 until ops) {
                sink += NumberFormatter.suffixLabel(SUFFIX_INDICES[i % SUFFIX_INDICES.size])!!.length
            }
            sink.toLong()
        }
        results += bench("Alphabetic format (1e3..1e15)", 50_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) {
                val value = 1_000.0 * Math.pow(10.0, (i % 13).toDouble())
                sink += NumberFormatter.alphabeticShort(value).length
            }
            sink
        }
        results += bench("Format 10,000 representative values", 10_000) { _ ->
            var sink = 0L
            for (value in representativeDoubles) sink += NumberFormatter.short(value).length
            sink
        }

        // ------------------------------------------------------------- baseline (pre-change)
        // The same workloads through the frozen copy of the previous implementation, in the same
        // process and with the same inputs, so the comparison is like-for-like.
        results += bench("BASELINE: legacy format integer < 1000", 100_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) sink += LegacyNumberFormatterOracle.short(representativeLongs[i % 64]).length
            sink
        }
        results += bench("BASELINE: legacy format suffix path (cold)", 10_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) {
                sink += LegacyNumberFormatterOracle.short(representativeDoubles[i % representativeDoubles.size]).length
            }
            sink
        }
        results += bench("BASELINE: legacy format 10,000 representative", 10_000) { _ ->
            var sink = 0L
            for (value in representativeDoubles) sink += LegacyNumberFormatterOracle.short(value).length
            sink
        }
        results += bench("BASELINE: legacy screen update (100 fields)", 100) { _ ->
            var sink = 0L
            for (i in 0 until 100) {
                sink += LegacyNumberFormatterOracle.short(representativeDoubles[i % representativeDoubles.size]).length
            }
            sink
        }

        // ------------------------------------------------------------ UI hot path
        NumberFormatter.clearCache()
        results += bench("Representative screen update (100 fields)", 100) { _ ->
            var sink = 0L
            for (i in 0 until 100) {
                val id = tierIds[i % tierIds.size]
                sink += TierConfig.colorOf(id)
                sink += TierConfig.rankOf(id)
                sink += NumberFormatter.short(representativeDoubles[i % representativeDoubles.size]).length
            }
            sink
        }

        NumberFormatter.clearCache()
        val holder = StableNumberText()
        val firstText = holder.text(12_345.0)
        val unchanged = measurePerOpStat("Unchanged UI value through StableNumberText", 200_000) { ops ->
            var sink = 0L
            for (i in 0 until ops) sink += holder.text(12_345.0).length
            sink
        }
        results += unchanged.copy(
            note = "first='$firstText', formatter calls=${holder.formatCalls} for " +
                "${200_000 * (BATCHES + WARMUP_BATCHES) + 1} reads (1 expected)"
        )

        // ------------------------------------------------------- sustained workload
        NumberFormatter.clearCache()
        val sustained = runSustainedWorkload(100_000)
        val cacheAfter = NumberFormatter.cacheStats()

        printReport(results, sustained, cacheAfter)
    }

    // ------------------------------------------------------------------ harness

    private fun bench(name: String, opsPerBatch: Int, block: (Int) -> Long): Stat {
        repeat(WARMUP_BATCHES) { block(opsPerBatch) }

        val timings = DoubleArray(BATCHES)
        var allocPerOp = -1.0
        for (batch in 0 until BATCHES) {
            val allocBefore = allocatedBytes()
            val start = System.nanoTime()
            val sink = block(opsPerBatch)
            val elapsed = System.nanoTime() - start
            val allocAfter = allocatedBytes()
            if (sink == Long.MIN_VALUE) println("impossible")
            timings[batch] = elapsed.toDouble() / opsPerBatch
            if (allocBefore >= 0 && allocAfter >= 0) {
                allocPerOp = (allocAfter - allocBefore).toDouble() / opsPerBatch
            }
        }
        return stat(name, timings, allocPerOp, opsPerBatch)
    }

    private fun benchCacheHit(name: String, value: Double): Stat {
        NumberFormatter.short(value) // populate
        val timings = DoubleArray(BATCHES)
        var allocPerOp = -1.0
        repeat(WARMUP_BATCHES) {
            var sink = 0L
            repeat(100_000) { sink += NumberFormatter.short(value).length }
            if (sink == Long.MIN_VALUE) println("impossible")
        }
        for (batch in 0 until BATCHES) {
            val allocBefore = allocatedBytes()
            val start = System.nanoTime()
            var sink = 0L
            repeat(100_000) { sink += NumberFormatter.short(value).length }
            val elapsed = System.nanoTime() - start
            val allocAfter = allocatedBytes()
            if (sink == Long.MIN_VALUE) println("impossible")
            timings[batch] = elapsed.toDouble() / 100_000
            if (allocBefore >= 0 && allocAfter >= 0) allocPerOp = (allocAfter - allocBefore).toDouble() / 100_000
        }
        return stat(name, timings, allocPerOp, 100_000, "cache=${NumberFormatter.cacheStats()}")
    }

    private fun benchCacheMiss(name: String, values: DoubleArray): Stat {
        NumberFormatter.clearCache()
        val timings = DoubleArray(BATCHES)
        var allocPerOp = -1.0
        var cursor = 0
        repeat(WARMUP_BATCHES) {
            var sink = 0L
            repeat(values.size) { sink += NumberFormatter.short(values[cursor++ % values.size]).length }
            if (sink == Long.MIN_VALUE) println("impossible")
        }
        for (batch in 0 until BATCHES) {
            NumberFormatter.clearCache()
            val allocBefore = allocatedBytes()
            val start = System.nanoTime()
            var sink = 0L
            for (value in values) sink += NumberFormatter.short(value).length
            val elapsed = System.nanoTime() - start
            val allocAfter = allocatedBytes()
            if (sink == Long.MIN_VALUE) println("impossible")
            timings[batch] = elapsed.toDouble() / values.size
            if (allocBefore >= 0 && allocAfter >= 0) allocPerOp = (allocAfter - allocBefore).toDouble() / values.size
        }
        return stat(name, timings, allocPerOp, values.size, "cache cleared per batch, ${values.size} distinct values")
    }

    private fun measurePerOpStat(
        name: String,
        ops: Int,
        block: (Int) -> Long
    ): Stat {
        repeat(WARMUP_BATCHES) { block(ops) }
        val timings = DoubleArray(BATCHES)
        var allocPerOp = -1.0
        for (batch in 0 until BATCHES) {
            val allocBefore = allocatedBytes()
            val start = System.nanoTime()
            val sink = block(ops)
            val elapsed = System.nanoTime() - start
            val allocAfter = allocatedBytes()
            if (sink == Long.MIN_VALUE) println("impossible")
            timings[batch] = elapsed.toDouble() / ops
            if (allocBefore >= 0 && allocAfter >= 0) allocPerOp = (allocAfter - allocBefore).toDouble() / ops
        }
        return stat(name, timings, allocPerOp, ops)
    }

    private fun stat(name: String, timings: DoubleArray, allocPerOp: Double, ops: Int, note: String = ""): Stat {
        val sorted = timings.sorted()
        return Stat(
            name = name,
            medianNs = median(sorted),
            p95Ns = percentile(sorted, 0.95),
            minNs = sorted.first(),
            maxNs = sorted.last(),
            allocBytesPerOp = allocPerOp,
            opsPerBatch = ops,
            note = note
        )
    }

    private fun median(sorted: List<Double>): Double = sorted[sorted.size / 2]

    private fun percentile(sorted: List<Double>, p: Double): Double {
        val index = ((sorted.size - 1) * p).toInt()
        return sorted[index]
    }

    // -------------------------------------------------------------- measurement

    /**
     * Allocation probe.
     *
     * `java.lang.management` and `com.sun.management` do not exist on Android, so both are reached
     * through reflection: on a device (or an Android compile classpath) the probe simply reports
     * allocation as unavailable instead of failing to compile or crashing the run.
     */
    private val threadMx by lazy {
        try {
            val factory = Class.forName("java.lang.management.ManagementFactory")
            val bean = factory.getMethod("getThreadMXBean").invoke(null) ?: return@lazy null
            val type = Class.forName("com.sun.management.ThreadMXBean")
            if (!type.isInstance(bean)) return@lazy null
            type.getMethod("setThreadAllocatedMemoryEnabled", Boolean::class.java).invoke(bean, true)
            val supported = type.getMethod("isThreadAllocatedMemorySupported").invoke(bean) as Boolean
            if (!supported) null else AllocProbe(bean, type.getMethod("getThreadAllocatedBytes", Long::class.java))
        } catch (unavailable: Throwable) {
            null
        }
    }

    private class AllocProbe(private val bean: Any, private val method: java.lang.reflect.Method) {
        fun bytes(): Long = try {
            // getId() is the only variant available on every supported API level.
            @Suppress("DEPRECATION")
            val threadId = Thread.currentThread().id
            method.invoke(bean, threadId) as Long
        } catch (ignored: Throwable) {
            -1L
        }
    }

    private fun allocatedBytes(): Long = threadMx?.bytes() ?: -1L

    /**
     * Loads [className] in a child class loader so its one-time initialiser actually runs again.
     *
     * The loader delegates everything outside `com.aistudio.*` to the parent, so Kotlin/JDK classes
     * are not re-read from disk and the measurement stays close to "class load + <clinit>" instead
     * of "unpack the whole standard library again". A class with an empty initialiser is measured
     * through the same path to show how much of the number is class loading rather than table build.
     */
    private fun measureClassInit(className: String): Double {
        val urls = (System.getProperty("java.class.path") ?: "")
            .split(java.io.File.pathSeparator)
            .filter { it.isNotEmpty() }
            .map { java.io.File(it).toURI().toURL() }
            .toTypedArray()
        val parent = EngineBenchmarkReport::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        val samples = DoubleArray(9)
        for (i in samples.indices) {
            val loader = IsolatingLoader(urls, parent)
            try {
                val start = System.nanoTime()
                // initialize = true runs <clinit>, which is the work being measured.
                Class.forName(className, true, loader)
                samples[i] = (System.nanoTime() - start).toDouble()
            } finally {
                loader.close()
            }
        }
        return median(samples.sorted())
    }

    /** Parent-first loader that isolates only `com.aistudio.*` so a singleton can be re-initialised. */
    private class IsolatingLoader(
        urls: Array<java.net.URL>,
        parent: ClassLoader
    ) : java.net.URLClassLoader(urls, parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (!name.startsWith("com.aistudio.")) return super.loadClass(name, resolve)
            // A loader-local monitor is enough: this loader only exists inside the benchmark.
            synchronized(this) {
                findLoadedClass(name)?.let { return it }
                val fresh = findClass(name)
                if (resolve) resolveClass(fresh)
                return fresh
            }
        }
    }

    // ------------------------------------------------------------ data + report

    private fun buildRepresentativeLongs(): LongArray = LongArray(64) { i -> (i * 977L) % 983L }

    private fun buildRepresentativeDoubles(): DoubleArray {
        val random = Random(4_242)
        return DoubleArray(10_000) { i ->
            when (i % 5) {
                0 -> random.nextDouble() * 999.0
                1 -> random.nextDouble() * 999_000.0
                2 -> random.nextDouble() * 999_000_000.0
                3 -> random.nextDouble() * 999_000_000_000.0
                else -> 1_234.5678
            }
        }
    }

    /** Indices whose labels are 1, 2, 3, 4, 5 and 10 characters long. */
    private val SUFFIX_INDICES = longArrayOf(
        0L, 25L, 26L, 51L, 52L, 701L, 702L, 18_277L, 12_356_630L, 146_813_779_479_500L
    )

    private val THRESHOLD_VALUES = doubleArrayOf(
        99.9, 100.0, 100.1, 999.9, 999.95, 1_000.0, 1_000.1, 9_999.0, 10_000.0, 10_000.1,
        99_999.0, 100_000.0, 100_000.1, 999_949.0, 999_950.0, 1_000_000.0, 1_000_001.0,
        9_999_999.0, 10_000_000.0, 99_999_999.0, 100_000_000.0, 999_950_000.0,
        1_000_000_000.0, 10_000_000_000.0, 100_000_000_000.0, 1_000_000_000_000.0, 1e15
    )

    private data class SustainedResult(
        val ops: Int,
        val elapsedMs: Double,
        val checksum: Long,
        val entries: Int,
        val estimatedRetainedBytes: Long
    )

    private fun runSustainedWorkload(ops: Int): SustainedResult {
        val random = Random(13)
        var checksum = 0L
        val start = System.nanoTime()
        repeat(ops) { i ->
            val value = when (i % 4) {
                0 -> random.nextLong(0, 1_000).toDouble()
                1 -> random.nextDouble() * 1_000_000
                2 -> random.nextDouble() * 1_000_000_000_000
                else -> 1_234.5678
            }
            checksum += NumberFormatter.short(value).length
            if (i % 3 == 0) checksum += NumberFormatter.suffixLabel((i % 702).toLong())!!.length
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val stats = NumberFormatter.cacheStats()
        // Documented estimate: Entry object (16 B header + 8 B key + 4 B ref, padded to 32 B) plus a
        // String (24 B header + byte[] header 16 B + ~6 chars).
        val estimated = stats.entries.toLong() * (32L + 24L + 16L + 8L)
        return SustainedResult(ops, elapsedMs, checksum, stats.entries, estimated)
    }

    private fun printReport(rows: List<Stat>, sustained: SustainedResult, cache: NumberFormatter.CacheStats) {
        println()
        println("=================== TRUE IDLE performance report ===================")
        println("runtime          : ${System.getProperty("java.vm.name")} ${System.getProperty("java.runtime.version")}")
        println("os / arch        : ${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
        println("cores            : ${Runtime.getRuntime().availableProcessors()}")
        println("batches          : $WARMUP_BATCHES warmup + $BATCHES measured, median/p95 of per-batch ns/op")
        println("allocation       : ${if (threadMx != null) "ThreadMXBean.getThreadAllocatedBytes" else "UNAVAILABLE (reported as -1)"}")
        println("NOTE             : JVM unit-test runtime, NOT an ART device measurement.")
        println()
        println(String.format("%-52s %10s %10s %10s %12s", "case", "median ns", "p95 ns", "min ns", "alloc B/op"))
        println("-".repeat(98))
        rows.forEach { row ->
            println(
                String.format(
                    "%-52s %10.1f %10.1f %10.1f %12s",
                    row.name,
                    row.medianNs,
                    row.p95Ns,
                    row.minNs,
                    if (row.allocBytesPerOp < 0) "n/a" else String.format("%.1f", row.allocBytesPerOp)
                )
            )
            if (row.note.isNotEmpty()) println("    note: ${row.note}")
        }
        println("-".repeat(98))
        println(
            "sustained ${sustained.ops} mixed ops: ${"%.1f".format(sustained.elapsedMs)} ms " +
                "(${"%.3f".format(sustained.elapsedMs * 1000.0 / sustained.ops)} us/op), checksum=${sustained.checksum}"
        )
        println(
            "cache: entries=${sustained.entries}/${cache.capacity} hits=${cache.hits} misses=${cache.misses} " +
                "evictions=${cache.evictions} hitRate=${"%.3f".format(cache.hitRate)} " +
                "estimatedRetained=${sustained.estimatedRetainedBytes} B"
        )
        println("tier table: ${TierConfig.size} tiers, retained ~${TierConfig.size * 64} B (strings + arrays, estimated)")
        println("===================================================================")
        println()
    }
}
