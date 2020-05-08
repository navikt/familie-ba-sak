package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.Vilkårsvurdering
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

    @Autowired
    private lateinit var stegService: StegService

    @Test
    fun `vilkårsvurdering med kun JA blir innvilget`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val behandlingSteg: Vilkårsvurdering = stegService.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        Assertions.assertNotNull(behandlingSteg)

        val skalVæreUgyldig = behandlingSteg.validerSteg(behandling)
        Assertions.assertFalse(skalVæreUgyldig)

        val barn: Person = personopplysningGrunnlag.barna.find { it.personIdent.ident == barnFnr }!!

        val vurdertPersonResultater: List<RestPersonResultat> = behandlingResultat.personResultater.map { personResultat ->
            RestPersonResultat(
                    personIdent = personResultat.personIdent,
                    vilkårResultater = personResultat.vilkårResultater.map {
                        if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                            RestVilkårResultat(
                                    vilkårType = it.vilkårType,
                                    resultat = Resultat.JA,
                                    begrunnelse = "",
                                    periodeFom = barn.fødselsdato,
                                    periodeTom = barn.fødselsdato.plusYears(18)
                            )
                        } else {
                            RestVilkårResultat(
                                    vilkårType = it.vilkårType,
                                    resultat = Resultat.JA,
                                    begrunnelse = "",
                                    periodeFom = LocalDate.now().minusYears(2),
                                    periodeTom = null
                            )
                        }
                    }
            )
        }

        vilkårService.lagBehandlingResultatFraRestPersonResultater(personResultater = vurdertPersonResultater,
                                                                   behandlingId = behandling.id)

        val skalVæreGyldig = behandlingSteg.validerSteg(behandling)
        Assertions.assertTrue(skalVæreGyldig)
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
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårForPart(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårForPart(PersonType.SØKER))
    }
}

fun vilkårsvurderingInnvilget(søkerIdent: String,
                              barnIdent: String,
                              barnFødselsdato: LocalDate): List<RestPersonResultat> = listOf(
        RestPersonResultat(
                personIdent = søkerIdent,
                vilkårResultater = listOf(RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                             resultat = Resultat.JA,
                                                             periodeFom = LocalDate.of(2018, 5, 8),
                                                             periodeTom = null,
                                                             begrunnelse = ""))),
        RestPersonResultat(
                personIdent = barnIdent,
                vilkårResultater = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.JA,
                                           periodeFom = LocalDate.of(2018, 5, 8),
                                           periodeTom = null,
                                           begrunnelse = ""),
                        RestVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR,
                                           resultat = Resultat.JA,
                                           periodeFom = barnFødselsdato,
                                           periodeTom = barnFødselsdato.plusYears(18),
                                           begrunnelse = ""),
                        RestVilkårResultat(vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                           resultat = Resultat.JA,
                                           periodeFom = LocalDate.of(2018, 5, 8),
                                           periodeTom = null,
                                           begrunnelse = ""),
                        RestVilkårResultat(vilkårType = Vilkår.BOR_MED_SØKER,
                                           resultat = Resultat.JA,
                                           periodeFom = LocalDate.of(2018, 5, 8),
                                           periodeTom = null,
                                           begrunnelse = "")
                )))

fun vilkårsvurderingAvslått(
        personIdent: String): List<RestPersonResultat> = listOf(
        RestPersonResultat(
                personIdent = personIdent,
                vilkårResultater = listOf(
                        RestVilkårResultat(
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.NEI,
                                periodeFom = LocalDate.now(),
                                periodeTom = LocalDate.now(),
                                begrunnelse = ""))
        ))