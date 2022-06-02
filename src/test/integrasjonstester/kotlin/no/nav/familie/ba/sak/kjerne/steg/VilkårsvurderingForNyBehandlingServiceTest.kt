package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagBarnVilkårResultat
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårsvurderingForNyBehandlingServiceTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

) : AbstractSpringIntegrationTest() {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flyttesak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 1, 14)
        val barnetsFødselsdato = LocalDate.now().minusMonths(6)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(fnr, true)
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = søkerFødselsdato,
                behandlingId = forrigeBehandling.id
            )
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(barnFnr, true)
        )
        barnPersonResultat.setSortedVilkårResultater(
            lagBarnVilkårResultat(
                barnPersonResultat = barnPersonResultat,
                barnetsFødselsdato = barnetsFødselsdato,
                behandlingId = forrigeBehandling.id,
                forrigeMigreringsdato = LocalDate.now().minusMonths(1),
                flytteSak = true
            )
        )

        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.now().minusMonths(5)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato
            )
        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater
        Assertions.assertTrue { søkerVilkårResultat.size == 2 }
        Assertions.assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        Assertions.assertTrue { barnVilkårResultat.size == 5 }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1)
            }
        }

        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flere perioder`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(fnr, true)
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = søkerFødselsdato,
                behandlingId = forrigeBehandling.id
            )
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(barnFnr, true)
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 4, 14),
                    periodeTom = LocalDate.of(2021, 8, 16),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 10, 5),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                )
            )
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato
            )
        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater
        Assertions.assertTrue { søkerVilkårResultat.size == 2 }
        Assertions.assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        Assertions.assertTrue { barnVilkårResultat.size == 6 }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == LocalDate.of(2021, 8, 16)
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == LocalDate.of(2021, 10, 5) &&
                    it.periodeTom == null
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1)
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for helmanuell migrering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnetsFødselsdato = LocalDate.of(2020, 8, 15)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato
        )

        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        Assertions.assertTrue { vilkårsvurdering.personResultater.size == 2 }

        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.size == 2 }
        Assertions.assertTrue {
            søkerPersonResultat.vilkårResultater.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.size == 5 }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.UNDER_18_ÅR &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1) &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.GIFT_PARTNERSKAP &&
                    it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.filter { !it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for helmanuell migrering med migreringsdato før barnetsfødselsdato`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnetsFødselsdato = LocalDate.of(2021, 8, 15)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr, true),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato
        )

        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        Assertions.assertTrue { vilkårsvurdering.personResultater.size == 2 }

        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.size == 2 }
        Assertions.assertTrue {
            søkerPersonResultat.vilkårResultater.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.size == 5 }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.UNDER_18_ÅR &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1) &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.GIFT_PARTNERSKAP &&
                    it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.filter { !it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }.all {
                it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
    }
}
