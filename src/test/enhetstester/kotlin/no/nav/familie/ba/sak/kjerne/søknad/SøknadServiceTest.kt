package no.nav.familie.ba.sak.kjerne.søknad

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanse
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SøknadServiceTest {
    private val søknadReferanseService: SøknadReferanseService = mockk()
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val søknadMapperLookup: SøknadMapper.Lookup = mockk()
    private val søknadService =
        SøknadService(
            søknadReferanseService = søknadReferanseService,
            integrasjonClient = integrasjonClient,
            søknadMapperLookup = søknadMapperLookup,
        )

    @Nested
    inner class HentSøknad {
        @Test
        fun `skal hente søknad fra dokarkiv og mappe VerjsonertBarnetrygdSøknad til Søknad`() {
            // Arrange
            val behandling = lagBehandling()
            val søknadReferanse = SøknadReferanse(behandlingId = behandling.id, journalpostId = "123456789")
            val barn1 = randomFnr()
            val barn2 = randomFnr()
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad = lagBarnetrygdSøknadV9(barnFnr = listOf(barn1, barn2), søknadstype = Søknadstype.ORDINÆR, erEøs = true, originalspråk = "nn"),
                )

            every { søknadReferanseService.hentSøknadReferanse(behandling.id) } returns søknadReferanse
            every { integrasjonClient.hentVersjonertBarnetrygdSøknad(søknadReferanse.journalpostId) } returns versjonertBarnetrygdSøknadV9
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV9.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV9()

            // Act
            val søknad = søknadService.hentSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNotNull
            assertThat(søknad?.barn).hasSize(2)
            assertThat(søknad?.barn?.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad?.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad?.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad?.målform).isEqualTo(Målform.NN)
        }

        @Test
        fun `skal returnere null dersom det ikke finnes noen SøknadReferanse for behandling`() {
            // Arrange
            val behandling = lagBehandling()

            every { søknadReferanseService.hentSøknadReferanse(behandling.id) } returns null

            // Act
            val søknad = søknadService.hentSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNull()
        }
    }
}
