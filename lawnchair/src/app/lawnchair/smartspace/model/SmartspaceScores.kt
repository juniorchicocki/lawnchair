package app.lawnchair.smartspace.model

object SmartspaceScores {
    const val SCORE_WEATHER = 0f
    const val SCORE_BATTERY = 1f
    const val SCORE_MEDIA = 2f
    const val SCORE_CALENDAR = 3f
    const val SCORE_HIGHLANDER_FINANCE = 3.5f
    const val SCORE_HIGHLANDER_VEHICLE = 4f
    const val SCORE_HIGHLANDER_TASKS = 4.5f
    const val SCORE_LOW_BATTERY = 10f
    const val SCORE_FLASHLIGHT = 11f
    // Acima de SCORE_LOW_BATTERY de propósito: uma tarefa/compromisso já
    // vencido é mais acionável agora do que a bateria estar baixa.
    const val SCORE_HIGHLANDER_TASKS_OVERDUE = 10.5f
    const val SCORE_ONBOARDING = 1000f
}
