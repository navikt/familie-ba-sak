package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.person.pdl.aktor.v2.Type

class PdlHentIdenterResponse(
    val pdlIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<IdentInformasjon>,
)

data class IdentInformasjon(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)

fun List<IdentInformasjon>.hentAktivFødselsnummer(): String =
    this.singleOrNull { it.gruppe == Type.FOLKEREGISTERIDENT.name && !it.historisk }?.ident
        ?: run {
            secureLogger.error("Finner ikke folkeregisterident i liste fra PDL: $this")
            throw Error("Finner ikke folkeregisteriden i Pdl")
        }

fun List<IdentInformasjon>.hentAktivFødselsnummerOrNull(): String? =
    this.singleOrNull { it.gruppe == Type.FOLKEREGISTERIDENT.name && !it.historisk }?.ident.also {
        if (it == null) {
            secureLogger.warn("Finner ikke folkeregisterident i liste fra PDL: $this")
        }
    }

fun List<IdentInformasjon>.hentAktivAktørId(): String =
    this.singleOrNull { it.gruppe == Type.AKTORID.name && !it.historisk }?.ident
        ?: run {
            secureLogger.error("Finner ikke folkeregisterident i liste fra PDL: $this")
            throw Error("Finner ikke aktørId i Pdl")
        }

fun List<IdentInformasjon>.hentAktørIder(): List<String> = filter { it.gruppe == Type.AKTORID.name }.map { it.ident }
