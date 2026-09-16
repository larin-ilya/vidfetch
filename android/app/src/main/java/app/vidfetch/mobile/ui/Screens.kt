package app.vidfetch.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vidfetch.mobile.data.ThemeMode
import app.vidfetch.mobile.ui.theme.Ok
import app.vidfetch.mobile.ui.theme.Warn

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    HomeContent(
        url = vm.url,
        state = vm.state,
        onUrl = vm::onUrl,
        onAnalyze = vm::analyze,
        onDemo = vm::demoUrl,
        onStart = vm::startDownload,
        onReset = vm::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    url: String,
    state: HomeState,
    onUrl: (String) -> Unit,
    onAnalyze: () -> Unit,
    onDemo: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("VidFetch")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Скачать видео или аудио", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Вставьте прямую ссылку http(s):// на файл — приложение проверит её и скачает.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrl,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://…/file.mp3") },
                leadingIcon = { Icon(Icons.Filled.Link, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDemo, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Пример")
                }
                Button(onClick = onAnalyze, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить")
                }
            }

            when (state) {
                is HomeState.Idle -> StatusPill("Готово", Ok)
                is HomeState.Loading -> {
                    StatusPill("Анализ ссылки…", Warn)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Анализ ссылки…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is HomeState.Error -> {
                    StatusPill("Ошибка", MaterialTheme.colorScheme.error)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is HomeState.Ready -> {
                    StatusPill("Готово", Ok)
                    InfoCard(state.title, state.kind.uppercase(), state.sizeText, onStart)
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).padding(0.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(color = color, radius = size.minDimension / 2f)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DownloadsScreen(vm: DownloadsViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    DownloadsContent(
        state = if (items.isEmpty()) ScreenState.EMPTY else ScreenState.CONTENT,
        items = items,
        onAction = { id, action ->
            when (action) {
                "pause" -> vm.pause(id)
                "resume" -> vm.resume(id)
                "cancel" -> vm.cancel(id)
                "delete" -> vm.delete(id)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(state: ScreenState, items: List<DownloadRow>, onAction: (Long, String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Загрузки")
        StateContainer(
            state = state,
            emptyTitle = "Нет активных загрузок",
            emptyText = "Проверьте ссылку на главном экране и начните загрузку.",
            errorText = "Не удалось получить список загрузок.",
            onRetry = {},
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { row ->
                    DownloadCard(row) { onAction(row.id, it) }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    HistoryContent(
        state = if (items.isEmpty()) ScreenState.EMPTY else ScreenState.CONTENT,
        items = items,
        onAction = { id, action -> if (action == "delete") vm.delete(id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(state: ScreenState, items: List<DownloadRow>, onAction: (Long, String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("История")
        StateContainer(
            state = state,
            emptyTitle = "История пуста",
            emptyText = "Здесь появятся завершённые загрузки.",
            errorText = "Не удалось загрузить историю.",
            onRetry = {},
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { row ->
                    DownloadCard(row) { onAction(row.id, it) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val theme by vm.theme.collectAsState()
    val language by vm.language.collectAsState()
    val folder by vm.folder.collectAsState()
    val maxConcurrent by vm.maxConcurrent.collectAsState()
    SettingsContent(
        theme = theme,
        language = language,
        folder = folder,
        maxConcurrent = maxConcurrent,
        onTheme = vm::setTheme,
        onLanguage = vm::setLanguage,
        onConcurrent = vm::setMaxConcurrent,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    theme: ThemeMode,
    language: String,
    folder: String,
    maxConcurrent: Int,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (String) -> Unit,
    onConcurrent: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Настройки")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionTitle("Внешний вид")
            SettingsCard {
                Text("Тема интерфейса", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = theme == mode, onClick = { onTheme(mode) }, label = { Text(mode.label) })
                    }
                }
            }

            SectionTitle("Загрузки")
            SettingsCard {
                InfoRow("Папка", folder)
                HorizontalDivider()
                Text("Одновременных загрузок", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5).forEach { n ->
                        FilterChip(selected = maxConcurrent == n, onClick = { onConcurrent(n) }, label = { Text("$n") })
                    }
                }
            }

            SectionTitle("Язык")
            SettingsCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Русский", "English").forEach { lang ->
                        FilterChip(selected = language == lang, onClick = { onLanguage(lang) }, label = { Text(lang) })
                    }
                }
            }

            SectionTitle("О приложении")
            SettingsCard {
                InfoRow("Приложение", "VidFetch")
                HorizontalDivider()
                InfoRow("Версия", "1.1.0")
                HorizontalDivider()
                InfoRow("Платформа", "Android · Jetpack Compose")
            }
        }
    }
}
