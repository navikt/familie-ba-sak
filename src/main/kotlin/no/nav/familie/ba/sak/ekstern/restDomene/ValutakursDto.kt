package no.nav.familie.ba.sak.ekstern.restDomene

import jakarta.validation.constraints.Pattern
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class ValutakursDto(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val valutakursdato: LocalDate?,
    @field:Pattern(regexp = "[A-Z]{3}", message = "Valutakode må ha store bokstaver og være tre bokstaver lang")
    val valutakode: String?,
    val kurs: BigDecimal?,
    val vurderingsform: Vurderingsform?,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT,
) : AbstractUtfyltStatus<ValutakursDto>() {
    override fun medUtfyltStatus(): ValutakursDto = this.copy(status = utfyltStatus(finnAntallUtfylt(listOf(this.valutakursdato, this.kurs)), 2))
}

fun ValutakursDto.tilValutakurs(barnAktører: List<Aktør>) =
    Valutakurs(
        fom = this.fom,
        tom = this.tom,
        barnAktører = barnAktører.toSet(),
        valutakursdato = this.valutakursdato,
        valutakode = this.valutakode,
        kurs = this.kurs,
        vurderingsform = Vurderingsform.MANUELL,
    )

fun Valutakurs.tilValutakursDto() =
    ValutakursDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
        valutakursdato = this.valutakursdato,
        valutakode = this.valutakode,
        kurs = this.kurs,
        vurderingsform = this.vurderingsform,
    ).medUtfyltStatus()
