package ink.blavingad.hcb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

/** Small, forgiving markdown renderer for HCB org descriptions (paragraphs, headings, lists, **bold**, *italic*, `code`, links, even "[x]\n(url)"). */
private sealed interface Md
private data class MdPara(val text: String) : Md
private data class MdHead(val level: Int, val text: String) : Md
private data class MdItem(val marker: String, val text: String) : Md
private data class MdQuote(val text: String) : Md

private fun parse(src: String): List<Md> {
    // glue "[label]\n(url)" and "[label] (url)" back together
    val s = src.replace("\r\n", "\n").replace(Regex("\\]\\s*\\n?\\s*\\((https?://|/)"), "]($1")
    val out = mutableListOf<Md>()
    val para = StringBuilder()
    fun flush() { if (para.isNotBlank()) out += MdPara(para.toString().trim()); para.clear() }
    for (raw in s.lines()) {
        val l = raw.trimEnd()
        val t = l.trimStart()
        when {
            t.isEmpty() -> flush()
            Regex("^#{1,6}\\s").containsMatchIn(t) -> { flush(); out += MdHead(t.takeWhile { it == '#' }.length, t.dropWhile { it == '#' }.trim()) }
            Regex("^[-*+]\\s").containsMatchIn(t) -> { flush(); out += MdItem("•", t.drop(2).trim()) }
            Regex("^\\d+[.)]\\s").containsMatchIn(t) -> { flush(); out += MdItem(t.substringBefore(' '), t.substringAfter(' ').trim()) }
            t.startsWith(">") -> { flush(); out += MdQuote(t.trimStart('>').trim()) }
            else -> {
                // continuation of a list item
                if (para.isEmpty() && out.lastOrNull() is MdItem && raw.startsWith(" ")) { val i = out.removeAt(out.lastIndex) as MdItem; out += i.copy(text = i.text + " " + t) }
                else { if (para.isNotEmpty()) para.append(if (raw.endsWith("  ")) "\n" else " "); para.append(t) }
            }
        }
    }
    flush()
    return out
}

private val inlineRe = Regex("""\[([^\]]+)]\(([^)\s]+)\)|\*\*(.+?)\*\*|__(.+?)__|(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*|(?<!\w)_(?!\s)(.+?)(?<!\s)_(?!\w)|`([^`]+)`|(https?://[^\s)]+)""")

@Composable
private fun inline(text: String, onLink: (String) -> Unit): AnnotatedString {
    val cs = MaterialTheme.colorScheme
    val linkStyle = TextLinkStyles(SpanStyle(color = cs.primary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        fun rec(t: String) {
            var i = 0
            for (m in inlineRe.findAll(t)) {
                append(t.substring(i, m.range.first)); i = m.range.last + 1
                val g = m.groupValues
                when {
                    g[1].isNotEmpty() -> { val url = g[2].let { if (it.startsWith("/")) "https://hcb.hackclub.com$it" else it }
                        withLink(LinkAnnotation.Clickable(url, linkStyle) { onLink(url) }) { rec(g[1]) } }
                    g[3].isNotEmpty() || g[4].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { rec(g[3] + g[4]) }
                    g[5].isNotEmpty() || g[6].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { rec(g[5] + g[6]) }
                    g[7].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = cs.surfaceContainerHighest)) { append(g[7]) }
                    else -> withLink(LinkAnnotation.Clickable(g[8], linkStyle) { onLink(g[8]) }) { append(g[8]) }
                }
            }
            append(t.substring(i))
        }
        rec(text)
    }
}

@Composable
fun Markdown(text: String, modifier: Modifier = Modifier, onLink: (String) -> Unit) {
    val ty = MaterialTheme.typography
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var prev: Md? = null
        for (b in parse(text)) {
            val gap = if (prev != null && (b !is MdItem || prev !is MdItem)) 4.dp else 0.dp
            when (b) {
                is MdPara -> Text(inline(b.text, onLink), style = ty.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = gap))
                is MdHead -> Text(inline(b.text, onLink), style = if (b.level <= 2) ty.titleMediumEmphasized else ty.titleSmallEmphasized, color = cs.onSurface, modifier = Modifier.padding(top = gap + 6.dp))
                is MdQuote -> Text(inline(b.text, onLink), style = ty.bodyMedium, fontStyle = FontStyle.Italic, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = gap))
                is MdItem -> Row(Modifier.padding(top = gap)) {
                    Text(b.marker, style = ty.bodyMedium, color = cs.primary, modifier = Modifier.width(22.dp))
                    Text(inline(b.text, onLink), style = ty.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            prev = b
        }
    }
}
