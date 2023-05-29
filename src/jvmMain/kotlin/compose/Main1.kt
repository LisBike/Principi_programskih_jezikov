package compose
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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


fun main() = application {
    Window(title = "Data Provider", onCloseRequest = ::exitApplication) {
        App()
    }
}

data class Station(val name: String, val availableBikes: Int)

@Composable
fun StationCard(
    station: Station,
    onStationDeleted: (Station) -> Unit,
    onStationEdited: (Station) -> Unit
) {
    var originalName by remember { mutableStateOf(station.name) }
    var originalAvailableBikes by remember { mutableStateOf(station.availableBikes) }
    var editedName by remember { mutableStateOf(station.name) }
    var editedAvailableBikes by remember { mutableStateOf(station.availableBikes) }
    var isEditing by remember { mutableStateOf(false) }
    // 3D
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
        elevation = 8.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Station Icon",
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "Station:",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(5.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape = RectangleShape)
                        .padding(5.dp),
                    label = { Text("Enter Station Name") },
                    textStyle = MaterialTheme.typography.body1,
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Available Bikes: $editedAvailableBikes",
                    style = MaterialTheme.typography.body1
                )
                Spacer(modifier = Modifier.width(20.dp))
                IconButton(onClick = {
                    val editedStation = Station(editedName, editedAvailableBikes)
                    onStationEdited(editedStation)
                    isEditing = false
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                }
                IconButton(onClick = {
                    editedName = originalName // Reset to original name
                    editedAvailableBikes = originalAvailableBikes // Reset to original available bikes
                    isEditing = false
                }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = station.name)
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            modifier = Modifier
                                .weight(1f),
                            text = "Available Bikes: ",
                            textAlign = TextAlign.Right,
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            modifier = Modifier
                                .width(20.dp),
                            textAlign = TextAlign.Right,
                            text = station.availableBikes.toString(),
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                }
                IconButton(onClick = {
                    originalName = editedName // Store the original name
                    originalAvailableBikes = editedAvailableBikes // Store the original available bikes
                    isEditing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onStationDeleted(station) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
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
    var selectedTab by remember { mutableStateOf(0) }

    val stationsData = remember { mutableStateListOf<Station>() }
    var isDataFetched by remember { mutableStateOf(false) }
    var showData by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BottomNavigation(backgroundColor = Color.Cyan) {
                BottomNavigationItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu Icon")
                            //Text(modifier = Modifier.padding(10.dp), text = "Invoices")
                            Text(modifier = Modifier.padding(10.dp), text = "LIST OF PARSED DATA")
                        }
                    }
                )
                BottomNavigationItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = "Info Icon")
                            Text(modifier = Modifier.padding(10.dp), text = "About")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                if (showData) {
                    InvoiceScreen(Modifier.padding(innerPadding), stationsData)
                    //InvoiceScreen(Modifier.padding(innerPadding))
                } else {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        if (!isDataFetched) {
                            Button(
                                onClick = {
                                    GlobalScope.launch(Dispatchers.IO) {
                                        val jsonData = fetchStationsData("https://api.jcdecaux.com/vls/v3/stations?apiKey=frifk0jbxfefqqniqez09tw4jvk37wyf823b5j1i&contract=maribor")
                                        val parsedData = parseJSONData(jsonData)
                                        stationsData.addAll(parsedData)
                                        isDataFetched = true
                                        showData = true
                                    }
                                    showData = true
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Get data")
                            }
                        }
                    }
                }
            }
            1 -> AboutScreen(Modifier.padding(innerPadding))
        }
    }
}

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

@Composable
fun InvoiceScreen(modifier: Modifier = Modifier, stationsData: SnapshotStateList<Station>) {

    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            val state = rememberLazyListState()
            LazyColumn(modifier.fillMaxSize().padding(end = 12.dp), state) {
                items(stationsData) { station ->
                    StationCard(station = station,
                        onStationDeleted = { removedStation ->
                            stationsData.remove(removedStation)
                        },
                        onStationEdited = { editedStation ->
                            val index = stationsData.indexOf(station)
                            stationsData[index] = editedStation
                        }
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = state
                )
            )
        }
        statusBar("List of parsed data")
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
            text = "You're viewing the \"$text\" tab",
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
        Text(text = "Subject: Principles of programming languages - Project", style = MaterialTheme.typography.h6)
        Text(text = "Author: Mladi Majmuni", style = MaterialTheme.typography.body1)
    }
}

