package app.lawnchair.smartspace.provider.highlander

import android.net.Uri

/**
 * Autoridades, colunas e componentes dos dois apps Highlander (Meu Diário,
 * Meu Veículo) que expõem dados pro At a Glance via ContentProvider
 * somente-leitura, protegido por uma permissão de nível
 * `signature|knownSigner` cada.
 *
 * Espelha exatamente o lado servidor de cada app (HighlanderDataProvider.kt
 * no Meu Diário, HighlanderVehicleDataProvider.kt no Meu Veículo) — os dois
 * lados foram escritos juntos, então qualquer mudança de schema precisa
 * mexer nos dois.
 *
 * Não existe mais um terceiro contrato pro Financas: aquele app foi
 * descontinuado e suas telas migraram pra dentro do Meu Diário (que manteve
 * o applicationId original, "com.highlander.tasks" — só o nome visível
 * mudou pra "Meu Diário"), então o resumo financeiro agora sai do MESMO
 * authority da agenda, só num path diferente (ver PATH_FINANCE_SUMMARY).
 */
internal object HighlanderTasksContract {
    const val PACKAGE_NAME = "com.highlander.tasks"
    const val MAIN_ACTIVITY = "com.projecttasks.project_tasks_mobile.MainActivity"
    const val AUTHORITY = "com.highlander.tasks.data"
    val SUMMARY_URI: Uri = Uri.parse("content://$AUTHORITY/summary")
    val FINANCE_SUMMARY_URI: Uri = Uri.parse("content://$AUTHORITY/finance_summary")

    // Abre a guia Compromissos/Finanças direto (ver AgendaSource.kt/
    // home_shell.dart no lado Dart) — mesmo esquema que os atalhos
    // estáticos do app usam. Host "tarefas" mantido por trás apesar do
    // rebrand pra "Meu Diário" — é só o identificador técnico do esquema.
    const val DEEP_LINK_AGENDA = "highlander://tarefas/agenda"
    const val DEEP_LINK_FINANCE = "highlander://tarefas/finance"

    const val COL_TODAY_COUNT = "today_count"
    const val COL_OVERDUE_COUNT = "overdue_count"
    const val COL_NEXT_TITLE = "next_title"
    const val COL_NEXT_START_AT = "next_start_at"

    const val COL_EXPENSE_MONTH = "expense_month"
    const val COL_INCOME_MONTH = "income_month"
    const val COL_BALANCE_MONTH = "balance_month"
}

internal object HighlanderVehicleContract {
    const val PACKAGE_NAME = "com.highlander.auto"
    const val AUTHORITY = "com.highlander.auto.data"
    val SUMMARY_URI: Uri = Uri.parse("content://$AUTHORITY/summary")

    const val COL_VEHICLE_NICKNAME = "vehicle_nickname"
    const val COL_FUEL_COST_MONTH = "fuel_cost_month"
    const val COL_MAINTENANCE_COST_MONTH = "maintenance_cost_month"
    const val COL_ACTIVE_ALERT_COUNT = "active_alert_count"
}
