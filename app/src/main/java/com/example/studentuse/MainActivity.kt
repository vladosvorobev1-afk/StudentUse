package com.example.studentuse

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Scaffold
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
import com.example.studentuse.pose.PosePoint
import com.example.studentuse.pose.SquatAnalyzer
import com.example.studentuse.ui.theme.StudentUseTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings = remember { AppSettings(applicationContext) }

            var themeMode by remember { mutableStateOf(appSettings.themeMode) }
            var vibrateOnCount by remember { mutableStateOf(appSettings.vibrateOnCount) }

            StudentUseTheme(themeMode = themeMode) {

                var selectedExercise by remember {
                    mutableStateOf<Exercise?>(null)
                }

                var showHistory by remember { mutableStateOf(false) }
                var showAppSettings by remember { mutableStateOf(false) }

                if (showAppSettings) {
                    SettingsAppScreen(
                        currentThemeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            appSettings.themeMode = it
                        },
                        vibrateOnCount = vibrateOnCount,
                        onVibrateOnCountChange = {
                            vibrateOnCount = it
                            appSettings.vibrateOnCount = it
                        },
                        onBack = { showAppSettings = false }
                    )
                    return@StudentUseTheme
                }

                if (showHistory) {
                    HistoryScreen(
                        onBack = { showHistory = false }
                    )
                    return@StudentUseTheme
                }

                when (val exercise = selectedExercise) {
                    null -> ExercisePickerScreen(
                        onExerciseSelected = { selectedExercise = it },
                        onHistoryClick = { showHistory = true },
                        onAppSettingsClick = { showAppSettings = true }
                    )

                    Exercise.SQUAT -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            var showSettings by remember { mutableStateOf(false) }

                            var targetReps by remember { mutableStateOf(appSettings.targetReps) }
                            var saySetComplete by remember {
                                mutableStateOf(appSettings.saySetComplete)
                            }
                            var vibrateOnSetComplete by remember {
                                mutableStateOf(appSettings.vibrateOnSetComplete)
                            }
                            var sayBack by remember { mutableStateOf(appSettings.sayBack) }
                            var sayHeels by remember { mutableStateOf(appSettings.sayHeels) }
                            var sayDeeper by remember { mutableStateOf(appSettings.sayDeeper) }
                            var sayGood by remember { mutableStateOf(appSettings.sayGood) }

                            if (showSettings) {
                                SettingsScreen(
                                    targetReps = targetReps,
                                    onTargetRepsChange = {
                                        targetReps = it
                                        appSettings.targetReps = it
                                    },
                                    saySetComplete = saySetComplete,
                                    onSaySetCompleteChange = {
                                        saySetComplete = it
                                        appSettings.saySetComplete = it
                                    },
                                    vibrateOnSetComplete = vibrateOnSetComplete,
                                    onVibrateOnSetCompleteChange = {
                                        vibrateOnSetComplete = it
                                        appSettings.vibrateOnSetComplete = it
                                    },
                                    sayBack = sayBack,
                                    onSayBackChange = {
                                        sayBack = it
                                        appSettings.sayBack = it
                                    },
                                    sayHeels = sayHeels,
                                    onSayHeelsChange = {
                                        sayHeels = it
                                        appSettings.sayHeels = it
                                    },
                                    sayDeeper = sayDeeper,
                                    onSayDeeperChange = {
                                        sayDeeper = it
                                        appSettings.sayDeeper = it
                                    },
                                    sayGood = sayGood,
                                    onSayGoodChange = {
                                        sayGood = it
                                        appSettings.sayGood = it
                                    },
                                    onBack = { showSettings = false },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            } else {
                                CameraScreen(
                                    onOpenSettings = { showSettings = true },
                                    onBack = { selectedExercise = null },
                                    appSettings = appSettings,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }

                    Exercise.PUSHUP -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            var showSettings by remember { mutableStateOf(false) }

                            var targetReps by remember { mutableStateOf(appSettings.targetReps) }
                            var saySetComplete by remember {
                                mutableStateOf(appSettings.saySetComplete)
                            }
                            var vibrateOnSetComplete by remember {
                                mutableStateOf(appSettings.vibrateOnSetComplete)
                            }
                            var sayBack by remember { mutableStateOf(appSettings.sayBack) }
                            var sayDeeper by remember { mutableStateOf(appSettings.sayDeeper) }
                            var sayGood by remember { mutableStateOf(appSettings.sayGood) }

                            if (showSettings) {
                                PushupSettingsScreen(
                                    targetReps = targetReps,
                                    onTargetRepsChange = {
                                        targetReps = it
                                        appSettings.targetReps = it
                                    },
                                    saySetComplete = saySetComplete,
                                    onSaySetCompleteChange = {
                                        saySetComplete = it
                                        appSettings.saySetComplete = it
                                    },
                                    vibrateOnSetComplete = vibrateOnSetComplete,
                                    onVibrateOnSetCompleteChange = {
                                        vibrateOnSetComplete = it
                                        appSettings.vibrateOnSetComplete = it
                                    },
                                    sayBodyLine = sayBack,
                                    onSayBodyLineChange = {
                                        sayBack = it
                                        appSettings.sayBack = it
                                    },
                                    sayDeeper = sayDeeper,
                                    onSayDeeperChange = {
                                        sayDeeper = it
                                        appSettings.sayDeeper = it
                                    },
                                    sayGood = sayGood,
                                    onSayGoodChange = {
                                        sayGood = it
                                        appSettings.sayGood = it
                                    },
                                    onBack = { showSettings = false },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            } else {
                                PushupCameraScreen(
                                    onOpenSettings = { showSettings = true },
                                    onBack = { selectedExercise = null },
                                    appSettings = appSettings,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }

                    Exercise.PLANK -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            var showSettings by remember { mutableStateOf(false) }

                            var targetSeconds by remember {
                                mutableStateOf(appSettings.targetSeconds)
                            }
                            var saySetComplete by remember {
                                mutableStateOf(appSettings.saySetComplete)
                            }
                            var vibrateOnSetComplete by remember {
                                mutableStateOf(appSettings.vibrateOnSetComplete)
                            }
                            var sayBodyLine by remember { mutableStateOf(appSettings.sayBodyLine) }
                            var saySeconds by remember { mutableStateOf(appSettings.saySeconds) }
                            var sayGood by remember { mutableStateOf(appSettings.sayGood) }

                            if (showSettings) {
                                PlankSettingsScreen(
                                    targetSeconds = targetSeconds,
                                    onTargetSecondsChange = {
                                        targetSeconds = it
                                        appSettings.targetSeconds = it
                                    },
                                    saySetComplete = saySetComplete,
                                    onSaySetCompleteChange = {
                                        saySetComplete = it
                                        appSettings.saySetComplete = it
                                    },
                                    vibrateOnSetComplete = vibrateOnSetComplete,
                                    onVibrateOnSetCompleteChange = {
                                        vibrateOnSetComplete = it
                                        appSettings.vibrateOnSetComplete = it
                                    },
                                    sayBodyLine = sayBodyLine,
                                    onSayBodyLineChange = {
                                        sayBodyLine = it
                                        appSettings.sayBodyLine = it
                                    },
                                    saySeconds = saySeconds,
                                    onSaySecondsChange = {
                                        saySeconds = it
                                        appSettings.saySeconds = it
                                    },
                                    sayGood = sayGood,
                                    onSayGoodChange = {
                                        sayGood = it
                                        appSettings.sayGood = it
                                    },
                                    onBack = { showSettings = false },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            } else {
                                PlankCameraScreen(
                                    onOpenSettings = { showSettings = true },
                                    onBack = { selectedExercise = null },
                                    appSettings = appSettings,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }

                    Exercise.LUNGE -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            var showSettings by remember { mutableStateOf(false) }

                            var targetReps by remember { mutableStateOf(appSettings.targetReps) }
                            var saySetComplete by remember {
                                mutableStateOf(appSettings.saySetComplete)
                            }
                            var vibrateOnSetComplete by remember {
                                mutableStateOf(appSettings.vibrateOnSetComplete)
                            }
                            var sayBack by remember { mutableStateOf(appSettings.sayBack) }
                            var sayDeeper by remember { mutableStateOf(appSettings.sayDeeper) }
                            var sayGood by remember { mutableStateOf(appSettings.sayGood) }

                            if (showSettings) {
                                LungeSettingsScreen(
                                    targetReps = targetReps,
                                    onTargetRepsChange = {
                                        targetReps = it
                                        appSettings.targetReps = it
                                    },
                                    saySetComplete = saySetComplete,
                                    onSaySetCompleteChange = {
                                        saySetComplete = it
                                        appSettings.saySetComplete = it
                                    },
                                    vibrateOnSetComplete = vibrateOnSetComplete,
                                    onVibrateOnSetCompleteChange = {
                                        vibrateOnSetComplete = it
                                        appSettings.vibrateOnSetComplete = it
                                    },
                                    sayBack = sayBack,
                                    onSayBackChange = {
                                        sayBack = it
                                        appSettings.sayBack = it
                                    },
                                    sayDeeper = sayDeeper,
                                    onSayDeeperChange = {
                                        sayDeeper = it
                                        appSettings.sayDeeper = it
                                    },
                                    sayGood = sayGood,
                                    onSayGoodChange = {
                                        sayGood = it
                                        appSettings.sayGood = it
                                    },
                                    onBack = { showSettings = false },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            } else {
                                LungeCameraScreen(
                                    onOpenSettings = { showSettings = true },
                                    onBack = { selectedExercise = null },
                                    appSettings = appSettings,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ ОБЁРТКИ ============

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    if (cameraPermission.status.isGranted) {
        PoseScreen(
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            appSettings = appSettings,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Разрешить камеру")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PushupCameraScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    if (cameraPermission.status.isGranted) {
        PushupScreen(
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            appSettings = appSettings,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Разрешить камеру")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PlankCameraScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    if (cameraPermission.status.isGranted) {
        PlankScreen(
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            appSettings = appSettings,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Разрешить камеру")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LungeCameraScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    if (cameraPermission.status.isGranted) {
        LungeScreen(
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            appSettings = appSettings,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Разрешить камеру")
            }
        }
    }
}

// ============ ВИБРАЦИЯ ============

internal fun vibrateShort(context: Context) {
    val vibrator = getVibrator(context)
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(150L)
    }
}

internal fun vibrateTick(context: Context) {
    val vibrator = getVibrator(context)
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50L)
    }
}

private fun getVibrator(context: Context): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

// ============ POSE SCREEN (ПРИСЕДАНИЯ) ============

@Composable
fun PoseScreen(
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
    val analyzer = remember { SquatAnalyzer() }
    var feedback by remember { mutableStateOf<SquatAnalyzer.Feedback?>(null) }

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
    var lastSpokenRepCount by remember { mutableStateOf(0) }
    var lastTargetReached by remember { mutableStateOf(0) }

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
                    val newFeedback = analyzer.analyze(pts)
                    feedback = newFeedback

                    if (ttsReady && voiceEnabled && newFeedback != null) {
                        if (newFeedback.repCount > lastSpokenRepCount) {
                            lastSpokenRepCount = newFeedback.repCount

                            if (appSettings.vibrateOnCount) {
                                vibrateTick(context)
                            }

                            val numberText = when (newFeedback.repCount) {
                                1 -> "Раз"; 2 -> "Два"; 3 -> "Три"; 4 -> "Четыре"
                                5 -> "Пять"; 6 -> "Шесть"; 7 -> "Семь"; 8 -> "Восемь"
                                9 -> "Девять"; 10 -> "Десять"
                                11 -> "Одиннадцать"; 12 -> "Двенадцать"
                                13 -> "Тринадцать"; 14 -> "Четырнадцать"
                                15 -> "Пятнадцать"; 16 -> "Шестнадцать"
                                17 -> "Семнадцать"; 18 -> "Восемнадцать"
                                19 -> "Девятнадцать"; 20 -> "Двадцать"
                                else -> newFeedback.repCount.toString()
                            }
                            tts.speak(numberText, TextToSpeech.QUEUE_ADD, null, "rep")

                            val target = appSettings.targetReps
                            if (target > 0 && newFeedback.repCount >= target &&
                                lastTargetReached < target) {
                                lastTargetReached = newFeedback.repCount
                                if (appSettings.saySetComplete) {
                                    tts.speak("Подход завершён", TextToSpeech.QUEUE_ADD, null, "set_complete")
                                }
                                if (appSettings.vibrateOnSetComplete) {
                                    vibrateShort(context)
                                }
                                scope.launch { repository.saveSquat(target) }
                            }
                        }

                        val hint = newFeedback.hint
                        val hintEnabled = when (hint) {
                            "Держи спину"   -> appSettings.sayBack
                            "Пятки на полу" -> appSettings.sayHeels
                            "Глубже"        -> appSettings.sayDeeper
                            "Хорошо"        -> appSettings.sayGood
                            else            -> true
                        }
                        if (hintEnabled && hint.isNotBlank() && hint != lastSpokenHint) {
                            lastSpokenHint = hint
                            tts.speak(hint, TextToSpeech.QUEUE_ADD, null, "hint")
                        }
                        if (hint.isBlank()) lastSpokenHint = ""
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
            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                val target = appSettings.targetReps
                val repsText = if (target > 0) "Повторений: ${fb.repCount} / $target"
                else "Повторений: ${fb.repCount}"
                Text(repsText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(fb.hint, color = Color.Yellow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // === ПОДСКАЗКА: 2 строки ===
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
                        text = "📐 Встань прямо (спереди) к камере",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📱 Камера на высоте 50–60 см от пола",
                        color = Color.White,
                        fontSize = 14.sp
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
                    lastSpokenRepCount = 0
                    lastTargetReached = 0
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000), contentColor = Color.White
                )
            ) { Text("Сброс", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}