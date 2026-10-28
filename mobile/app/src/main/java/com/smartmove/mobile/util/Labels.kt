package com.smartmove.mobile.util

object Labels {
    private val installationStatuses = mapOf(
        "DRAFT" to "작성 중",
        "SUBMITTED" to "접수 완료",
        "ANALYZING" to "분석 중",
        "NEEDS_REVIEW" to "확인 필요",
        "QUOTED" to "견적 완료",
        "RESERVED" to "예약 확정",
        "IN_PROGRESS" to "작업 중",
        "COMPLETED" to "완료",
        "CANCELLED" to "취소",
    )

    private val quoteStatuses = mapOf(
        "DRAFT" to "작성 중",
        "ISSUED" to "발행됨",
        "ACCEPTED" to "수락됨",
        "EXPIRED" to "만료",
        "CANCELLED" to "취소",
    )

    private val reservationStatuses = mapOf(
        "PENDING" to "확정 대기",
        "CONFIRMED" to "예약 확정",
        "RESCHEDULE_REQUESTED" to "변경 요청",
        "CANCELLED" to "취소",
        "EXPIRED" to "만료",
        "COMPLETED" to "완료",
    )

    private val assignmentStatuses = mapOf(
        "PENDING" to "배정 대기",
        "ACCEPTED" to "수락",
        "REJECTED" to "거절",
        "ON_THE_WAY" to "이동 중",
        "WORK_STARTED" to "작업 중",
        "WORK_COMPLETED" to "작업 완료",
        "CANCELLED" to "취소",
    )

    private val approvalStatuses = mapOf(
        "PENDING" to "승인 대기",
        "APPROVED" to "승인됨",
        "REJECTED" to "거절됨",
    )

    private val dayNames = mapOf(
        1 to "월",
        2 to "화",
        3 to "수",
        4 to "목",
        5 to "금",
        6 to "토",
        7 to "일",
    )

    fun installationStatus(status: String): String = installationStatuses[status] ?: status
    fun quoteStatus(status: String): String = quoteStatuses[status] ?: status
    fun reservationStatus(status: String): String = reservationStatuses[status] ?: status
    fun assignmentStatus(status: String): String = assignmentStatuses[status] ?: status
    fun approvalStatus(status: String): String = approvalStatuses[status] ?: status
    fun dayName(dayOfWeek: Int): String = dayNames[dayOfWeek] ?: dayOfWeek.toString()

    fun eventStatusLabel(eventType: String): String = when (eventType) {
        "ON_THE_WAY" -> "출발"
        "WORK_STARTED" -> "작업 시작"
        "WORK_COMPLETED" -> "작업 완료"
        else -> assignmentStatus(eventType)
    }
}
