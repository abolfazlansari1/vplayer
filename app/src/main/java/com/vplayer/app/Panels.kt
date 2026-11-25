package com.vplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SidePanel(
    title: String,
    widthFraction: Float,
    maxWidthDp: Float,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cfg = LocalConfiguration.current
    val w = minOf(cfg.screenWidthDp * widthFraction, maxWidthDp).dp
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(w)
                .background(PanelBg)
                .pointerInput(Unit) { detectTapGestures { } }
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (selected) Accent else Color.White.copy(alpha = 0.2f))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = if (enabled) TextPrimary else TextSecondary,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!enabled) {
            Text("پشتیبانی نمی‌شود", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 15.sp)
    }
}

@Composable
fun SubtitlePanel(
    textOff: Boolean,
    embeddedOptions: List<TrackOption>,
    siblings: List<File>,
    extName: String?,
    onOff: () -> Unit,
    onPickSibling: (File) -> Unit,
    onPickFile: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
) {
    SidePanel("زیرنویس", 0.6f, 380f, onClose) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OptionRow("خاموش", textOff) { onOff() }
            if (embeddedOptions.isNotEmpty()) {
                SectionLabel("زیرنویس‌های داخل فایل")
                embeddedOptions.forEach { o ->
                    OptionRow(o.label, o.selected, o.supported) { o.onClick() }
                }
            }
            if (siblings.isNotEmpty()) {
                SectionLabel("فایل‌های کنار ویدیو")
                siblings.forEach { f ->
                    OptionRow(f.name, extName == f.name) { onPickSibling(f) }
                }
            } else if (extName != null) {
                SectionLabel("زیرنویس خارجی")
                OptionRow(extName, true) { }
            }
            Spacer(Modifier.height(8.dp))
            ActionRow("انتخاب فایل زیرنویس…") { onPickFile() }
            ActionRow("ویرایش زیرنویس") { onEdit() }
            ActionRow("ظاهر و تأخیر زیرنویس") { onSettings() }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
fun OptionsPanel(
    title: String,
    options: List<TrackOption>,
    emptyText: String,
    onClose: () -> Unit,
) {
    SidePanel(title, 0.6f, 380f, onClose) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (options.isEmpty()) {
                Text(emptyText, color = TextSecondary, fontSize = 14.sp)
            }
            options.forEach { o ->
                OptionRow(o.label, o.selected, o.supported) { o.onClick() }
            }
        }
    }
}

@Composable
fun SpeedPanel(speed: Float, onSpeed: (Float) -> Unit, onClose: () -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f)
    SidePanel("سرعت پخش", 0.5f, 300f, onClose) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            speeds.forEach { s ->
                OptionRow(String.format(Locale.US, "%.2fx", s), abs1(s - speed)) { onSpeed(s) }
            }
        }
    }
}

private fun abs1(d: Float): Boolean = d > -0.001f && d < 0.001f

@Composable
fun SubSettingsPanel(
    hasExternal: Boolean,
    delay: Long,
    onDelay: (Long) -> Unit,
    textSize: Float,
    onSize: (Float) -> Unit,
    bold: Boolean,
    onBold: (Boolean) -> Unit,
    color: Int,
    onColor: (Int) -> Unit,
    bg: Float,
    onBg: (Float) -> Unit,
    bottom: Float,
    onBottom: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val colors = listOf(
        0xFFFFFFFF.toInt(),
        0xFFFFEB3B.toInt(),
        0xFF4DD0E1.toInt(),
        0xFF9CCC65.toInt(),
        0xFFFF8A65.toInt(),
    )
    SidePanel("ظاهر و تأخیر زیرنویس", 0.6f, 400f, onClose) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // delay
            Text("تأخیر زیرنویس", color = TextSecondary, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton("-0.5") { onDelay(delay - 500L) }
                StepButton("-0.1") { onDelay(delay - 100L) }
                Text(
                    String.format(Locale.US, "%+.1f s", delay / 1000.0),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                StepButton("+0.1") { onDelay(delay + 100L) }
                StepButton("+0.5") { onDelay(delay + 500L) }
            }
            if (!hasExternal) {
                Text(
                    "تأخیر فقط برای زیرنویس خارجی (فایل srt/ass/vtt) کار می‌کند.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            } else {
                ActionRow("بازنشانی تأخیر") { onDelay(0L) }
            }

            Text("اندازه متن: " + textSize.roundToInt(), color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = textSize,
                onValueChange = onSize,
                valueRange = 12f..48f,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
            )

            Text("فاصله از پایین: " + bottom.roundToInt(), color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = bottom,
                onValueChange = onBottom,
                valueRange = 0f..140f,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
            )

            Text("شفافیت پس‌زمینه: " + (bg * 100).roundToInt() + "%", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = bg,
                onValueChange = onBg,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("متن پررنگ", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = bold,
                    onCheckedChange = onBold,
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                )
            }

            Text("رنگ متن", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .clickable { onColor(c) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (c == color) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
fun EditorPanel(
    hasExternal: Boolean,
    name: String,
    cues: SnapshotStateList<SubCue>,
    version: Int,
    onShiftAll: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onPickFile: () -> Unit,
    onExport: () -> Unit,
    onSaveBeside: () -> Unit,
    onClose: () -> Unit,
) {
    SidePanel("ویرایش زیرنویس", 0.7f, 520f, onClose) {
        if (!hasExternal) {
            Text(
                "برای ویرایش، ابتدا یک فایل زیرنویس (srt / ass / vtt) انتخاب کنید. زیرنویس‌های داخل فایل mkv قابل ویرایش نیستند، اما قابل نمایش‌اند.",
                color = TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            ActionRow("انتخاب فایل زیرنویس…") { onPickFile() }
        } else {
            EditorBody(name, cues, version, onShiftAll, onSeek, onPickFile, onExport, onSaveBeside)
        }
    }
}

@Composable
private fun ColumnScope.EditorBody(
    name: String,
    cues: SnapshotStateList<SubCue>,
    version: Int,
    onShiftAll: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onPickFile: () -> Unit,
    onExport: () -> Unit,
    onSaveBeside: () -> Unit,
) {
    run {
        var shiftText by remember { mutableStateOf("0") }
        Text(name + "  (" + cues.size + ")", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("جابجایی همه (ثانیه)", color = TextPrimary, fontSize = 13.sp)
            BasicTextField(
                value = shiftText,
                onValueChange = { shiftText = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier
                    .width(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Card)
                    .padding(8.dp),
            )
            StepButton("اعمال") {
                val s = shiftText.trim().replace(',', '.').toDoubleOrNull()
                if (s != null) onShiftAll((s * 1000).toLong())
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton("ذخیره به عنوان…") { onExport() }
            StepButton("ذخیره کنار ویدیو") { onSaveBeside() }
            StepButton("فایل دیگر…") { onPickFile() }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(cues.size, key = { cues[it].id }) { i ->
                val cue = cues[i]
                CueRow(
                    cue = cue,
                    number = i + 1,
                    version = version,
                    onChange = { updated ->
                        val idx = cues.indexOfFirst { it.id == cue.id }
                        if (idx >= 0) cues[idx] = updated
                    },
                    onDelete = {
                        val idx = cues.indexOfFirst { it.id == cue.id }
                        if (idx >= 0) cues.removeAt(idx)
                    },
                    onSeek = { onSeek(cue.startMs) },
                )
            }
        }
    }
}

@Composable
private fun CueRow(
    cue: SubCue,
    number: Int,
    version: Int,
    onChange: (SubCue) -> Unit,
    onDelete: () -> Unit,
    onSeek: () -> Unit,
) {
    var startText by remember(cue.id, version) { mutableStateOf(SubtitleIO.fmt(cue.startMs)) }
    var endText by remember(cue.id, version) { mutableStateOf(SubtitleIO.fmt(cue.endMs)) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$number", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp))
            TimeField(startText) { v ->
                startText = v
                val t = SubtitleIO.parseTime(v)
                if (t != null) onChange(cue.copy(startMs = t))
            }
            Text("→", color = TextSecondary, fontSize = 12.sp)
            TimeField(endText) { v ->
                endText = v
                val t = SubtitleIO.parseTime(v)
                if (t != null) onChange(cue.copy(endMs = t))
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent.copy(alpha = 0.25f))
                    .clickable { onSeek() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("پخش", color = TextPrimary, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = cue.text,
            onValueChange = { onChange(cue.copy(text = it)) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(8.dp),
        )
    }
}

@Composable
private fun TimeField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier
            .width(98.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
    )
}
