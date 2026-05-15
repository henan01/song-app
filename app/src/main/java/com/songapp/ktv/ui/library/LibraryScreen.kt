package com.songapp.ktv.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.songapp.ktv.KtvApp
import com.songapp.ktv.data.SongFilter
import com.songapp.ktv.data.SongSort
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.components.SongRow
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.ui.theme.primaryGradient
import com.songapp.ktv.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    vm: LibraryViewModel = viewModel(),
    onOpenPlayer: () -> Unit
) {
    val ctx = LocalContext.current
    val songs = vm.songs.collectAsLazyPagingItems()
    val query by vm.query.collectAsState()
    val filter by vm.filter.collectAsState()
    val sort by vm.sort.collectAsState()
    val importing by vm.importing.collectAsState()
    val toast by vm.toast.collectAsState()
    val total by vm.totalCount.collectAsState()
    val nowPlaying by vm.nowPlaying.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortMenuOpen by remember { mutableStateOf(false) }

    val pickFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.importUris(uris) }

    LaunchedEffect(toast) {
        toast?.let {
            scope.launch { snackbar.showSnackbar(it) }
            vm.consumeToast()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickFiles.launch(arrayOf("audio/mpeg", "audio/mp3", "audio/*")) },
                containerColor = NeonPink,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("导入") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            HeaderBar(total = total)
            Spacer(Modifier.height(8.dp))
            SearchBar(
                query = query,
                onQuery = vm::setQuery,
                onSubmit = { vm.rememberSearch() }
            )
            Spacer(Modifier.height(10.dp))
            FilterRow(
                filter = filter,
                onFilter = vm::setFilter,
                sort = sort,
                onSortClick = { sortMenuOpen = true },
                sortMenuOpen = sortMenuOpen,
                onSortDismiss = { sortMenuOpen = false },
                onSortPick = { vm.setSort(it); sortMenuOpen = false }
            )

            nowPlaying.currentSong?.let { np ->
                NowPlayingBar(
                    title = np.title,
                    artist = np.artist,
                    isPlaying = nowPlaying.isPlaying,
                    onTogglePlay = { vm.togglePlayPause() },
                    onOpen = onOpenPlayer
                )
            }

            AnimatedVisibility(visible = importing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在导入…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (songs.itemCount == 0 && songs.loadState.refresh !is LoadState.Loading) {
                EmptyState(onImport = { pickFiles.launch(arrayOf("audio/mpeg", "audio/mp3", "audio/*")) })
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    items(
                        count = songs.itemCount,
                        key = { idx -> songs.peek(idx)?.id ?: idx }
                    ) { idx ->
                        val song = songs[idx] ?: return@items
                        val isCurrent = nowPlaying.currentSong?.id == song.id
                        SongRow(
                            song = song,
                            isCurrentlyPlaying = isCurrent,
                            onPlay = {
                                KtvApp.get().player.playSong(song)
                                onOpenPlayer()
                            },
                            onEnqueue = { vm.enqueue(song) },
                            onToggleFavorite = { vm.toggleFavorite(song) }
                        )
                    }
                    if (songs.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(primaryGradient()),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.Black) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("我的曲库", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$total 首歌  ·  长按行进入更多",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onSubmit: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 28.dp
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("搜索歌名 / 歌手 / 拼音首字母") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = NeonPink
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterRow(
    filter: SongFilter,
    onFilter: (SongFilter) -> Unit,
    sort: SongSort,
    onSortClick: () -> Unit,
    sortMenuOpen: Boolean,
    onSortDismiss: () -> Unit,
    onSortPick: (SongSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChips(filter = filter, onFilter = onFilter, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = onSortClick) {
                Icon(Icons.Filled.Sort, contentDescription = "排序", tint = NeonViolet)
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = onSortDismiss) {
                listOf(
                    SongSort.RECENT to "最近导入",
                    SongSort.TITLE to "歌名",
                    SongSort.ARTIST to "歌手",
                    SongSort.MOST_PLAYED to "最常播放"
                ).forEach { (s, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onSortPick(s) })
                }
            }
        }
    }
}

@Composable
private fun FilterChips(filter: SongFilter, onFilter: (SongFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            SongFilter.ALL to "全部",
            SongFilter.SEPARATED to "已分离",
            SongFilter.PENDING to "未分离",
            SongFilter.FAVORITE to "收藏"
        ).forEach { (f, label) ->
            FilterChip(
                selected = filter == f,
                onClick = { onFilter(f) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonPink.copy(alpha = 0.22f),
                    selectedLabelColor = NeonPink,
                    containerColor = Color.White.copy(alpha = 0.04f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun NowPlayingBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpen: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onOpen),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPink, NeonViolet))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Equalizer, contentDescription = null, tint = Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "正在播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Text(
                    "$title  ·  $artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = NeonViolet
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onImport: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPink.copy(alpha = .35f), NeonCyan.copy(alpha = .35f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "曲库还是空的",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "导入本地 MP3，开启你的演唱会",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("选择 MP3 文件")
            }
        }
    }
}
