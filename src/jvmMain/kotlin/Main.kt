/*import org.jsoup.Jsoup

data class Station(val name: String, val temperature: String)

data class ExtractedData(var stations: List<Station> = listOf())

fun main() {
    val extractedData = ExtractedData()

    val doc = Jsoup.connect("https://meteo.arso.gov.si/uploads/probase/www/observ/surface/text/sl/observationAms_si_latest.html").get()

    val stationRows = doc.select("tr")

    extractedData.stations = stationRows.mapNotNull { row ->
        val nameElement = row.select("td.meteoSI-th")
        val temperatureElement = row.select("td.t")

        if (nameElement.isNotEmpty() && temperatureElement.isNotEmpty()) {
            val name = nameElement.text().trim()
            val temperature = temperatureElement.text().trim()

            Station(name, temperature)
        } else {
            null
        }
    }

    println("----------------------------------------")
    println("-------Temperature v mestu --------")
    println("----------------------------------------")
    println(String.format("%-25s%s", "MESTO", "TEMPERATURA"))
    println("----------------------------------------")
    extractedData.stations.forEach {
        println(String.format("%-25s%s", it.name, it.temperature))
    }
    println("----------------------------------------")
}*/
/*import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import java.io.File

data class Station(val name: String, val temperature: String,val vlaznost:String)

data class ExtractedData(var stations: List<Station> = listOf())

fun main() {
    val extractedData = ExtractedData()

    val doc = Jsoup.connect("https://meteo.arso.gov.si/uploads/probase/www/observ/surface/text/sl/observationAms_si_latest.html").get()
    val doc1=Jsoup.connect("https://api.jcdecaux.com/vls/v3/stations?apiKey=frifk0jbxfefqqniqez09tw4jvk37wyf823b5j1i&contract=maribor").get()


    val stationRows = doc.select("tr")

    extractedData.stations = stationRows.mapNotNull { row: Element ->
        val nameElement = row.select("td.meteoSI-th")
        val temperatureElement = row.select("td.t")
        val vlaznostElement=row.select("td.rh")

        if (nameElement.isNotEmpty() && temperatureElement.isNotEmpty()) {
            val name = nameElement.text().trim()
            val temperature = temperatureElement.text().trim()
            val vlaznost=vlaznostElement.text().trim()

            Station(name, temperature,vlaznost)
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
}*/

import org.json.JSONArray
import org.json.JSONObject
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
data class Station(val name: String, val availableBikes: Int)

fun parseJSONData(jsonData: String): List<Station> {
    val stations = mutableListOf<Station>()

    val jsonArray = JSONArray(jsonData)
    for (i in 0 until jsonArray.length()) {
        val jsonObject: JSONObject = jsonArray.getJSONObject(i)
        val name = jsonObject.getString("name")
        val totalStandsObject = jsonObject.getJSONObject("totalStands")
        val number = totalStandsObject.getJSONObject("availabilities")
        val availableBikes = number.getInt("stands")
        val station = Station(name, availableBikes)
        stations.add(station)
    }

    return stations
}

fun fetchDataFromAPI(apiUrl: String): String {
    val url = URL(apiUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
    val response = StringBuilder()

    bufferedReader.use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
    }

    return response.toString()
}

fun generateHTMLPage(stations: List<Station>): String {
    val html = StringBuilder()

    html.append("<html><head><title>Stations Data</title></head><body>")
    html.append("<table><tr><th>Station Name</th><th>Available Bikes</th></tr>")

    for (station in stations) {
        html.append("<tr><td>${station.name}</td><td>${station.availableBikes}</td></tr>")
    }

    html.append("</table></body></html>")

    return html.toString()
}
fun main() {
    val apiUrl = "https://api.jcdecaux.com/vls/v3/stations?apiKey=frifk0jbxfefqqniqez09tw4jvk37wyf823b5j1i&contract=maribor"

    // Step 1: Fetch data from the API
    val jsonData = fetchDataFromAPI(apiUrl)

    // Step 2: Parse JSON data
    val stations = parseJSONData(jsonData)

    // Step 3: Generate HTML page
    val htmlPage = generateHTMLPage(stations)

    // Write HTML to a file (optional)
    val outputFile = "output.html"
    File(outputFile).writeText(htmlPage)

    // Open the generated HTML page in the default browser
    Desktop.getDesktop().browse(File(outputFile).toURI())
}



