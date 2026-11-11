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
                    barnetrygdSøknad =
                        lagBarnetrygdSøknadV9(
                            barnFnr = listOf(barn1, barn2),
                            søknadstype = Søknadstype.ORDINÆR,
                            erEøs = true,
                            originalspråk = "nn",
                            inneholderVedlegg = true,
                            erFosterbarn = true,
                            erIBeredskapshjem = true,
                            harKryssetForDeltBosted = true,
                            oppholderSegIInstitusjon = true,
                            søkerErAsylsøker = true,
                            barnErAsylsøker = true,
                            erAdoptertFraUtland = true,
                            borFastMedSøker = true,
                            barnHarOppholdtSegUtenforNorgeSiste12Mnd = true,
                            søkerHarOppholdtSegSammenhengendeINorgeSiste12Mnd = false,
                        ),
                )

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV9
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV9.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV9()

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNotNull
            assertThat(søknad?.barn).hasSize(2)
            assertThat(søknad?.barn?.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad?.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad?.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad?.målform).isEqualTo(Målform.NN)
            assertThat(søknad?.harKryssetPåEøsSpørsmål).isTrue()
            assertThat(søknad?.inneholderVedlegg).isTrue()
            assertThat(søknad?.barn?.map { it.erFosterbarn }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erIBeredskapshjem }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.harKryssetForDeltBosted }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.oppholderSegIInstitusjon }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erAsylsøker }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erAdoptertFraUtland }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.borFastMedSøker }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.harOppholdtSegUtenforNorgeSiste12Mnd }).containsOnly(true)
            assertThat(søknad?.søker?.erAsylsøker).isTrue()
            assertThat(søknad?.søker?.harOppholdtSegSammenhengendeINorgeSiste12Mnd).isFalse()
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
                    barnetrygdSøknad =
                        lagBarnetrygdSøknadV10(
                            barnFnr = listOf(barn1, barn2),
                            søknadstype = Søknadstype.ORDINÆR,
                            erEøs = true,
                            originalspråk = "nn",
                            inneholderVedlegg = true,
                            erFosterbarn = true,
                            erIBeredskapshjem = true,
                            harKryssetForDeltBosted = true,
                            oppholderSegIInstitusjon = true,
                            søkerErAsylsøker = true,
                            barnErAsylsøker = true,
                            erAdoptertFraUtland = true,
                            borFastMedSøker = true,
                            barnHarOppholdtSegUtenforNorgeSiste12Mnd = true,
                            søkerHarOppholdtSegSammenhengendeINorgeSiste12Mnd = false,
                        ),
                )

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV10
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV10.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV10()

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNotNull
            assertThat(søknad?.barn).hasSize(2)
            assertThat(søknad?.barn?.map { it.fnr }).containsExactlyInAnyOrder(barn1, barn2)
            assertThat(søknad?.behandlingKategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(søknad?.behandlingUnderkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
            assertThat(søknad?.målform).isEqualTo(Målform.NN)
            assertThat(søknad?.harKryssetPåEøsSpørsmål).isTrue()
            assertThat(søknad?.inneholderVedlegg).isTrue()
            assertThat(søknad?.barn?.map { it.erFosterbarn }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erIBeredskapshjem }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.harKryssetForDeltBosted }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.oppholderSegIInstitusjon }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erAsylsøker }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erAdoptertFraUtland }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.borFastMedSøker }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.harOppholdtSegUtenforNorgeSiste12Mnd }).containsOnly(true)
            assertThat(søknad?.søker?.erAsylsøker).isTrue()
            assertThat(søknad?.søker?.harOppholdtSegSammenhengendeINorgeSiste12Mnd).isFalse()
        }

        @Test
        fun `skal mappe hvert svar om institusjon, asyl og adopsjon fra VersjonertBarnetrygdSøknadV10 til riktig felt`() {
            // Arrange
            val behandling = lagBehandling()
            val journalpostId = "123456789"
            val versjonertBarnetrygdSøknadV10 =
                VersjonertBarnetrygdSøknadV10(
                    barnetrygdSøknad =
                        lagBarnetrygdSøknadV10(
                            oppholderSegIInstitusjon = false,
                            søkerErAsylsøker = false,
                            barnErAsylsøker = true,
                            erAdoptertFraUtland = true,
                        ),
                )

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV10
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV10.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV10()

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad?.barn?.map { it.oppholderSegIInstitusjon }).containsOnly(false)
            assertThat(søknad?.søker?.erAsylsøker).isFalse()
            assertThat(søknad?.barn?.map { it.erAsylsøker }).containsOnly(true)
            assertThat(søknad?.barn?.map { it.erAdoptertFraUtland }).containsOnly(true)
        }

        @Test
        fun `skal mappe fakta om søknaden til false når søker ikke har krysset av og det ikke er lastet opp vedlegg`() {
            // Arrange
            val behandling = lagBehandling()
            val journalpostId = "123456789"
            val versjonertBarnetrygdSøknadV10 =
                VersjonertBarnetrygdSøknadV10(
                    barnetrygdSøknad = lagBarnetrygdSøknadV10(barnFnr = listOf(randomFnr())),
                )

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV10
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV10.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV10()

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad?.harKryssetPåEøsSpørsmål).isFalse()
            assertThat(søknad?.inneholderVedlegg).isFalse()
            assertThat(søknad?.barn?.map { it.erFosterbarn }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.erIBeredskapshjem }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.harKryssetForDeltBosted }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.oppholderSegIInstitusjon }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.erAsylsøker }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.erAdoptertFraUtland }).containsOnly(false)
            assertThat(søknad?.barn?.map { it.harOppholdtSegUtenforNorgeSiste12Mnd }).containsOnly(false)
            assertThat(søknad?.søker?.erAsylsøker).isFalse()
        }

        @Test
        fun `skal mappe at søker har oppholdt seg sammenhengende i Norge når søker har svart ja på spørsmålet i VersjonertBarnetrygdSøknadV10`() {
            // Arrange
            val behandling = lagBehandling()
            val journalpostId = "123456789"
            val versjonertBarnetrygdSøknadV10 =
                VersjonertBarnetrygdSøknadV10(
                    barnetrygdSøknad = lagBarnetrygdSøknadV10(søkerHarOppholdtSegSammenhengendeINorgeSiste12Mnd = true),
                )

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns journalpostId
            every { integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId) } returns versjonertBarnetrygdSøknadV10
            every { søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknadV10.barnetrygdSøknad.kontraktVersjon) } returns SøknadMapperV10()

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad?.søker?.harOppholdtSegSammenhengendeINorgeSiste12Mnd).isTrue()
        }

        @Test
        fun `skal returnere null dersom det ikke finnes noen BehandlingSøknadsinfo for behandling`() {
            // Arrange
            val behandling = lagBehandling()

            every { behandlingSøknadsinfoService.finnDigitalSøknad(behandling.id)?.journalpostId } returns null

            // Act
            val søknad = søknadService.finnDigitalSøknad(behandlingId = behandling.id)

            // Assert
            assertThat(søknad).isNull()
        }
    }
}
