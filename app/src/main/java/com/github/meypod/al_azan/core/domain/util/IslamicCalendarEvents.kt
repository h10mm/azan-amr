package com.github.meypod.al_azan.core.domain.util

import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Islamic Event and Fasting Day classification.
 */
data class IslamicDayEvent(
    val title: String,
    val type: IslamicEventType,
    val badge: String,
    val description: String? = null,
)

enum class IslamicEventType {
    Occasion,          // مناسبة إسلامية (رأس السنة، المولد، الإسراء والمعراج، العيدان، عرفة...)
    FastingFard,       // صيام فريضة (رمضان المبارك)
    FastingSunnah,     // صيام تطوع مسنون (الإثنين والخميس، تاسوعاء، عاشوراء، عرفة، عشر ذي الحجة، ست من شوال)
    WhiteDay,          // الأيام البيض (13، 14، 15 من الشهر الهجري)
    FastingForbidden,  // أيام يحرم صيامها شرعاً (العيدان وأيام التشريق)
}

object IslamicCalendarEvents {

    /**
     * Resolves all Islamic occasions, white days, and fasting statuses for a civil [date]
     * under the specified [lunarSystem].
     */
    fun getEventsForDate(date: LocalDate, lunarSystem: CalendarSystem): List<IslamicDayEvent> {
        val hijriDate = try {
            calendarDateOf(date, lunarSystem)
        } catch (_: Exception) {
            return emptyList()
        }

        val month = hijriDate.month
        val day = hijriDate.day
        val dayOfWeek = date.dayOfWeek

        val events = mutableListOf<IslamicDayEvent>()

        // 1. الأيام المحرم صيامها (عيد الفطر، عيد الأضحى، وأيام التشريق)
        val isForbiddenFasting = when {
            month == 10 && day == 1 -> true // عيد الفطر
            month == 12 && day in 10..13 -> true // عيد الأضحى وأيام التشريق (11، 12، 13)
            else -> false
        }

        // 2. المناسبات الدينية السنوية الكبرى
        when (month) {
            1 -> {
                when (day) {
                    1 -> events.add(
                        IslamicDayEvent(
                            title = "رأس السنة الهجرية",
                            type = IslamicEventType.Occasion,
                            badge = "🕌",
                            description = "بداية العام الهجري الجديد وغرة محرم من الأشهر الحُرُم.",
                        )
                    )
                    9 -> events.add(
                        IslamicDayEvent(
                            title = "تاسوعاء",
                            type = IslamicEventType.Occasion,
                            badge = "🌙",
                            description = "اليوم التاسع من محرم، يُسن صيامه مع عاشوراء مخالفةً لأهل الكتاب.",
                        )
                    )
                    10 -> events.add(
                        IslamicDayEvent(
                            title = "يوم عاشوراء",
                            type = IslamicEventType.Occasion,
                            badge = "⭐",
                            description = "اليوم العاشر من محرم، نجّى الله فيه موسى وقومه، صيامه يُكفّر ذنوب سنة ماضية.",
                        )
                    )
                }
            }
            3 -> {
                if (day == 12) {
                    events.add(
                        IslamicDayEvent(
                            title = "المولد النبوي الشريف",
                            type = IslamicEventType.Occasion,
                            badge = "🕌",
                            description = "ذكرى مولد خير الأنام محمد ﷺ رحمةً للعالمين.",
                        )
                    )
                }
            }
            7 -> {
                when (day) {
                    1 -> events.add(
                        IslamicDayEvent(
                            title = "غرة شهر رجب",
                            type = IslamicEventType.Occasion,
                            badge = "🌙",
                            description = "غرة شهر رجب الأصب وهو من الأشهر الحُرُم الأربعة.",
                        )
                    )
                    27 -> events.add(
                        IslamicDayEvent(
                            title = "ذكرى الإسراء والمعراج",
                            type = IslamicEventType.Occasion,
                            badge = "🕌",
                            description = "معجزة إسراء النبي ﷺ إلى المسجد الأقصى وعروجه إلى السماوات العُلى وفرض الصلوات الخمس.",
                        )
                    )
                }
            }
            8 -> {
                when (day) {
                    1 -> events.add(
                        IslamicDayEvent(
                            title = "غرة شهر شعبان",
                            type = IslamicEventType.Occasion,
                            badge = "🌙",
                            description = "بداية شهر شعبان المبارك، شهر ترفع فيه الأعمال ويستحب فيه الإكثار من الصيام.",
                        )
                    )
                    15 -> events.add(
                        IslamicDayEvent(
                            title = "ليلة النصف من شعبان",
                            type = IslamicEventType.Occasion,
                            badge = "⭐",
                            description = "ليلة مباركة يغفر الله فيها لخلقه إلا لمشرك أو مشاحن، وفيها كان تحويل القبلة.",
                        )
                    )
                }
            }
            9 -> {
                when {
                    day == 1 -> events.add(
                        IslamicDayEvent(
                            title = "غرة شهر رمضان المبارك",
                            type = IslamicEventType.Occasion,
                            badge = "🌙",
                            description = "أول أيام شهر رمضان المبارك، شهر الصيام والقرآن والرحمة والمغفرة.",
                        )
                    )
                    day == 27 -> events.add(
                        IslamicDayEvent(
                            title = "ليلة 27 رمضان (تحري ليلة القدر)",
                            type = IslamicEventType.Occasion,
                            badge = "🌟",
                            description = "أرجى ليالي الوتر في العشر الأواخر لتحري ليلة القدر المباركة خير من ألف شهر.",
                        )
                    )
                    day in 21..30 -> events.add(
                        IslamicDayEvent(
                            title = "العشر الأواخر من رمضان",
                            type = IslamicEventType.Occasion,
                            badge = "✨",
                            description = "أيام وليالي العتق من النار والاعتكاف والاجتهاد في العبادة.",
                        )
                    )
                }
            }
            10 -> {
                when (day) {
                    1 -> events.add(
                        IslamicDayEvent(
                            title = "عيد الفطر المبارك (اليوم الأول)",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "أول أيام عيد الفطر المبارك، يوم الفرح والجائزة وإتمام الصيام (يحرم صيامه).",
                        )
                    )
                    2 -> events.add(
                        IslamicDayEvent(
                            title = "ثاني أيام عيد الفطر",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "ثاني أيام العيد، وبداية إمكان صيام الست من شوال لمن أراد.",
                        )
                    )
                    3 -> events.add(
                        IslamicDayEvent(
                            title = "ثالث أيام عيد الفطر",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "ثالث أيام عيد الفطر السعيد.",
                        )
                    )
                }
            }
            11 -> {
                if (day == 1) {
                    events.add(
                        IslamicDayEvent(
                            title = "غرة شهر ذي القعدة",
                            type = IslamicEventType.Occasion,
                            badge = "🌙",
                            description = "أول أشهر الحج وأحد الأشهر الحُرُم الأربعة.",
                        )
                    )
                }
            }
            12 -> {
                when (day) {
                    1 -> events.add(
                        IslamicDayEvent(
                            title = "غرة ذي الحجة (العشر الأوائل)",
                            type = IslamicEventType.Occasion,
                            badge = "✨",
                            description = "بداية الأيام العشر الأولى من ذي الحجة، العمل الصالح فيها أحب إلى الله من غيرها.",
                        )
                    )
                    8 -> events.add(
                        IslamicDayEvent(
                            title = "يوم التروية",
                            type = IslamicEventType.Occasion,
                            badge = "🕋",
                            description = "اليوم الثامن من ذي الحجة، يتوجه فيه الحجاج إلى مِنى تروياً واستعداداً ليوم عرفة.",
                        )
                    )
                    9 -> events.add(
                        IslamicDayEvent(
                            title = "يوم عرفة (خير يوم طلعت فيه الشمس)",
                            type = IslamicEventType.Occasion,
                            badge = "🕋",
                            description = "الركن الأعظم للحج، وصيامه لغير الحاج يكفر السنة الماضية والباقية.",
                        )
                    )
                    10 -> events.add(
                        IslamicDayEvent(
                            title = "عيد الأضحى المبارك (يوم النحر)",
                            type = IslamicEventType.Occasion,
                            badge = "🐑",
                            description = "أعظم أيام الدهر، يوم النحر وأول أيام عيد الأضحى (يحرم صيامه).",
                        )
                    )
                    11 -> events.add(
                        IslamicDayEvent(
                            title = "ثاني أيام الأضحى (يوم القَرّ)",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "أول أيام التشريق ويوم القرّ حيث يستقر الحجاج بمنى (يحرم صيامه).",
                        )
                    )
                    12 -> events.add(
                        IslamicDayEvent(
                            title = "ثالث أيام الأضحى (يوم النفر الأول)",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "ثاني أيام التشريق ويوم النفر الأول للمتعجلين من منى (يحرم صيامه).",
                        )
                    )
                    13 -> events.add(
                        IslamicDayEvent(
                            title = "رابع أيام الأضحى (يوم النفر الثاني)",
                            type = IslamicEventType.Occasion,
                            badge = "🎉",
                            description = "ثالث أيام التشريق ويوم النفر الأخير وإتمام مناسك الحج (يحرم صيامه).",
                        )
                    )
                }
            }
        }

        // 3. الأيام البيض (13، 14، 15 من كل شهر هجري)
        // تنبيه فقهي: يوم 13 ذي الحجة من أيام التشريق التي يحرم صيامها.
        if (month != 9) { // في رمضان كله صيام فريضة
            if (month == 12) {
                if (day in 14..15) {
                    events.add(
                        IslamicDayEvent(
                            title = "الأيام البيض (اليوم $day من ذي الحجة)",
                            type = IslamicEventType.WhiteDay,
                            badge = "🌕",
                            description = "سنة صيام الأيام البيض المستحبة (يوم 13 كان تشريقاً محرماً ويستحب صيام 14 و15).",
                        )
                    )
                }
            } else {
                if (day in 13..15) {
                    events.add(
                        IslamicDayEvent(
                            title = "الأيام البيض (اليوم $day من الشهر)",
                            type = IslamicEventType.WhiteDay,
                            badge = "🌕",
                            description = "عن النبي ﷺ: صيام ثلاثة أيام من كل شهر صيام الدهر (13 و14 و15).",
                        )
                    )
                }
            }
        }

        // 4. حالة الصيام الشرعي لليوم (فريضة / مسنون / محرم)
        if (isForbiddenFasting) {
            events.add(
                IslamicDayEvent(
                    title = "يحرم صيامه شرعاً",
                    type = IslamicEventType.FastingForbidden,
                    badge = "⛔",
                    description = "نهى رسول الله ﷺ عن صيام يومي الفطر والأضحى وأيام التشريق.",
                )
            )
        } else if (month == 9) {
            // شهر رمضان المبارك كامل: فريضة
            events.add(
                IslamicDayEvent(
                    title = "صيام رمضان (فريضة)",
                    type = IslamicEventType.FastingFard,
                    badge = "🌙",
                    description = "ركن الإسلام الرابع، صيام يوم $day من شهر رمضان المبارك.",
                )
            )
        } else {
            // أيام الصيام المسنونة الخاصة بالمناسبات
            when {
                month == 1 && day == 9 -> events.add(
                    IslamicDayEvent(
                        title = "سنة صيام تاسوعاء",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "قال النبي ﷺ: «لئن بقيت إلى قابل لأصومن التاسع».",
                    )
                )
                month == 1 && day == 10 -> events.add(
                    IslamicDayEvent(
                        title = "سنة صيام عاشوراء المؤكدة",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "صيام عاشوراء أحتسب على الله أن يكفر السنة التي قبله.",
                    )
                )
                month == 12 && day == 9 -> events.add(
                    IslamicDayEvent(
                        title = "سنة صيام يوم عرفة",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "صيام يوم عرفة لغير الحاج يكفر ذنوب سنتين (الماضية والقادمة).",
                    )
                )
                month == 12 && day in 1..8 -> events.add(
                    IslamicDayEvent(
                        title = "صيام العشر الأوائل من ذي الحجة",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "يستحب صيام الأيام التسعة الأولى من ذي الحجة لعظم فضلها.",
                    )
                )
                // صيام الإثنين والخميس الأسبوعي المستمر
                dayOfWeek == DayOfWeek.MONDAY -> events.add(
                    IslamicDayEvent(
                        title = "سنة صيام يوم الإثنين",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "عن النبي ﷺ: «تُعرض الأعمال يوم الإثنين والخميس، فأحب أن يُعرض عملي وأنا صائم».",
                    )
                )
                dayOfWeek == DayOfWeek.THURSDAY -> events.add(
                    IslamicDayEvent(
                        title = "سنة صيام يوم الخميس",
                        type = IslamicEventType.FastingSunnah,
                        badge = "🌙",
                        description = "عن النبي ﷺ: «تُعرض الأعمال يوم الإثنين والخميس، فأحب أن يُعرض عملي وأنا صائم».",
                    )
                )
            }
        }

        return events
    }

    /**
     * Checks if this day is one of the White Days (الأيام البيض: 13, 14, 15).
     */
    fun isWhiteDay(date: LocalDate, lunarSystem: CalendarSystem): Boolean {
        return try {
            val h = calendarDateOf(date, lunarSystem)
            if (h.month == 9) false // In Ramadan, whole month is fard
            else if (h.month == 12) h.day in 14..15 // 13 Dhu al-Hijjah is Tashriq (forbidden)
            else h.day in 13..15
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if fasting is recommended or mandatory on this day.
     */
    fun isFastingDay(date: LocalDate, lunarSystem: CalendarSystem): Boolean {
        val events = getEventsForDate(date, lunarSystem)
        return events.any {
            it.type == IslamicEventType.FastingFard ||
                it.type == IslamicEventType.FastingSunnah ||
                it.type == IslamicEventType.WhiteDay
        }
    }

    /**
     * Returns true if there is an Islamic occasion / holiday on this day.
     */
    fun hasOccasion(date: LocalDate, lunarSystem: CalendarSystem): Boolean {
        val events = getEventsForDate(date, lunarSystem)
        return events.any { it.type == IslamicEventType.Occasion }
    }
}
