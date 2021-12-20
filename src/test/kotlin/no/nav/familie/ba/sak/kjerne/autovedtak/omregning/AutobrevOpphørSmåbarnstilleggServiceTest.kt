package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
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
    val vedtakService = mockk<VedtakService>(relaxed = true)
    val stegService = mockk<StegService>()
    val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val autovedtakService = mockk<AutovedtakService>(relaxed = true)
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
    fun `Verifiser at løpende behandling med småbarnstillegg sender opphørsbrev måneden etter yngste barn ble 3 år`() {

        val behandling = lagBehandling()
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned)

        every { behandlingService.hent(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
        every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } returns emptyList()
        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs

        autobrevOpphørSmåbarnstilleggService
            .kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(behandlingId = behandling.id)

        verify(exactly = 1) {
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 1) { taskRepository.save(any()) }
    }

    /**
     * Tester overgangstønadOpphørerDenneMåneden
     */

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

    /**
     * Tester minsteBarnFylteTreÅrForrigeMåned
     */

    val behandlingId: Long = 1

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn som fylte tre forrige måned gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned)

        assertTrue(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn som fylte tre forrige måned og et eldre gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val barnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned, barnOverTre)

        assertTrue(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn som fylte tre forrige måned gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val ekstraBarn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned, ekstraBarn3ForrigeMåned)

        assertTrue(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn over tre gir false`() {
        val barnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val ekstraBarnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnOverTre, ekstraBarnOverTre)

        assertFalse(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn under tre gir false`() {
        val barnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(2))
        val ekstraBarnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnUnderTre, ekstraBarnUnderTre)

        assertFalse(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn under tre og et barn 3 forrige måned gir false`() {
        val barnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(2))
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnUnderTre, barn3ForrigeMåned)

        assertFalse(autobrevOpphørSmåbarnstilleggService.minsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
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
