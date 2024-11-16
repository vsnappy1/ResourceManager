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
        return """
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Migration Report</title>
                <style>
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
        
                    th,
                    td {
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
                </style>
            </head>
            <body>
                <h1>Resource Manager Migration Report</h1>
                <p>Generated on ${getFormattedDate()}</p> ${
            files.joinToString(separator = "") { file ->
                """ 
                <div class="file-section">
                <h2>File: ${file.name}</h2>
                <p>
                    <strong>Path:</strong> ${file.path}
                </p>
                <table>
                    <tr>
                        <th style="width: 10%;">Line Number</th>
                        <th style="width: 55%;">Original Code</th>
                        <th style="width: 35%;">Updated Code</th>
                    </tr> ${
                    file.changes.joinToString(separator = "") { change ->
                    """ 
                    <tr>
                        <td class="line-number">${change.lineNumber}</td>
                        <td class="current">${change.current.escapeHtml()}</td>
                        <td class="updated">${change.updated.escapeHtml()}</td>
                    </tr> 
                    """
                    }
                }
                    </table>
                </div> 
                """
            }
        }
            </body>
        </html>
        """.trimIndent()
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