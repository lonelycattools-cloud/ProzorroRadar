package ua.gov.prozorro.radar

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.model.TenderParser

class ProzorroLogicTest {

    @Test
    fun testCpvPrefixMatching() {
        val configuredCpvs = "3023, 4500, 3360-1"
        val prefixes = configuredCpvs.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }
            .map { clean ->
                val short = clean.trimEnd('0')
                if (short.length < 2) clean.take(2) else short
            }

        // 3023 -> "3023"
        // 4500 -> "45"
        // 3360 -> "336"
        assertTrue(prefixes.contains("3023"))
        assertTrue(prefixes.contains("45"))
        assertTrue(prefixes.contains("336"))

        val testCpv1 = "30230000-0"
        val match1 = prefixes.any { testCpv1.startsWith(it) }
        assertTrue(match1)

        val testCpv2 = "45210000-2"
        val match2 = prefixes.any { testCpv2.startsWith(it) }
        assertTrue(match2)

        val testCpv3 = "79710000-4"
        val match3 = prefixes.any { testCpv3.startsWith(it) }
        assertFalse(match3)
    }

    @Test
    fun testTenderParser() {
        val sampleJson = """
        {
            "id": "e42718fa8b2542a2b02888cf3cbf7934",
            "tenderID": "UA-2026-11-09-000456-a",
            "title": "Постачання серверного обладнання",
            "description": "Сервери для дата-центру",
            "status": "active.tendering",
            "dateModified": "2026-11-09T08:30:00+02:00",
            "value": {
                "amount": 250000.50,
                "currency": "UAH"
            },
            "procuringEntity": {
                "name": "АТ Укрзалізниця",
                "identifier": {
                    "id": "40075815"
                }
            },
            "classification": {
                "id": "30230000-0"
            },
            "items": [
                {
                    "classification": {
                        "id": "30232000-4"
                    }
                }
            ],
            "tenderPeriod": {
                "endDate": "2026-11-25T18:00:00+02:00"
            }
        }
        """.trimIndent()

        val jsonElement = Json.parseToJsonElement(sampleJson).jsonObject
        val tender = TenderParser.parseTender(jsonElement)

        assertEquals("e42718fa8b2542a2b02888cf3cbf7934", tender.id)
        assertEquals("UA-2026-11-09-000456-a", tender.tenderId)
        assertEquals("Постачання серверного обладнання", tender.title)
        assertEquals("АТ Укрзалізниця", tender.procuringEntityName)
        assertEquals("40075815", tender.procuringEntityEdrpou)
        assertEquals(250000.50, tender.valueAmount, 0.01)
        assertEquals("active.tendering", tender.status)
        assertEquals("Подання пропозицій", tender.displayStatus)
        assertTrue(tender.cpvCodes.contains("30230000-0"))
        assertTrue(tender.cpvCodes.contains("30232000-4"))
        assertEquals("2026-11-25T18:00:00+02:00", tender.deadline)
        assertEquals("https://prozorro.gov.ua/tender/e42718fa8b2542a2b02888cf3cbf7934", tender.tenderUrl)
    }

    @Test
    fun testTenderFormatting() {
        val tender = Tender(
            id = "test-1",
            title = "Паливо автомобільне",
            valueAmount = 1500000.0,
            valueCurrency = "UAH",
            status = "active.auction"
        )

        assertEquals("Аукціон", tender.displayStatus)
        assertTrue(tender.formattedBudget.contains("1") && tender.formattedBudget.contains("500") && tender.formattedBudget.contains("UAH"))
    }

    @Test
    fun testCpvItemParsing() {
        val sampleEntry = """
        {
            "18100000-0": "Формений одяг, спеціальний робочий одяг та аксесуари",
            "30230000-0": "Комп’ютерне обладнання"
        }
        """.trimIndent()

        val json = Json.parseToJsonElement(sampleEntry).jsonObject
        val name1810 = json["18100000-0"]?.toString()?.replace("\"", "")
        assertTrue(name1810 != null && name1810.contains("одяг"))
    }
}
