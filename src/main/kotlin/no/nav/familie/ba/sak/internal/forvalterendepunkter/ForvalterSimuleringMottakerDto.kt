package no.nav.familie.ba.sak.internal.forvalterendepunkter

import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import java.math.BigDecimal
import java.time.LocalDate

data class ForvalterSimuleringMottakerDto(
    val id: Long,
    val behandlingId: Long,
    val mottakerNummer: String?,
    val mottakerType: MottakerType,
    val posteringer: List<ForvalterSimuleringPosteringDto>,
) {
    override fun toString(): String = "ForvalterSimuleringMottakerDto(id=$id, behandlingId=$behandlingId, mottakerType=$mottakerType, posteringer=$posteringer)"
}

data class ForvalterSimuleringPosteringDto(
    val id: Long,
    val fagOmrådeKode: FagOmrådeKode,
    val fom: LocalDate,
    val tom: LocalDate,
    val betalingType: BetalingType,
    val beløp: BigDecimal,
    val posteringType: PosteringType,
    val forfallsdato: LocalDate,
    val utenInntrekk: Boolean,
    val fagsakId: Long?,
)

fun ØkonomiSimuleringMottaker.tilForvalterSimuleringMottakerDto(): ForvalterSimuleringMottakerDto =
    ForvalterSimuleringMottakerDto(
        id = id,
        behandlingId = behandling.id,
        mottakerNummer = mottakerNummer,
        mottakerType = mottakerType,
        posteringer = økonomiSimuleringPostering.map { it.tilForvalterSimuleringPosteringDto() },
    )

fun ØkonomiSimuleringPostering.tilForvalterSimuleringPosteringDto(): ForvalterSimuleringPosteringDto =
    ForvalterSimuleringPosteringDto(
        id = id,
        fagOmrådeKode = fagOmrådeKode,
        fom = fom,
        tom = tom,
        betalingType = betalingType,
        beløp = beløp,
        posteringType = posteringType,
        forfallsdato = forfallsdato,
        utenInntrekk = utenInntrekk,
        fagsakId = fagsakId,
    )
