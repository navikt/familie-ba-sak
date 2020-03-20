package no.nav.familie.ba.sak.logg

import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev")
class LoggServiceTest(
        @Autowired
        private val loggService: LoggService,

        @Autowired
        private val stegService: StegService
) {

    @Test
    fun `Skal lage noen logginnslag på forskjellige behandlinger og hente dem fra databasen`() {
        val behandling: Behandling = lagBehandling()
        val behandling1: Behandling = lagBehandling()

        val logg1 = Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Førstegangsbehandling opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg1)

        val logg2 = Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Revurdering opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg2)

        val logg3 = Logg(
                behandlingId = behandling1.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Førstegangsbehandling opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg3)

        val loggForBehandling = loggService.hentLoggForBehandling(behandling.id)
        Assertions.assertEquals(2, loggForBehandling.size)

        val loggForBehandling1 = loggService.hentLoggForBehandling(behandling1.id)
        Assertions.assertEquals(1, loggForBehandling1.size)
    }

    @Test
    fun `Skal lage logginnslag ved stegflyt for automatisk behandling`() {
        val behandling = stegService.håndterNyBehandlingFraHendelse(NyBehandlingHendelse(
                søkersIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr())
        ))

        val loggForBehandling = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, loggForBehandling.size)
        Assertions.assertTrue(loggForBehandling.any { it.type == LoggType.FØDSELSHENDELSE })
        Assertions.assertTrue(loggForBehandling.any { it.type == LoggType.BEHANDLING_OPPRETTET })
        Assertions.assertTrue(loggForBehandling.none { it.rolle != BehandlerRolle.SYSTEM })
    }

    @Test
    fun `Skal lage nye vilkårslogger og endringer`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0L)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerFnr),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(2019, 1, 1),
                           navn = "",
                           kjønn = Kjønn.KVINNE)

        val barn = Person(aktørId = randomAktørId(),
                          personIdent = PersonIdent(barnFnr),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.of(2019, 1, 1),
                          navn = "",
                          kjønn = Kjønn.MANN)

        val behandling = lagBehandling()
        val vilkårsvurdering = BehandlingResultat(
                id = behandling.id,
                behandling = behandling,
                aktiv = true,
                periodeResultater = mutableSetOf(
                        PeriodeResultat(vilkårResultater = mutableSetOf(VilkårResultat(person = søker,
                                                                                       vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                       resultat = Resultat.NEI),
                                                                        VilkårResultat(person = søker,
                                                                                      vilkårType = Vilkår.STØNADSPERIODE,
                                                                                      resultat = Resultat.JA),
                                                                        VilkårResultat(person = barn,
                                                                                      vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                      resultat = Resultat.NEI),
                                                                        VilkårResultat(person = barn,
                                                                                      vilkårType = Vilkår.STØNADSPERIODE,
                                                                                      resultat = Resultat.NEI)),
                                        periodeFom = LocalDate.now(), periodeTom = LocalDate.now())))//TODO: Oppdater med periode
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(behandling, null, vilkårsvurdering)

        Assertions.assertNotNull(vilkårsvurderingLogg)
        Assertions.assertEquals("Opprettet vilkårsvurdering", vilkårsvurderingLogg.tittel)


        val nyVilkårsvurdering = BehandlingResultat(
                id = behandling.id,
                behandling = behandling,
                aktiv = true,
                periodeResultater = mutableSetOf(
                        PeriodeResultat(vilkårResultater = mutableSetOf(VilkårResultat(person = søker,
                                                                                       vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                       resultat = Resultat.JA),
                                                                        VilkårResultat(person = søker,
                                                                                      vilkårType = Vilkår.STØNADSPERIODE,
                                                                                      resultat = Resultat.JA),
                                                                        VilkårResultat(person = barn,
                                                                                      vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                      resultat = Resultat.JA),
                                                                        VilkårResultat(person = barn,
                                                                                      vilkårType = Vilkår.STØNADSPERIODE,
                                                                                      resultat = Resultat.JA)),
                                        periodeFom = LocalDate.now(), periodeTom = LocalDate.now())))//TODO: Oppdater med periode
        val nyVilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(behandling, vilkårsvurdering, nyVilkårsvurdering)

        Assertions.assertNotNull(nyVilkårsvurderingLogg)
        Assertions.assertEquals("Endring på vilkårsvurdering", nyVilkårsvurderingLogg.tittel)

        val logger = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, logger.size)
    }
}