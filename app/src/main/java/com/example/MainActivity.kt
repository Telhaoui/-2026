package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.GeminiClient
import com.example.data.DiscoveryPost
import com.example.data.HistoricalFall
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MeteorTab
import com.example.viewmodel.MeteorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MeteorViewModel = viewModel()
            MyApplicationTheme(isNightObservationMode = viewModel.isNightObservationMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeteorAppScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MeteorAppScreen(viewModel: MeteorViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isNight = viewModel.isNightObservationMode

    // Real-time alerts system container
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MeteorBottomNavigationBar(
                activeTab = viewModel.activeTab,
                onTabSelected = { viewModel.activeTab = it },
                isNight = isNight
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (isNight) Color.Black 
                    else MaterialTheme.colorScheme.background
                )
        ) {
            // Stars background rendering behind screen if in standard mode
            if (!isNight) {
                StarryBackgroundEffect()
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar (العنوان ونمط التتبع الليلي وصافرات التنبيه)
                if (viewModel.activeTab != MeteorTab.SKY_MAP) {
                    MeteorHeaderBar(viewModel)
                }

                // Event Alerts Sweeper Notification Ticker
                AnimatedVisibility(
                    visible = viewModel.simulationAlertTriggered != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    viewModel.simulationAlertTriggered?.let { alertMessage ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isNight) Color(0xFF660000) else Color(0xFFFF3333))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clickable { viewModel.dismissSimulationAlert() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "تحذير",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = alertMessage,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Main Views Switcher
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (viewModel.activeTab) {
                        MeteorTab.SKY_MAP -> SkyMapTabContent(viewModel)
                        MeteorTab.HISTORY -> HistoryTabContent(viewModel)
                        MeteorTab.MICROSCOPE -> MicroscopeTabContent(viewModel)
                        MeteorTab.TRAJECTORY -> TrajectoryTabContent(viewModel)
                        MeteorTab.COMMUNITY -> CommunityTabContent(viewModel)
                        MeteorTab.OBSERVATORY -> ObservatoryTabContent(viewModel)
                    }
                }
            }
        }
    }
}

// --- Background Decorative Starts Animating ---
private data class StarSpec(val xRel: Float, val yRel: Float, val r: Float, val isGlow: Boolean)

@Composable
fun StarryBackgroundEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    val stars = remember {
        val random = java.util.Random(42)
        List(81) {
            StarSpec(
                xRel = random.nextFloat(),
                yRel = random.nextFloat(),
                r = random.nextFloat() * 2.5f + 1f,
                isGlow = random.nextBoolean()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        for (star in stars) {
            drawCircle(
                color = Color(0xFF00FFCC).copy(alpha = if (star.isGlow) alphaAnim else 0.4f),
                radius = star.r,
                center = Offset(star.xRel * width, star.yRel * height)
            )
        }
    }
}

// --- Header Component ---
@Composable
fun MeteorHeaderBar(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Surface(
        tonalElevation = 8.dp,
        color = if (isNight) Color.Black else Color(0xFF0F172A).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, if (isNight) Color(0xFF3A0000) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App branding Left side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isNight) Color(0xFF200000) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            activeColor,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "لوغو",
                        tint = activeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "راصد النيازك",
                        color = if (isNight) Color(0xFFFF1E1E) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "منظومة المراقبة والتحليل المتقدمة",
                        color = if (isNight) Color(0xFF9F0000) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            // Controllers Right Side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // simulated test alerts button
                IconButton(
                    onClick = {
                        val events = listOf(
                            "رصد كرة فلاش نيزكية شديدة فوق الربع الخالي منذ دقيقة!",
                            "تم تحديث بيانات التفتيش الميداني لنيزك تشيليابنسك بالذكاء الاصطناعي.",
                            "تنبيه: مرصد هالة بالاسيت يسجل زيادة نشاط الغبار الفلكي الليلة.",
                            "إنذار تلسكوبي فوري: وميض نيزكي مكتشف في كوكبة الجسر المحترقة."
                        )
                        viewModel.triggerSimulationAlert(events.random())
                    },
                    modifier = Modifier.testTag("alert_simulation_btn")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "الإنذارات",
                            tint = activeColor
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(activeColor, shape = CircleShape)
                        )
                    }
                }

                // Toggle Night mode
                Button(
                    onClick = {
                        viewModel.isNightObservationMode = !viewModel.isNightObservationMode
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNight) Color(0xFF330000) else Color(0xFF1E293B),
                        contentColor = activeColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, activeColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "نمط ليلي",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isNight) "رصد ليلي" else "تتبع عادي",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- Bottom Navigation Menu ---
@Composable
fun MeteorBottomNavigationBar(
    activeTab: MeteorTab,
    onTabSelected: (MeteorTab) -> Unit,
    isNight: Boolean
) {
    val trackingColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)
    val inactiveColor = if (isNight) Color(0xFF661E1E) else Color(0xFF64748B)

    NavigationBar(
        containerColor = if (isNight) Color.Black else Color(0xFF0B1222),
        tonalElevation = 12.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(
                BorderStroke(
                    1.dp,
                    if (isNight) Color(0xFF2A0000) else Color(0xFF1E293B)
                )
            )
    ) {
        val tabs = listOf(
            Triple(MeteorTab.SKY_MAP, "الخريطة", Icons.Default.Star),
            Triple(MeteorTab.HISTORY, "السجل", Icons.Default.Search),
            Triple(MeteorTab.MICROSCOPE, "المجهر AI", Icons.Default.Info),
            Triple(MeteorTab.TRAJECTORY, "المسار", Icons.Default.LocationOn),
            Triple(MeteorTab.COMMUNITY, "المجتمع", Icons.Default.Person),
            Triple(MeteorTab.OBSERVATORY, "المرصد", Icons.Default.Warning)
        )

        tabs.forEach { (tab, title, icon) ->
            val isSelected = activeTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) trackingColor else inactiveColor,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        color = if (isSelected) trackingColor else inactiveColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (isNight) Color(0xFF200000) else Color(0xFF1E293B)
                )
            )
        }
    }
}

// ==========================================================
// 1. SKY MAP TAB CONTENT (الخريطة السماوية التفاعلية والشهب)
// ==========================================================
@Composable
fun SkyMapTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)
    val coroutineScope = rememberCoroutineScope()

    var activeSubView by remember { mutableStateOf<String?>(null) }
    var showOrangeCard by remember { mutableStateOf(true) }
    var showBottomCards by remember { mutableStateOf(true) }
    var isScanningMain by remember { mutableStateOf(false) }

    // Detector game states
    var detectorFreq by remember { mutableStateOf(42.3f) }
    var isScanningDetector by remember { mutableStateOf(false) }
    var detectorResult by remember { mutableStateOf<String?>(null) }
    var detectorType by remember { mutableStateOf<String?>(null) }
    var detectorInventory by remember { mutableStateOf(listOf<String>()) }

    // Camera tracker states
    var isCameraFlashing by remember { mutableStateOf(false) }
    var showCameraSuccessDialog by remember { mutableStateOf(false) }

    // Dialogs
    var showSignalsDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }

    // Original Map States
    var azimuthOffset by remember { mutableStateOf(0f) }
    var altitudeOffset by remember { mutableStateOf(0f) }
    var triggerShootingMeteor by remember { mutableStateOf(false) }
    val meteorSpeedAnim = remember { Animatable(0f) }

    if (isScanningMain) {
        // Main Scanning Loader Animation Screen
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF070B19)),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "main_scanner")
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
                label = "sweep"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(150.dp).border(1.dp, activeColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(activeColor.copy(alpha = 0.05f))
                        rotate(sweepAngle) {
                            drawLine(
                                brush = Brush.linearGradient(listOf(Color.Transparent, activeColor)),
                                start = Offset(size.width/2, size.height/2),
                                end = Offset(size.width/2, 0f),
                                strokeWidth = 3f
                            )
                        }
                    }
                    Text("AI SCAN", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("جاري تشغيل الأشعة المجهرية ومكافحة الانحراف الجاذبي...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else if (activeSubView == "sky_map") {
        // High fidelity satellite map of the Sahara desert (e.g. Morocco/Algeria/Western Sahara)
        // Supporting pan, zoom, custom concentric circles, interactive legend panel, overlay header with live blinking dot,
        // zoom controls, floating menu, and click details card.

        var mapScale by remember { mutableStateOf(1f) }
        var mapOffset by remember { mutableStateOf(Offset(0f, 0f)) }

        var gridVisible by remember { mutableStateOf(true) }
        var radarScanVisible by remember { mutableStateOf(true) }

        val infiniteTransition = rememberInfiniteTransition(label = "blinking")
        val liveBlinkAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "live_blink"
        )

        val radarRotAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing)
            ),
            label = "radar_rot"
        )

        // Target models for meteor falls matching the screenshot and real geography
        data class MeteorFallPoint(
            val id: String,
            val nameAr: String,
            val nameEn: String,
            val subtitle: String,
            val date: String,
            val brightness: String,
            val source: String,
            val relativeX: Float,
            val relativeY: Float,
            val typeColor: Color,
            val isHotspot: Boolean = false
        )

        val targetPoints = remember {
            listOf(
                MeteorFallPoint(
                    id = "tata",
                    nameAr = "شرق طاطا وفور المحاميد",
                    nameEn = "Morocco",
                    subtitle = "رصد نيزكي",
                    date = "2026-05-18",
                    brightness = "-9.2",
                    source = "Moroccan Meteorite Network",
                    relativeX = 10f,
                    relativeY = -200f,
                    typeColor = Color(0xFF00E676) // Green
                ),
                MeteorFallPoint(
                    id = "assa",
                    nameAr = "شمال غرب آسا الزاك",
                    nameEn = "Western Sahara",
                    subtitle = "سقوط حديث مؤكد",
                    date = "2026-05-20",
                    brightness = "-8.0",
                    source = "Moroccan Meteorite Network",
                    relativeX = -180f,
                    relativeY = 0f,
                    typeColor = Color(0xFFFF9100) // Orange
                ),
                MeteorFallPoint(
                    id = "tindouf",
                    nameAr = "بيانات ناسا - حدود تندوف",
                    nameEn = "CNEOS Data",
                    subtitle = "رصد راداري",
                    date = "2026-05-15",
                    brightness = "-10.5",
                    source = "NASA CNEOS Database",
                    relativeX = 220f,
                    relativeY = -120f,
                    typeColor = Color(0xFFFF1744) // Red
                ),
                MeteorFallPoint(
                    id = "smara",
                    nameAr = "بؤرة فضائية - شرق السمارة",
                    nameEn = "Algeria/Morocco Border",
                    subtitle = "بؤرة نشاط تساقط",
                    date = "2026-05-19",
                    brightness = "-5.4",
                    source = "Astrophysical Hotspot Monitor",
                    relativeX = -150f,
                    relativeY = -220f,
                    typeColor = Color(0xFFFFD600), // Yellow
                    isHotspot = true
                ),
                MeteorFallPoint(
                    id = "mali_border",
                    nameAr = "جنوب شرق تيرس زمور",
                    nameEn = "Mali/Mauritania Border",
                    subtitle = "مسار سقوط تاريخي",
                    date = "2026-04-30",
                    brightness = "-12.1",
                    source = "Sahara Science Alliance",
                    relativeX = 180f,
                    relativeY = 250f,
                    typeColor = Color(0xFFFFD600), // Yellow
                    isHotspot = true
                )
            )
        }

        var selectedFallId by remember { mutableStateOf<String?>("tata") }
        val selectedFall = targetPoints.find { it.id == selectedFallId }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B19))
        ) {
            // Interactive Map Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxOffsetX = 800f * mapScale
                            val maxOffsetY = 800f * mapScale
                            mapOffset = Offset(
                                x = (mapOffset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (mapOffset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
            ) {
                // Background Image
                Image(
                    painter = painterResource(id = R.drawable.img_sahara_map),
                    contentDescription = "جغرافية الصحراء الكبرى",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = mapScale,
                            scaleY = mapScale,
                            translationX = mapOffset.x,
                            translationY = mapOffset.y
                        ),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                // SVG Grid Canvas Overlay
                if (gridVisible) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = mapScale,
                                scaleY = mapScale,
                                translationX = mapOffset.x,
                                translationY = mapOffset.y
                            )
                    ) {
                        val strokeWidth = 1f / mapScale
                        val step = 100f
                        
                        val columns = if (size.width > 0f) (size.width / step).toInt() else 0
                        for (i in 0..columns) {
                            val lineX = i * step
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(lineX, 0f),
                                end = Offset(lineX, size.height),
                                strokeWidth = strokeWidth
                            )
                        }

                        val rows = if (size.height > 0f) (size.height / step).toInt() else 0
                        for (j in 0..rows) {
                            val lineY = j * step
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
                }

                // Hotspot radar scanning lines
                if (radarScanVisible) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = mapScale,
                                scaleY = mapScale,
                                translationX = mapOffset.x,
                                translationY = mapOffset.y
                            )
                    ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxRadius = size.minDimension
                        rotate(radarRotAngle, Offset(cx, cy)) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF00FFCC).copy(alpha = 0.15f)),
                                    start = Offset(cx, cy),
                                    end = Offset(cx + maxRadius, cy)
                                ),
                                start = Offset(cx, cy),
                                end = Offset(cx + maxRadius, cy),
                                strokeWidth = 3f / mapScale
                            )
                        }
                    }
                }

                // Place Interactive Concentric Circle Markers
                targetPoints.forEach { point ->
                    val isSelected = point.id == selectedFallId

                    val markerPulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "marker_pulse_${point.id}"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer(
                                translationX = mapOffset.x + (point.relativeX * mapScale),
                                translationY = mapOffset.y + (point.relativeY * mapScale)
                            )
                            .clickable { selectedFallId = point.id }
                    ) {
                        Canvas(modifier = Modifier.size(52.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val coreRadius = 8f * mapScale
                            val outerRingRadiusMax = 22f * mapScale

                            if (point.isHotspot) {
                                drawCircle(
                                    color = Color.Black,
                                    radius = 11f * mapScale,
                                    center = center
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 9f * mapScale,
                                    center = center,
                                    style = Stroke(width = 2.5f * mapScale)
                                )
                                drawCircle(
                                    color = Color(0xFFFFD600),
                                    radius = 4.5f * mapScale,
                                    center = center
                                )
                                drawCircle(
                                    color = Color(0xFFFFD600).copy(alpha = 0.15f),
                                    radius = 18f * mapScale,
                                    center = center
                                )
                            } else {
                                drawCircle(
                                    color = point.typeColor.copy(alpha = 0.2f * markerPulseAlpha),
                                    radius = outerRingRadiusMax * 1.5f,
                                    center = center
                                )
                                drawCircle(
                                    color = point.typeColor,
                                    radius = outerRingRadiusMax,
                                    center = center,
                                    style = Stroke(width = 3.5f * mapScale)
                                )
                                drawCircle(
                                    color = point.typeColor,
                                    radius = coreRadius,
                                    center = center
                                )
                            }

                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    radius = outerRingRadiusMax * 1.8f,
                                    center = center,
                                    style = Stroke(width = 2.5f * mapScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // OVERLAYS (UI HUD ELEMENTS)
            // ==========================================

            // 1. Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left Close Button - Red circle with white X
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFEA4335), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { activeSubView = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق الخارطة",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Top Right Navigation & Header Pill
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1425).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "خارطة السقوط العالمية",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Right
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "تتبع بث حي",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00FFCC).copy(alpha = liveBlinkAlpha), CircleShape)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF2563EB), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. Legend / Filters Panel Overlay (Below the top-right header)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE10B1325)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 115.dp, end = 16.dp)
                    .width(220.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(Color.White.copy(alpha = 0.05f))
                                rotate(309f, canvasCenter) {
                                    drawLine(
                                        color = Color(0xFFFF3333),
                                        start = canvasCenter,
                                        end = Offset(size.width / 2f, 4f),
                                        strokeWidth = 2.5f
                                    )
                                    drawLine(
                                        color = Color.White,
                                        start = canvasCenter,
                                        end = Offset(size.width / 2f, size.height - 4f),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                            Text(
                                text = "N",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                        
                        Text(
                            text = "309°",
                            color = Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    listOf(
                        Triple("سقوط حديث مؤكد", Color(0xFF00FFCC), true),
                        Triple("مواقع نيازك تاريخية", Color(0xFFFF9100), false),
                        Triple("بيانات ناسا (CNEOS)", Color(0xFFFF1744), false),
                        Triple("بؤر فضائية: جزائر/مغرب", Color(0xFFFFD600), false)
                    ).forEach { (title, color, isCompassSub) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = title,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCompassSub) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Click Active Info Card Overlay
            selectedFall?.let { point ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEC09101E)),
                    border = BorderStroke(2.dp, point.typeColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 70.dp)
                        .width(285.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "☄️ ${point.subtitle}",
                                color = Color(0xFFFFB300),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "${point.nameAr} (${point.nameEn})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "${point.date} | قدر السطوع: ${point.brightness}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "المصدر: ${point.source}",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(24.dp)
                                .clickable { selectedFallId = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "أغلق التفاصيل",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. Zoom Controls (Bottom Right of the screen)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE10B1325)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp)
                    .width(42.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { mapScale = (mapScale + 0.5f).coerceAtMost(4.0f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 6.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { mapScale = (mapScale - 0.5f).coerceAtLeast(1.0f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Floating Action Map Control Bar (Bottom Center)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE10A0F1D)),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 45.dp)
                    .width(240.dp)
                    .height(58.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .clickable {
                                mapScale = 1.0f
                                mapOffset = Offset(0f, 0f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "ملئ الشاشة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (radarScanVisible) Color(0xFF2563EB).copy(alpha = 0.15f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (radarScanVisible) Color(0xFF2563EB) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { radarScanVisible = !radarScanVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تتبع راداري",
                            tint = if (radarScanVisible) Color(0xFF93C5FD) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (gridVisible) Color(0xFFFFB300).copy(alpha = 0.12f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (gridVisible) Color(0xFFFFB300) else Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .clickable { gridVisible = !gridVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "تفعيل الشبكة الجغرافية",
                            tint = if (gridVisible) Color(0xFFFFB300) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } else if (activeSubView == "detector") {
        // GOLD & DIAMOND DETECTOR VIEW
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeSubView = null }) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Color(0xFFFFB300)) }
                Text("كاشف الذهب والألماس بالصحراء", color = Color(0xFFFFB300), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            }
            Text("يقوم تتبع التذبذبات بنظام السونار النبضي بمسح الذرات في رمال الصحراء بحثاً عن شظايا النيازك الثمينة والذهب الخام.", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Right)
            
            // Radar sweeping visualizer
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).background(Color(0xFF060913), RoundedCornerShape(16.dp)).border(BorderStroke(2.dp, Color(0xFFFFB300).copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition("radar")
                val pulseAnim by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse))
                val rotateAnim by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing)))
                
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension / 2.2f
                    drawCircle(Color(0xFFFFB300).copy(alpha = 0.15f * pulseAnim), radius = maxR)
                    drawCircle(Color(0xFFFFB300).copy(alpha = 0.3f), radius = maxR * 0.7f, style = Stroke(2f))
                    drawCircle(Color(0xFFFFB300).copy(alpha = 0.5f), radius = 10f)
                    rotate(rotateAnim, Offset(cx, cy)) {
                        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color(0xFFFFB300))), Offset(cx, cy), Offset(cx, cy - maxR), strokeWidth = 3f)
                    }
                }
                Text("${detectorFreq.toInt()} µT", color = Color(0xFFFFB300), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }

            LaunchedEffect(isScanningDetector) {
                while(true) {
                    delay(400)
                    detectorFreq = if (isScanningDetector) (45..180).random().toFloat() else 42.0f + (0..15).random().toFloat() / 10f
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isScanningDetector = true
                        detectorResult = null
                        delay(2000)
                        isScanningDetector = false
                        val items = listOf("جزء نيزك حديدي ثقيل ☄️", "شظية زجاج صحراوي نادر 🔋", "عرق ذهب خام فائق النقاوة 🪙", "بلورة ألماس لونسداليت صدمية خام 💎", "رمال داكنة غنية بالمغناطيسية 🏖️")
                        val itemSelected = items.random()
                        detectorResult = itemSelected
                        if (itemSelected != "رمال داكنة غنية بالمغناطيسية 🏖️") {
                            detectorInventory = detectorInventory + itemSelected
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                enabled = !isScanningDetector,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isScanningDetector) Text("جاري فحص الكثبان الرملية...") else Text("امسح التربة الرملية", fontWeight = FontWeight.Bold)
            }

            detectorResult?.let { res ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F29)), border = BorderStroke(1.dp, Color(0xFFFFB300)), modifier = Modifier.fillMaxWidth()) {
                    Text(res, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                }
            }
        }
    } else if (activeSubView == "record") {
        // CAMERA SIGHT SIGHT VIEW
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeSubView = null }) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Color(0xFFFF3333)) }
                Text("شاشة رصد وتصوير السماء الكاميرا", color = Color(0xFFFF3333), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(16.dp)).border(Stroke(2f).width.dp, Color(0xFFFF3333), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                StarryBackgroundEffect()
                if (isCameraFlashing) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.15f)))
                    Text("جاري التقاط مسار التفريغ الفوسفوري...", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("تم وضع الكاميرا على الوضع التلقائي اللحظي\nREC ● ISO 1600", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        isCameraFlashing = true
                        delay(1500)
                        isCameraFlashing = false
                        showCameraSuccessDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("محاكاة وميض شهاب ساقط فوري", fontWeight = FontWeight.Bold)
            }
            
            if (showCameraSuccessDialog) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F0D0D)), border = BorderStroke(1.dp, Color(0xFFFF3333)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تم التقاط سقوط شهاب نيزكي سليم! 🎉", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { showCameraSuccessDialog = false; viewModel.activeTab = MeteorTab.COMMUNITY }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333))) {
                            Text("مشاركة الاكتشاف مع المجتمع")
                        }
                    }
                }
            }
        }
    } else {
        // ==========================================
        // MAIN ARABIC LANDING DASHBOARD (SCREENSHOT)
        // ==========================================
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Customized Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { viewModel.activeTab = MeteorTab.OBSERVATORY }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, "خارطة فلكية", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Box(modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { activeSubView = "record" }, contentAlignment = Alignment.Center) {
                        Text("AR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Middle Pill Shape: "+ سقوط النيازك"
                Box(modifier = Modifier.background(Color(0xFF131B2E), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Text("سقوط النيازك", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Blue avatar
                Box(modifier = Modifier.size(38.dp).background(Color(0xFF2563EB), CircleShape), contentAlignment = Alignment.Center) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // 2. Real-time Live Global Observation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    // Left Text Side (Arabic layout details)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رصد عالمي مباشر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        Text("5 أحداث سقوط حديثة تم رصدها", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Clique "تصفح الخارطة 🗺️"
                        Row(modifier = Modifier.clickable { activeSubView = "sky_map" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            Text("تصفح الخارطة", color = Color(0xFF02F4C5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🗺️", fontSize = 12.sp)
                        }
                    }
                    
                    // Orbit Animation Card
                    Box(modifier = Modifier.size(75.dp).background(Color(0xFF060913), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        val infiniteTransition = rememberInfiniteTransition("orbit")
                        val orbitAngle by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)))
                        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val r = size.minDimension / 2.2f
                            drawCircle(Color(0xFF1E293B), radius = r, style = Stroke(width = 1.5f))
                            drawCircle(Color(0xFFFFB300), radius = 3.5f)
                            val rad = Math.toRadians(orbitAngle.toDouble())
                            val sx = cos(rad).toFloat() * r + size.width/2
                            val sy = sin(rad).toFloat() * r + size.height/2
                            drawCircle(Color(0xFFFF5722), radius = 4f, center = Offset(sx, sy))
                        }
                    }
                }
            }

            // 3. Huge Blue-purple scan button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isScanningMain = true
                        delay(2500)
                        isScanningMain = false
                        activeSubView = "sky_map"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) {
                    Text("بدء المسح عن النيازك", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 4. "عرض أقل -" Toggle Button
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp).clickable { showBottomCards = !showBottomCards }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (showBottomCards) "عرض أقل —" else "عرض المزيد +",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Collapsible Cards Section
            AnimatedVisibility(visible = showBottomCards) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Orange Field Guide Card
                    if (showOrangeCard) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { viewModel.activeTab = MeteorTab.MICROSCOPE }) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Text("دليل التعرف الميداني (صور حقيقية)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                                }
                            }
                            // Small badge button
                            Box(modifier = Modifier.offset((-4).dp, (-4).dp).size(18.dp).background(Color.Black, CircleShape).align(Alignment.TopEnd).clickable { showOrangeCard = false }, contentAlignment = Alignment.Center) {
                                Text("—", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    // Green Live Chat Card
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF00897B)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { viewModel.activeTab = MeteorTab.COMMUNITY }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("دردشة الباحثين المباشرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                        }
                    }

                    // Two Columns Side-By-Side Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF090D1A)), border = BorderStroke(2.dp, Color(0xFFFFB300)), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(74.dp).clickable { activeSubView = "detector" }) {
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("كاشف الذهب والألماس", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF090D1A)), border = BorderStroke(2.dp, Color(0xFFFF3333)), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(74.dp).clickable { activeSubView = "record" }) {
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFFF3333), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("راصد السماء (تسجيل سقوط)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Blue pill button: Global observatories
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)), border = BorderStroke(1.5.dp, Color(0xFF2563EB)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp).clickable { viewModel.activeTab = MeteorTab.OBSERVATORY }) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("راصدات النيازك العالمية", color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Green Map pill
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9F7F)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp).clickable { activeSubView = "sky_map" }) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خارطة السقوط العالمية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row of 3 Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(Icons.Default.Refresh, "سجل الفحص", Color(0xFFC084FC)),
                            Triple(Icons.Default.Warning, "دليل الإشارات", Color(0xFFFBBF24)),
                            Triple(Icons.Default.Info, "دليل النيازك", Color(0xFF38BDF8))
                        ).forEach { (icon, title, tint) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).clickable {
                                    if (title == "دليل الإشارات") showSignalsDialog = true
                                    else viewModel.activeTab = MeteorTab.HISTORY
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    // Bottom Row: User guide & gallery photo upload
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).clickable { showGuideDialog = true }) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Text("دليل الاستخدام", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1.3f).clickable { showUploadDialog = true }) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Share, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Text("رفع صور/فيديو", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Right, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- POPUP DIALOGS ---
    if (showSignalsDialog) {
        AlertDialog(
            onDismissRequest = { showSignalsDialog = false },
            confirmButton = { Button(onClick = { showSignalsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = activeColor, contentColor = Color.Black)) { Text("فهمت") } },
            title = { Text("دليل الإشارات والموجات النيزكية", fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("يفسر هذا الملحق طبيعة ذبذبات الصخور الفضائية علمياً:", textAlign = TextAlign.Right)
                    val signals = listOf(
                        "التأين الكهرومغناطيسي: يترك مرور الشهب سلكية في ذرات الغاز المشحون.",
                        "المجهرية الانصهارية: قشرة ريغماغليبتس المصقولة تدلل على الكثافة الفولاذية.",
                        "الانبعاث الصوتي: موجات تحت صوتية تسمع من مسافات شاسعة عند تفتت الكتلة النجرية."
                    )
                    signals.forEach { Text("• $it", textAlign = TextAlign.Right, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()) }
                }
            }
        )
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            confirmButton = { Button(onClick = { showGuideDialog = false }) { Text("إغلاق") } },
            title = { Text("دليل الاستخدام الميداني", fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text("خطوات بسيطة تضمن سلامتك واكتشافاتك الليلة:\n\n1. افعل وضع المراقبة الليلي لتجنب إجهاد العين بوهج الفضاء.\n2. افحص التربة والصخور الصحراوية المشكوك بها بجهاز السونار النبضي لكشف ترددات الكوندريت.\n3. سجل الاكتشاف البصري بكاميرا الرصد وشاركه فوراً لتقييمه بمجهر الذكاء الاصطناعي.", textAlign = TextAlign.Right, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            confirmButton = { Button(onClick = { showUploadDialog = false; viewModel.triggerSimulationAlert("تم حفظ وتعبئة الصورة الملتقطة بنجاح في ذاكرة الفحص الميداني للقسم!") }) { Text("تحميل وتأكيد") } },
            title = { Text("تحميل وسائط نيزكية", fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text("اختر ملف صورة مجهرية أو فيديو فلكي من الاستوديو لمسحه تلقائياً بمجهر الذكاء الاصطناعي وتقدير تركيب وحجم النيكر فائق الدقة.", textAlign = TextAlign.Right, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
        )
    }
}

// ==========================================================
// 2. HISTORY TAB CONTENT (السجل التاريخي للنيازك المتفرقة)
// ==========================================================
@Composable
fun HistoryTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    val searchState by viewModel.searchQuery.collectAsState()
    val falls by viewModel.historicalFalls.collectAsState()
    val currentSelectedFall = viewModel.selectedFall

    if (currentSelectedFall != null) {
        // Render detailed dialog for selected fall
        FallDetailView(fall = currentSelectedFall, onDismiss = { viewModel.selectedFall = null }, isNight = isNight)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "قاعدة بيانات السقوطات التاريخية الصدمية",
                style = MaterialTheme.typography.titleMedium,
                color = activeColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )

            // Search text field
            OutlinedTextField(
                value = searchState,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = {
                    Text(
                        "ابحث بالاسم، الموقع، النوع الصخري...",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, "بحث", tint = activeColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = if (isNight) Color(0xFF4A0000) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(textAlign = TextAlign.Right),
                modifier = Modifier.fillMaxWidth().testTag("history_search_input")
            )

            // List of falls
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(falls.size) { index ->
                    val fall = falls[index]
                    HistoricalFallItemCard(fall = fall, onClick = { viewModel.selectedFall = fall }, isNight = isNight)
                }
                if (falls.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لم يتم العثور على نتائج تطابق معايير البحث.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalFallItemCard(fall: HistoricalFall, onClick: () -> Unit, isNight: Boolean) {
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isNight) Color(0xFF0C0000) else Color(0xFF101524)
        ),
        border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Arrow indicator left side
            Icon(Icons.Default.ArrowBack, "المزيد", tint = activeColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "(${fall.nameEn})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = fall.nameAr,
                        color = if (isNight) Color(0xFFFF3333) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = fall.locationAr,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "موقع",
                        tint = activeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isNight) Color(0xFF2A0000) else Color(0xFF1E293B),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(fall.typeAr, color = activeColor, fontSize = 10.sp)
                    }
                    Text(
                        text = "سنة السقوط: ${fall.year}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Detail View overlay for Historical fall
@Composable
fun FallDetailView(fall: HistoricalFall, onDismiss: () -> Unit, isNight: Boolean) {
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header back row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNight) Color(0xFF250000) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("رجوع", color = activeColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, "خروج", tint = activeColor)
            }

            Text(
                text = "تفاصيل السقوط",
                style = MaterialTheme.typography.titleMedium,
                color = activeColor,
                fontWeight = FontWeight.Bold
            )
        }

        // Title Area
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNight) Color(0xFF100202) else Color(0xFF121B2E)
            ),
            border = BorderStroke(1.2.dp, activeColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fall.nameAr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isNight) Color(0xFFFF1E1E) else Color.White
                )
                Text(
                    text = "الاسم العلمي الفلكي: ${fall.nameEn}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Divider(color = activeColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(fall.year, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("سنة السقوط:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(fall.mass, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("الكتلة المقدرة:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(fall.typeAr, color = activeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("التصنيف الصخري:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(fall.impactEnergy, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("الطاقة الحركية الكامنة:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        // Description Card
        Text(
            text = "القصة والآثار العلمية الصدمية",
            fontWeight = FontWeight.Bold,
            color = activeColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNight) Color(0xFF0C0000) else Color(0xFF101524)
            ),
            border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = fall.descriptionAr,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        // Fall dispersion coordinates (Strewn field simulator)
        Text(
            text = "محاكاة حقل التناثر والتشتت (Strewn Field Map)",
            fontWeight = FontWeight.Bold,
            color = activeColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    if (isNight) Color(0xFF0F0101) else Color(0xFF121829),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    BorderStroke(1.dp, if (isNight) Color(0xFF3B0000) else Color(0xFF1E293B)),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw coordinate grids
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, cy), Offset(size.width, cy), 1f)
                drawLine(Color.White.copy(alpha = 0.05f), Offset(cx, 0f), Offset(cx, size.height), 1f)

                // Draw potential strewn area ellipse (dispersed strewn field)
                rotate(degrees = -30f, pivot = Offset(cx, cy)) {
                    drawOval(
                        color = activeColor.copy(alpha = 0.15f),
                        topLeft = Offset(cx - 70f, cy - 30f),
                        size = Size(140f, 60f)
                    )
                    drawOval(
                        color = activeColor,
                        topLeft = Offset(cx - 70f, cy - 30f),
                        size = Size(140f, 60f),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )

                    // Impact impact point
                    drawCircle(Color.Red, radius = 5f, center = Offset(cx + 40f, cy))
                    // Fine pieces
                    drawCircle(activeColor, radius = 2f, center = Offset(cx - 50f, cy - 10f))
                    drawCircle(activeColor, radius = 3f, center = Offset(cx - 20f, cy - 5f))
                    drawCircle(activeColor, radius = 4f, center = Offset(cx + 10f, cy + 5f))
                }

                // Reference point
                drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
            }

            // Map stats overlay description
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("إحداثيات الصدمة:", color = activeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Lat: ${fall.latitude} | Lon: ${fall.longitude}", color = Color.White, fontSize = 9.sp)
            }

            Text(
                text = "محاكاة اتجاه مسار السقوط الفلكي",
                color = activeColor.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
        }
    }
}

// ==========================================================
// 3. MICROSCOPE TAB CONTENT (التحليل المجهري الإلكتروني عالي الدقة AI)
// ==========================================================
@Composable
fun MicroscopeTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "تحليل مجهري إلكتروني عالي الدقة (المدعم بالذكاء الاصطناعي)",
            style = MaterialTheme.typography.titleMedium,
            color = activeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Text(
            text = "اختر عينة مجهرية لتركيب النيازك صخرية أو حديدية لمعاينتها عبر مجهر إلكتروني ماسح (SEM) وتحليلها فورياً بالذكاء الاصطناعي لتأكيد نشأتها الكونية.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        // Microscope sample selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val samples = listOf(
                Pair("chondrite", "كوندريت حجرية"),
                Pair("widmanstatten", "نقش حديدي"),
                Pair("pallasite", "بالاسيت أوليفين")
            )

            samples.forEach { (code, title) ->
                val isSelected = viewModel.selectedMicroscopeCode == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) activeColor.copy(alpha = 0.15f) 
                            else if (isNight) Color(0xFF080000) else Color(0xFF101524),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) activeColor 
                                else if (isNight) Color(0xFF2E0000) else Color(0xFF1E293B)
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.selectedMicroscopeCode = code
                            viewModel.microscopeAnalysisResult = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Microscope Visualizer Dome Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(2.dp, activeColor),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Procedural render of the selected microscope view on a canvas
            ProceduralMicroscopeCanvas(
                sampleCode = viewModel.selectedMicroscopeCode,
                activeColor = activeColor
            )

            // Scanning laser effect if analyzing
            if (viewModel.isAnalyzingMicroscope) {
                ScanningLaserOverlay(activeColor)
            }

            // Magnification marker badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SEM Mag: 50,000X",
                    color = activeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Trigger Button
        Button(
            onClick = {
                // Generate high fidelity bitmap matching the requested preset to pass into Gemini
                val width = 450
                val height = 300
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint()
                // Set background of canvas
                paint.color = android.graphics.Color.DKGRAY
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                // Draw some structural components matching the description of meteor materials
                paint.color = android.graphics.Color.YELLOW
                canvas.drawCircle(width / 2f, height / 2f, 80f, paint)
                paint.color = android.graphics.Color.WHITE
                canvas.drawCircle(width / 2.8f, height / 1.5f, 50f, paint)

                viewModel.analyzeMicroscopeImage(bitmap)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                contentColor = if (isNight) Color.Black else Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("microscope_analyze_btn"),
            enabled = !viewModel.isAnalyzingMicroscope
        ) {
            if (viewModel.isAnalyzingMicroscope) {
                CircularProgressIndicator(color = if (isNight) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("جاري تشغيل الأشعة المجهرية وتحليل التركيب فائق الدقة...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Info, "مجهر الكتروني")
                Spacer(modifier = Modifier.width(8.dp))
                Text("بدء التحليل بالمجهر الإلكتروني المدعم بالذكاء الاصطناعي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Analysis Result View section
        if (viewModel.microscopeAnalysisResult != null || viewModel.isAnalyzingMicroscope) {
            Text(
                text = "تقرير الفحص والتحليل الجيولوجي الفلكي:",
                fontWeight = FontWeight.Bold,
                color = activeColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF0C0000) else Color(0xFF101524)
                ),
                border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (viewModel.isAnalyzingMicroscope) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("جاري استلام حزمة النبضات الفائقة ومطابقة قشرة التآكل والمصفوفة مع كويكبات حزام الأجرام...", color = activeColor, fontSize = 12.sp, textAlign = TextAlign.Center)
                            LinearProgressIndicator(color = activeColor, modifier = Modifier.fillMaxWidth().height(4.dp))
                        }
                    } else {
                        Text(
                            text = viewModel.microscopeAnalysisResult ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProceduralMicroscopeCanvas(sampleCode: String, activeColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "microscope")
    val gridAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        when (sampleCode) {
            "chondrite" -> {
                // Procedural Chondrules (circular colorful spheres)
                val random = java.util.Random(10)
                // Matrix background dots
                for (i in 0..100) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    drawCircle(Color.Gray.copy(alpha = 0.3f), random.nextFloat() * 2f + 1f, Offset(x, y))
                }
                // Large Chondrules
                val positions = listOf(
                    Offset(cx - 70f, cy - 30f) to 25f,
                    Offset(cx + 60f, cy + 40f) to 32f,
                    Offset(cx - 10f, cy + 50f) to 18f,
                    Offset(cx + 80f, cy - 40f) to 22f,
                    Offset(cx - 100f, cy + 30f) to 15f
                )
                positions.forEach { (pos, radius) ->
                    // Outer ring
                    drawCircle(activeColor.copy(alpha = 0.5f), radius + 2f, pos, style = Stroke(width = 1.5f))
                    // Mineral crystals inner texture
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF9900).copy(alpha = 0.8f), activeColor.copy(alpha = 0.3f)),
                            center = pos,
                            radius = radius
                        ),
                        radius = radius,
                        center = pos
                    )
                }
            }
            "widmanstatten" -> {
                // Interweaving metallic lines matching nickel-iron structure
                rotate(degrees = gridAngle, pivot = Offset(cx, cy)) {
                    val lineSpacing = 30f
                    val strokeWidth = 1.5f

                    // Diagonal down
                    for (i in -15..15) {
                        val offset = i * lineSpacing
                        drawLine(
                            color = activeColor.copy(alpha = 0.4f),
                            start = Offset(offset, 0f),
                            end = Offset(size.width + offset, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    // Diagonal up (interlacing)
                    for (i in -15..15) {
                        val offset = i * lineSpacing
                        drawLine(
                            color = activeColor.copy(alpha = 0.25f),
                            start = Offset(offset, size.height),
                            end = Offset(size.width + offset, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
            "pallasite" -> {
                // Olivine yellow/green crystals surrounded by steel framework
                val random = java.util.Random(99)
                // Draw metal matrix steel skeleton
                drawRect(
                    color = Color.DarkGray.copy(alpha = 0.8f),
                    size = size
                )

                // Draw olivine crystals in matrix
                for (i in 0..14) {
                    val rx = random.nextFloat() * (size.width - 60f) + 30f
                    val ry = random.nextFloat() * (size.height - 60f) + 30f
                    val sizeGrain = random.nextFloat() * 15f + 14f

                    // draw olivine gemstone green-yellow grain
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFCCFF00), Color(0xFFFF9900)),
                            start = Offset(rx, ry),
                            end = Offset(rx + sizeGrain, ry + sizeGrain)
                        ),
                        topLeft = Offset(rx, ry),
                        size = Size(sizeGrain, sizeGrain)
                    )
                    // Crystal border outline
                    drawRect(
                        color = activeColor,
                        topLeft = Offset(rx, ry),
                        size = Size(sizeGrain, sizeGrain),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanningLaserOverlay(activeColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = size.height * laserY
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, activeColor, Color.Transparent)
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3f
        )
    }
}

// ==========================================================
// 4. TRAJECTORY DETERMINATOR AI TAB (تحديد المسارات والجهد الفلكي)
// ==========================================================
@Composable
fun TrajectoryTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "تحديد مسارات وسقوط النيازك بدقة عالية (AI)",
            style = MaterialTheme.typography.titleMedium,
            color = activeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Text(
            text = "أدخل بيانات دخول ومراقبة الجرم الفضائي لتقوم خوارزميات محاكاة الغلاف الجوي بتقدير مكان السقوط ومساحة حقل التناثر بدقة عالية.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        // Trajectory simulation canvas view panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    if (isNight) Color(0xFF0C0000) else Color(0xFF0B1222),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AtmosphericEntrySimulation(
                angle = viewModel.entryAngle.toFloatOrNull() ?: 45f,
                speed = viewModel.entrySpeed.toFloatOrNull() ?: 22f,
                activeColor = activeColor
            )
        }

        // Configuration Inputs
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNight) Color(0xFF070000) else Color(0xFF0B1428)
            ),
            border = BorderStroke(1.dp, if (isNight) Color(0xFF350000) else Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Entry Speed
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${viewModel.entrySpeed} كم/ثانية", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("سرعة دخول الغلاف الجوي (Speed entry):", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Slider(
                        value = viewModel.entrySpeed.toFloatOrNull() ?: 22f,
                        onValueChange = { viewModel.entrySpeed = it.toInt().toString() },
                        valueRange = 11f..72f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = activeColor,
                            inactiveTrackColor = activeColor.copy(alpha = 0.2f)
                        )
                    )
                }

                // Entry Angle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${viewModel.entryAngle}° درجة", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("زاوية السقوط المرصودة (Observed angle):", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Slider(
                        value = viewModel.entryAngle.toFloatOrNull() ?: 45f,
                        onValueChange = { viewModel.entryAngle = it.toInt().toString() },
                        valueRange = 5f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = activeColor,
                            inactiveTrackColor = activeColor.copy(alpha = 0.2f)
                        )
                    )
                }

                // Initial Mass
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${viewModel.initialMass} كجم", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("الكتلة الابتدائية الافتراضية للكتلة:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Slider(
                        value = viewModel.initialMass.toFloatOrNull() ?: 1500f,
                        onValueChange = { viewModel.initialMass = it.toInt().toString() },
                        valueRange = 100f..15000f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = activeColor,
                            inactiveTrackColor = activeColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Action calculate button
        Button(
            onClick = {
                viewModel.calculateMeteorTrajectory()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                contentColor = if (isNight) Color.Black else Color(0xFF070B14)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("trajectory_calculate_btn"),
            enabled = !viewModel.isAnalyzingTrajectory
        ) {
            if (viewModel.isAnalyzingTrajectory) {
                CircularProgressIndicator(color = if (isNight) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("جاري تقدير المسار ومكافحة الانحراف الجاذبي بالذكاء الاصطناعي...", fontSize = 12.sp)
            } else {
                Icon(Icons.Default.PlayArrow, "حساب")
                Spacer(modifier = Modifier.width(8.dp))
                Text("تقدير مسار السقوط الفلكي بالذكاء الاصطناعي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Trajectory Output Results
        if (viewModel.trajectoryAnalysisResult != null || viewModel.isAnalyzingTrajectory) {
            Text(
                text = "نتائج الفحص والتحليل الديناميكي:",
                fontWeight = FontWeight.Bold,
                color = activeColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF0A0000) else Color(0xFF101524)
                ),
                border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (viewModel.isAnalyzingTrajectory) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("جاري حساب زوايا الدوران الجاذبي والمقاومة الجوية وطبقات التأين المتوالية...", color = activeColor, fontSize = 12.sp, textAlign = TextAlign.Center)
                            LinearProgressIndicator(color = activeColor, modifier = Modifier.fillMaxWidth().height(4.dp))
                        }
                    } else {
                        Text(
                            text = viewModel.trajectoryAnalysisResult ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtmosphericEntrySimulation(angle: Float, speed: Float, activeColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "entry")
    val translationProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "entry_travel"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw atmospheric layers arcs
        val layerColors = listOf(
            Color(0xFF3B82F6).copy(alpha = 0.05f) to 0.7f,  // Thermosphere
            Color(0xFF60A5FA).copy(alpha = 0.1f) to 0.5f,   // Mesosphere
            Color(0xFF93C5FD).copy(alpha = 0.15f) to 0.3f,  // Stratosphere
            Color(0xFFBFDBFE).copy(alpha = 0.25f) to 0.15f  // Troposphere
        )

        layerColors.forEach { (color, radFactor) ->
            drawCircle(
                color = color,
                radius = width * radFactor,
                center = Offset(width / 2f, height)
            )
        }

        // Earth line
        drawLine(
            color = Color.Green.copy(alpha = 0.15f),
            start = Offset(0f, height - 10f),
            end = Offset(width, height - 10f),
            strokeWidth = 3f
        )

        // Trajectory vector line
        val startX = width * 0.1f
        val startY = height * 0.1f

        // End computed vector using angle
        val radians = Math.toRadians((90f - angle).toDouble())
        val vecX = cos(radians).toFloat() * 150f
        val vecY = sin(radians).toFloat() * 100f

        val endX = width * 0.9f
        val endY = height * 0.85f

        // Draw entry path line
        drawLine(
            color = activeColor.copy(alpha = 0.15f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )

        // Draw animating flying meteor
        val curX = startX + (endX - startX) * translationProgress
        val curY = startY + (endY - startY) * translationProgress

        if (translationProgress >= 0f && translationProgress <= 1.0f) {
            // Drawn back blaze trail
            val trailLength = 80f
            val trailX = curX - (endX - startX) * (trailLength / width)
            val trailY = curY - (endY - startY) * (trailLength / height)

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFF9900), activeColor),
                    start = Offset(trailX, trailY),
                    end = Offset(curX, curY)
                ),
                start = Offset(trailX, trailY),
                end = Offset(curX, curY),
                strokeWidth = 4f
            )

            // Explody shock tip
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(curX, curY)
            )
            drawCircle(
                color = activeColor.copy(alpha = 0.4f),
                radius = 12f,
                center = Offset(curX, curY)
            )
        }
    }
}

// ==========================================================
// 5. COMMUNITY DISCOVERY SHARE TAB (المجتمع والمشاركات)
// ==========================================================
@Composable
fun CommunityTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    val posts by viewModel.communityDiscoveries.collectAsState()

    // Posting Form States
    var showForm by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputLocation by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var inputClassification by remember { mutableStateOf("") }
    var inputTrajectory by remember { mutableStateOf("") }
    var selectedImgCode by remember { mutableStateOf("stony") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showForm = !showForm },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = if (isNight) Color.Black else Color(0xFF070B14)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("toggle_form_btn")
            ) {
                Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, "إضافة")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showForm) "إغلاق النموذج" else "أضف مشاركة واكتشاف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "مجتمع الفلك ومشاركة الاكتشافات",
                style = MaterialTheme.typography.titleMedium,
                color = activeColor,
                fontWeight = FontWeight.Bold
            )
        }

        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF080000) else Color(0xFF101524)
                ),
                border = BorderStroke(1.2.dp, activeColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("سجل اكتشافاً ميدانياً فلكياً جديداً (SQLite):", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("اسم الراصد / الباحث", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyle(textAlign = TextAlign.Right),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeColor),
                        modifier = Modifier.fillMaxWidth().testTag("add_discovery_name")
                    )

                    OutlinedTextField(
                        value = inputLocation,
                        onValueChange = { inputLocation = it },
                        placeholder = { Text("موقع العثور أو الرصد الجغرافي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyle(textAlign = TextAlign.Right),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeColor),
                        modifier = Modifier.fillMaxWidth().testTag("add_discovery_loc")
                    )

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        placeholder = { Text("ملاحظات تفصيلية عن العينة أو الشهاب المرئي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyle(textAlign = TextAlign.Right),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeColor),
                        modifier = Modifier.fillMaxWidth().testTag("add_discovery_notes")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputTrajectory,
                            onValueChange = { inputTrajectory = it },
                            placeholder = { Text("اتجاه المسار المتوقع", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(textAlign = TextAlign.Right),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeColor),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = inputClassification,
                            onValueChange = { inputClassification = it },
                            placeholder = { Text("التصنيف المقترح الصخري", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(textAlign = TextAlign.Right),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeColor),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Preset rock texture code selection
                    Text("نوع مظهر العينة الفلكية الدليلية:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val imgPresets = listOf(
                            "stony" to "حجرية كوندريت",
                            "iron" to "حديدية مصبوغة",
                            "pallasite" to "بالاسيت زجاجية"
                        )
                        imgPresets.forEach { (code, text) ->
                            val isSelected = selectedImgCode == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) activeColor else Color.Gray.copy(alpha = 0.3f)),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedImgCode = code }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text, fontSize = 10.sp, color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.shareDiscovery(
                                observerName = inputName,
                                location = inputLocation,
                                notes = inputNotes,
                                classification = inputClassification,
                                trajectory = inputTrajectory,
                                imageCode = selectedImgCode
                            )
                            // Clean inputs
                            inputName = ""
                            inputLocation = ""
                            inputNotes = ""
                            inputClassification = ""
                            inputTrajectory = ""
                            showForm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor, contentColor = if (isNight) Color.Black else Color(0xFF070B14)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_discovery_btn")
                    ) {
                        Text("نشر الاكتشاف في مجتمع الفلك العلمي الـ SQLite", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // List posts
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("المجتمع فارغ حالياً. كن أول من يكتب اكتشافاً ميدانياً!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            posts.forEach { post ->
                DiscoveryCommunityItemCard(post = post, onDelete = { viewModel.deletePost(post.id) }, isNight = isNight)
            }
        }
    }
}

@Composable
fun DiscoveryCommunityItemCard(post: DiscoveryPost, onDelete: () -> Unit, isNight: Boolean) {
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNight) Color(0xFF080000) else Color(0xFF101524)
        ),
        border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row with observer name and date delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(post.observerName, color = if (isNight) Color(0xFFFF3333) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(post.locationName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isNight) Color(0xFF2A0000) else Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, "فلكي مشارك", tint = activeColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Description notes
            Text(
                text = post.notesAr,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Extra classifications labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(if (isNight) Color(0xFF2E0000) else Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("اتجاه السقوط: ${post.estimatedTrajectoryAr}", color = activeColor, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .background(if (isNight) Color(0xFF2E0000) else Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("بنية الصخر: ${post.classificationAr}", color = activeColor, fontSize = 10.sp)
                }
            }

            // Small Micro rendering visual of the post
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, activeColor.copy(alpha = 0.2f)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                ProceduralMicroscopeCanvas(post.sampleImageCode, activeColor)
            }
        }
    }
}

// ==========================================================
// 6. OBSERVATORY INTEGRATION & SOUND ALERTS (المراصد والإنذارات)
// ==========================================================
@Composable
fun ObservatoryTabContent(viewModel: MeteorViewModel) {
    val isNight = viewModel.isNightObservationMode
    val activeColor = if (isNight) Color(0xFFFF1E1E) else Color(0xFF00FFCC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الاتصال بالمراصد العالمية المفتوحة والإنذارات الفورية",
            style = MaterialTheme.typography.titleMedium,
            color = activeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Text(
            text = "تتلقى هذه المنصة إشارات البث الحية وقواعد البيانات الفلكية التابعة لمرصدي NASA CNEOS و ESA NEOCC الفورية لسلامة كوكب الأرض وتنبيه الراصدين.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        // Configuration Checklist (Smartphone notifications + Email Alerts)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNight) Color(0xFF090000) else Color(0xFF101524)
            ),
            border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تعيين قنوات الاتصال والتحذيرات الرادارية اللحظية:",
                    color = activeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                // Push Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = viewModel.pushAlertsEnabled,
                        onCheckedChange = { viewModel.pushAlertsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.4f))
                    )
                    Text("إرسال الإشعارات الصوتية اللحظية للهاتف والاهتزاز", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }

                Divider(color = activeColor.copy(alpha = 0.12f))

                // Email Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = viewModel.emailAlertsEnabled,
                        onCheckedChange = { viewModel.emailAlertsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.4f))
                    )
                    Text("إرسال تقرير فني فوري للبريد الإلكتروني للراصدين", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
            }
        }

        // Live Alarms list
        Text(
            text = "شريط تنبيهات ومزامنات الرادارات المفتوحة المباشر:",
            fontWeight = FontWeight.Bold,
            color = activeColor,
            fontSize = 13.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        viewModel.observatoryAlerts.forEach { alert ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF0C0000) else Color(0xFF101524)
                ),
                border = BorderStroke(1.dp, if (isNight) Color(0xFF330000) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alert.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(alert.source, color = activeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Icon(Icons.Default.Warning, "مصدر", tint = activeColor, modifier = Modifier.size(13.dp))
                        }
                    }

                    Text(
                        text = alert.titleAr,
                        color = if (isNight) Color(0xFFFF5555) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right
                    )

                    Text(
                        text = alert.detailsAr,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = activeColor.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(alert.impactRiskAr, color = Color.Green.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(alert.magnitude, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
