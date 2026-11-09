package ua.gov.prozorro.radar.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.gov.prozorro.radar.model.ReportResult
import ua.gov.prozorro.radar.model.Tender
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    suspend fun exportTendersToExcel(context: Context, tenders: List<Tender>): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, "Prozorro_Tenders_$timeStamp.xlsx")

                val headers = listOf(
                    "Дата модифікації",
                    "ID Prozorro",
                    "Номер тендера",
                    "Статус",
                    "Замовник",
                    "ЄДРПОУ",
                    "Очікувана вартість",
                    "Валюта",
                    "Класифікатор CPV",
                    "Назва закупівлі",
                    "Кінцевий строк",
                    "Посилання на Prozorro",
                    "Email статус"
                )

                val rows = tenders.map { t ->
                    listOf(
                        t.dateModified,
                        t.id,
                        t.tenderId,
                        t.displayStatus,
                        t.procuringEntityName,
                        t.procuringEntityEdrpou,
                        t.valueAmount.toString(),
                        t.valueCurrency,
                        t.cpvCodes,
                        t.title,
                        t.deadline,
                        t.tenderUrl,
                        t.emailStatus
                    )
                }

                writeSimpleXlsx(file, "Тендери", headers, rows)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                Result.success(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    suspend fun exportReportToExcel(context: Context, report: ReportResult): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, "Prozorro_Report_$timeStamp.xlsx")

                val headers = listOf(
                    "Показник / Об'єкт",
                    "Опис / Додатково",
                    "Кількість тендерів",
                    "Сума (грн)",
                    "Примітка"
                )

                val rows = report.rows.map { r ->
                    listOf(
                        r.title,
                        r.subtitle,
                        r.count.toString(),
                        r.totalAmount.toString(),
                        r.extra
                    )
                }

                writeSimpleXlsx(file, "Аналітичний звіт", headers, rows)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                Result.success(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    fun shareExcelFile(context: Context, uri: Uri, title: String = "Експорт Prozorro Excel") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun writeSimpleXlsx(
        targetFile: File,
        sheetName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/>
                    </sheets>
                </workbook>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 5. xl/styles.xml
            zos.putNextEntry(ZipEntry("xl/styles.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="2">
                        <font><sz val="11"/><name val="Calibri"/></font>
                        <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                    </fonts>
                    <fills count="3">
                        <fill><patternFill patternType="none"/></fill>
                        <fill><patternFill patternType="gray125"/></fill>
                        <fill><patternFill patternType="solid"><fgColor rgb="FF008F39"/></patternFill></fill>
                    </fills>
                    <borders count="1">
                        <border><left/><right/><top/><bottom/></border>
                    </borders>
                    <cellXfs count="2">
                        <xf fontId="0" fillId="0" borderId="0"/>
                        <xf fontId="1" fillId="2" borderId="0" applyFont="1" applyFill="1"/>
                    </cellXfs>
                </styleSheet>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetBuilder = StringBuilder()
            sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sheetBuilder.append("""<sheetData>""")

            // Header row (Row 1, style 1 = bold white on green)
            sheetBuilder.append("""<row r="1">""")
            headers.forEachIndexed { colIdx, h ->
                val colRef = getColumnRef(colIdx) + "1"
                sheetBuilder.append("""<c r="$colRef" t="inlineStr" s="1">""")
                sheetBuilder.append("""<is><t>${escapeXml(h)}</t></is>""")
                sheetBuilder.append("""</c>""")
            }
            sheetBuilder.append("""</row>""")

            // Data rows
            rows.forEachIndexed { rowIdx, rowList ->
                val rNum = rowIdx + 2
                sheetBuilder.append("""<row r="$rNum">""")
                rowList.forEachIndexed { colIdx, cellValue ->
                    val colRef = getColumnRef(colIdx) + rNum
                    val num = cellValue.toDoubleOrNull()
                    if (num != null && colIdx in listOf(6, 3)) {
                        // Numeric
                        sheetBuilder.append("""<c r="$colRef">""")
                        sheetBuilder.append("""<v>$num</v>""")
                        sheetBuilder.append("""</c>""")
                    } else {
                        // Text
                        sheetBuilder.append("""<c r="$colRef" t="inlineStr">""")
                        sheetBuilder.append("""<is><t>${escapeXml(cellValue)}</t></is>""")
                        sheetBuilder.append("""</c>""")
                    }
                }
                sheetBuilder.append("""</row>""")
            }

            sheetBuilder.append("""</sheetData>""")
            sheetBuilder.append("""</worksheet>""")

            zos.write(sheetBuilder.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun getColumnRef(colIdx: Int): String {
        var n = colIdx
        var ref = ""
        while (n >= 0) {
            ref = ('A' + (n % 26)) + ref
            n = (n / 26) - 1
        }
        return ref
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
