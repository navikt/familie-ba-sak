package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.fake.FakePdlRestKlient.Companion.leggTilBostedsadresseIPDL
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilBostedsadresserIPersonInfo
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilOppholdsadresserIPersonInfo
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilRelasjonIPersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG_UTEN_DATO
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG_UTEN_DATO
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms.ALTA
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
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
    private val vegadresseIFinnmark =
        Vegadresse(
            matrikkelId = 1L,
            husnummer = "1",
            husbokstav = "A",
            bruksenhetsnummer = "H0101",
            adressenavn = "Adresseveien",
            kommunenummer = ALTA.kommunenummer,
            tilleggsnavn = "Under broen",
            postnummer = "9510",
        )

    private val vegadresseUtenforFinnmark =
        Vegadresse(
            matrikkelId = 1L,
            husnummer = "1",
            husbokstav = "A",
            bruksenhetsnummer = "H0101",
            adressenavn = "Adresseveien",
            kommunenummer = "0301",
            tilleggsnavn = "Under broen",
            postnummer = "0562",
        )

    @Test
    fun `Skal innvilge barnetrygd i fødselshendelse førstegangsbehandling hvis mor har bodd minst 6 mnd i Norge og barn bor i Norge ved fødsel`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barnFnr = leggTilPersonInfo(barnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val bostedsadresseUtenforFinnmark =
            Bostedsadresse(
                gyldigFraOgMed = barnFødselsdato.minusMonths(6),
                vegadresse = vegadresseUtenforFinnmark,
            )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = barnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilBostedsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresser = listOf(bostedsadresseUtenforFinnmark),
        )

        leggTilBostedsadresseIPDL(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresse = bostedsadresseUtenforFinnmark,
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

        assertThat(innvilgedeAndeler).allSatisfy {
            assertThat(it.type).isEqualTo(YtelseType.ORDINÆR_BARNETRYGD)
            assertThat(it.stønadFom).isEqualTo(barnFødselsdato.nesteMåned())
        }

        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).allSatisfy {
            assertThat { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE.toString() }
        }
    }

    @Test
    fun `Skal ikke innvilge barnetrygd i fødselshendelse førstegangsbehandling hvis mor har bodd mindre enn 6 mnd i Norge`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barnFnr = leggTilPersonInfo(barnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val bostedsadresseUtenforFinnmark =
            Bostedsadresse(
                gyldigFraOgMed = barnFødselsdato.minusMonths(6).plusDays(1),
                vegadresse = vegadresseUtenforFinnmark,
            )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = barnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilBostedsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresser = listOf(bostedsadresseUtenforFinnmark),
        )

        leggTilBostedsadresseIPDL(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresse = bostedsadresseUtenforFinnmark,
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

        assertThat(innvilgedeAndeler).hasSize(0)
    }

    @Test
    fun `Skal innvilge finnmarkstillegg i fødselshendelse førstegangsbehandling hvis mor og barn bor i Finnmark og øvrige krav til bosatt i riket er oppfylt`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barnFnr = leggTilPersonInfo(barnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val bostedsadresseIFinnmark =
            Bostedsadresse(
                gyldigFraOgMed = barnFødselsdato.minusMonths(6),
                vegadresse = vegadresseIFinnmark,
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

        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_FINNMARKSTILLEGG_UTEN_DATO.toString() }
        }
    }

    @Test
    fun `Skal innvilge finnmarkstillegg i fødselshendelse revurdering hvis mor og barn bor i Finnmark`() {
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
                vegadresse = vegadresseIFinnmark,
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

        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_FINNMARKSTILLEGG_UTEN_DATO.toString() }
        }
    }

    @Test
    fun `Skal innvilge svalbardtillegg i fødselshendelse førstegangsbehandling hvis mor og barn bor på Svalbard og øvrige krav til bosatt i riket er oppfylt`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barnFnr = leggTilPersonInfo(barnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val oppholdsadressePåSvalbard =
            Oppholdsadresse(
                gyldigFraOgMed = barnFødselsdato.minusMonths(6),
                gyldigTilOgMed = null,
                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD.name,
            )

        val bostedsadresse =
            Bostedsadresse(
                gyldigFraOgMed = barnFødselsdato,
                vegadresse = vegadresseUtenforFinnmark,
            )

        leggTilRelasjonIPersonInfo(
            personIdent = søkerFnr,
            relatertPersonsIdent = barnFnr,
            relatertPersonsRelasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        )

        leggTilBostedsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresser = listOf(bostedsadresse),
        )

        leggTilOppholdsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, barnFnr),
            oppholdsadresser = listOf(oppholdsadressePåSvalbard),
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
            assertThat(it.type).isEqualTo(SVALBARDTILLEGG)
            assertThat(it.stønadFom).isEqualTo(barnFødselsdato.nesteMåned())
        }

        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_SVALBARDTILLEGG_UTEN_DATO.toString() }
        }
    }

    @Test
    fun `Skal innvilge svalbardtillegg i fødselshendelse revurdering hvis mor og barn bor på Svalbard`() {
        // Arrange
        val eldsteBarnFødselsdato = LocalDate.of(2020, 1, 1)
        val yngsteBarnFødselsdato = LocalDate.of(2025, 9, 15)

        val eldsteBarnFnr = leggTilPersonInfo(eldsteBarnFødselsdato)
        val yngsteBarnFnr = leggTilPersonInfo(yngsteBarnFødselsdato)
        val søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))

        val yngsteBarnAktør = personidentService.hentAktør(yngsteBarnFnr)
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val oppholdsadressePåSvalbard =
            Oppholdsadresse(
                gyldigFraOgMed = yngsteBarnFødselsdato,
                gyldigTilOgMed = null,
                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD.name,
            )

        val bostedsadresse =
            Bostedsadresse(
                gyldigFraOgMed = yngsteBarnFødselsdato,
                vegadresse = vegadresseUtenforFinnmark,
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
            personIdenter = listOf(søkerFnr, eldsteBarnFnr, yngsteBarnFnr),
            bostedsadresser = listOf(bostedsadresse),
        )

        leggTilOppholdsadresserIPersonInfo(
            personIdenter = listOf(søkerFnr, eldsteBarnFnr, yngsteBarnFnr),
            oppholdsadresser = listOf(oppholdsadressePåSvalbard),
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
            assertThat(it.type).isEqualTo(SVALBARDTILLEGG)
            assertThat(it.stønadFom).isEqualTo(yngsteBarnFødselsdato.nesteMåned())
            assertThat(it.aktør).isEqualTo(yngsteBarnAktør)
        }

        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandling.id)
        assertThat(vedtaksperioder.single().begrunnelser).anySatisfy {
            assertThat { it.standardbegrunnelse == INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN.toString() }
            assertThat { it.standardbegrunnelse == INNVILGET_SVALBARDTILLEGG_UTEN_DATO.toString() }
        }
    }
}
