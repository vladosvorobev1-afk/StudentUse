package com.example.studentuse

import android.graphics.Bitmap
import android.graphics.Matrix
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.studentuse.pose.PlankAnalyzer
import com.example.studentuse.pose.PosePoint
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun PlankScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val scope = rememberCoroutineScope()
    val repository = remember { WorkoutRepository(context) }

    var landmarks by remember { mutableStateOf<List<PosePoint>?>(null) }
    val analyzer = remember { PlankAnalyzer() }
    var feedback by remember { mutableStateOf<PlankAnalyzer.Feedback?>(null) }

    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(5000)
        showHint = false
    }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }

    var voiceEnabled by remember { mutableStateOf(true) }

    DisposableEffect(ttsReady) {
        if (ttsReady) tts.language = Locale("ru", "RU")
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    var lastSpokenHint by remember { mutableStateOf("") }
    var lastSpokenSecondsBucket by remember { mutableStateOf(0) }
    var lastTargetReached by remember { mutableStateOf(false) }

    val poseLandmarker = remember {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .setDelegate(Delegate.CPU)
            .build()
        PoseLandmarker.createFromOptions(
            context,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                    val firstPose = result.landmarks().firstOrNull()
                    val pts = firstPose?.map { lm -> PosePoint(lm.x(), lm.y(), 1.0f) }
                    landmarks = pts

                    val ts = System.currentTimeMillis()
                    val newFeedback = analyzer.analyze(pts, ts)
                    feedback = newFeedback

                    if (ttsReady && voiceEnabled && newFeedback != null) {

                        // === СЧЁТ СЕКУНД + ВИБРАЦИЯ КАЖДЫЕ 10 СЕК ===
                        if (newFeedback.state == PlankAnalyzer.State.HOLDING) {
                            val bucket = newFeedback.secondsHeld / 10
                            if (bucket > lastSpokenSecondsBucket && bucket > 0) {
                                lastSpokenSecondsBucket = bucket

                                // Вибрация при счёте (каждые 10 сек)
                                if (appSettings.vibrateOnCount) {
                                    vibrateTick(context)
                                }

                                // Озвучка секунд (если включена)
                                if (appSettings.saySeconds) {
                                    val secText = (bucket * 10).toString()
                                    tts.speak(secText, TextToSpeech.QUEUE_ADD, null, "sec")
                                }
                            }
                        }

                        val hint = newFeedback.hint
                        val hintEnabled = when (hint) {
                            "Держи тело ровно" -> appSettings.sayBodyLine
                            else               -> true
                        }
                        if (hintEnabled && hint.isNotBlank() && hint != lastSpokenHint) {
                            lastSpokenHint = hint
                            tts.speak(hint, TextToSpeech.QUEUE_ADD, null, "hint")
                        }
                        if (hint.isBlank()) lastSpokenHint = ""

                        val target = appSettings.targetSeconds
                        if (target > 0 &&
                            newFeedback.secondsHeld >= target &&
                            !lastTargetReached
                        ) {
                            lastTargetReached = true
                            if (appSettings.saySetComplete) {
                                tts.speak("Подход завершён", TextToSpeech.QUEUE_ADD, null, "set_complete")
                            }
                            if (appSettings.vibrateOnSetComplete) {
                                vibrateShort(context)
                            }
                            if (appSettings.sayGood) {
                                tts.speak("Хорошо", TextToSpeech.QUEUE_ADD, null, "good")
                            }
                            scope.launch { repository.savePlank(target) }
                        }
                    }
                }
                .setErrorListener { e -> e.printStackTrace() }
                .build()
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val matrix = Matrix().apply {
                            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        }
                        val bitmap = imageProxy.toBitmap()
                        val rotated = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        val mpImage = BitmapImageBuilder(rotated).build()
                        poseLandmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp / 1000)
                        imageProxy.close()
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, analysis
                        )
                    } catch (e: Exception) { e.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        val pts = landmarks
        if (pts != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val connections = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 7,
                    0 to 4, 4 to 5, 5 to 6, 6 to 8,
                    9 to 10,
                    11 to 12, 11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
                    12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
                    11 to 23, 12 to 24, 23 to 24,
                    23 to 25, 25 to 27, 27 to 29, 29 to 31, 27 to 31,
                    24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32
                )
                connections.forEach { (a, b) ->
                    val pa = pts.getOrNull(a)
                    val pb = pts.getOrNull(b)
                    if (pa != null && pb != null) {
                        drawLine(
                            color = Color.Green,
                            start = Offset(pa.px * w, pa.py * h),
                            end = Offset(pb.px * w, pb.py * h),
                            strokeWidth = 6f
                        )
                    }
                }
                pts.forEach { p ->
                    drawCircle(Color.Yellow, 8f, Offset(p.px * w, p.py * h))
                }
            }
        }

        val fb = feedback
        if (fb != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val target = appSettings.targetSeconds
                val secondsText = if (target > 0) "${fb.secondsHeld} / $target"
                else "${fb.secondsHeld}"

                val timerColor = when (fb.state) {
                    PlankAnalyzer.State.HOLDING   -> Color.Green
                    PlankAnalyzer.State.BROKEN    -> Color.Yellow
                    PlankAnalyzer.State.INVISIBLE -> Color.Gray
                    PlankAnalyzer.State.IDLE      -> Color.White
                }

                Text(
                    text = secondsText,
                    color = timerColor,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val stateText = when (fb.state) {
                    PlankAnalyzer.State.HOLDING   -> "Держишь"
                    PlankAnalyzer.State.BROKEN    -> "Держи тело ровно"
                    PlankAnalyzer.State.INVISIBLE -> "Вне кадра"
                    PlankAnalyzer.State.IDLE      -> "Начни планку"
                }

                Text(
                    text = stateText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC000000))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📐 Встань боком к камере",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📱 Камера на высоте 50–60 см от пола",
                        color = Color.White, fontSize = 14.sp
                    )
                }
            }
        }

        Row(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000), contentColor = Color.White
                )
            ) { Text("←", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    voiceEnabled = !voiceEnabled
                    if (!voiceEnabled) tts.stop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000), contentColor = Color.White
                )
            ) { Text(if (voiceEnabled) "🔊" else "🔇", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000), contentColor = Color.White
                )
            ) { Text("⚙️", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    analyzer.reset()
                    feedback = null
                    landmarks = null
                    lastSpokenHint = ""
                    lastSpokenSecondsBucket = 0
                    lastTargetReached = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000), contentColor = Color.White
                )
            ) { Text("Сброс", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}