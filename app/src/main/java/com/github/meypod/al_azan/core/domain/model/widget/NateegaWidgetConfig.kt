package com.github.meypod.al_azan.core.domain.model.widget

import kotlinx.serialization.Serializable

@Serializable
data class NateegaWidgetConfig(
    val clockPosition: NateegaClockPosition = NateegaClockPosition.Hidden,
    val showCopticDate: Boolean = true,
    val showCountdown: Boolean = true,
    val showWisdom: Boolean = true,
    val languageMode: NateegaLanguageMode = NateegaLanguageMode.AppLanguage,
    val themeMode: NateegaThemeMode = NateegaThemeMode.MatchAppTheme,
    val dateDisplayMode: NateegaDateDisplayMode = NateegaDateDisplayMode.Both,
    val dateOrder: NateegaDateOrder = NateegaDateOrder.HijriRightGregorianLeft,
    val compactThemeMode: NateegaThemeMode = NateegaThemeMode.MatchAppTheme,
    val compactDateDisplayMode: NateegaDateDisplayMode = NateegaDateDisplayMode.Both,
    val compactDateOrder: NateegaDateOrder = NateegaDateOrder.HijriRightGregorianLeft,
    val compactShowCountdown: Boolean = true,
    val compactShowWisdom: Boolean = true,
    val nextPrayerShape: NextPrayerShape = NextPrayerShape.Circle,
    val nextPrayerBackground: NextPrayerBackground = NextPrayerBackground.Transparent,
    val nextPrayerProgressColor: WidgetColorOption = WidgetColorOption.Emerald,
    val nextPrayerCountdownColor: WidgetColorOption = WidgetColorOption.Gold,
    val nextPrayerNameColor: WidgetColorOption = WidgetColorOption.White,
    val deskCalendarShowCountdown: Boolean = true,
    val deskCalendarType: DeskCalendarType = DeskCalendarType.Gregorian,
    val deskCalendarHighlightNextPrayer: Boolean = true,
    val deskCalendarThemeMode: NateegaThemeMode = NateegaThemeMode.MatchAppTheme,
    val dialThemeMode: DialThemeMode = DialThemeMode.MatchAppTheme,
    val squareThemeMode: SquareThemeMode = SquareThemeMode.MatchAppTheme,
    val slimBarThemeMode: SlimBarThemeMode = SlimBarThemeMode.MatchAppTheme,
    val slimBarShowCity: Boolean = true,
    val slimBarDateOrder: SlimBarDateOrder = SlimBarDateOrder.HijriFirst,
    val mosqueArchThemeMode: MosqueArchThemeMode = MosqueArchThemeMode.MatchAppTheme,
    val smartPillThemeMode: SmartPillThemeMode = SmartPillThemeMode.MatchAppTheme,
    val smartPillShowWordBadge: Boolean = false,
    val showSpiritualPillNotification: Boolean = true,
    val spiritualCapsuleMode: SpiritualCapsuleMode = SpiritualCapsuleMode.SmartMoments,
    val customDhikrEnabled: Boolean = false,
    val customDhikrTimeMinutes: Int = 540,
    val customDhikrText: String = "سبحان الله وبحمده، سبحان الله العظيم",
)

@Serializable
enum class SpiritualCapsuleMode {
    SmartMoments,
    PersistentShade,
    Off;

    fun labelArabic(): String = when (this) {
        SmartMoments -> "تنبيهات لحظية ذكية (المحرك العبادي اللحظي)"
        PersistentShade -> "إشعار دائم مستمر في شريط الإشعارات"
        Off -> "متوقف"
    }
}

@Serializable
enum class SmartPillThemeMode {
    MatchAppTheme,
    Transparent,
    AmoledDark,
    MidnightBlue,
    ClassicWhite;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر الواجهة)"
        Transparent -> "شفاف زجاجي (بدون خلفية مع حدود ناعمة)"
        AmoledDark -> "أسود أموليد (AMOLED داكن)"
        MidnightBlue -> "أزرق ليلي (Midnight Blue فاخر)"
        ClassicWhite -> "أبيض كلاسيكي (Classic White)"
    }
}

@Serializable
enum class MosqueArchThemeMode {
    MatchAppTheme,
    Transparent,
    AmoledDark,
    MidnightBlue,
    ClassicWhite;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر الواجهة)"
        Transparent -> "شفاف (بدون خلفية مع حدود ناعمة)"
        AmoledDark -> "أسود أموليد (AMOLED داكن وفاخر)"
        MidnightBlue -> "أزرق ليلي تراثي (Midnight Blue إسلامي)"
        ClassicWhite -> "أبيض كلاسيكي (Classic White كارت ناصع)"
    }
}

@Serializable
enum class SlimBarDateOrder {
    HijriFirst,
    GregorianFirst;

    fun labelArabic(): String = when (this) {
        HijriFirst -> "الهجري أولاً (في الأعلى) | الميلادي ثانياً"
        GregorianFirst -> "الميلادي أولاً (في الأعلى) | الهجري ثانياً"
    }
}

@Serializable
enum class SlimBarThemeMode {
    MatchAppTheme,
    Transparent,
    AmoledDark,
    MidnightBlue,
    ClassicWhite;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر الواجهة)"
        Transparent -> "شفاف (بدون خلفية مع حدود ناعمة)"
        AmoledDark -> "أسود أموليد (AMOLED داكن وفاخر)"
        MidnightBlue -> "أزرق ليلي (Midnight Blue كحلي أنيق)"
        ClassicWhite -> "أبيض كلاسيكي (Classic White كارت ناصع)"
    }
}

@Serializable
enum class SquareThemeMode {
    MatchAppTheme,
    Transparent,
    AmoledDark,
    MidnightBlue,
    ClassicWhite;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر الواجهة)"
        Transparent -> "شفاف (بدون خلفية مع حدود ناعمة)"
        AmoledDark -> "أسود أموليد (AMOLED داكن وفاخر)"
        MidnightBlue -> "أزرق ليلي (Midnight Blue كحلي أنيق)"
        ClassicWhite -> "أبيض كلاسيكي (Classic White كارت ناصع)"
    }
}

@Serializable
enum class DialThemeMode {
    MatchAppTheme,
    Transparent,
    AmoledDark,
    MidnightBlue,
    ClassicWhite;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر الواجهة)"
        Transparent -> "شفاف (بدون خلفية مع توهج الحواف)"
        AmoledDark -> "أسود أموليد (AMOLED داكن وفاخر)"
        MidnightBlue -> "أزرق ليلي (Midnight Blue كحلي أنيق)"
        ClassicWhite -> "أبيض كلاسيكي (Classic White كارت ناصع)"
    }
}

@Serializable
enum class DeskCalendarType {
    Gregorian,
    Hijri,
    Coptic;

    fun labelArabic(): String = when (this) {
        Gregorian -> "التقويم الميلادي"
        Hijri -> "التقويم الهجري"
        Coptic -> "التقويم القبطي"
    }
}

@Serializable
enum class NextPrayerBackground {
    Transparent,
    Dark;

    fun labelArabic(): String = when (this) {
        Transparent -> "خلفية شفافة (تظهر خلفية الشاشة)"
        Dark -> "خلفية داكنة (قرص معتم)"
    }
}

@Serializable
enum class NextPrayerShape {
    Circle,
    Square;

    fun labelArabic(): String = when (this) {
        Circle -> "دائري (مع شريط محيطي)"
        Square -> "مربع (مع شريط أفقي)"
    }
}

@Serializable
enum class WidgetColorOption(val colorHex: Long, val labelAr: String) {
    Emerald(0xFF10B981, "زمردي"),
    Gold(0xFFF59E0B, "ذهبي"),
    Cyan(0xFF06B6D4, "سماوي"),
    Blue(0xFF3B82F6, "أزرق ملكي"),
    White(0xFFF8FAFC, "أبيض ناصع"),
    Coral(0xFFEF4444, "مرجاني");
}

@Serializable
enum class NateegaDateDisplayMode {
    Both,
    HijriOnly,
    GregorianOnly;

    fun labelArabic(): String = when (this) {
        Both -> "كلاهما معاً (هجري وميلادي)"
        HijriOnly -> "هجري فقط"
        GregorianOnly -> "ميلادي فقط"
    }
}

@Serializable
enum class NateegaDateOrder {
    HijriRightGregorianLeft,
    GregorianRightHijriLeft;

    fun labelArabic(): String = when (this) {
        HijriRightGregorianLeft -> "هجري يمين | ميلادي يسار"
        GregorianRightHijriLeft -> "ميلادي يمين | هجري يسار"
    }
}

@Serializable
enum class NateegaClockPosition {
    Hidden,
    CenterLarge,
    InlineWithDay,
    Side;

    fun labelArabic(): String = when (this) {
        Hidden -> "بدون ساعة (الافتراضي)"
        CenterLarge -> "كبيرة في المنتصف"
        InlineWithDay, Side -> "جانبية مع اليوم (يمين ويسار)"
    }
}

@Serializable
enum class NateegaLanguageMode {
    AppLanguage,
    ArabicOnly,
    EnglishOnly,
    Bilingual;

    fun labelArabic(): String = when (this) {
        AppLanguage -> "تلقائي (حسب لغة التطبيق)"
        ArabicOnly -> "عربي فقط"
        EnglishOnly -> "إنجليزي فقط"
        Bilingual -> "ثنائي اللغة (عربي + إنجليزي)"
    }
}

@Serializable
enum class NateegaThemeMode {
    MatchAppTheme,
    ClassicPaper,
    Amoled,
    MidnightBlue;

    fun labelArabic(): String = when (this) {
        MatchAppTheme -> "تلقائي (حسب مظهر التطبيق)"
        ClassicPaper -> "ورق كلاسيكي مصري"
        Amoled -> "أسود فاحم (AMOLED)"
        MidnightBlue -> "أزرق منتصف الليل (Midnight Blue)"
    }
}
