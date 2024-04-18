package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpphørsperiodeTest {
    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    val personopplysningGrunnlag =
        PersonopplysningGrunnlag(
            behandlingId = 0L,
            personer = mutableSetOf(søker, barn1, barn2),
        )

    @Test
    fun `Skal slå sammen to like opphørsperioder`() {
        val periode12MånederFraInneværendeMåned = inneværendeMåned().minusMonths(12).toLocalDate()

        val toLikePerioder =
            listOf(
                Opphørsperiode(
                    periodeFom = periode12MånederFraInneværendeMåned,
                    periodeTom = inneværendeMåned().toLocalDate(),
                ),
                Opphørsperiode(
                    periodeFom = periode12MånederFraInneværendeMåned,
                    periodeTom = inneværendeMåned().toLocalDate(),
                ),
            )

        assertEquals(1, slåSammenOpphørsperioder(toLikePerioder).size)
    }

    @Test
    fun `Skal slå sammen to opphørsperioder med ulik sluttdato`() {
        val toPerioderMedUlikSluttdato =
            listOf(
                Opphørsperiode(
                    periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                    periodeTom = inneværendeMåned().toLocalDate(),
                ),
                Opphørsperiode(
                    periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                    periodeTom = inneværendeMåned().nesteMåned().toLocalDate(),
                ),
            )
        val enPeriodeMedSluttDatoNesteMåned = slåSammenOpphørsperioder(toPerioderMedUlikSluttdato)

        assertEquals(1, enPeriodeMedSluttDatoNesteMåned.size)
        assertEquals(inneværendeMåned().nesteMåned().toLocalDate(), enPeriodeMedSluttDatoNesteMåned.first().periodeTom)
    }

    @Test
    fun `Skal slå sammen to opphørsperioder med ulik startdato`() {
        val toPerioderMedUlikStartdato =
            listOf(
                Opphørsperiode(
                    periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                    periodeTom = inneværendeMåned().toLocalDate(),
                ),
                Opphørsperiode(
                    periodeFom = inneværendeMåned().minusMonths(13).toLocalDate(),
                    periodeTom = inneværendeMåned().toLocalDate(),
                ),
            )
        val enPeriodeMedStartDato13MånederTilbake = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

        assertEquals(1, enPeriodeMedStartDato13MånederTilbake.size)
        assertEquals(
            inneværendeMåned().minusMonths(13).toLocalDate(),
            enPeriodeMedStartDato13MånederTilbake.first().periodeFom,
        )
    }

    @Test
    fun `Skal slå sammen to opphørsperioder som overlapper`() {
        val førsteOpphørsperiodeFom = inneværendeMåned().minusMonths(12).toLocalDate()
        val sisteOpphørsperiodeTom = inneværendeMåned().plusMonths(1).toLocalDate()
        val toPerioderMedUlikStartdato =
            listOf(
                Opphørsperiode(
                    periodeFom = førsteOpphørsperiodeFom,
                    periodeTom = inneværendeMåned().minusMonths(2).toLocalDate(),
                ),
                Opphørsperiode(
                    periodeFom = inneværendeMåned().minusMonths(6).toLocalDate(),
                    periodeTom = sisteOpphørsperiodeTom,
                ),
            )
        val enOpphørsperiodeMedFørsteFomOgSisteTom = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

        assertEquals(1, enOpphørsperiodeMedFørsteFomOgSisteTom.size)
        assertEquals(førsteOpphørsperiodeFom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeFom)
        assertEquals(sisteOpphørsperiodeTom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeTom)
    }
}
