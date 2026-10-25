package com.sitevoice.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.llm.NoteClassifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryOption(
    val key: String,
    val title: String,
    val code: String
)

val CATEGORY_OPTIONS = listOf(
    CategoryOption(NoteClassifier.LABEL_THI_CONG, "Thi công", "TC"),
    CategoryOption(NoteClassifier.LABEL_NGHIEM_THU, "Nghiệm thu", "NT"),
    CategoryOption(NoteClassifier.LABEL_AN_TOAN, "An toàn & VSMT", "AT"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_CDT, "Ý kiến Chủ đầu tư", "CĐT"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_TVGS, "Ý kiến TVGS", "TVGS"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_NHA_THAU, "Ý kiến Nhà thầu", "NT-YK"),
    CategoryOption(NoteClassifier.LABEL_CHUA_PHAN_LOAI, "Chưa phân loại", "---")
)

@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    audioPlayer: AudioPlayer,
    onUpdateText: (Long, String) -> Unit,
    onUpdateLabel: (Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onRetryStt: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingNoteId by remember { mutableStateOf<Long?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF3EFE6)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Architectural Ledger Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171A))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "NHẬT KÝ GHI ÂM TRONG NGÀY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.notes_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFAF8F3)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF21262B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_count, notes.size).uppercase(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAF8F3)
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF8F3), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFD6CFC2), RoundedCornerShape(4.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "SỔ GHI CHÚ ĐANG TRỐNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.notes_empty_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14171A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.notes_empty_desc),
                            fontSize = 14.sp,
                            color = Color(0xFF5C6268),
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val isPlaying = currentlyPlayingPath == note.audioPath

                        NoteItemCard(
                            note = note,
                            isPlaying = isPlaying,
                            onPlayToggle = {
                                if (isPlaying) {
                                    audioPlayer.stop()
                                    currentlyPlayingPath = null
                                } else {
                                    audioPlayer.play(note.audioPath) {
                                        currentlyPlayingPath = null
                                    }
                                    currentlyPlayingPath = note.audioPath
                                }
                            },
                            onEditClick = { editingNote = note },
                            onDeleteClick = { deletingNoteId = note.id },
                            onRetryStt = { onRetryStt(note) },
                            onSelectLabel = { newLabel -> onUpdateLabel(note.id, newLabel) }
                        )
                    }
                }
            }
        }

        editingNote?.let { note ->
            EditNoteDialog(
                initialText = note.text.orEmpty(),
                onSave = { newText ->
                    onUpdateText(note.id, newText)
                    editingNote = null
                },
                onDismiss = { editingNote = null }
            )
        }

        deletingNoteId?.let { noteId ->
            DeleteConfirmDialog(
                onConfirm = {
                    onDeleteNote(noteId)
                    deletingNoteId = null
                },
                onDismiss = { deletingNoteId = null }
            )
        }
    }
}

@Composable
fun NoteItemCard(
    note: NoteEntity,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryStt: () -> Unit,
    onSelectLabel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(note.createdAt) { timeFormat.format(Date(note.createdAt)) }
    val durationSec = note.durationMs / 1000

    var showLabelMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F3)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6CFC2))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF14171A), RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAF8F3)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${durationSec}s",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF5C6268)
                    )
                }

                when {
                    note.error != null -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFDE8E8), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFF8B4B4), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_error).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9B1C1C)
                            )
                        }
                    }
                    note.status == "RECORDED" -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_waiting).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                    note.status == "TRANSCRIBED" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFEBE5D8), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFFD84315)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.queue_badge_classifying).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14171A)
                            )
                        }
                    }
                    else -> {
                        val currentCategory = CATEGORY_OPTIONS.find { it.key == note.label }
                            ?: CategoryOption(NoteClassifier.LABEL_CHUA_PHAN_LOAI, stringResource(id = R.string.notes_unclassified), "---")

                        val isUnclassified = note.label.isNullOrBlank() ||
                                note.label == NoteClassifier.LABEL_CHUA_PHAN_LOAI ||
                                note.label == "unclassified"

                        val labelBg = if (isUnclassified) Color(0xFFEBE5D8) else Color(0xFF14171A)
                        val labelColor = if (isUnclassified) Color(0xFF5C6268) else Color(0xFFFAF8F3)

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(labelBg, RoundedCornerShape(4.dp))
                                    .clickable { showLabelMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "[${currentCategory.code}] ${currentCategory.title}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 10.sp,
                                    color = labelColor
                                )
                            }

                            DropdownMenu(
                                expanded = showLabelMenu,
                                onDismissRequest = { showLabelMenu = false }
                            ) {
                                CATEGORY_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "[${option.code}] ${option.title}",
                                                fontSize = 13.sp,
                                                fontWeight = if (option.key == note.label) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectLabel(option.key)
                                            showLabelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                note.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFDE8E8), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Lỗi nhận diện: ${note.error}",
                            fontSize = 13.sp,
                            color = Color(0xFF9B1C1C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRetryStt,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.notes_retry_stt),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9B1C1C)
                            )
                        }
                    }
                }
                note.text.isNullOrBlank() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEBE5D8), RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.queue_status_waiting_network),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14171A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.queue_status_audio_saved_hint),
                                fontSize = 11.sp,
                                color = Color(0xFF5C6268)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = note.text,
                        fontSize = 15.sp,
                        color = Color(0xFF14171A),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPlayToggle,
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPlaying) "DỪNG PHÁT" else "PHÁT ÂM THANH GỐC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14171A)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_edit).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14171A)
                        )
                    }

                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_delete).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD84315)
                        )
                    }
                }
            }
        }
    }
}
