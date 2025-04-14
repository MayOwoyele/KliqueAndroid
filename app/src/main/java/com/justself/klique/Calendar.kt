package com.justself.klique

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justself.klique.DiaryViewModel.RequestType
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.SelectedDate._selectedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CalendarBackgroundStyle {
    Primary, Background
}

@Composable
fun CalendarUI(
    onDayClick: (LocalDate) -> Unit,
    calendarBackground: CalendarBackgroundStyle = CalendarBackgroundStyle.Primary,
    allowFutureDates: Boolean = true,
    allEntries: (List<LocalDate>)? = null,
    currentMonthParam: YearMonth = YearMonth.now(),
) {
    var currentMonth by remember { mutableStateOf(currentMonthParam) }
    var showYearSelector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (showYearSelector) {
            val currentYear = currentMonth.year
            val startYear = currentYear - 100
            val endYear = currentYear + 100
            val yearList = (startYear..endYear).toList()
            val listState = rememberLazyListState()
            val itemsCount = remember { mutableIntStateOf(0) }
            var hasScrolled by remember { mutableStateOf(false) }

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
                    .collect { visibleCount ->
                        if (visibleCount > 0) itemsCount.intValue = visibleCount
                    }
            }

            LaunchedEffect(showYearSelector) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
                    .distinctUntilChanged()
                    .collect { visibleCount ->
                        if (showYearSelector && visibleCount > 0 && !hasScrolled) {
                            val currentYearIndex = yearList.indexOf(currentYear)
                            if (currentYearIndex >= 0) {
                                val divided = visibleCount / 2
                                val indexToScrollTo = currentYearIndex - divided
                                listState.animateScrollToItem(indexToScrollTo.coerceAtLeast(0))
                                hasScrolled = true
                            }
                        }
                    }
            }

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(yearList) { year ->
                    Text(
                        text = year.toString(),
                        modifier = Modifier
                            .clickable {
                                currentMonth = YearMonth.of(year, currentMonth.month)
                                showYearSelector = false
                            }
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "< Prev",
                    modifier = Modifier.clickable { currentMonth = currentMonth.minusMonths(1) },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${
                        currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    } ${currentMonth.year}",
                    modifier = Modifier.clickable { showYearSelector = true },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Next >",
                    modifier = Modifier.clickable { currentMonth = currentMonth.plusMonths(1) },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayNames.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                val targetMonthValue = targetState.year * 12 + targetState.monthValue
                val initialMonthValue = initialState.year * 12 + initialState.monthValue
                if (targetMonthValue > initialMonthValue) {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    ) +
                            fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300)
                        ) +
                                fadeOut(animationSpec = tween(300))
                    )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    ) +
                            fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) +
                                fadeOut(animationSpec = tween(300))
                    )
                }.using(SizeTransform(false))
            }
        ) { month ->
            val firstDay = month.atDay(1)
            val startOffset = firstDay.dayOfWeek.value % 7
            val totalDays = month.lengthOfMonth()
            val totalCells = startOffset + totalDays
            val daysList: List<LocalDate?> = (1..totalCells).map { index ->
                if (index <= startOffset) null else month.atDay(index - startOffset)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(daysList.size) { index ->
                    val date = daysList[index]
                    if (date != null) {
                        val isFuture = date.isAfter(LocalDate.now())
                        val allowedDatesInNoFuture = !allowFutureDates && !isFuture
                        val clickableModifier = if (allowFutureDates || (allowedDatesInNoFuture)) {
                            Modifier.clickable { onDayClick(date) }
                        } else {
                            Modifier
                        }
                        val cellBackground = if (date == LocalDate.now()) {
                            if (calendarBackground == CalendarBackgroundStyle.Primary)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary
                        } else {
                            if (calendarBackground == CalendarBackgroundStyle.Primary)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.background
                        }
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .then(clickableModifier)
                                .clip(RoundedCornerShape(24.dp))
                                .background(cellBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            val dayColor = if (date == LocalDate.now()) {
                                MaterialTheme.colorScheme.background
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = dayColor
                            )
                            if (allEntries != null && date in allEntries) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dayColor)
                                )
                            }
                            if (!allowFutureDates && isFuture) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Gray.copy(alpha = 0.5f))
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                }
            }
        }
        if (!allowFutureDates) {
            Button(onClick = { onDayClick(LocalDate.now()) }) {
                Text("Jump to today")
            }
        }
    }
}

class DiaryViewModel : ViewModel() {
    private val _diaryEntry = MutableStateFlow<DiaryEntry?>(null)
    val diaryEntry: StateFlow<DiaryEntry?> = _diaryEntry
    private val diaryEntryDao = DatabaseProvider.getDiaryDatabase(appContext).diaryEntryDao()

    enum class RequestType(val value: String) {
        Restoration("restoration"),
        Syncing("syncing"),
        Deletion("deletion")
    }

    init {
        loadEntry(_selectedDate.value)
        shouldRestoreDiary()
    }

    fun loadEntry(date: LocalDate) {
        viewModelScope.launch {
            _diaryEntry.value = diaryEntryDao.getEntryByDate(date)
            _selectedDate.value = date
        }
    }

    private fun shouldRestoreDiary() {
        if (UserAppSettings.shouldRestoreDiary(appContext)) {
            restoreServerBackup()
        }
    }

    private fun restoreServerBackup() {
        viewModelScope.launch {
            try {
                val action: suspend (NetworkUtils.JwtTriple) -> Unit = { theTriple ->
                    val info = JSONArray(theTriple.toNetworkTriple().second)
                    val list = mutableListOf<DiaryEntry>()
                    for (i in 0 until info.length()) {
                        val item = info.getJSONObject(i)
                        val content = item.getString("content")
                        val timestamp = item.getLong("timestamp")
                        val serverLocalDate = item.getString("localDate")
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                        val date = LocalDate.parse(serverLocalDate, formatter)
                        val currentTimestamp = System.currentTimeMillis()
                        val diaryObject = DiaryEntry(
                            date = date,
                            content = content,
                            currentTimestamp,
                            currentTimestamp,
                            timestampMillis = timestamp
                        )
                        list.add(diaryObject)
                    }
                    diaryEntryDao.insertDiaryEntries(list)
                    UserAppSettings.setShouldRestoreDiary(appContext, false)
                }
                val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {}
                networkRequest(JSONObject(), RequestType.Restoration, action, errorAction)
            } catch (e: Exception) {
                Log.e("DiaryViewModel", "Error restoring diary", e)
            }
        }
    }

    private fun saveEntry() {
        viewModelScope.launch {
            val entry = _diaryEntry.value?.let {
                val currentTimestamp = System.currentTimeMillis()
                DiaryEntry(
                    date = _selectedDate.value,
                    content = it.content,
                    lastModified = currentTimestamp,
                    timestampMillis = currentTimestamp,
                )
            }
            if (entry != null) {
                diaryEntryDao.upsertEntry(entry)
            }
        }
    }

    private fun updateEntryContent(content: String) {
        viewModelScope.launch {
            _diaryEntry.value?.let {
                _diaryEntry.value = it.copy(content = content)
            } ?: run {
                _diaryEntry.value = DiaryEntry(date = _selectedDate.value, content = content)
            }
        }
    }

    private var saveJob: Job? = null
    fun onTextChanged(newContent: String) {
        updateEntryContent(newContent)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(3000)
            saveEntry()
        }
    }

    fun loadAllDatesEntries(): Flow<List<LocalDate>> {
        return diaryEntryDao.getAllEntryDates()
    }

    fun delete(date: LocalDate) {
        viewModelScope.launch {
            val listOfJson = listOf(JSONObject().put("date", date))
            val action: suspend (NetworkUtils.JwtTriple) -> Unit = {
                diaryEntryDao.deleteEntries(listOf(date))
            }
            val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Error deleting entry maybe it's internet",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            constructDeletionJsonAndSend(listOfJson, action, errorAction)
        }
    }
}

object SelectedDate {
    val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate
}

suspend fun networkRequest(
    json: JSONObject,
    request: RequestType,
    action: suspend (NetworkUtils.JwtTriple) -> Unit,
    errorAction: suspend (NetworkUtils.JwtTriple) -> Unit
) {
    val theJson = json.put("type", request.value)
    NetworkUtils.makeJwtRequest(
        "diaryEndpoint",
        KliqueHttpMethod.POST,
        emptyMap(),
        theJson.toString(),
        action = action,
        errorAction = errorAction
    )
}