package app.lawnchair.smartspace.provider

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.Icon
import android.net.Uri
import app.lawnchair.smartspace.model.SmartspaceAction
import app.lawnchair.smartspace.model.SmartspaceScores
import app.lawnchair.smartspace.model.SmartspaceTarget
import app.lawnchair.smartspace.provider.highlander.HighlanderTasksContract
import app.lawnchair.smartspace.provider.highlander.getStringOrNull
import app.lawnchair.smartspace.provider.highlander.highlanderPollingFlow
import app.lawnchair.util.isPackageInstalledAndEnabled
import com.android.launcher3.R
import kotlinx.coroutines.flow.map

/**
 * Compromissos/tarefas com prazo do Highlander Tarefas (app companheiro,
 * não o Highlander Auto Launcher do carro) — lê o resumo exposto por
 * HighlanderDataProvider.kt via ContentResolver, já que o launcher não
 * alcança o SQLite local daquele app.
 *
 * Suprime o card quando não há nada hoje/atrasado/agendado — mesma
 * convenção de BatteryStatusProvider (só aparece quando há algo
 * acionável).
 */
class HighlanderTasksProvider(context: Context) :
    SmartspaceDataSource(
        context,
        R.string.smartspace_highlander_tasks,
        { smartspaceHighlanderTasks },
    ) {

    override val isAvailable: Boolean =
        context.packageManager.isPackageInstalledAndEnabled(HighlanderTasksContract.PACKAGE_NAME)

    private data class Summary(
        val todayCount: Int,
        val overdueCount: Int,
        val nextTitle: String?,
    )

    override val internalTargets = highlanderPollingFlow(
        context = context,
        uri = HighlanderTasksContract.SUMMARY_URI,
        parse = ::toSummary,
    ).map { summary -> listOfNotNull(summary?.let(::toTarget)) }

    private fun toSummary(cursor: Cursor): Summary = Summary(
        todayCount = cursor.getInt(cursor.getColumnIndexOrThrow(HighlanderTasksContract.COL_TODAY_COUNT)),
        overdueCount = cursor.getInt(cursor.getColumnIndexOrThrow(HighlanderTasksContract.COL_OVERDUE_COUNT)),
        nextTitle = cursor.getStringOrNull(HighlanderTasksContract.COL_NEXT_TITLE),
    )

    private fun toTarget(summary: Summary): SmartspaceTarget? {
        val headline = when {
            summary.overdueCount > 0 -> context.resources.getQuantityString(
                R.plurals.highlander_tasks_overdue,
                summary.overdueCount,
                summary.overdueCount,
            )
            summary.todayCount > 0 -> context.resources.getQuantityString(
                R.plurals.highlander_tasks_today,
                summary.todayCount,
                summary.todayCount,
            )
            summary.nextTitle != null -> summary.nextTitle
            else -> null
        } ?: return null

        val subtitle = summary.nextTitle
            ?.takeIf { it != headline }
            ?.let { context.getString(R.string.highlander_tasks_next, it) }

        return SmartspaceTarget(
            id = "highlanderTasks",
            headerAction = SmartspaceAction(
                id = "highlanderTasksAction",
                icon = Icon.createWithResource(context, R.drawable.ic_highlander_tasks),
                title = headline,
                subtitle = subtitle,
                intent = agendaIntent(),
            ),
            score = if (summary.overdueCount > 0) {
                SmartspaceScores.SCORE_HIGHLANDER_TASKS_OVERDUE
            } else {
                SmartspaceScores.SCORE_HIGHLANDER_TASKS
            },
            featureType = SmartspaceTarget.FeatureType.FEATURE_REMINDER,
        )
    }

    private fun agendaIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setClassName(HighlanderTasksContract.PACKAGE_NAME, HighlanderTasksContract.MAIN_ACTIVITY)
        data = Uri.parse(HighlanderTasksContract.DEEP_LINK_AGENDA)
    }
}
