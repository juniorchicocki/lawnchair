package app.lawnchair.smartspace.provider

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Icon
import app.lawnchair.smartspace.model.SmartspaceAction
import app.lawnchair.smartspace.model.SmartspaceScores
import app.lawnchair.smartspace.model.SmartspaceTarget
import app.lawnchair.smartspace.provider.highlander.HighlanderFinanceContract
import app.lawnchair.smartspace.provider.highlander.formatBrl
import app.lawnchair.smartspace.provider.highlander.highlanderPollingFlow
import app.lawnchair.util.isPackageInstalledAndEnabled
import com.android.launcher3.R
import kotlinx.coroutines.flow.map

/**
 * Saldo/receitas/despesas do mês do Highlander Finanças — lê o resumo
 * exposto por HighlanderFinanceDataProvider.kt via ContentResolver.
 *
 * Suprime o card quando não há nenhum lançamento no mês ainda (receita e
 * despesa ambas zero) — mesmo critério dos outros dois provedores
 * Highlander.
 */
class HighlanderFinanceProvider(context: Context) :
    SmartspaceDataSource(
        context,
        R.string.smartspace_highlander_finance,
        { smartspaceHighlanderFinance },
    ) {

    override val isAvailable: Boolean =
        context.packageManager.isPackageInstalledAndEnabled(HighlanderFinanceContract.PACKAGE_NAME)

    private data class Summary(val expenseMonth: Double, val incomeMonth: Double, val balanceMonth: Double)

    override val internalTargets = highlanderPollingFlow(
        context = context,
        uri = HighlanderFinanceContract.SUMMARY_URI,
        parse = ::toSummary,
    ).map { summary -> listOfNotNull(summary?.let(::toTarget)) }

    private fun toSummary(cursor: Cursor): Summary = Summary(
        expenseMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderFinanceContract.COL_EXPENSE_MONTH)),
        incomeMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderFinanceContract.COL_INCOME_MONTH)),
        balanceMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderFinanceContract.COL_BALANCE_MONTH)),
    )

    private fun toTarget(summary: Summary): SmartspaceTarget? {
        if (summary.expenseMonth == 0.0 && summary.incomeMonth == 0.0) return null

        return SmartspaceTarget(
            id = "highlanderFinance",
            headerAction = SmartspaceAction(
                id = "highlanderFinanceAction",
                icon = Icon.createWithResource(context, R.drawable.ic_highlander_finance),
                title = context.getString(R.string.highlander_finance_balance, formatBrl(summary.balanceMonth)),
                subtitle = context.getString(
                    R.string.highlander_finance_breakdown,
                    formatBrl(summary.incomeMonth),
                    formatBrl(summary.expenseMonth),
                ),
                intent = context.packageManager.getLaunchIntentForPackage(HighlanderFinanceContract.PACKAGE_NAME),
            ),
            score = SmartspaceScores.SCORE_HIGHLANDER_FINANCE,
            featureType = SmartspaceTarget.FeatureType.FEATURE_REMINDER,
        )
    }
}
