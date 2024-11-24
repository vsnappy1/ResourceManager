package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.model.SourceFileDetails
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility object for generating an HTML report summarizing file migrations.
 * The report includes details of changes made to each file, such as the line number,
 * original code, and updated code.
 */
internal object ReportGenerator {
    /**
     * Generates an HTML report summarizing the migration of source files.
     *
     * @param files A list of [SourceFileDetails], each representing a file and the changes made to it.
     * @return A String containing the complete HTML content of the report.
     */
    fun generateMigrationReport(files: List<SourceFileDetails>): String {
        val report = StringBuilder()

        fun StringBuilder.appendLine(
            indentation: Int = 0,
            content: String
        ) {
            repeat(indentation) { append("\t") }
            appendLine(content)
        }

        report.apply {
            appendLine(0, "<!DOCTYPE html>")
            appendLine(0, "<html lang=\"en\">")
            appendLine(1, "<head>")
            appendLine(2, "<meta charset=\"UTF-8\">")
            appendLine(2, "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine(2, "<title>Migration Report</title>")
            appendLine(2, "<style>")
            appendLine(
                """
                body {
                    font-family: Arial, sans-serif;
                    margin: 20px;
                }
                h1 {
                    color: #333;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 20px;
                }
                th, td {
                    padding: 10px;
                    border: 1px solid #ccc;
                }
                th {
                    background-color: #f2f2f2;
                    text-align: left;
                }
                tr:nth-child(even) {
                    background-color: #f9f9f9;
                }
                .file-section {
                    margin-top: 20px;
                }
                .change-header {
                    font-weight: bold;
                    color: #555;
                    margin-top: 10px;
                }
                .line-number {
                    font-style: italic;
                    color: #888;
                }
                .current {
                    color: #d9534f;
                }
                .updated {
                    color: #5cb85c;
                }
                """.trimIndent().prependIndent("\t\t\t")
            )
            appendLine(2, "</style>")
            appendLine(1, "</head>")
            appendLine(1, "<body>")
            appendLine(2, "<h1>Resource Manager Migration Report</h1>")
            appendLine(2, "<p>Generated on ${getFormattedDate()}</p>")
            val filesCount = files.count()
            val changeCount = files.flatMap { it.changes }.size
            val fileText = if (filesCount > 1) "files" else "file"
            val changeText = if (changeCount > 1) "changes" else "change"
            appendLine(2, "<p>Modified $filesCount $fileText with a total of $changeCount $changeText applied.</p>")

            for (file in files) {
                appendLine(2, "<div class=\"file-section\">")
                appendLine(3, "<h2>File: ${file.name}</h2>")
                appendLine(3, "<p><strong>Path:</strong> ${file.path}</p>")
                appendLine(3, "<table>")
                appendLine(4, "<tr>")
                appendLine(5, "<th style=\"width: 10%;\">Line Number</th>")
                appendLine(5, "<th style=\"width: 55%;\">Original Code</th>")
                appendLine(5, "<th style=\"width: 35%;\">Updated Code</th>")
                appendLine(4, "</tr>")

                for (change in file.changes.sortedBy { it.lineNumber }) {
                    appendLine(4, "<tr>")
                    appendLine(5, "<td class=\"line-number\">${change.lineNumber}</td>")
                    appendLine(5, "<td class=\"current\">${change.current.escapeHtml()}</td>")
                    appendLine(5, "<td class=\"updated\">${change.updated.escapeHtml()}</td>")
                    appendLine(4, "</tr>")
                }

                appendLine(3, "</table>")
                appendLine(2, "</div>")
            }

            appendLine(1, "</body>")
            appendLine(0, "</html>")
        }

        return report.toString()
    }

    /**
     * Escapes special HTML characters in a string to ensure safe rendering in the HTML report.
     */
    private fun String.escapeHtml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Generates the current date and time formatted as "Month DD, YYYY at HH:MM AM/PM".
     */
    private fun getFormattedDate(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")
        return currentDateTime.format(formatter)
    }
}