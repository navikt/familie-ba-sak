package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskVilkårsvurderingIntegrasjonTest(
    @Autowired val stegService: StegService,
    @Autowired val persongrunnlagService: PersongrunnlagService,
    @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Ikke bosatt i riket, skal ikke passere vilkår`() {
        val mockSøkerUtenHjem = genererAutomatiskTestperson(bostedsadresser = emptyList())

        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato(), mockSøkerUtenHjem)
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato(), mockBarnAutomatiskBehandling)

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår =
            stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(Behandlingsresultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Barnet er gift, skal ikke passere vilkår`() {
        val mockBarnGift = genererAutomatiskTestperson(sivilstander = listOf(Sivilstand(SIVILSTANDTYPE.GIFT)))

        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato(), mockSøkerAutomatiskBehandling)
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato(), mockBarnGift)

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår =
            stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(Behandlingsresultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn er over 18`() {
        val barnFødselsdato = LocalDate.parse("1999-10-10")
        val barn = genererAutomatiskTestperson(barnFødselsdato, emptySet(), emptyList())

        val søkerFødselsdato = LocalDate.parse("1998-10-10")
        val søker =
            genererAutomatiskTestperson(
                søkerFødselsdato,
                setOf(
                    ForelderBarnRelasjon(
                        lagAktør(randomFnr(randomBarnFødselsdato())),
                        FORELDERBARNRELASJONROLLE.BARN,
                    ),
                ),
                emptyList(),
            )

        val søkerFnr = leggTilPersonInfo(søkerFødselsdato, barn)
        val barnFnr = leggTilPersonInfo(barnFødselsdato, søker)

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår =
            stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(Behandlingsresultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn ikke bor med mor`() {
        val barnFødselsdato = LocalDate.now()
        val barn = genererAutomatiskTestperson(barnFødselsdato, emptySet(), emptyList())
        val barnFnr = leggTilPersonInfo(barnFødselsdato, barn)

        val søkerFødselsdato = LocalDate.parse("1998-10-10")
        val søker =
            genererAutomatiskTestperson(
                søkerFødselsdato,
                setOf(
                    ForelderBarnRelasjon(
                        lagAktør(barnFnr),
                        FORELDERBARNRELASJONROLLE.BARN,
                    ),
                ),
                emptyList(),
                bostedsadresser =
                    listOf(
                        Bostedsadresse(
                            gyldigFraOgMed = null,
                            gyldigTilOgMed = null,
                            vegadresse =
                                Vegadresse(
                                    matrikkelId = 1111111111,
                                    husnummer = "36",
                                    husbokstav = "D",
                                    bruksenhetsnummer = null,
                                    adressenavn = "IkkeSamme -veien",
                                    kommunenummer = "5423",
                                    tilleggsnavn = null,
                                    postnummer = "9050",
                                ),
                            matrikkeladresse = null,
                            ukjentBosted = null,
                        ),
                    ),
            )

        val søkerFnr = leggTilPersonInfo(søkerFødselsdato, søker)

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår =
            stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(Behandlingsresultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }
}
