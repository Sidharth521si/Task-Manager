package com.example.practiceee

import android.annotation.SuppressLint
import android.app.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.practiceee.data.TaskDao
import com.example.practiceee.data.TaskDatabase
import com.example.practiceee.data.TaskEntity
import com.example.practiceee.ui.theme.PracticeeeTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter.State.Empty.painter
import com.example.practiceee.data.Article
import com.example.practiceee.data.Note
import com.example.practiceee.network.RetrofitInstance
import com.example.practiceee.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class TaskItemData(
    val task: String,
    val date: String?,
    val time: String?,
    val selectedPriority: String = "Medium"
)
sealed class BottomNavItem(val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
     object Tasks : BottomNavItem("News", Icons.Default.PlayArrow)
     object Settings : BottomNavItem("Settings", Icons.Default.Settings)
}

@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Tasks,
            BottomNavItem.
            Settings
        ).forEach { item ->
            NavigationBarItem(
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selectedItem == item) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary
                    )
                },
                label = {
                    Text(
                        item.label,
                        color = if (selectedItem == item) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}



class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels() // ViewModel to hold theme state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up notification and edge-to-edge
        createNotificationChannel(this)
        enableEdgeToEdge()

        // Get DAO for tasks
        val db = TaskDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()

        // Check permission for exact alarm scheduling on Android S and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Toast.makeText(this, "Please enable permission to schedule exact alarms", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme // Get current theme from ViewModel

            // Use the selected theme
            PracticeeeTheme(darkTheme = isDarkTheme) {

                    // If PIN is set, show the home screen
                    HomeScreen(taskDao = taskDao, themeViewModel = themeViewModel)

                }
            }
        }
    }





@SuppressLint("ScheduleExactAlarm")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(taskDao: TaskDao, themeViewModel: ThemeViewModel) {
    var showPriorityDialog by remember { mutableStateOf(false) }
    val taskList = remember { mutableStateListOf<TaskEntity>() }
    var task by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var lastAddedTask by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf("Medium") }
    val context = LocalContext.current
    val isDarkTheme by themeViewModel.isDarkTheme
    val coroutineScope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }
    val navController = rememberNavController()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val onItemSelected: (BottomNavItem) -> Unit = { item -> selectedItem = item }

    LaunchedEffect(Unit) {
        val existingTasks = taskDao.getAllTasks()
        taskList.addAll(existingTasks)
    }

    Scaffold(
        topBar = {
            if (currentRoute?.equals("settings") != true) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Task Manager",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = onItemSelected
            )
        },
        containerColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF4F4F4)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                BottomNavItem.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Add New Task",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = task,
                                    onValueChange = { task = it },
                                    label = { Text("What needs to be done?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (task.isNotBlank()) {
                                            lastAddedTask = task
                                            showPriorityDialog = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Task")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Your Tasks (${taskList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (taskList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No tasks yet", color = Color.Gray)
                                    Text("Add your first task above", color = Color.LightGray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(taskList) { item ->
                                    val taskItemData = TaskItemData(
                                        task = item.task,
                                        date = item.date,
                                        time = item.time,
                                        selectedPriority = item.priority ?: "Medium"
                                    )
                                    TaskItem(
                                        taskItem = taskItemData,
                                        onDelete = {
                                            taskList.remove(item)
                                            coroutineScope.launch {
                                                taskDao.deleteTaskById(item.id)
                                            }
                                            Toast.makeText(context, "Task Deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }


                        BottomNavItem.Tasks -> {
                            NewsApp()
                }
                BottomNavItem.Settings -> {
                    SettingsScreenContent(themeViewModel)
                }
            }
        }
    }
// dialog box for priority pick
    if (showPriorityDialog) {
        PriorityDialog(
            selectedPriority = selectedPriority,
            onPrioritySelected = { newPriority ->
                selectedPriority = newPriority
                lastAddedTask = task
                showDatePicker = true
            },
            onDismiss = { showPriorityDialog = false }
        )
    }
// dialog for date pick
    if (showDatePicker) {
        val now = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
                showDatePicker = false
                showTimePicker = true
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showTimePicker) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val pickedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(pickedTime.time)
                val newTask = TaskEntity(
                    task = lastAddedTask,
                    date = selectedDate,
                    time = formattedTime,
                    priority = selectedPriority
                )

                taskList.add(newTask)

                coroutineScope.launch {
                    taskDao.insertTasks(listOf(newTask))
                }

                showTimePicker = false
                selectedDate = null

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("task", lastAddedTask)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    pickedTime.timeInMillis,
                    pendingIntent
                )
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        ).show()
    }

}


// Setting Screen Layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(themeViewModel: ThemeViewModel) {
    val context = LocalContext.current
    val isDarkTheme by themeViewModel.isDarkTheme
    var isSoundEnabled by rememberSaveable { mutableStateOf(true) }


    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
//            Dashboard(viewModel = statsViewModel)


            // Dark/light mode switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Dark Mode") },
                    supportingContent = { Text(" dark theme") },
                    leadingContent = {
                        Icon(Icons.Filled.Build, contentDescription = "Theme")
                    },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { themeViewModel.toggleTheme() }
                        )
                    }
                )
            }
            Divider(
                color = Color.Gray,
                thickness = 2.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )


            // GitHub link
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Open GitHub") },
                    supportingContent = { Text("Visit my GitHub profile") },
                    leadingContent = {
                        Icon(Icons.Filled.Person, contentDescription = "GitHub")
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/Sidharth521si")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Feedback link
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Send Feedback") },
                    supportingContent = { Text("Let us know what you think") },
                    leadingContent = {
                        Icon(Icons.Filled.Email, contentDescription = "Feedback")
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("sidharthramachandran27@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Task Manager Feedback")
                        }
                        context.startActivity(intent)
                    }
                )
            }


        }
    }
}


@Composable
fun PriorityDialog(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Priority Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Priority",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose a priority level for this task:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Divider(

                )
                PrioritySelector(
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { priority ->
                        onPrioritySelected(priority)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
// Add task field

@Composable
fun AddTask(
    task: String,
    onTaskChanged: (String) -> Unit,
    onTaskAdded: () -> Unit  
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            TextField(
                value = task,
                onValueChange = onTaskChanged,
                label = { Text("Add a new task") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedButton(
            onClick = onTaskAdded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF3A8DFF))
        ) {
            Text("Add Task", color = Color.White)
        }
    }
}

// List of task items
@Composable
fun TaskItem(
    taskItem: TaskItemData,
    onDelete: () -> Unit
) {
    Log.d("TaskItemDebug", "Task: ${taskItem.task}")
    val priorityColor = when (taskItem.selectedPriority) {
        "High" -> Color(0xFFFFCDD2) 
        "Medium" -> Color(0xFFFFF9C4) 
        "Low" -> Color(0xFFC8E6C9) 
        else -> Color.White
    }

    val priorityTextColor = when (taskItem.selectedPriority) {
        "High" -> Color(0xFFC62828)
        "Medium" -> Color(0xFFF57F17)
        "Low" -> Color(0xFF2E7D32)
        else -> Color.Black
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = priorityColor,
            contentColor = Color.Black
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier) {
                    Text(
                        text = taskItem.task,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )

                    if (!taskItem.date.isNullOrBlank() || !taskItem.time.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (!taskItem.date.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.calendar_month_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                        contentDescription = "Date",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = taskItem.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (!taskItem.time.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.schedule_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                        contentDescription = "Time",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = taskItem.time,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = priorityTextColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    text = taskItem.selectedPriority,
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


//@Composable
//fun CustomBottomAppBar(onMenuClick: () -> Unit) {
//    BottomAppBar(
//        containerColor = Color(0xFF3A8DFF), // BottomAppBar color
//        contentColor = Color.White,         // Content (icon) color
//        tonalElevation = 8.dp               // Elevation for shadow
//    ) {
//        // Menu Button
//        IconButton(onClick = onMenuClick) {
//            Icon(Icons.Default.Menu, contentDescription = "Menu")
//        }
//
//        // Spacer to push items to the right
//        Spacer(modifier = Modifier.weight(1f))
//
//        // You can add more actions (like "Add Task" button) here if needed
//        // IconButton(onClick = { /* Handle add task action */ }) {
//        //     Icon(Icons.Default.Add, contentDescription = "Add Task")
//        // }
//    }
//}
@Composable
fun AddTaskButtonWithPriority(
    task: String,
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    onTaskAdded: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        // Ensure the PrioritySelector composable is working as expected
        PrioritySelector(
            selectedPriority = selectedPriority,
            onPrioritySelected = onPrioritySelected
        )

        ElevatedButton(
            onClick = onTaskAdded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF3A8DFF))
        ) {
            Text("Add Task", color = Color.White)
        }
    }
}


@Composable
fun PrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit
) {
    val priorities = listOf("Low", "Medium", "High")
    val priorityColors = mapOf(
        "Low" to Color(0xFF4CAF50),
        "Medium" to Color(0xFFFFA500),
        "High" to Color.Red
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        priorities.forEach { priority ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPrioritySelected(priority) },
                colors = CardDefaults.cardColors(
                    containerColor = if (priority == selectedPriority) {
                        priorityColors[priority]?.copy(alpha = 0.2f) ?: Color.LightGray
                    } else {
                        Color.Transparent
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = priorityColors[priority] ?: Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = priority,
                        style = MaterialTheme.typography.bodyLarge,
                        color = priorityColors[priority] ?: Color.Gray
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    PracticeeeTheme {

    }
}
// code for notification manager
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "task_reminder_channel"
        val channelName = "Task Reminders"
        val channelDescription = "Notifications for task reminders"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = channelDescription
            enableLights(true)
            lightColor = android.graphics.Color.RED
        }

        notificationManager.createNotificationChannel(channel)
    }
}
// class for dark/light theme

class ThemeViewModel : ViewModel() {
    private val _isDarkTheme = mutableStateOf(false)
    val isDarkTheme: State<Boolean> = _isDarkTheme

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }
}

// class for database
class TaskRepository(private val dao: TaskDao) {
    suspend fun statsSince(since: Long): Pair<Int,Int> {
        val created = dao.countCreatedSince(since)
        val completed = dao.countCompletedSince(since)
        return created to completed
    }
}

class StatsViewModel(
    private val repo: TaskRepository
): ViewModel() {
    private val _todayStats = MutableStateFlow(0 to 0)  // created, completed
    val todayStats: StateFlow<Pair<Int,Int>> = _todayStats

    private val _weekStats = MutableStateFlow(0 to 0)
    val weekStats: StateFlow<Pair<Int,Int>> = _weekStats
    fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getStartOfWeekMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }


    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfDay = getStartOfTodayMillis()
            val startOfWeek = getStartOfWeekMillis()

            _todayStats.value = repo.statsSince(startOfDay)
            _weekStats.value = repo.statsSince(startOfWeek)
        }
    }

    fun completionRate(created: Int, completed: Int): Float =
        if (created == 0) 0f else completed.toFloat() / created
}



@Composable
fun StatsCard(
    label: String,
    created: Int,
    completed: Int,
    rate: Float
) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Created: $created")
            Text("Completed: $completed")
            Text("Completion Rate: ${String.format("%.0f%%", rate * 100)}")
        }
    }
}

@Composable
fun Dashboard(viewModel: StatsViewModel = viewModel()) {
    val today by viewModel.todayStats.collectAsState()
    val week by viewModel.weekStats.collectAsState()

    Column {
        StatsCard(
            label = "Today",
            created = today.first,
            completed = today.second,
            rate = viewModel.completionRate(today.first, today.second)
        )
        StatsCard(
            label = "This Week",
            created = week.first,
            completed = week.second,
            rate = viewModel.completionRate(week.first, week.second)
        )
    }
}
// class for new API

class NewsViewModel : ViewModel() {
    var newsList by mutableStateOf<List<Article>>(emptyList())
    var isLoading by mutableStateOf(true)

    private val repository = NewsRepository(RetrofitInstance.api)
    private val apiKey = "d34a07b22ad04916939f74795dae3ed7"

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = repository.getNews(apiKey)
                newsList = response.articles
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
// news page layout

@Composable
fun NewsCard(article: Article) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { /* Open article.url in browser */ },
        elevation = CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            article.urlToImage?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(text = article.title, fontWeight = FontWeight.Bold)
            article.description?.let {
                Text(text = it, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = "Published: ${article.publishedAt}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// new page layout
@Composable
fun NewsApp(viewModel: NewsViewModel = viewModel()) {
    val articles = viewModel.newsList
     val loading = viewModel.isLoading


        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(articles) { article ->
                    NewsCard(article)
                }
            }
        }
    }











