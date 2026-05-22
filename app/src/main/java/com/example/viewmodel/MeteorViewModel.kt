package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MeteorTab {
    SKY_MAP,       // الخريطة السماوية
    HISTORY,       // السجل التاريخي
    MICROSCOPE,    // مجهر التحليل
    TRAJECTORY,    // تحديد المسارات
    COMMUNITY,     // المجتمع الفلكي
    OBSERVATORY    // المراصد والإنذارات
}

class MeteorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = androidx.room.Room.databaseBuilder(
        application,
        MeteorDatabase::class.java,
        "meteor_hunter_db"
    ).fallbackToDestructiveMigration().build()

    val repository = MeteorRepository(database.meteorDao())

    // --- Tab and Appearance ---
    var activeTab by mutableStateOf(MeteorTab.SKY_MAP)
    var isNightObservationMode by mutableStateOf(false)

    // --- Search Query ---
    val searchQuery = MutableStateFlow("")

    // --- Historical Falls (Filtered by Query) ---
    val historicalFalls: StateFlow<List<HistoricalFall>> = searchQuery
        .combine(repository.allHistoricalFalls) { query, falls ->
            if (query.isBlank()) {
                falls
            } else {
                falls.filter {
                    it.nameAr.contains(query, ignoreCase = true) ||
                    it.nameEn.contains(query, ignoreCase = true) ||
                    it.locationAr.contains(query, ignoreCase = true) ||
                    it.typeAr.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Selected Fall for Detail ---
    var selectedFall by mutableStateOf<HistoricalFall?>(null)

    // --- Community Feed ---
    val communityDiscoveries: StateFlow<List<DiscoveryPost>> = repository.allDiscoveries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Notification & Alerts Configuration ---
    var emailAlertsEnabled by mutableStateOf(true)
    var pushAlertsEnabled by mutableStateOf(true)
    var simulationAlertTriggered by mutableStateOf<String?>(null)

    // --- Microscope AI Analyzer State ---
    var selectedMicroscopeCode by mutableStateOf("chondrite") // "chondrite", "widmanstatten", "pallasite", "custom"
    var customMicroscopeBitmap by mutableStateOf<Bitmap?>(null)
    var microscopeAnalysisResult by mutableStateOf<String?>(null)
    var isAnalyzingMicroscope by mutableStateOf(false)

    // --- Trajectory AI State ---
    var entryAltitude by mutableStateOf("120") // km
    var entryAngle by mutableStateOf("45")     // degrees
    var entrySpeed by mutableStateOf("22")     // km/s
    var initialMass by mutableStateOf("1500")  // kg
    var observerLatitude by mutableStateOf("24.7136")  // Riyadh
    var observerLongitude by mutableStateOf("46.6753")
    var trajectoryAnalysisResult by mutableStateOf<String?>(null)
    var isAnalyzingTrajectory by mutableStateOf(false)

    // --- Observatory Feed Alerts ---
    val observatoryAlerts = listOf(
        ObservatoryAlert(
            source = "NASA CNEOS",
            titleAr = "رصد كرة نارية ساطعة فوق المحيط الأطلسي",
            time = "منذ 12 دقيقة",
            magnitude = "الطاقة الإجمالية المقدرة: 0.15 كيلوطن TNT",
            impactRiskAr = "لا يوجد خطر تصادم (احترق في الغلاف الجوي)",
            detailsAr = "رصدت محطات الاستشعار التابعة للحكومة الأمريكية كرة نارية ساطعة (Superbolide) عند خط عرض 15.2 شمالاً وخط طول 45.1 غرباً على ارتفاع 34 كم."
        ),
        ObservatoryAlert(
            source = "ESA NEOCC",
            titleAr = "كويكب صغير يقترب من الأرض بشكل آمن (2026 MD1)",
            time = "منذ ساعتين",
            magnitude = "القطر المقدر: 8-12 متر",
            impactRiskAr = "خطر الاصطدام: 0% (مسافة المرور: 0.8 المسافة القمرية)",
            detailsAr = "تم تصنيف الجرم ككويكب عابر لمدار الأرض بشكل آمن تماماً، مع فرصة ممتازة للمراصد غير المحترفة لتتبعه فوتوغرافياً الليلة."
        ),
        ObservatoryAlert(
            source = "مركز الكواكب الصغيرة (MPC)",
            titleAr = "تسجيل وابل شهب Perseids السنوي - النشاط يفوق المتوسط",
            time = "منذ 4 ساعات",
            magnitude = "معدل الشهب الساعي: 110 شهاب/ساعة",
            impactRiskAr = "حدث دوري فلكي رائع ومستمر",
            detailsAr = "سجلت المراصد العالمية زيادة واضحة في تأين ذرات الغبار المذنبية الصادرة عن المذنب Swift-Tuttle. نقطة الإشعاع تقع في كوكبة حامل رأس الغول."
        )
    )

    init {
        // Pre-populate Database and Community Posts on First Launch
        viewModelScope.launch {
            // Check if databases are empty
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (repository.allHistoricalFalls.first().isEmpty()) {
                        populateInitialFalls()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            // Check if databases are empty
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (repository.allDiscoveries.first().isEmpty()) {
                        populateInitialDiscoveries()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun populateInitialFalls() {
        val initialList = listOf(
            HistoricalFall(
                nameAr = "نيزك تشيليابنسك",
                nameEn = "Chelyabinsk",
                locationAr = "روسيا (أوبلاست تشيليابنسك)",
                year = "2013",
                mass = "12,000 طن (الكتلة الابتدائية)",
                typeAr = "كوندريت حجرية عادية LL5",
                descriptionAr = "أكبر صدمة نيزكية مسجلة منذ Tunguska. انفجر في الهواء على ارتفاع 29.7 كم بطاقة تفوق 400 كيلوطن من الـ TNT، مما أدى لتهشم النوافذ وإصابة أكثر من 1600 شخص بشظايا الزجاج العشوائية.",
                latitude = 55.1644,
                longitude = 61.4368,
                impactEnergy = "440 Kilotons TNT equivalent"
            ),
            HistoricalFall(
                nameAr = "حدث تونغوسكا الكوني",
                nameEn = "Tunguska Event",
                locationAr = "سيبيريا (نهر تونغوسكا)",
                year = "1908",
                mass = "100,000+ طن (مقدرة)",
                typeAr = "نيزك حجري أو نواة مذنب متفجرة",
                descriptionAr = "أدى انفجار النيزك في الجو إلى تسوية أكثر من 80 مليون شجرة بالأرض وصناعة موجات اهتزازية قوية سجلت في جميع أنحاء الكوكب دون ترك فوهة صدمية حقيقية بسبب تفجره الكامل غلاف الجو.",
                latitude = 60.9167,
                longitude = 101.9500,
                impactEnergy = "10-15 Megatons TNT"
            ),
            HistoricalFall(
                nameAr = "نيزك هوبا الحديدي",
                nameEn = "Hoba Meteorite",
                locationAr = "ناميبيا (قرب جروتفونتين)",
                year = "1920 (تم اكتشافه)",
                mass = "60 طناً",
                typeAr = "حديدية غنية بالنيكل (أتاكسيت)",
                descriptionAr = "أكبر نيزك سليم من قطعة واحدة تم العثور عليه على سطح كوكب الأرض على الإطلاق. لم يخلف أي فوهة صدمية تذكر لأن الغلاف الجوي فرمل حركته تدريجياً ليسقط سقوطاً حراً ناعماً.",
                latitude = -19.5925,
                longitude = 17.9336,
                impactEnergy = "مخمد بالاحتكاك الجوي"
            ),
            HistoricalFall(
                nameAr = "نيزك أليندي الكربوني",
                nameEn = "Allende Meteorite",
                locationAr = "المكسيك (ولاية تشيواوا)",
                year = "1969",
                mass = "2 طن",
                typeAr = "كوندريت كربونية CV3",
                descriptionAr = "يسمى 'حجر روزيتا لعلم الفلك'. يحتوي على شوائب الكالسيوم والألومنيوم (CAIs) والتي يعود تكوينها لـ 4.567 مليار سنة مضت، وهي أقدم مادة صلبة تشكلت في مجموعتنا الشمسية.",
                latitude = 26.9667,
                longitude = -105.2667,
                impactEnergy = "صدمة محلية متفرقة"
            ),
            HistoricalFall(
                nameAr = "فوهة بارينجر النيزكية",
                nameEn = "Barringer Crater",
                locationAr = "الولايات المتحدة (أريزونا)",
                year = "-50,000 سنة",
                mass = "300,000 طن",
                typeAr = "حديدية (نيازك كانيون ديابلو)",
                descriptionAr = "أول فوهة صدمية نيزكية مؤكدة بوضوح على الأرض. انفجر النيزك بالكامل تاركاً وراءه حفرة مذهلة يبلغ قطرها 1.2 كم وبعمق 170 متراً، زارها رواد فضاء أبولو للتدرب على بيئة القمر.",
                latitude = 35.0272,
                longitude = -111.0225,
                impactEnergy = "10 Megatons TNT"
            )
        )
        repository.insertFalls(initialList)
    }

    private suspend fun populateInitialDiscoveries() {
        val initialPosts = listOf(
            DiscoveryPost(
                observerName = "د. كمال التازي",
                locationName = "صحراء أدرار، موريتانيا",
                notesAr = "عثرنا على عينة صخرية داكنة جداً تزن 140 جراماً ذات قشرة انصهار مصقولة برياح الصحراء. تعطي مؤشراً قوياً على كونها كوندريت معدنية.",
                classificationAr = "كوندريت حجرية عادية",
                estimatedTrajectoryAr = "شمال غرب بزاوية سقوط 38 درجة",
                microAnalysisAr = "تفحص أولي يشير لوجود حبيبات حديدية لامعة داخل نسيج أوليفيني ناعم.",
                sampleImageCode = "stony"
            ),
            DiscoveryPost(
                observerName = "م. سارة الحربي",
                locationName = "صحراء الربع الخالي، السعودية",
                notesAr = "تم العثور على حجر معدني ذو وزن نوعي عالٍ جداً ومغناطيسية قوية مع فوهات انصهار ريغماغليبتس كلاسيكية تشبه حفر بصمات الأصابع.",
                classificationAr = "نيزك حديدي أوكتاهيدريت",
                estimatedTrajectoryAr = "مسار جنوبي شرقي مائل",
                microAnalysisAr = "نسيج Widmanstätten الهندسي يظهر بوضوح بعد حفر السطح بحمض النيتريك الخفيف للتلميع.",
                sampleImageCode = "iron"
            )
        )
        for (post in initialPosts) {
            repository.insertDiscovery(post)
        }
    }

    // --- Action Methods ---

    /**
     * Submit a Discovery Post by user
     */
    fun shareDiscovery(
        observerName: String,
        location: String,
        notes: String,
        classification: String,
        trajectory: String,
        imageCode: String
    ) {
        viewModelScope.launch {
            val newPost = DiscoveryPost(
                observerName = observerName.ifBlank { "راصد هاوٍ" },
                locationName = location.ifBlank { "موقع مراقبة غير مسمى" },
                notesAr = notes.ifBlank { "تم رصد سقوط نيزكي ومطابقة قشرة الانصهار." },
                classificationAr = classification.ifBlank { "تصنيف غير معروف" },
                estimatedTrajectoryAr = trajectory.ifBlank { "غير مسجل" },
                sampleImageCode = imageCode
            )
            repository.insertDiscovery(newPost)
            // Trigger simulated push notification
            triggerSimulationAlert("أنشأت اكتشافاً علمياً جديداً بنجاح في مجتمع علم الفلك!")
        }
    }

    /**
     * Delete user post
     */
    fun deletePost(id: Int) {
        viewModelScope.launch {
            repository.deleteDiscovery(id)
        }
    }

    /**
     * Call AI to analyze Electron Microscope Images
     */
    fun analyzeMicroscopeImage(presetBitmap: Bitmap?) {
        viewModelScope.launch {
            isAnalyzingMicroscope = true
            microscopeAnalysisResult = null

            val targetBitmap = if (selectedMicroscopeCode == "custom") {
                customMicroscopeBitmap
            } else {
                presetBitmap
            }

            if (targetBitmap == null) {
                microscopeAnalysisResult = "الرجاء اختيار أو تحميل صورة مجهر إلكتروني عالي الدقة صالحة للتحليل."
                isAnalyzingMicroscope = false
                return@launch
            }

            val prompt = """
                قم بإجراء تحليل مجهري الكتروني (SEM/EDX) فائق الدقة لهذه الصورة لعينة نيزك مفترضة باللغة العربية.
                يرجى هيكلة التحليل كالتالي:
                1. المكونات المعدنية الأساسية (مثل الحديد، النيكل، الأوليفين، البيروكسين، حبيبات الكوندريت).
                2. البنية المجهرية والنسيجية (البلورات، شبكات فيدمان شتاتن، أو الصدمات المجهرية).
                3. تصنيف النيزك المقترح علمياً بناءً على هذه المجهريات (نيازك حديدية، كوندريت حجرية، بالاسيت زجاجية).
                4. احتمالية النشأة الفضائية خارج الأرض ومكان الصدر المحتمل في حزام الكويكبات.
                اجعل التحليل دقيقاً واحترافياً ومثيراً للحماس والهواة والمحترفين على حد سواء في فقرات منسقة بدون استخدام الرموز المعقدة وبصيغة عربية فصيغة ملهمة.
            """.trimIndent()

            val result = GeminiClient.analyzeMeteorImage(targetBitmap, prompt)
            microscopeAnalysisResult = result
            isAnalyzingMicroscope = false
        }
    }

    /**
     * Call AI to compute high-accuracy meteor trajectory and strewn field
     */
    fun calculateMeteorTrajectory() {
        viewModelScope.launch {
            isAnalyzingTrajectory = true
            trajectoryAnalysisResult = null

            val prompt = """
                بصفتك خبيراً في ميكانيكا الأجسام الفضائية والمسارات النيزكية في الإدارة الوطنية للملاحة الجوية والفضاء،
                احسب مسار النيزك الدقيق والنتيجة المحتملة لحقل السقوط (Strewn Field) والخصائص الصدمية لتلك البيانات:
                - ارتفاع دخول الغلاف الجوي: $entryAltitude كم
                - زاوية الدخول المرصودة: $entryAngle درجة بالنسبة للأفق
                - السرعة الابتدائية للدخول: $entrySpeed كم/ثانية
                - الكتلة المقدرة للجرم: $initialMass كجم
                - موقع الراصد الجغرافي: خط عرض $observerLatitude، خط طول $observerLongitude
                
                يرجى محاكاة الميكانيكا الفيزيائية بالذكاء الاصطناعي وتقديم تقرير علمي عربي مفصل يغطي:
                1. معدل الفقد الكتلي بالتبخير والفرملة الجوية (Ablation and Drag dynamics).
                2. نقطة التموج والانفجار الجوي المتوقعة (Airburst details & Altitude).
                3. تقدير دقيق للغاية لإحداثيات حقل السقوط المحتمل (Strewn Field Ellipse Coordinates) على شكل نقاط جغرافية نسبية مقترحة للتفتيش والبحث الميداني.
                4. مصدر الجرم الفلكي المحتمل في النظام الشمسي (مدار الكويكبات القريبة من الأرض أو عائلة مذنبية محددة).
                أجب بشكل احترافي رائع ومفصل لتمكين المهتمين من العمل الميداني.
            """.trimIndent()

            val result = GeminiClient.generateText(prompt, systemInstruction = "أنت حاسوب فلكي ذكي مدعم بالذكاء الاصطناعي مخصص لحساب مسارات تساقط الصخور الفضائية وتأثيراتها الغلافية.")
            trajectoryAnalysisResult = result
            isAnalyzingTrajectory = false
        }
    }

    /**
     * Trigger simulated visual push notification inside the app
     */
    fun triggerSimulationAlert(message: String) {
        simulationAlertTriggered = message
    }

    fun dismissSimulationAlert() {
        simulationAlertTriggered = null
    }
}

// Data class to represent Observatory Alerts
data class ObservatoryAlert(
    val source: String,
    val titleAr: String,
    val time: String,
    val magnitude: String,
    val impactRiskAr: String,
    val detailsAr: String
)
