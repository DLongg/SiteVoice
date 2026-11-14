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
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

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
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            // Header: Tiêu đề và tổng số lượng ghi chú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.notes_title),
                    fontSize = TextSizes.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(id = R.string.notes_count, notes.size),
                    fontSize = TextSizes.body,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.padding(Spacing.xl)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📝",
                                fontSize = TextSizes.display
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.lg))

                        Text(
                            text = stringResource(id = R.string.notes_empty_title),
                            fontSize = TextSizes.subtitle,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Text(
                            text = stringResource(id = R.string.notes_empty_desc),
                            fontSize = TextSizes.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = TextSizes.bodyLine,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Danh sách ghi chú cuộn mượt
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(bottom = Spacing.xl)
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Header item: Giờ ghi + Label Badge hoặc Queue Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    fontSize = TextSizes.body,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    note.error != null -> {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = Spacing.sm, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_error),
                                fontSize = TextSizes.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    note.status == "RECORDED" -> {
                        Box(
                            modifier = Modifier
                                .background(SiteVoiceTheme.colors.warningContainer, MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = Spacing.sm, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.queue_badge_waiting),
                                fontSize = TextSizes.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = SiteVoiceTheme.colors.warning
                            )
                        }
                    }
                    note.status == "TRANSCRIBED" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = Spacing.sm, vertical = 3.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(id = R.string.queue_badge_classifying),
                                fontSize = TextSizes.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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

                        val labelBg = if (isUnclassified) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        val labelColor = if (isUnclassified) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(labelBg, MaterialTheme.shapes.small)
                                    .clickable { showLabelMenu = true }
                                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                            ) {
                                Text(
                                    text = "${currentCategory.icon} ${currentCategory.title}",
                                    fontSize = TextSizes.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "▾",
                                    fontSize = TextSizes.caption,
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
                                                fontSize = TextSizes.body,
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

            Spacer(modifier = Modifier.height(Spacing.md))

            // Nội dung văn bản hoặc trạng thái STT / hàng đợi
            when {
                note.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .padding(Spacing.md)
                    ) {
                        Text(
                            text = "Lỗi nhận diện: ${note.error}",
                            fontSize = TextSizes.body,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        OutlinedButton(
                            onClick = onRetryStt,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(id = R.string.notes_retry_stt),
                                fontSize = TextSizes.caption,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                note.text.isNullOrBlank() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text(
                                text = stringResource(id = R.string.queue_status_waiting_network),
                                fontSize = TextSizes.body,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = stringResource(id = R.string.queue_status_audio_saved_hint),
                                fontSize = TextSizes.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = note.text,
                        fontSize = TextSizes.body,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = TextSizes.bodyLine
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Footer item: Nghe lại, Sửa, Xóa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút nghe lại âm thanh gốc
                OutlinedButton(
                    onClick = onPlayToggle,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(
                        text = if (isPlaying) "■ Dừng" else "▶ ${durationSec}s",
                        fontSize = TextSizes.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // Nút Sửa
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_edit),
                            fontSize = TextSizes.caption,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Nút Xóa (Soft delete)
                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(id = R.string.notes_delete),
                            fontSize = TextSizes.caption,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
