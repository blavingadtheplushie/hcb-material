package ink.blavingad.hcb.ui

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage
import ink.blavingad.hcb.data.Transaction

/** simple-icons (CC0) monochrome SVGs, matched on merchant name, tinted with the theme. */
object Brands {
    @Volatile private var map: Map<String, String>? = null
    private val noise = setOf("inc", "llc", "ltd", "com", "www", "the", "co", "corp", "gmbh", "sa", "sas", "bv", "pbc", "store", "online", "payment", "payments", "help", "bill", "billing", "subscription", "services", "service", "intl")

    private fun load(ctx: Context): Map<String, String> = map ?: synchronized(this) {
        map ?: ctx.assets.open("brands.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull { l -> l.split('\t').takeIf { it.size >= 2 }?.let { it[0] to it[1] } }.toMap()
        }.also { map = it }
    }

    fun slugFor(ctx: Context, merchant: String?): String? {
        if (merchant.isNullOrBlank()) return null
        val m = load(ctx)
        val clean = merchant.lowercase().replace(Regex("\\*.*$"), " ").replace(Regex("\\.(com|io|net|org|co|app|dev)\\b"), " ")
        val words = clean.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() && it !in noise && !it.all(Char::isDigit) }
        if (words.isEmpty()) return null
        val candidates = buildList {
            add(words.joinToString(""))
            if (words.size >= 3) add(words.take(3).joinToString(""))
            if (words.size >= 2) add(words.take(2).joinToString(""))
            if (words[0].length >= 3) add(words[0])
        }
        return candidates.firstNotNullOfOrNull { m[it] }
    }

    fun url(slug: String) = "https://cdn.jsdelivr.net/npm/simple-icons@15.22.0/icons/$slug.svg"
}

fun Transaction.merchantName(): String? = card_charge?.merchant?.let { it.smart_name ?: it.name }

@Composable
fun MerchantIcon(t: Transaction, fallback: ImageVector, tint: Color, size: Dp) {
    val ctx = LocalContext.current
    val slug = remember(t.id) { Brands.slugFor(ctx, t.merchantName()) }
    var failed by remember(slug) { mutableStateOf(false) }
    if (slug == null || failed) { Icon(fallback, null, Modifier.size(size), tint = tint); return }
    SubcomposeAsyncImage(
        model = Brands.url(slug), contentDescription = null, modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
        loading = { Icon(fallback, null, Modifier.size(size), tint = tint) },
        error = { failed = true },
    )
}
