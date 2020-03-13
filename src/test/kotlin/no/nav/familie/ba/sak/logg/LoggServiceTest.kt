package no.nav.familie.ba.sak.logg

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
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
        val vilkårsvurdering =
                SamletVilkårResultat(behandlingId = behandling.id,
                                     samletVilkårResultat = setOf(VilkårResultat(person = søker,
                                                                                 vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                 utfallType = UtfallType.IKKE_OPPFYLT),
                                                                  VilkårResultat(person = søker,
                                                                                 vilkårType = VilkårType.STØNADSPERIODE,
                                                                                 utfallType = UtfallType.OPPFYLT),
                                                                  VilkårResultat(person = barn,
                                                                                 vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                 utfallType = UtfallType.IKKE_OPPFYLT),
                                                                  VilkårResultat(person = barn,
                                                                                 vilkårType = VilkårType.STØNADSPERIODE,
                                                                                 utfallType = UtfallType.IKKE_OPPFYLT)))
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(behandling, null, vilkårsvurdering)

        Assertions.assertNotNull(vilkårsvurderingLogg)
        Assertions.assertEquals("Opprettet vilkårsvurdering", vilkårsvurderingLogg.tittel)


        val nyVilkårsvurdering =
                SamletVilkårResultat(behandlingId = behandling.id,
                                     samletVilkårResultat = setOf(VilkårResultat(person = søker,
                                                                                 vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                 utfallType = UtfallType.OPPFYLT),
                                                                  VilkårResultat(person = søker,
                                                                                 vilkårType = VilkårType.STØNADSPERIODE,
                                                                                 utfallType = UtfallType.OPPFYLT),
                                                                  VilkårResultat(person = barn,
                                                                                 vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                 utfallType = UtfallType.OPPFYLT),
                                                                  VilkårResultat(person = barn,
                                                                                 vilkårType = VilkårType.STØNADSPERIODE,
                                                                                 utfallType = UtfallType.OPPFYLT)))
        val nyVilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(behandling, vilkårsvurdering, nyVilkårsvurdering)

        Assertions.assertNotNull(nyVilkårsvurderingLogg)
        Assertions.assertEquals("Endring på vilkårsvurdering", nyVilkårsvurderingLogg.tittel)

        val logger = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, logger.size)
    }
}