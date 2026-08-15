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
import app.lawnchair.smartspace.provider.highlander.formatBrl
import app.lawnchair.smartspace.provider.highlander.highlanderPollingFlow
import app.lawnchair.util.isPackageInstalledAndEnabled
import com.android.launcher3.R
import kotlinx.coroutines.flow.map

/**
 * Saldo/receitas/despesas do mês do Meu Diário (Highlander Tarefas antes do
 * rebrand) — lê `/finance_summary` do MESMO ContentProvider/authority que
 * [HighlanderTasksProvider] já usa pra agenda.
 *
 * Não existe mais um app Financas separado: aquele app foi descontinuado e
 * suas telas migraram pra dentro do Meu Diário (ver comentário em
 * HighlanderTasksContract), que já expõe o resumo financeiro num path
 * próprio no mesmo ContentProvider da agenda — daí este provider apontar
 * pro mesmo AUTHORITY/PACKAGE_NAME de [HighlanderTasksProvider], só num Uri
 * diferente.
 *
 * Continua sendo uma fonte separada (não fundida em HighlanderTasksProvider)
 * de propósito: o usuário liga/desliga agenda e finanças
 * independentemente, mesmo vindo do mesmo app.
 *
 * Suprime o card quando não há nenhum lançamento no mês ainda (receita e
 * despesa ambas zero) — mesmo critério dos outros provedores Highlander.
 */
class HighlanderFinanceProvider(context: Context) :
    SmartspaceDataSource(
        context,
        R.string.smartspace_highlander_finance,
        { smartspaceHighlanderFinance },
    ) {

    override val isAvailable: Boolean =
        context.packageManager.isPackageInstalledAndEnabled(HighlanderTasksContract.PACKAGE_NAME)

    private data class Summary(val expenseMonth: Double, val incomeMonth: Double, val balanceMonth: Double)

    override val internalTargets = highlanderPollingFlow(
        context = context,
        uri = HighlanderTasksContract.FINANCE_SUMMARY_URI,
        parse = ::toSummary,
    ).map { summary -> listOfNotNull(summary?.let(::toTarget)) }

    private fun toSummary(cursor: Cursor): Summary = Summary(
        expenseMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderTasksContract.COL_EXPENSE_MONTH)),
        incomeMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderTasksContract.COL_INCOME_MONTH)),
        balanceMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderTasksContract.COL_BALANCE_MONTH)),
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
                intent = financeIntent(),
            ),
            score = SmartspaceScores.SCORE_HIGHLANDER_FINANCE,
            featureType = SmartspaceTarget.FeatureType.FEATURE_REMINDER,
        )
    }

    private fun financeIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setClassName(HighlanderTasksContract.PACKAGE_NAME, HighlanderTasksContract.MAIN_ACTIVITY)
        data = Uri.parse(HighlanderTasksContract.DEEP_LINK_FINANCE)
    }
}
