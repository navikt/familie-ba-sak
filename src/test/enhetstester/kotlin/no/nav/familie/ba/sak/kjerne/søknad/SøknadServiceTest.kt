package no.nav.familie.ba.sak.kjerne.søknad

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV10
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV10
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SøknadServiceTest {
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val søknadMapperLookup: SøknadMapper.Lookup = mockk()
    private val søknadService =
        SøknadService(
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            integrasjonKlient = integrasjonKlient,
            søknadMapperLookup = søknadMapperLookup,
        )

    @Nested
    inner class HentSøknad {
        @Test
        fun `skal hente søknad fra dokarkiv og mappe VerjsonertBarnetrygdSøknadV9 til Søknad`() {
            // Arrange
            val behandling = lagBehandling()
            val journalpostId = "123456789"
            val barn1 = randomFnr()
            val barn2 = randomFnr()
            val versjonertBarnetrygdSøknadV9 =
                VersjonertBarnetrygdSøknadV9(
                    barnetrygdSøknad = lagBarnetrygdSøknadV9(barnFnr = listOf(barn1, barn2), søknadstype = Søknadstype.ORDINÆR, erEøs = true, originalspråk = "nn"),
                )

            every { behandlingSøknadsinfoService.hentJournalpostId(behandling.id) } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV9
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV9.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV9()

            // Act
            val søknad = søknadService.finnSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNotNull
            assertThat(søknad?.barn).hasSize(2)
            assertThat(søknad?.barn?.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad?.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad?.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad?.målform).isEqualTo(Målform.NN)
        }

        @Test
        fun `skal hente søknad fra dokarkiv og mappe VerjsonertBarnetrygdSøknadV10 til Søknad`() {
            // Arrange
            val behandling = lagBehandling()
            val journalpostId = "123456789"
            val barn1 = randomFnr()
            val barn2 = randomFnr()
            val versjonertBarnetrygdSøknadV10 =
                VersjonertBarnetrygdSøknadV10(
                    barnetrygdSøknad = lagBarnetrygdSøknadV10(barnFnr = listOf(barn1, barn2), søknadstype = Søknadstype.ORDINÆR, erEøs = true, originalspråk = "nn"),
                )

            every { behandlingSøknadsinfoService.hentJournalpostId(behandling.id) } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV10
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV10.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV10()

            // Act
            val søknad = søknadService.finnSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNotNull
            assertThat(søknad?.barn).hasSize(2)
            assertThat(søknad?.barn?.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad?.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad?.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad?.målform).isEqualTo(Målform.NN)
        }

        @Test
        fun `skal returnere null dersom det ikke finnes noen BehandlingSøknadsinfo for behandling`() {
            // Arrange
            val behandling = lagBehandling()

            every { behandlingSøknadsinfoService.hentJournalpostId(behandling.id) } returns null

            // Act
            val søknad = søknadService.finnSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNull()
        }
    }
}
