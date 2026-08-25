package com.mobdysseus.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.data.CalendarStore
import com.mobdysseus.app.data.Event
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(store: CalendarStore) {
    var events by remember { mutableStateOf(store.load()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editing by remember { mutableStateOf(false) }

    fun persist(list: List<Event>) {
        events = list
        store.save(list)
    }

    if (editing) {
        EventEditor(
            initialDate = selectedDate,
            onSave = { title, date, notes ->
                val zone = ZoneId.systemDefault()
                val start = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
                val end = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
                persist(events + Event(store.newId(), title, start, end, notes))
                selectedDate = date
                editing = false
            },
            onCancel = { editing = false },
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Calendar", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(8.dp))

            MonthGrid(
                month = YearMonth.from(selectedDate),
                selectedDate = selectedDate,
                events = events,
                onSelectDate = { selectedDate = it },
            )

            Spacer(Modifier.height(16.dp))

            val dayEvents = events.filter { isOnDay(it, selectedDate) }
            Text(
                "Events — ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                style = MaterialTheme.typography.titleMedium,
            )

            if (dayEvents.isEmpty()) {
                Text(
                    "No events on this day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(dayEvents, key = { it.id }) { event ->
                        EventRow(event)
                    }
                }
            }
        }
    }
}

private fun isOnDay(event: Event, day: LocalDate): Boolean {
    val zone = ZoneId.systemDefault()
    val start = LocalDate.ofInstant(Instant.ofEpochMilli(event.startEpochMs), zone)
    val end = LocalDate.ofInstant(Instant.ofEpochMilli(event.endEpochMs), zone)
    return !day.isBefore(start) && !day.isAfter(end)
}

private fun monthDays(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val days = mutableListOf<LocalDate?>()
    repeat(first.dayOfWeek.value - 1) { days.add(null) }
    for (d in 1..month.lengthOfMonth()) days.add(month.atDay(d))
    return days
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
        ) {
            items(monthDays(month), key = { it?.toEpochDay() ?: Long.MIN_VALUE }) { day ->
                if (day == null) {
                    Spacer(Modifier.size(40.dp))
                } else {
                    DayCell(
                        day = day,
                        hasEvent = events.any { isOnDay(it, day) },
                        selected = day == selectedDate,
                        onClick = { onSelectDate(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    hasEvent: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .size(6.dp)
                .then(
                    if (hasEvent) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
        )
    }
}

@Composable
private fun EventRow(event: Event) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(event.title, style = MaterialTheme.typography.bodyLarge)
        if (event.notes.isNotBlank()) {
            Text(
                event.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventEditor(
    initialDate: LocalDate,
    onSave: (title: String, date: LocalDate, notes: String) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(initialDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var notes by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf(false) }

    val parsedDate = try {
        LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        null
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("New Event", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            label = { Text("Title") },
            singleLine = true,
        )

        OutlinedTextField(
            value = dateText,
            onValueChange = {
                dateText = it
                dateError = false
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            isError = dateError,
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Notes") },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = title.isNotBlank() && parsedDate != null,
                onClick = {
                    val date = parsedDate
                    if (title.isNotBlank() && date != null) {
                        onSave(title.trim(), date, notes.trim())
                    } else {
                        dateError = true
                    }
                },
            ) { Text("Save") }
        }
    }
}
