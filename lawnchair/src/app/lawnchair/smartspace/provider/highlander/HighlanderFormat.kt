package app.lawnchair.smartspace.provider.highlander

import java.text.NumberFormat
import java.util.Locale

/** Todo o ecossistema Highlander é BRL-only — o próprio app Finanças
 * formata com `NumberFormat.currency(locale: 'pt_BR', symbol: 'R$')` fixo,
 * não com o locale do usuário (ver transactions_screen.dart), então usar
 * pt-BR fixo aqui reproduz exatamente o que o app de origem já mostra, em
 * vez de arriscar um símbolo/formatação diferente do valor "real" do app. */
private val brlFormat: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

internal fun formatBrl(amount: Double): String = brlFormat.format(amount)
