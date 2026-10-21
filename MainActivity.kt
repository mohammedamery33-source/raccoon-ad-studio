package com.raccoon.adstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF101216)
private val Panel = Color(0xFF1A1D23)
private val Accent = Color(0xFFD7FF64)
private val Muted = Color(0xFF9AA2AF)

data class Scene(val uri: String, val seconds: Int = 3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 18, 22)
        window.navigationBarColor = android.graphics.Color.rgb(16, 18, 22)
        setContent { StudioScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
private fun StudioScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("raccoon_project", Context.MODE_PRIVATE) }
    var projectName by remember { mutableStateOf(prefs.getString("name", "حملة منتج جديدة") ?: "حملة منتج جديدة") }
    var scenes by remember { mutableStateOf(loadScenes(prefs.getString("scenes", "[]") ?: "[]")) }
    var busy by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("وضع الاستوديو المحلي · لا يحتاج إلى API مدفوع") }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    fun persist() {
        prefs.edit().putString("name", projectName).putString("scenes", scenesToJson(scenes)).apply()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.mapNotNull { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
                Scene(uri.toString(), 3)
            }
            scenes = scenes + added
            persist()
            exportMessage = "تمت إضافة ${added.size} مشهدًا. الصور الأصلية لم تُعدّل."
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White, secondary = Muted
    )) {
        Scaffold(containerColor = Ink, topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RACCOON", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 19.sp)
                        Text("AD STUDIO  /  LOCAL EDITION", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(10.dp))
                Text("استوديو إعلانات المنتجات", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("حوّل صورك إلى فيديو إعلاني متعدد المشاهد", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("المشروع الحالي", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = projectName, onValueChange = { projectName = it; persist() },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("اسم الحملة") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent, focusedLabelColor = Accent,
                                unfocusedBorderColor = Color(0xFF424752), cursorColor = Accent
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.AddPhotoAlternate, null)
                                Spacer(Modifier.width(6.dp)); Text("إضافة صور")
                            }
                            OutlinedButton(onClick = { persist(); exportMessage = "تم حفظ المشروع على هذا الجهاز." }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("حفظ المشروع")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("تسلسل المشاهد", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${scenes.size} مشهد", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))

                if (scenes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = Accent, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("ابدأ بإضافة صور المنتج", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("كل صورة ستصبح مشهدًا مستقلًا في الفيديو", color = Muted, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        itemsIndexed(scenes, key = { index, scene -> "${scene.uri}-$index" }) { index, scene ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AndroidView(
                                        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true } },
                                        update = { image -> try { image.setImageURI(Uri.parse(scene.uri)) } catch (_: Exception) { image.setImageDrawable(null) } },
                                        modifier = Modifier.size(76.dp).background(Color.DarkGray, RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("مشهد ${index + 1}", fontWeight = FontWeight.Bold)
                                        Text("${scene.seconds} ثوانٍ · صورة أصلية", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("المدة", color = Muted, fontSize = 11.sp)
                                            Slider(value = scene.seconds.toFloat(), onValueChange = { value ->
                                                scenes = scenes.toMutableList().also { it[index] = scene.copy(seconds = value.toInt().coerceIn(2, 10)) }
                                                persist()
                                            }, valueRange = 2f..10f, steps = 7, modifier = Modifier.weight(1f))
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = {
                                            if (index > 0) { scenes = scenes.toMutableList().also { val item = it.removeAt(index); it.add(index - 1, item) }; persist() }
                                        }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, "تحريك لأعلى", tint = if (index > 0) Accent else Muted) }
                                        IconButton(onClick = {
                                            if (index < scenes.lastIndex) { scenes = scenes.toMutableList().also { val item = it.removeAt(index); it.add(index + 1, item) }; persist() }
                                        }, enabled = index < scenes.lastIndex) { Icon(Icons.Default.ArrowDownward, "تحريك لأسفل", tint = if (index < scenes.lastIndex) Accent else Muted) }
                                        IconButton(onClick = { scenes = scenes.toMutableList().also { it.removeAt(index) }; persist() }) { Icon(Icons.Default.Delete, "حذف", tint = Color(0xFFFF7777)) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF222A1B)), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(exportMessage, fontSize = 12.sp, color = Color(0xFFE5EBD9))
                            if (scenes.isNotEmpty()) Text("المدة التقريبية: ${scenes.sumOf { it.seconds }} ثانية", fontSize = 11.sp, color = Muted)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        persist()
                        if (scenes.isEmpty()) { exportMessage = "أضف صورة واحدة على الأقل قبل التصدير."; return@Button }
                        busy = true; exportedFile = null; exportMessage = "جارٍ تجهيز الفيديو على الجهاز…"
                        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                        if (!outputDir.exists()) outputDir.mkdirs()
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val safeName = projectName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24).ifBlank { "campaign" }
                        val output = File(outputDir, "Raccoon_${safeName}_$stamp.mp4")
                        try {
                            val items = scenes.map { scene ->
                                val media = MediaItem.Builder().setUri(Uri.parse(scene.uri)).setImageDurationMs(scene.seconds * 1000L).build()
                                EditedMediaItem.Builder(media).setRemoveAudio(true).build()
                            }
                            val sequence = EditedMediaItemSequence.withVideoFrom(items)
                            val composition = Composition.Builder(sequence).build()
                            val transformer = Transformer.Builder(context)
                                .addListener(object : Transformer.Listener {
                                    override fun onCompleted(composition: Composition, result: ExportResult) {
                                        busy = false
                                        if (output.exists() && output.length() > 0L) {
                                            exportedFile = output
                                            exportMessage = "اكتمل التصدير · ${output.length() / 1024} KB · الملف موجود وغير فارغ"
                                            Toast.makeText(context, "تم إنشاء الفيديو: ${output.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            exportedFile = null
                                            exportMessage = "انتهت عملية التصدير لكن ملف الناتج غير صالح أو فارغ. لم يتم تأكيد النجاح."
                                        }
                                    }
                                    override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                                        busy = false; output.delete()
                                        exportMessage = "فشل التصدير: ${exception.errorCodeName}. جرّب صورًا أخرى أو جهازًا متوافقًا."
                                    }
                                }).build()
                            transformer.start(composition, output.absolutePath)
                        } catch (e: Exception) {
                            busy = false; output.delete(); exportMessage = "تعذر بدء التصدير: ${e.localizedMessage ?: "خطأ غير معروف"}"
                        }
                    }, enabled = !busy && scenes.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Ink); Spacer(Modifier.width(10.dp)); Text("جارٍ التصدير…") }
                    else { Icon(Icons.Default.Movie, null); Spacer(Modifier.width(8.dp)); Text("تصدير فيديو MP4", fontWeight = FontWeight.Bold) }
                }
                exportedFile?.let { file ->
                    TextButton(onClick = {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        try { context.startActivity(Intent.createChooser(intent, "مشاركة الفيديو")) } catch (_: Exception) { exportMessage = "الفيديو محفوظ في: ${file.absolutePath}" }
                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("مشاركة الفيديو / فتح خيارات التسليم") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun scenesToJson(scenes: List<Scene>): String = JSONArray().apply {
    scenes.forEach { put(JSONObject().put("uri", it.uri).put("seconds", it.seconds)) }
}.toString()

private fun loadScenes(raw: String): List<Scene> = try {
    val json = JSONArray(raw)
    (0 until json.length()).mapNotNull { index ->
        val obj = json.optJSONObject(index) ?: return@mapNotNull null
        val uri = obj.optString("uri")
        if (uri.isBlank()) null else Scene(uri, obj.optInt("seconds", 3).coerceIn(2, 10))
    }
} catch (_: Exception) { emptyList() }
