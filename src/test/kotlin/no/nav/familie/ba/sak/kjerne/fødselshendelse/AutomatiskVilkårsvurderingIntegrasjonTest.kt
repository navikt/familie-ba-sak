package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.mockk.every
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskVilkårsvurderingIntegrasjonTest(
    @Autowired val stegService: StegService,
    @Autowired val personopplysningerService: PersonopplysningerService,
    @Autowired val persongrunnlagService: PersongrunnlagService,
    @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Ikke bosatt i riket, skal ikke passere vilkår`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val mockSøkerUtenHjem = genererAutomatiskTestperson(bostedsadresser = emptyList())

        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerFnr) } returns mockSøkerUtenHjem
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnFnr) } returns mockBarnAutomatiskBehandling

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Barnet er gift, skal ikke passere vilkår`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val mockBarnGift = genererAutomatiskTestperson(sivilstander = listOf(Sivilstand(SIVILSTAND.GIFT)))

        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerFnr) } returns mockSøkerAutomatiskBehandling
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnFnr) } returns mockBarnGift

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn er over 18`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn = genererAutomatiskTestperson(LocalDate.parse("1999-10-10"), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(
            LocalDate.parse("1998-10-10"),
            setOf(
                ForelderBarnRelasjon(
                    Personident(barnFnr),
                    FORELDERBARNRELASJONROLLE.BARN
                )
            ),
            emptyList()
        )
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnFnr) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerFnr) } returns søker

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn ikke bor med mor`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn = genererAutomatiskTestperson(LocalDate.now(), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(
            LocalDate.parse("1998-10-10"),
            setOf(
                ForelderBarnRelasjon(
                    Personident(barnFnr),
                    FORELDERBARNRELASJONROLLE.BARN
                )
            ),
            emptyList(),
            bostedsadresser = listOf(
                Bostedsadresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    vegadresse = Vegadresse(
                        matrikkelId = 1111111111,
                        husnummer = "36",
                        husbokstav = "D",
                        bruksenhetsnummer = null,
                        adressenavn = "IkkeSamme -veien",
                        kommunenummer = "5423",
                        tilleggsnavn = null,
                        postnummer = "9050"
                    ),
                    matrikkeladresse = null,
                    ukjentBosted = null,
                )
            )
        )

        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnFnr) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerFnr) } returns søker

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår =
            stegService.håndterVilkårsvurdering(behandlingFørVilkår.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING))
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }
}
