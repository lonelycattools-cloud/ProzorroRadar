package ua.gov.prozorro.radar.util

object CpvMatcher {

    /**
     * Extracts the significant hierarchical prefix from a CPV code or query.
     * Examples:
     * - "18100000-0" -> "181" (covers 18100000, 18110000, 18130000, 18141000, etc.)
     * - "1810000"   -> "181"
     * - "181"       -> "181"
     * - "30230000-0" -> "3023"
     * - "09130000-9" -> "0913"
     * - "45000000-7" -> "45"
     */
    fun extractPrefix(codeOrInput: String): String {
        val clean = codeOrInput.split("-")[0].trim()
        val withoutZeroes = clean.trimEnd('0')
        return if (withoutZeroes.length < 2) clean.take(2) else withoutZeroes
    }

    /**
     * Checks if any CPV code attached to a tender matches the filter query prefix.
     * Includes all child categories under the parent prefix.
     */
    fun matches(tenderCpvString: String, filterQuery: String): Boolean {
        if (filterQuery.isBlank()) return true
        val targetPrefix = extractPrefix(filterQuery)
        val tenderCodes = tenderCpvString.split(",").map { it.trim().split("-")[0].trim() }
        return tenderCodes.any { it.startsWith(targetPrefix) }
    }

    /**
     * Checks if tender matches any of the active CPV prefixes.
     */
    fun matchesAny(tenderCpvString: String, activePrefixes: List<String>): Boolean {
        if (activePrefixes.isEmpty()) return true
        val tenderCodes = tenderCpvString.split(",").map { it.trim().split("-")[0].trim() }
        return tenderCodes.any { tc ->
            activePrefixes.any { prefix -> tc.startsWith(prefix) }
        }
    }
}
