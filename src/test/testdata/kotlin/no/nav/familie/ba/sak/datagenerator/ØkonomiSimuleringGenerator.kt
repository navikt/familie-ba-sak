package no.nav.familie.ba.sak.datagenerator

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import java.time.LocalDate

fun lagØkonomiSimuleringMottaker(
    id: Long = 0,
    mottakerNummer: String? = randomFnr(),
    mottakerType: MottakerType = MottakerType.BRUKER,
    behandling: Behandling = mockk(relaxed = true),
    økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(lagØkonomiSimuleringPostering()),
) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)

fun lagØkonomiSimuleringPostering(
    økonomiSimuleringMottaker: ØkonomiSimuleringMottaker = mockk(relaxed = true),
    beløp: Int = 0,
    fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.BARNETRYGD,
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now().minusMonths(1),
    betalingType: BetalingType = if (beløp >= 0) BetalingType.DEBIT else BetalingType.KREDIT,
    posteringType: PosteringType = PosteringType.YTELSE,
    forfallsdato: LocalDate = LocalDate.now().minusMonths(1),
    utenInntrekk: Boolean = false,
    fagsakId: Long = 0,
) = ØkonomiSimuleringPostering(
    økonomiSimuleringMottaker = økonomiSimuleringMottaker,
    fagOmrådeKode = fagOmrådeKode,
    fom = fom,
    tom = tom,
    betalingType = betalingType,
    beløp = beløp.toBigDecimal(),
    posteringType = posteringType,
    forfallsdato = forfallsdato,
    utenInntrekk = utenInntrekk,
    fagsakId = fagsakId,
)
