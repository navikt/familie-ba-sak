package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.fake.MockPdlRestClient.Companion.leggTilBostedsadresseIPDL
import no.nav.familie.ba.sak.fake.MockPersonopplysningerService.Companion.leggTilBostedsadresserIPersonInfo
import no.nav.familie.ba.sak.fake.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.MockPersonopplysningerService.Companion.leggTilRelasjonIPersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse.INNVILGET_AUTOVEDTAK_FØDSEL_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms.ALTA
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class FødselshendelseTest(
    @Autowired private val autovedtakStegService: AutovedtakStegService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val stegService: StegService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val beregningService: BeregningService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal innvilge finnmarkstillegg i fødselshendelse førstegangsbehandling hvis mor og barn bor i finnmark`() {
        // Arrange
        val barnFødselsdato = LocalDate.of(2025, 9, 15)
        val barnFnr = leggTilPersonInfo(barnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val bostedsadresseIFinnmark =
            Bostedsadresse(
                gyldigFraOgMed = barnFødselsdato,
                vegadresse =
                    Vegadresse(
                        matrikkelId = 1L,
                        husnummer = "1",
                        husbokstav = "A",
                        bruksenhetsnummer = "H0101",
                        adressenavn = "Adresseveien",
                        kommunenummer = ALTA.kommunenummer,
                        tilleggsnavn = "Under broen",
                        postnummer = "9510",
                    ),
            )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = barnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilBostedsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresser = listOf(bostedsadresseIFinnmark),
        )

        leggTilBostedsadresseIPDL(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresse = bostedsadresseIFinnmark,
        )

        // Act
        autovedtakStegService.kjørBehandlingFødselshendelse(
            mottakersAktør = søkerAktør,
            nyBehandlingHendelse =
                NyBehandlingHendelse(
                    morsIdent = søkerFnr,
                    barnasIdenter = listOf(barnFnr),
                ),
            førstegangKjørt = LocalDateTime.now(),
        )

        // Assert
        val fagsak = fagsakService.hentFagsakerPåPerson(søkerAktør).single()
        val behandling = behandlingHentOgPersisterService.hentBehandlinger(fagsak.id).single()

        val innvilgedeAndeler = beregningService.hentTilkjentYtelseForBehandling(behandling.id).andelerTilkjentYtelse

        assertThat(innvilgedeAndeler).anySatisfy {
            assertThat(it.type).isEqualTo(FINNMARKSTILLEGG)
            assertThat(it.stønadFom).isEqualTo(barnFødselsdato.nesteMåned())
        }

        val vedtaksperioder = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_AUTOVEDTAK_FØDSEL_FINNMARKSTILLEGG.toString() }
        }
    }

    @Test
    fun `Skal innvilge finnmarkstillegg i fødselshendelse revurdering hvis mor og barn bor i finnmark`() {
        // Arrange
        val eldsteBarnFødselsdato = LocalDate.of(2020, 1, 1)
        val yngsteBarnFødselsdato = LocalDate.of(2025, 9, 15)

        val eldsteBarnFnr = leggTilPersonInfo(eldsteBarnFødselsdato)
        val yngsteBarnFnr = leggTilPersonInfo(yngsteBarnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))

        val yngsteBarnAktør = personidentService.hentAktør(yngsteBarnFnr)
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val bostedsadresseIFinnmark =
            Bostedsadresse(
                gyldigFraOgMed = yngsteBarnFødselsdato,
                vegadresse =
                    Vegadresse(
                        matrikkelId = 1L,
                        husnummer = "1",
                        husbokstav = "A",
                        bruksenhetsnummer = "H0101",
                        adressenavn = "Adresseveien",
                        kommunenummer = ALTA.kommunenummer,
                        tilleggsnavn = "Under broen",
                        postnummer = "9510",
                    ),
            )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = eldsteBarnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = yngsteBarnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilBostedsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, yngsteBarnFnr),
            bostedsadresser = listOf(bostedsadresseIFinnmark),
        )

        leggTilBostedsadresseIPDL(
            personIdenter = listOf(søkerFnr, eldsteBarnFnr, yngsteBarnFnr),
            bostedsadresse = bostedsadresseIFinnmark,
        )

        val forrigeBehandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(eldsteBarnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
                vilkårInnvilgetFom = LocalDate.now().minusYears(1),
            )

        // Act
        autovedtakStegService.kjørBehandlingFødselshendelse(
            mottakersAktør = søkerAktør,
            nyBehandlingHendelse =
                NyBehandlingHendelse(
                    morsIdent = søkerFnr,
                    barnasIdenter = listOf(yngsteBarnFnr),
                ),
            førstegangKjørt = LocalDateTime.now(),
        )

        // Assert
        val fagsak = fagsakService.hentFagsakerPåPerson(søkerAktør).single()
        val behandling = behandlingHentOgPersisterService.hentBehandlinger(fagsak.id).first { it.id != forrigeBehandling.id }

        val innvilgedeAndeler = beregningService.hentTilkjentYtelseForBehandling(behandling.id).andelerTilkjentYtelse

        assertThat(innvilgedeAndeler).anySatisfy {
            assertThat(it.type).isEqualTo(FINNMARKSTILLEGG)
            assertThat(it.stønadFom).isEqualTo(yngsteBarnFødselsdato.nesteMåned())
            assertThat(it.aktør).isEqualTo(yngsteBarnAktør)
        }

        val vedtaksperioder = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_AUTOVEDTAK_FØDSEL_FINNMARKSTILLEGG.toString() }
        }
    }
}
