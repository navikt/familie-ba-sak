package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.stream.Stream
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker as TilbakekrevingBrevmottaker

class TilbakekrevingServiceTest(
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val stegService: StegService,
    @Autowired private val tilbakekrevingService: TilbakekrevingService,
    @Autowired private val tilbakekrevingRepository: TilbakekrevingRepository,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val brevmottakerRepository: BrevmottakerRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel`() {
        // Arrange
        val søkerFnr = leggTilPersonInfo(fødselsdato = randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(fødselsdato = randomBarnFødselsdato())
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val restTilbakekreving =
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                varsel = "Varsel",
                begrunnelse = "Begrunnelse",
            )
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        // Act
        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        // Assert
        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Test
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel for institusjon`() {
        // Arrange
        val barnFnr = leggTilPersonInfo(fødselsdato = randomBarnFødselsdato())
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                søkerFnr = barnFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                institusjon = InstitusjonDto(orgNummer = "998765432", tssEksternId = "8000000"),
                brevmalService = brevmalService,
            )

        val restTilbakekreving =
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                varsel = "Varsel",
                begrunnelse = "Begrunnelse",
            )
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        // Act
        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        // Assert
        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun `lagOpprettTilbakekrevingRequest sender brevmottakere i kall mot familie-tilbake`(arguments: Pair<MottakerType, Vergetype>) {
        // Arrange
        val søkerFnr = leggTilPersonInfo(fødselsdato = randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(fødselsdato = randomBarnFødselsdato())

        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val brevmottaker =
            BrevmottakerDb(
                behandlingId = behandling.id,
                type = arguments.first,
                navn = "Donald Duck",
                adresselinje1 = "Andebyveien 1",
                postnummer = "0000",
                poststed = "OSLO",
                landkode = "NO",
            )
        brevmottakerRepository.saveAndFlush(brevmottaker)

        // Act
        val opprettTilbakekrevingRequest = tilbakekrevingService.lagOpprettTilbakekrevingRequest(behandling)

        // Assert
        assertEquals(1, opprettTilbakekrevingRequest.manuelleBrevmottakere.size)
        val actualBrevmottaker = opprettTilbakekrevingRequest.manuelleBrevmottakere.first()

        assertBrevmottakerEquals(brevmottaker, actualBrevmottaker)
        assertEquals(arguments.second, actualBrevmottaker.vergetype)
    }

    private fun assertBrevmottakerEquals(
        expected: BrevmottakerDb,
        actual: TilbakekrevingBrevmottaker,
    ) {
        assertEquals(expected.navn, actual.navn)
        assertEquals(expected.type.name, actual.type.name)
        assertEquals(expected.adresselinje1, actual.manuellAdresseInfo?.adresselinje1)
        assertEquals(expected.postnummer, actual.manuellAdresseInfo?.postnummer)
        assertEquals(expected.poststed, actual.manuellAdresseInfo?.poststed)
        assertEquals(expected.landkode, actual.manuellAdresseInfo?.landkode)
    }

    private class TestProvider : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext,
        ): Stream<out Arguments> =
            Stream.of(
                Arguments.of(Pair(MottakerType.FULLMEKTIG, Vergetype.ANNEN_FULLMEKTIG)),
                Arguments.of(Pair(MottakerType.VERGE, Vergetype.VERGE_FOR_VOKSEN)),
                Arguments.of(Pair(MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, null)),
            )
    }
}
