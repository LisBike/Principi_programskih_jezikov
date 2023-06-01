
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

data class Weather(val name: String, val temperature: String,val vlaznost:String)

data class ExtractedData(var stations: List<Weather> = listOf())

fun main() {
    val extractedData = ExtractedData()

    val doc = Jsoup.connect("https://meteo.arso.gov.si/uploads/probase/www/observ/surface/text/sl/observationAms_si_latest.html").get()
    val stationRows = doc.select("tr")

    extractedData.stations = stationRows.mapNotNull { row: Element ->
        val nameElement = row.select("td.meteoSI-th")
        val temperatureElement = row.select("td.t")
        val vlaznostElement=row.select("td.rh")

        if (nameElement.isNotEmpty() && temperatureElement.isNotEmpty()) {
            val name = nameElement.text().trim()
            val temperature = temperatureElement.text().trim()
            val vlaznost=vlaznostElement.text().trim()

            Weather(name, temperature,vlaznost)
        } else {
            null
        }
    }

    val outputFile = File("output.html")
    outputFile.bufferedWriter().use { writer ->
        writer.write("""
            <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        table {
                            font-family: Arial, sans-serif;
                            border-collapse: collapse;
                            width: 100%;
                        }

                        th, td {
                            text-align: left;
                            padding: 8px;
                        }

                        th {
                            background-color: grey;
                            color: white;
                        }

                        tr:nth-child(even) {
                            background-color: lightblue;
                        }
                    </style>
                </head>
                <body>
                    <table>
                        <thead>
                            <tr>
                                <th>MESTO</th>
                                <th>TEMPERATURA</th>
                                <th>Vlaznost</th>
                            </tr>
                        </thead>
                        <tbody>
        """.trimIndent())
        extractedData.stations.forEach {
            writer.write("<tr><td>${it.name}</td><td>${it.temperature}</td><td>${it.vlaznost}</td></tr>")
        }
        writer.write("""
                        </tbody>
                    </table>
                </body>
            </html>
        """.trimIndent())
    }

    println("Data written to ${outputFile.absolutePath}")
}
