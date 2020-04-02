package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriodeResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class VilkårServiceTest {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårService: VilkårService

    @Test
    fun `vilkårsvurdering med kun JA blir innvilget`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val behandlingResultat =
                vilkårService.kontrollerVurderteVilkårOgLagResultat(vilkårsvurderingInnvilget(fnr),
                                                                    behandling.id
                )
        Assertions.assertEquals(behandlingResultat.periodeResultater.size,
                                vilkårsvurderingInnvilget(fnr).size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(setOf(
                Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårForPart(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårForPart(PersonType.SØKER))
    }
}

fun vilkårsvurderingInnvilget(personIdent: String): List<RestPeriodeResultat> = listOf(RestPeriodeResultat(
        personIdent = personIdent,
        periodeFom = LocalDate.now(),
        periodeTom = LocalDate.now(),
        vilkårResultater = listOf(RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                     resultat = Resultat.JA))
))

fun vilkårsvurderingAvslått(personIdent: String): List<RestPeriodeResultat> = listOf(RestPeriodeResultat(
        personIdent = personIdent,
        periodeFom = LocalDate.now(),
        periodeTom = LocalDate.now(),
        vilkårResultater = listOf(RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                     resultat = Resultat.NEI))
))

fun vilkårsvurderingDelvisInnvilget(personIdent: String): List<RestPeriodeResultat> = listOf(RestPeriodeResultat(
        personIdent = personIdent,
        periodeFom = LocalDate.now(),
        periodeTom = LocalDate.now(),
        vilkårResultater = listOf(RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                     resultat = Resultat.JA),
                                  RestVilkårResultat(vilkårType = Vilkår.STØNADSPERIODE,
                                                     resultat = Resultat.NEI))
))