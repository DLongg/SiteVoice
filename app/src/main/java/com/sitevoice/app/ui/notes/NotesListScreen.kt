package com.sitevoice.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    audioPlayer: AudioPlayer,
    onUpdateText: (Long, String) -> Unit,
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
                            onRetryStt = { onRetryStt(note) }
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
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(note.createdAt) { timeFormat.format(Date(note.createdAt)) }
    val durationSec = note.durationMs / 1000

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header item: Giờ ghi + Label Badge
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

                // Nhãn mục
                val labelText = when (note.label) {
                    null, "", "unclassified", "chua_phan_loai" -> stringResource(id = R.string.notes_unclassified)
                    "thi_cong" -> "Thi công"
                    "nghiem_thu" -> "Nghiệm thu"
                    "an_toan" -> "An toàn"
                    "vat_tu" -> "Vật tư"
                    else -> note.label
                }

                val labelBg = if (note.label.isNullOrBlank() || note.label == "unclassified" || note.label == "chua_phan_loai") {
                    Color(0xFFF1F5F9)
                } else {
                    Color(0xFFEFF6FF)
                }

                val labelColor = if (note.label.isNullOrBlank() || note.label == "unclassified" || note.label == "chua_phan_loai") {
                    Color(0xFF64748B)
                } else {
                    Color(0xFF1D4ED8)
                }

                Box(
                    modifier = Modifier
                        .background(labelBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = labelText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nội dung văn bản hoặc trạng thái STT
            if (note.text.isNullOrBlank()) {
                if (note.error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
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
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF3B82F6)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.notes_transcribing),
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                Text(
                    text = note.text,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    lineHeight = 20.sp
                )
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
