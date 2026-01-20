package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilDomene
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RegistrereSøknadTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val behandlingstemaService = mockk<BehandlingstemaService>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val loggService = mockk<LoggService>()
    private val vedtakService = mockk<VedtakService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

    private val registrereSøknad: RegistrereSøknad =
        RegistrereSøknad(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            søknadGrunnlagService = søknadGrunnlagService,
            persongrunnlagService = persongrunnlagService,
            loggService = loggService,
            vedtakService = vedtakService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @Test
        fun `skal registrer søknad uten å oppdatere behandlingstema`() {
            // Arrange
            val behandling = lagBehandling(underkategori = BehandlingUnderkategori.ORDINÆR)

            val søknadDTO =
                lagSøknadDTO(
                    søkerIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                )

            val registrerSøknadDto =
                RegistrerSøknadDto(
                    søknad = søknadDTO,
                    bekreftEndringerViaFrontend = false,
                )

            val vedtak = lagVedtak(behandling = behandling)

            every { søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) } returns null
            every { loggService.opprettRegistrertSøknadLogg(behandling, false) } just runs
            every { søknadGrunnlagService.lagreOgDeaktiverGammel(any()) } returnsArgument 0
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling) } returns null
            every { persongrunnlagService.registrerBarnFraSøknad(søknadDTO, behandling, null) } just runs
            every { tilbakestillBehandlingService.initierOgSettBehandlingTilVilkårsvurdering(behandling, registrerSøknadDto.bekreftEndringerViaFrontend) } just runs
            every { vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vedtak
            every { vedtakService.oppdater(vedtak) } returnsArgument 0

            // Act
            val stegType =
                registrereSøknad.utførStegOgAngiNeste(
                    behandling = behandling,
                    data = registrerSøknadDto,
                )

            // Assert
            verify(exactly = 0) { behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(any(), any()) }
            verify(exactly = 1) { loggService.opprettRegistrertSøknadLogg(behandling, false) }
            verify(exactly = 1) { søknadGrunnlagService.lagreOgDeaktiverGammel(any()) }
            verify(exactly = 1) { persongrunnlagService.registrerBarnFraSøknad(søknadDTO, behandling, null) }
            verify(exactly = 1) { tilbakestillBehandlingService.initierOgSettBehandlingTilVilkårsvurdering(behandling, registrerSøknadDto.bekreftEndringerViaFrontend) }
            verify(exactly = 1) { vedtakService.oppdater(vedtak) }
            assertThat(stegType).isEqualTo(StegType.VILKÅRSVURDERING)
            assertThat(behandling.underkategori).isEqualTo(søknadDTO.underkategori.tilDomene())
        }

        @Test
        fun `skal registrer søknad og oppdatere behandlingstema`() {
            // Arrange
            val behandling = lagBehandling(underkategori = BehandlingUnderkategori.ORDINÆR)

            val søknadDTO =
                lagSøknadDTO(
                    søkerIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val registrerSøknadDto =
                RegistrerSøknadDto(
                    søknad = søknadDTO,
                    bekreftEndringerViaFrontend = false,
                )

            val vedtak = lagVedtak(behandling = behandling)

            every { behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(behandling, søknadDTO.underkategori.tilDomene()) } answers {
                behandling.underkategori = søknadDTO.underkategori.tilDomene()
                behandling
            }
            every { søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) } returns null
            every { loggService.opprettRegistrertSøknadLogg(behandling, false) } just runs
            every { søknadGrunnlagService.lagreOgDeaktiverGammel(any()) } returnsArgument 0
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling) } returns null
            every { persongrunnlagService.registrerBarnFraSøknad(søknadDTO, behandling, null) } just runs
            every { tilbakestillBehandlingService.initierOgSettBehandlingTilVilkårsvurdering(behandling, registrerSøknadDto.bekreftEndringerViaFrontend) } just runs
            every { vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vedtak
            every { vedtakService.oppdater(vedtak) } returnsArgument 0

            // Act
            val stegType =
                registrereSøknad.utførStegOgAngiNeste(
                    behandling = behandling,
                    data = registrerSøknadDto,
                )

            // Assert
            verify(exactly = 1) { behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(behandling, søknadDTO.underkategori.tilDomene()) }
            verify(exactly = 1) { loggService.opprettRegistrertSøknadLogg(behandling, false) }
            verify(exactly = 1) { søknadGrunnlagService.lagreOgDeaktiverGammel(any()) }
            verify(exactly = 1) { persongrunnlagService.registrerBarnFraSøknad(søknadDTO, behandling, null) }
            verify(exactly = 1) { tilbakestillBehandlingService.initierOgSettBehandlingTilVilkårsvurdering(behandling, registrerSøknadDto.bekreftEndringerViaFrontend) }
            verify(exactly = 1) { vedtakService.oppdater(vedtak) }
            assertThat(stegType).isEqualTo(StegType.VILKÅRSVURDERING)
            assertThat(behandling.underkategori).isEqualTo(søknadDTO.underkategori.tilDomene())
        }
    }
}
