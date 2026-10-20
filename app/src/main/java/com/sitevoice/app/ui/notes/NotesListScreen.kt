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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val icon: String
)

val CATEGORY_OPTIONS = listOf(
    CategoryOption(NoteClassifier.LABEL_THI_CONG, "Thi công", "🏗️"),
    CategoryOption(NoteClassifier.LABEL_NGHIEM_THU, "Nghiệm thu", "📐"),
    CategoryOption(NoteClassifier.LABEL_AN_TOAN, "An toàn & Vệ sinh", "🦺"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_CDT, "Ý kiến Chủ đầu tư", "🏛️"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_TVGS, "Ý kiến TVGS", "📋"),
    CategoryOption(NoteClassifier.LABEL_Y_KIEN_NHA_THAU, "Ý kiến Nhà thầu", "🏢"),
    CategoryOption(NoteClassifier.LABEL_CHUA_PHAN_LOAI, "Chưa phân loại", "❓")
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
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Tiêu đề và tổng số lượng ghi chú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.notes_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = stringResource(id = R.string.notes_count, notes.size),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            if (notes.isEmpty()) {
                // Trạng thái trống (Empty State)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📝",
                                fontSize = 28.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(id = R.string.notes_empty_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.notes_empty_desc),
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Danh sách ghi chú cuộn mượt
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
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

        // Dialog sửa văn bản
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

        // Dialog xác nhận xóa
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header item: Giờ ghi + Label Badge hoặc Queue Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                when {
                    note.error != null -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_error),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    note.status == "RECORDED" -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_waiting),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                    note.status == "TRANSCRIBED" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(id = R.string.queue_badge_classifying),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                    else -> {
                        // Nhãn mục có thể bấm để đổi khi đã hoàn thành phân loại
                        val currentCategory = CATEGORY_OPTIONS.find { it.key == note.label }
                            ?: CategoryOption(NoteClassifier.LABEL_CHUA_PHAN_LOAI, stringResource(id = R.string.notes_unclassified), "❓")

                        val isUnclassified = note.label.isNullOrBlank() ||
                                note.label == NoteClassifier.LABEL_CHUA_PHAN_LOAI ||
                                note.label == "unclassified"

                        val labelBg = if (isUnclassified) Color(0xFFF1F5F9) else Color(0xFFEFF6FF)
                        val labelColor = if (isUnclassified) Color(0xFF64748B) else Color(0xFF1D4ED8)

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(labelBg, RoundedCornerShape(6.dp))
                                    .clickable { showLabelMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${currentCategory.icon} ${currentCategory.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
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
                                                text = "${option.icon} ${option.title}",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Nội dung văn bản hoặc trạng thái STT / hàng đợi
            when {
                note.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Lỗi nhận diện: ${note.error}",
                            fontSize = 13.sp,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onRetryStt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.notes_retry_stt),
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
                note.text.isNullOrBlank() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.queue_status_waiting_network),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.queue_status_audio_saved_hint),
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = note.text,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer item: Nghe lại, Sửa, Xóa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút nghe lại âm thanh gốc
                OutlinedButton(
                    onClick = onPlayToggle,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPlaying) "■ Dừng" else "▶ ${durationSec}s",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Nút Sửa
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_edit),
                            fontSize = 12.sp,
                            color = Color(0xFF2563EB)
                        )
                    }

                    // Nút Xóa (Soft delete)
                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_delete),
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}
