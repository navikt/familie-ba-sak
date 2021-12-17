package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AutobrevOpphørSmåbarnstilleggServiceTest {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val behandlingService = mockk<BehandlingService>()
    val vedtakService = mockk<VedtakService>()

    val taskRepository = mockk<TaskRepositoryWrapper>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val autovedtakService = mockk<AutovedtakService>()
    val periodeOvergangsstønadGrunnlagRepository = mockk<PeriodeOvergangsstønadGrunnlagRepository>()

    private val autobrevOpphørSmåbarnstilleggService = AutobrevOpphørSmåbarnstilleggService(
        personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
        behandlingService = behandlingService,
        vedtakService = vedtakService,
        taskRepository = taskRepository,
        vedtaksperiodeService = vedtaksperiodeService,
        autovedtakService = autovedtakService,
        periodeOvergangsstønadGrunnlagRepository = periodeOvergangsstønadGrunnlagRepository
    )

    @Test
    fun `overgangstønadOpphørerDenneMåneden - en periode med opphør denne måneden gir true`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now()
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom)
        )
        val overgangstønadOpphørerDenneMåneden =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørerDenneMåneden(input)
        assertTrue(overgangstønadOpphørerDenneMåneden)
    }

    @Test
    fun `overgangstønadOpphørerDenneMåneden - tom liste gir false`() {
        val input: List<PeriodeOvergangsstønadGrunnlag> = emptyList()
        val overgangstønadOpphørerDenneMåneden =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørerDenneMåneden(input)
        assertFalse(overgangstønadOpphørerDenneMåneden)
    }

    @Test
    fun `overgangstønadOpphørerDenneMåneden - neste måned gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().førsteDagINesteMåned()
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom)
        )
        val overgangstønadOpphørerDenneMåneden =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørerDenneMåneden(input)
        assertFalse(overgangstønadOpphørerDenneMåneden)
    }

    @Test
    fun `overgangstønadOpphørerDenneMåneden - forrige måned gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().minusMonths(1)
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom)
        )
        val overgangstønadOpphørerDenneMåneden =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørerDenneMåneden(input)
        assertFalse(overgangstønadOpphørerDenneMåneden)
    }

    @Test
    fun `overgangstønadOpphørerDenneMåneden - ett år siden gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().minusYears(1)
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom)
        )
        val overgangstønadOpphørerDenneMåneden =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørerDenneMåneden(input)
        assertFalse(overgangstønadOpphørerDenneMåneden)
    }

    private fun lagPeriodeOvergangsstønadGrunnlag(
        fom: LocalDate,
        tom: LocalDate
    ) = PeriodeOvergangsstønadGrunnlag(
        id = 1,
        behandlingId = 1,
        personIdent = "1234",
        aktør = randomAktørId(),
        fom = fom,
        tom = tom,
        datakilde = PeriodeOvergangsstønad.Datakilde.EF
    )
}
