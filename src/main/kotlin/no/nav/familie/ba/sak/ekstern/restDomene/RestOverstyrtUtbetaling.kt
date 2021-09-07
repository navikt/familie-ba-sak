package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.overstyring.domene.Årsak
import java.time.YearMonth

class RestOverstyrtUtbetaling(
    val id: Long?,
    val personIdent: String,
    val prosent: Int,
    val fom: YearMonth,
    val tom: YearMonth,
    val årsak: Årsak,
    val begrunnelse: String,
)