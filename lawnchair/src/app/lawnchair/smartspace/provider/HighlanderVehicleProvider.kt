package app.lawnchair.smartspace.provider

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Icon
import app.lawnchair.smartspace.model.SmartspaceAction
import app.lawnchair.smartspace.model.SmartspaceScores
import app.lawnchair.smartspace.model.SmartspaceTarget
import app.lawnchair.smartspace.provider.highlander.HighlanderVehicleContract
import app.lawnchair.smartspace.provider.highlander.formatBrl
import app.lawnchair.smartspace.provider.highlander.getStringOrNull
import app.lawnchair.smartspace.provider.highlander.highlanderPollingFlow
import app.lawnchair.util.isPackageInstalledAndEnabled
import com.android.launcher3.R
import kotlinx.coroutines.flow.map

/**
 * Custo de combustível/manutenção do mês do Highlander Meu Veículo — lê o
 * resumo exposto por HighlanderVehicleDataProvider.kt via ContentResolver.
 *
 * Suprime o card quando não houve gasto nem alerta ativo no mês (mesmo
 * critério que os outros dois provedores Highlander e o resto do módulo:
 * só aparece quando há algo a mostrar).
 */
class HighlanderVehicleProvider(context: Context) :
    SmartspaceDataSource(
        context,
        R.string.smartspace_highlander_vehicle,
        { smartspaceHighlanderVehicle },
    ) {

    override val isAvailable: Boolean =
        context.packageManager.isPackageInstalledAndEnabled(HighlanderVehicleContract.PACKAGE_NAME)

    private data class Summary(
        val vehicleNickname: String?,
        val fuelCostMonth: Double,
        val maintenanceCostMonth: Double,
        val activeAlertCount: Int,
    )

    override val internalTargets = highlanderPollingFlow(
        context = context,
        uri = HighlanderVehicleContract.SUMMARY_URI,
        parse = ::toSummary,
    ).map { summary -> listOfNotNull(summary?.let(::toTarget)) }

    private fun toSummary(cursor: Cursor): Summary = Summary(
        vehicleNickname = cursor.getStringOrNull(HighlanderVehicleContract.COL_VEHICLE_NICKNAME),
        fuelCostMonth = cursor.getDouble(cursor.getColumnIndexOrThrow(HighlanderVehicleContract.COL_FUEL_COST_MONTH)),
        maintenanceCostMonth = cursor.getDouble(
            cursor.getColumnIndexOrThrow(HighlanderVehicleContract.COL_MAINTENANCE_COST_MONTH),
        ),
        activeAlertCount = cursor.getInt(
            cursor.getColumnIndexOrThrow(HighlanderVehicleContract.COL_ACTIVE_ALERT_COUNT),
        ),
    )

    private fun toTarget(summary: Summary): SmartspaceTarget? {
        val parts = buildList {
            if (summary.fuelCostMonth > 0) {
                add(context.getString(R.string.highlander_vehicle_fuel_cost, formatBrl(summary.fuelCostMonth)))
            }
            if (summary.maintenanceCostMonth > 0) {
                add(
                    context.getString(
                        R.string.highlander_vehicle_maintenance_cost,
                        formatBrl(summary.maintenanceCostMonth),
                    ),
                )
            }
            if (summary.activeAlertCount > 0) {
                add(
                    context.resources.getQuantityString(
                        R.plurals.highlander_vehicle_alerts,
                        summary.activeAlertCount,
                        summary.activeAlertCount,
                    ),
                )
            }
        }
        if (parts.isEmpty()) return null

        return SmartspaceTarget(
            id = "highlanderVehicle",
            headerAction = SmartspaceAction(
                id = "highlanderVehicleAction",
                icon = Icon.createWithResource(context, R.drawable.ic_highlander_vehicle),
                title = summary.vehicleNickname ?: context.getString(R.string.smartspace_highlander_vehicle),
                subtitle = parts.joinToString(" · "),
                intent = context.packageManager.getLaunchIntentForPackage(HighlanderVehicleContract.PACKAGE_NAME),
            ),
            score = SmartspaceScores.SCORE_HIGHLANDER_VEHICLE,
            featureType = SmartspaceTarget.FeatureType.FEATURE_REMINDER,
        )
    }
}
