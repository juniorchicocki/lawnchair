package app.lawnchair.smartspace.provider.highlander

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Ver [highlanderPollingFlow]. */
private const val DEFAULT_POLL_INTERVAL_MS = 5 * 60_000L

private const val TAG = "HighlanderContentFlow"

/**
 * Consulta periodicamente um Uri de ContentProvider e emite o resultado já
 * convertido — polling em vez de [android.database.ContentObserver] porque
 * os três apps Highlander escrevem no SQLite local via Drift, sem passar
 * pelo ContentResolver de forma alguma: não existe write do lado deles que
 * dispare `notifyChange()` num Uri que um observer daqui pudesse escutar.
 *
 * O laço para sozinho quando o [Flow] é cancelado (a fonte foi desabilitada
 * ou o app saiu de tela) — mesmo ciclo de vida cooperativo que o resto dos
 * [app.lawnchair.smartspace.provider.SmartspaceDataSource] já assume.
 *
 * `null` emitido cobre igualmente "app não instalado", "sem permissão"
 * (assinado com chave diferente) e "sem linha ainda" — [parse] nunca vê um
 * cursor vazio, só um cursor posicionado na primeira (e única) linha.
 */
internal fun <T> highlanderPollingFlow(
    context: Context,
    uri: Uri,
    pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    parse: (Cursor) -> T,
): Flow<T?> = flow {
    val resolver = context.contentResolver
    while (true) {
        emit(queryFirstRow(resolver, uri, parse))
        delay(pollIntervalMs)
    }
}

/** Coluna nullable — as três colunas "opcionais" dos providers Highlander
 * (próximo item da agenda, apelido do veículo, etc.) usam essa forma em vez
 * de [Cursor.getColumnIndexOrThrow], já que ausência de valor é um estado
 * válido, não um erro de schema. */
internal fun Cursor.getStringOrNull(column: String): String? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getString(index)
}

private fun <T> queryFirstRow(resolver: ContentResolver, uri: Uri, parse: (Cursor) -> T): T? = try {
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) parse(cursor) else null
    }
} catch (e: SecurityException) {
    // Permissão signature não concedida — assinado com uma chave diferente
    // da do app dono. Não é um estado transitório, mas ainda assim só
    // silencia (a fonte já não vai passar por isAvailable de qualquer
    // forma quando isso acontece por engano, e logar toda consulta a cada
    // ciclo de poll seria ruído).
    Log.d(TAG, "sem permissão pra ler $uri", e)
    null
} catch (e: Exception) {
    Log.d(TAG, "falha ao ler $uri", e)
    null
}
