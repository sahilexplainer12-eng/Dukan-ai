package com.dukaanai.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dukaanai.app.audio.RecordingStatus
import com.dukaanai.app.audio.TranscribeState
import com.dukaanai.app.ui.theme.BrandPrimary
import com.dukaanai.app.ui.theme.CreditRed
import com.dukaanai.app.ui.theme.PaymentGreen
import com.dukaanai.app.ui.viewmodel.AppScreen
import com.dukaanai.app.ui.viewmodel.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val recorder = viewModel.audioRecorder
    val recordingStatus by recorder.status.collectAsState()
    val transcribeState by viewModel.speechToTextEngine.state.collectAsState()
    val currentLanguage by viewModel.speechToTextEngine.currentLanguage.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.clearVoiceTest()
            recorder.startRecording()
        }
    }

    // Automatically trigger offline transcription when recording finishes
    LaunchedEffect(recordingStatus) {
        if (recordingStatus is RecordingStatus.Recorded) {
            viewModel.transcribeCurrentAudio()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Offline Voice Recognition",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                        Text(
                            text = "Hindi & Hinglish Shop Ledger",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearVoiceTest()
                        viewModel.navigateTo(AppScreen.Home)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color(0xFF1E293B),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Safety Info Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% Offline Mode: Audio is transcribed locally on your device without internet or cloud APIs. No transactions are saved.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 16.sp
                    )
                }
            }

            // Language Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = currentLanguage == "hi",
                    onClick = { viewModel.setSttLanguage("hi") },
                    label = {
                        Text("Hindi (हिंदी) • Default", fontWeight = FontWeight.SemiBold)
                    },
                    leadingIcon = if (currentLanguage == "hi") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )

                FilterChip(
                    selected = currentLanguage == "",
                    onClick = { viewModel.setSttLanguage("") },
                    label = {
                        Text("Auto-Detect")
                    },
                    leadingIcon = if (currentLanguage == "") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            // Main Recording Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (val status = recordingStatus) {
                        is RecordingStatus.Idle -> {
                            IdleRecordingView()
                        }
                        is RecordingStatus.Recording -> {
                            ActiveRecordingView(
                                elapsedSeconds = status.elapsedSeconds,
                                maxSeconds = status.maxSeconds,
                                amplitude = status.amplitudeLevel
                            )
                        }
                        is RecordingStatus.Recorded -> {
                            RecordedAudioView(
                                durationSeconds = status.durationSeconds,
                                fileSizeBytes = status.fileSizeBytes,
                                isPlaying = status.isPlaying,
                                onPlayClick = {
                                    if (status.isPlaying) {
                                        recorder.stopPlayback()
                                    } else {
                                        recorder.playRecordedAudio()
                                    }
                                }
                            )
                        }
                        is RecordingStatus.Error -> {
                            ErrorRecordingView(message = status.message)
                        }
                    }

                    // Action Buttons (Record / Stop / Clear / Try Again)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (recordingStatus) {
                            is RecordingStatus.Recording -> {
                                Button(
                                    onClick = { recorder.stopRecording() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Stop & Transcribe",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            is RecordingStatus.Recorded -> {
                                OutlinedButton(
                                    onClick = { viewModel.clearVoiceTest() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear")
                                }

                                Button(
                                    onClick = {
                                        viewModel.clearVoiceTest()
                                        if (hasPermission) {
                                            recorder.startRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try Again")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        viewModel.clearVoiceTest()
                                        if (hasPermission) {
                                            recorder.startRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Record Voice",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transcribed Text Result Section
            if (recordingStatus is RecordingStatus.Recorded) {
                TranscribedResultCard(
                    state = transcribeState,
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied recognized text to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onRetryTranscription = {
                        viewModel.transcribeCurrentAudio()
                    }
                )
            }

            // Official Hindi Kirana Shop Test Phrases Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Phrases (Hindi / Hinglish)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }

                    Text(
                        text = "Speak one of these exact phrases into the mic to test Hindi speech recognition:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    TestPhraseItem(
                        tag = "Test 1: Detergent Sale",
                        phrase = "रिजवान भाई दस रुपये का डिटर्जेंट ले गए हैं।",
                        tagColor = Color(0xFF2563EB)
                    )

                    TestPhraseItem(
                        tag = "Test 2: Udhaar / Credit",
                        phrase = "राजू दो किलो चावल और एक तेल ले गया, एक सौ अस्सी रुपये उधार।",
                        tagColor = CreditRed
                    )

                    TestPhraseItem(
                        tag = "Test 3: Jama / Payment",
                        phrase = "मुन्ना ने पिछले खाते के पाँच सौ रुपये जमा किए।",
                        tagColor = PaymentGreen
                    )

                    TestPhraseItem(
                        tag = "Test 4: Grocery Order",
                        phrase = "भैया एक किलो चीनी और आधा किलो दाल दे देना।",
                        tagColor = Color(0xFF7C3AED)
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscribedResultCard(
    state: TranscribeState,
    onCopyText: (String) -> Unit,
    onRetryTranscription: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recognized Hindi Text",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }

                if (state is TranscribeState.Success) {
                    val secText = String.format(Locale.US, "%.1fs", state.timeTakenSeconds)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PaymentGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Hindi • $secText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PaymentGreen
                        )
                    }
                }
            }

            when (state) {
                is TranscribeState.Idle -> {
                    Text(
                        text = "Waiting for transcription to start...",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                is TranscribeState.LoadingModel -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Loading offline multilingual Whisper model...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                is TranscribeState.Transcribing -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Transcribing Hindi offline... Please wait.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                is TranscribeState.Success -> {
                    // Transcript Text Box (Devanagari Hindi)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = state.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            lineHeight = 24.sp
                        )
                    }

                    // Copy Button
                    OutlinedButton(
                        onClick = { onCopyText(state.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = BrandPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Recognized Text",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary
                        )
                    }
                }
                is TranscribeState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CreditRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Transcription Issue",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditRed
                            )
                        }
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B),
                            lineHeight = 16.sp
                        )
                        TextButton(
                            onClick = onRetryTranscription,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Retry Transcription", color = CreditRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleRecordingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Ready to Record",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )

        Text(
            text = "Tap 'Record Voice' and speak clearly in Hindi into your microphone.",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActiveRecordingView(
    elapsedSeconds: Int,
    maxSeconds: Int,
    amplitude: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(CreditRed.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CreditRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Live Timer
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val maxMinutes = maxSeconds / 60
        val maxSecs = maxSeconds % 60
        val timerString = String.format(Locale.US, "%02d:%02d / %02d:%02d", minutes, seconds, maxMinutes, maxSecs)

        Text(
            text = timerString,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CreditRed
        )

        // Live Audio Amplitude Level Bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            LinearProgressIndicator(
                progress = { (amplitude * 2.5f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CreditRed,
                trackColor = Color(0xFFFEE2E2)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Listening to audio input...",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun RecordedAudioView(
    durationSeconds: Double,
    fileSizeBytes: Long,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val durationText = String.format(Locale.US, "%.1f sec", durationSeconds)
    val sizeText = String.format(Locale.US, "%.1f KB", fileSizeBytes / 1024.0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PaymentGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PaymentGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Audio Captured",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )

        // Recorded Audio Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Duration", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        text = durationText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = Color(0xFFCBD5E1)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "File Size", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        text = sizeText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = Color(0xFFCBD5E1)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Audio Quality", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        text = "16kHz PCM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaymentGreen
                    )
                }
            }
        }

        // Audio Playback Preview Button
        OutlinedButton(
            onClick = onPlayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = BrandPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPlaying) "Playing... Tap to Stop" else "Play Recorded Audio Preview",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = BrandPrimary
            )
        }
    }
}

@Composable
private fun ErrorRecordingView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = CreditRed,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Recording Failed",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CreditRed
        )

        Text(
            text = message,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TestPhraseItem(
    tag: String,
    phrase: String,
    tagColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tagColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tagColor
                )
            }
        }
        Text(
            text = "\"$phrase\"",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B),
            lineHeight = 20.sp
        )
    }
}
