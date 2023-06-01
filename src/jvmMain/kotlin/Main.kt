import org.json.JSONArray
import org.json.JSONObject
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import kotlin.*
import kotlin.io.writeText
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

fun getConfigValues(): JSONObject {
    val configFilePath = "C:\\Users\\Dell\\IdeaProjects\\LIS_BIKE\\src\\jvmMain\\kotlin\\config.json"
    val configContent = String(Files.readAllBytes(Paths.get(configFilePath)))
    return JSONObject(configContent)
}

fun saveStationsToDatabase(stations: List<Station>) {
    val config = getConfigValues()
    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    Class.forName("com.mysql.cj.jdbc.Driver")
    val connection: Connection = DriverManager.getConnection(url, username, password)

    val insertStatement = "INSERT INTO stations (name, available_bikes) VALUES (?, ?)"
    val preparedStatement: PreparedStatement = connection.prepareStatement(insertStatement)

    for (station in stations) {
        preparedStatement.setString(1, station.name)
        preparedStatement.setInt(2, station.availableBikes)
        preparedStatement.addBatch()
    }

    preparedStatement.executeBatch()

    connection.close()
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


    val jsonData = fetchDataFromAPI(apiUrl)


    val stations = parseJSONData(jsonData)


    val htmlPage = generateHTMLPage(stations)
    saveStationsToDatabase(stations)


    val outputFile = "output.html"
    File(outputFile).writeText(htmlPage)

    Desktop.getDesktop().browse(File(outputFile).toURI())
}



