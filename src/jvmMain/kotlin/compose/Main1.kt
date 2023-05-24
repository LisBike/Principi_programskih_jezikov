package compose
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

data class Station(val name: String, val availableBikes: Int)

fun parseJSONData(jsonData: String): List<Station> {
    val stations = mutableListOf<Station>()

    val jsonArray = JSONArray(jsonData)
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val name = jsonObject.getString("name")
        val availableBikes = jsonObject.getJSONObject("totalStands")
            .getJSONObject("availabilities")
            .getInt("bikes")
        val station = Station(name, availableBikes)
        stations.add(station)
    }

    return stations
}

fun fetchStationsData(apiUrl: String): String {
    val url = URL(apiUrl)
    return url.readText()
}

fun main() = application {
    Window(title = "vodenje racunov", onCloseRequest = ::exitApplication) {
        App()
    }
}

@Composable
fun App() {
    MaterialTheme {
        StartScreen()
    }
}

@Composable
fun StartScreen() {
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(0) }
    val stationsData = remember { mutableStateListOf<Station>() }

    // Fetch data from API and parse it
    GlobalScope.launch(Dispatchers.IO) {
        val jsonData = fetchStationsData("https://api.jcdecaux.com/vls/v3/stations?apiKey=frifk0jbxfefqqniqez09tw4jvk37wyf823b5j1i&contract=maribor")
        val parsedData = parseJSONData(jsonData)
        stationsData.addAll(parsedData)
    }

    Scaffold(
        topBar = {
            BottomNavigation(backgroundColor = Color.Cyan) {
                BottomNavigationItem(
                    selected = selectedTab == 0,
                    onClick = { setSelectedTab(0) },
                    icon = {
                        Row(verticalAlignment = Alignment.CenterVertically,) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu Icon")
                            Text(modifier = Modifier.padding(10.dp), text = "Invoices")
                        }
                    },
                )
                BottomNavigationItem(
                    selected = selectedTab == 1,
                    onClick = { setSelectedTab(1) },
                    icon = {
                        Row(verticalAlignment = Alignment.CenterVertically,) {
                            Icon(Icons.Filled.Menu, contentDescription = "Info Icon")
                            Text(modifier = Modifier.padding(10.dp), text = "About")
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> InvoiceScreen(Modifier.padding(innerPadding), stationsData)
            1 -> AboutScreen(Modifier.padding(innerPadding))
        }
    }
}


@Composable
fun InvoiceScreen(modifier: Modifier = Modifier, stationsData: MutableList<Station>) {
    val lazyListState = rememberLazyListState()
    val stationsToDelete = remember { mutableStateListOf<Int>() }
    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "INVOICES CONTENT", style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
        }
        //fun deleteStation(index: Int) {
          //  stationsData.removeAt(index)
      //  }
        // Display the parsed data
        Box(Modifier) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState
            ) {
                itemsIndexed(stationsData) { index, station ->
                    StationCard(
                        station = station,
                        onStationEdited = { editedStation ->
                            stationsData[index] = editedStation
                        },
                        onStationDeleted = {
                            stationsData.removeAt(index)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = lazyListState
                )
            )
        }
    }
}

@Composable
fun StationCard(
    station: Station,
    onStationEdited: (Station) -> Unit,
    onStationDeleted: () -> Unit
) {

    var editedName by remember { mutableStateOf(station.name) }
    var editedAvailableBikes by remember { mutableStateOf(station.availableBikes) }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Station:",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Enter Station Name") },
                    textStyle = MaterialTheme.typography.body1,
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors()
                )
            } else {
                Text(
                    text = editedName,
                    style = MaterialTheme.typography.body1
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Available Bikes: $editedAvailableBikes",
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (isEditing) {
                IconButton(onClick = {
                    val editedStation = Station(editedName, editedAvailableBikes)
                    onStationEdited(editedStation)
                    isEditing = false
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                }
                IconButton(onClick = {
                    isEditing = false
                }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                }
            } else {
                IconButton(onClick = {
                    isEditing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
            IconButton(onClick = {onStationDeleted() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }

}







@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
        ) {
            AboutText()
        }
        statusBar("About")
    }
}

@Composable
private fun statusBar(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .background(Color.Cyan)
    ) {
        Text(
            text = "You're viewing the \"$text\' tab",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AboutText() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "About Application",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )
        Text(text = "Subject: Principles of programming languages", style = MaterialTheme.typography.h6)
        Text(text = "Author: Sara Hristova", style = MaterialTheme.typography.body1)
    }
}


