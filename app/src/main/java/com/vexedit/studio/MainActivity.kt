package com.vexedit.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vexedit.studio.data.project.ProjectEntity
import com.vexedit.studio.data.project.ProjectRepository
import com.vexedit.studio.data.project.VexDatabase
import com.vexedit.studio.ui.components.VexPanel
import com.vexedit.studio.ui.components.VexPrimaryButton
import com.vexedit.studio.ui.components.VexSecondaryButton
import com.vexedit.studio.ui.components.VexSectionHeader
import com.vexedit.studio.ui.theme.VexDimensions
import com.vexedit.studio.ui.theme.VexEditTheme
import com.vexedit.studio.ui.theme.VexPalette
import com.vexedit.studio.ui.projects.ProjectViewModel
import java.text.DateFormat
import java.util.Date

private enum class AppDestination(val label: String) {
    HOME("Ana Sayfa"), PROJECTS("Projeler"), CREATE("Oluştur"), AUDIO("Ses"), SETTINGS("Ayarlar")
}

private enum class ProjectLayout { GRID, LIST }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ProjectRepository(VexDatabase.getInstance(applicationContext).projectDao())
        setContent {
            VexEditTheme {
                val projectViewModel: ProjectViewModel = viewModel(
                    factory = remember { ProjectViewModel.Factory(repository) }
                )
                VexStudioApp(projectViewModel)
            }
        }
    }
}

@Composable
private fun VexStudioApp(viewModel: ProjectViewModel) {
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var showCreate by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(ProjectLayout.GRID) }
    var search by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var openedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    val projects by viewModel.projects.collectAsState()
    val trash by viewModel.trash.collectAsState()

    fun navigate(target: AppDestination) {
        if (target == AppDestination.CREATE) showCreate = true else destination = target
    }

    Scaffold(
        containerColor = VexPalette.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("VEX", color = VexPalette.Purple, style = MaterialTheme.typography.labelSmall)
                        Text("EDIT STUDIO", color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (destination == AppDestination.PROJECTS) {
                        TextButton(onClick = { destination = AppDestination.SETTINGS }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Çöp kutusu", tint = VexPalette.TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VexPalette.Background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = VexPalette.Panel) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { navigate(item) },
                        icon = {
                            when (item) {
                                AppDestination.HOME -> Icon(Icons.Rounded.Home, contentDescription = item.label)
                                AppDestination.PROJECTS -> Icon(Icons.Rounded.FolderOpen, contentDescription = item.label)
                                AppDestination.CREATE -> Icon(Icons.Rounded.Add, contentDescription = item.label)
                                AppDestination.AUDIO -> Icon(Icons.Rounded.AudioFile, contentDescription = item.label)
                                AppDestination.SETTINGS -> Icon(Icons.Rounded.Settings, contentDescription = item.label)
                            }
                        },
                        label = { Text(item.label, maxLines = 1) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == AppDestination.HOME || destination == AppDestination.PROJECTS) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = VexPalette.Purple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(VexDimensions.CornerMedium)
                ) { Icon(Icons.Rounded.Add, contentDescription = "Yeni proje") }
            }
        }
    ) { padding ->
        when (destination) {
            AppDestination.HOME -> HomeScreen(
                projects = projects,
                padding = padding,
                onCreate = { showCreate = true },
                onOpen = { openedProject = it },
                onAllProjects = { destination = AppDestination.PROJECTS }
            )
            AppDestination.PROJECTS -> ProjectsScreen(
                projects = projects,
                trashCount = trash.size,
                padding = padding,
                layout = layout,
                onLayoutChange = { layout = it },
                search = search,
                onSearchChange = { search = it },
                onOpen = { openedProject = it },
                onRename = { renameTarget = it },
                onDuplicate = { viewModel.duplicate(it.id) },
                onTrash = { viewModel.trash(it.id) },
                onTrashOpen = { destination = AppDestination.SETTINGS }
            )
            AppDestination.AUDIO -> PlaceholderScreen("Audio Studio", "Ses düzenleme araçları sonraki geliştirme aşamasında.", padding)
            AppDestination.SETTINGS -> TrashScreen(
                projects = trash,
                padding = padding,
                onRestore = { viewModel.restore(it.id) },
                onDelete = { viewModel.deleteForever(it.id) }
            )
            AppDestination.CREATE -> Unit
        }
    }

    if (showCreate) {
        NewProjectDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, ratio, resolution, fps ->
                viewModel.create(name, ratio, resolution, fps) { created ->
                    showCreate = false
                    destination = AppDestination.PROJECTS
                    openedProject = created
                }
            }
        )
    }
    renameTarget?.let { project ->
        RenameProjectDialog(
            initialName = project.name,
            onDismiss = { renameTarget = null },
            onConfirm = { viewModel.rename(project.id, it); renameTarget = null }
        )
    }
    openedProject?.let { project ->
        AlertDialog(
            onDismissRequest = { openedProject = null },
            title = { Text(project.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Proje kaydı başarıyla açıldı.")
                    Text("${project.aspectRatio}  •  ${project.resolution}  •  ${project.fps} FPS", color = VexPalette.TextSecondary)
                    Text("Video editörü ve timeline entegrasyonu sonraki aşamada bağlanacak.", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { openedProject = null }) { Text("Tamam") } }
        )
    }
}

@Composable
private fun HomeScreen(
    projects: List<ProjectEntity>,
    padding: PaddingValues,
    onCreate: () -> Unit,
    onOpen: (ProjectEntity) -> Unit,
    onAllProjects: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yaratıcılığını başlat.", style = MaterialTheme.typography.headlineLarge, color = VexPalette.TextPrimary)
                Text("Fikirlerini videoya dönüştür.", style = MaterialTheme.typography.bodyMedium, color = VexPalette.TextSecondary)
                VexPrimaryButton("Yeni Proje Oluştur", onCreate, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
        }
        item {
            VexSectionHeader("Son Projeler", "Kaldığın yerden devam et", trailing = {
                TextButton(onClick = onAllProjects) { Text("Tümünü gör", color = VexPalette.Purple) }
            })
        }
        if (projects.isEmpty()) {
            item {
                EmptyState(
                    title = "Henüz projen yok",
                    message = "İlk projenin ayarlarını belirle ve düzenlemeye başla.",
                    action = "İlk projeyi oluştur",
                    onAction = onCreate
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(projects.take(6), key = { it.id }) { project ->
                        ProjectCard(project = project, compact = true, onOpen = { onOpen(project) })
                    }
                }
            }
        }
        item {
            VexSectionHeader("Hızlı Erişim", "Üretim araçların")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickToolCard("Audio Studio", "Ses düzenleme", Icons.Rounded.AudioFile, Modifier.weight(1f))
                QuickToolCard("Projeler", "Kütüphanen", Icons.Rounded.Folder, Modifier.weight(1f), onAllProjects)
            }
        }
        item {
            VexPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VEX EDIT STUDIO", color = VexPalette.Purple, style = MaterialTheme.typography.labelSmall)
                    Text("Projelerin cihazında saklanır.", color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Çevrimdışı proje yönetimi altyapısı aktif.", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ProjectsScreen(
    projects: List<ProjectEntity>,
    trashCount: Int,
    padding: PaddingValues,
    layout: ProjectLayout,
    onLayoutChange: (ProjectLayout) -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    onOpen: (ProjectEntity) -> Unit,
    onRename: (ProjectEntity) -> Unit,
    onDuplicate: (ProjectEntity) -> Unit,
    onTrash: (ProjectEntity) -> Unit,
    onTrashOpen: () -> Unit
) {
    val filtered = projects.filter { it.name.contains(search, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Projelerim", style = MaterialTheme.typography.headlineMedium, color = VexPalette.TextPrimary)
                Text("${projects.size} proje", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onTrashOpen) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = VexPalette.TextSecondary)
                Text("Çöp kutusu ($trashCount)", color = VexPalette.TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Proje ara") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(VexDimensions.CornerSmall)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.End) {
            FilterChip(selected = layout == ProjectLayout.GRID, onClick = { onLayoutChange(ProjectLayout.GRID) }, label = { Text("Grid") }, leadingIcon = { Icon(Icons.Rounded.GridView, null) })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = layout == ProjectLayout.LIST, onClick = { onLayoutChange(ProjectLayout.LIST) }, label = { Text("Liste") }, leadingIcon = { Icon(Icons.Rounded.ViewList, null) })
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (search.isBlank()) "Proje bulunamadı" else "Aramana uygun proje yok",
                    message = if (search.isBlank()) "Yeni bir proje oluşturarak başlayabilirsin." else "Farklı bir arama terimi dene.",
                    action = null,
                    onAction = {}
                )
            }
        } else if (layout == ProjectLayout.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(filtered, key = { it.id }) { project ->
                    ProjectCard(project, compact = false, onOpen = { onOpen(project) }, onRename = { onRename(project) }, onDuplicate = { onDuplicate(project) }, onTrash = { onTrash(project) })
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(filtered, key = { it.id }) { project ->
                    ProjectListRow(project, onOpen = { onOpen(project) }, onRename = { onRename(project) }, onDuplicate = { onDuplicate(project) }, onTrash = { onTrash(project) })
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    compact: Boolean,
    onOpen: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onTrash: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = (if (compact) Modifier.width(220.dp) else Modifier.fillMaxWidth()).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = VexPalette.Panel),
        shape = RoundedCornerShape(VexDimensions.CornerMedium)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(if (compact) 104.dp else 104.dp).background(VexPalette.PurpleTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = VexPalette.Purple, modifier = Modifier.size(38.dp))
                if (!compact && onRename != null) {
                    Box(Modifier.align(Alignment.TopEnd)) {
                        TextButton(onClick = { menuExpanded = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Proje işlemleri", tint = VexPalette.TextPrimary) }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menuExpanded = false; onRename() })
                            DropdownMenuItem(text = { Text("Kopyala") }, onClick = { menuExpanded = false; onDuplicate?.invoke() })
                            DropdownMenuItem(text = { Text("Çöp kutusuna taşı") }, onClick = { menuExpanded = false; onTrash?.invoke() })
                        }
                    }
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${project.aspectRatio} • ${project.resolution} • ${project.fps} FPS", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text("Düzenlendi: ${formatDate(project.updatedAt)}", color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProjectListRow(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    VexPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.clickable(onClick = onOpen).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(VexPalette.PurpleTint, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = VexPalette.Purple)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${project.aspectRatio} • ${project.resolution} • ${project.fps} FPS", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(formatDate(project.updatedAt), color = VexPalette.TextDisabled, style = MaterialTheme.typography.labelSmall)
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Proje işlemleri") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menuExpanded = false; onRename() })
                    DropdownMenuItem(text = { Text("Kopyala") }, onClick = { menuExpanded = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Çöp kutusuna taşı") }, onClick = { menuExpanded = false; onTrash() })
                }
            }
        }
    }
}

@Composable
private fun TrashScreen(projects: List<ProjectEntity>, padding: PaddingValues, onRestore: (ProjectEntity) -> Unit, onDelete: (ProjectEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Çöp Kutusu", style = MaterialTheme.typography.headlineMedium, color = VexPalette.TextPrimary)
        Text("Buradaki projeler kalıcı olarak silinmedikçe geri yüklenebilir.", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        if (projects.isEmpty()) {
            EmptyState("Çöp kutusu boş", "Silinen projeler burada görünecek.", null, {})
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects, key = { it.id }) { project ->
                    VexPanel(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(project.name, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Text("Silinmiş proje", color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { onRestore(project) }) { Icon(Icons.Rounded.Restore, null); Text("Geri al") }
                            TextButton(onClick = { onDelete(project) }) { Icon(Icons.Rounded.Delete, null, tint = VexPalette.Error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("Yeni Proje") }
    var ratio by remember { mutableStateOf("16:9") }
    var resolution by remember { mutableStateOf("1080p") }
    var fps by remember { mutableStateOf(30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Proje Oluştur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Proje adı") }, singleLine = true)
                Text("En-boy oranı", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("16:9", "9:16", "1:1", "4:5").forEach { value ->
                        FilterChip(selected = ratio == value, onClick = { ratio = value }, label = { Text(value) })
                    }
                }
                Text("Çözünürlük", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("720p", "1080p", "2K", "4K").forEach { value ->
                        FilterChip(selected = resolution == value, onClick = { resolution = value }, label = { Text(value) })
                    }
                }
                Text("Kare hızı", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(24, 30, 60).forEach { value ->
                        FilterChip(selected = fps == value, onClick = { fps = value }, label = { Text("$value FPS") })
                    }
                }
                Text("Bu ayarlar proje hedefidir; dışa aktarma cihaz desteğine göre doğrulanır.", color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, ratio, resolution, fps) }, enabled = name.isNotBlank()) { Text("Oluştur") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun RenameProjectDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Projeyi yeniden adlandır") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Proje adı") }) },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun EmptyState(title: String, message: String, action: String?, onAction: () -> Unit) {
    VexPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = VexPalette.Purple, modifier = Modifier.size(34.dp))
            Text(title, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(message, color = VexPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (action != null) VexSecondaryButton(action, onAction)
        }
    }
}

@Composable
private fun QuickToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    VexPanel(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = VexPalette.Purple, modifier = Modifier.size(24.dp))
            Text(title, color = VexPalette.TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = VexPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
        EmptyState(title, description, null, {})
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
