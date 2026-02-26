package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagGrUtenlandskOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBosattIRiketForFødselshendelserServiceTest {
    private val persongrunnlagService: PersongrunnlagService = mockk()

    private val preutfyllBosattIRiketForFødselshendelserService =
        PreutfyllBosattIRiketForFødselshendelserService(
            persongrunnlagService = persongrunnlagService,
        )

    @Test
    fun `skal gi oppfylt bosatt i riket vilkår for søker og barn dersom søker har vært bosatt i Norge i minst 6 mnd og barn har vært bosatt fra fødsel`() {
        // Arrange

        val barnFødselsdato = LocalDate.now()
        val barn = lagPerson(fødselsdato = barnFødselsdato, type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val fagsak = lagFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasFødselsdatoer = listOf(barnFødselsdato),
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                barnAktør = listOf(barn.aktør),
                søkerAktør = søker.aktør,
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    when (it.type) {
                        PersonType.SØKER -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6),
                                            ),
                                        matrikkelId = 12345L,
                                    ),
                                )
                        }

                        PersonType.BARN -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato,
                                            ),
                                        matrikkelId = 12345L,
                                    ),
                                )
                        }

                        else -> {
                            it.bostedsadresser = mutableListOf()
                        }
                    }
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør),
                        lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            barnSomSkalVurderesIFødselshendelse = listOf(barn.aktør.aktivFødselsnummer()),
        )

        // Assert
        val vilkårResultatSøker =
            vilkårsvurdering.personResultater
                .single { it.aktør == søker.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultatSøker.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatSøker.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Norsk bostedsadresse i minst 6 måneder.")
        assertThat(vilkårResultatSøker.begrunnelseForManuellKontroll).isNull()

        val vilkårResultatBarn =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultatBarn.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatBarn.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Bosatt i Norge siden fødsel.")
        assertThat(vilkårResultatBarn.begrunnelseForManuellKontroll).isNull()
    }

    @Test
    fun `skal gi ikke oppfylt bosatt i riket vilkår for søker og barn dersom søker ikke har vært bosatt i Norge i minst 6 mnd og barnet ikke har vært bosatt fra fødsel`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barn = lagPerson(fødselsdato = barnFødselsdato, type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val fagsak = lagFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasFødselsdatoer = listOf(barnFødselsdato),
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                barnAktør = listOf(barn.aktør),
                søkerAktør = søker.aktør,
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    when (it.type) {
                        PersonType.SØKER -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6).plusDays(1),
                                            ),
                                        matrikkelId = 12345L,
                                    ),
                                )
                        }

                        PersonType.BARN -> {
                            it.oppholdsadresser =
                                mutableListOf(
                                    lagGrUtenlandskOppholdsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6).plusDays(1),
                                            ),
                                    ),
                                )
                        }

                        else -> {
                            it.bostedsadresser = mutableListOf()
                        }
                    }
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør),
                        lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            barnSomSkalVurderesIFødselshendelse = listOf(barn.aktør.aktivFødselsnummer()),
        )

        // Assert
        val vilkårResultatSøker =
            vilkårsvurdering.personResultater
                .single { it.aktør == søker.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultatSøker.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårResultatSøker.evalueringÅrsaker).containsExactly(VilkårIkkeOppfyltÅrsak.HAR_IKKE_BODD_I_RIKET_6_MND.name)

        val vilkårResultatBarn =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultatBarn.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårResultatBarn.evalueringÅrsaker).containsExactly(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET.name)
    }

    @Test
    fun `skal gi oppfylt bosatt i riket vilkår med utdypende vilkårsvurdering bosatt på Svalbard dersom søker og barn bor på Svalbard og øvrige krav er oppfylt`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barn = lagPerson(fødselsdato = barnFødselsdato, type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val fagsak = lagFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasFødselsdatoer = listOf(barnFødselsdato),
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                barnAktør = listOf(barn.aktør),
                søkerAktør = søker.aktør,
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    when (it.type) {
                        PersonType.SØKER -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6),
                                            ),
                                        matrikkelId = 12345L,
                                    ),
                                )
                            it.oppholdsadresser =
                                mutableListOf(
                                    lagGrVegadresseOppholdsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6),
                                            ),
                                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                    ),
                                )
                        }

                        PersonType.BARN -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato,
                                            ),
                                        matrikkelId = 12345L,
                                    ),
                                )
                            it.oppholdsadresser =
                                mutableListOf(
                                    lagGrVegadresseOppholdsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato,
                                            ),
                                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                    ),
                                )
                        }

                        else -> {
                            it.bostedsadresser = mutableListOf()
                        }
                    }
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør),
                        lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            barnSomSkalVurderesIFødselshendelse = listOf(barn.aktør.aktivFødselsnummer()),
        )

        // Assert
        val vilkårResultatSøker =
            vilkårsvurdering.personResultater
                .single { it.aktør == søker.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultatSøker.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatSøker.utdypendeVilkårsvurderinger).containsExactly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        assertThat(vilkårResultatSøker.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Norsk bostedsadresse i minst 6 måneder.")
        assertThat(vilkårResultatSøker.begrunnelseForManuellKontroll).isNull()

        val vilkårResultatBarn =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultatBarn.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatSøker.utdypendeVilkårsvurderinger).containsExactly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        assertThat(vilkårResultatBarn.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Bosatt i Norge siden fødsel.")
        assertThat(vilkårResultatBarn.begrunnelseForManuellKontroll).isNull()
    }

    @Test
    fun `skal gi oppfylt bosatt i riket vilkår med utdypende vilkårsvurdering bosatt i Finnmark eller NordTroms dersom søker og barn bor i Finnmark eller NordTroms og øvrige krav er oppfylt`() {
        // Arrange
        val barnFødselsdato = LocalDate.now()
        val barn = lagPerson(fødselsdato = barnFødselsdato, type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val fagsak = lagFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasFødselsdatoer = listOf(barnFødselsdato),
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                barnAktør = listOf(barn.aktør),
                søkerAktør = søker.aktør,
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    when (it.type) {
                        PersonType.SØKER -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato.minusMonths(6),
                                            ),
                                        kommunenummer = KommunerIFinnmarkOgNordTroms.BERLEVÅG.kommunenummer,
                                        matrikkelId = 12345L,
                                    ),
                                )
                        }

                        PersonType.BARN -> {
                            it.bostedsadresser =
                                mutableListOf(
                                    lagGrVegadresseBostedsadresse(
                                        periode =
                                            DatoIntervallEntitet(
                                                fom = barnFødselsdato,
                                            ),
                                        kommunenummer = KommunerIFinnmarkOgNordTroms.BERLEVÅG.kommunenummer,
                                        matrikkelId = 12345L,
                                    ),
                                )
                        }

                        else -> {
                            it.bostedsadresser = mutableListOf()
                        }
                    }
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør),
                        lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            barnSomSkalVurderesIFødselshendelse = listOf(barn.aktør.aktivFødselsnummer()),
        )

        // Assert
        val vilkårResultatSøker =
            vilkårsvurdering.personResultater
                .single { it.aktør == søker.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultatSøker.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatSøker.utdypendeVilkårsvurderinger).containsExactly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        assertThat(vilkårResultatSøker.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Norsk bostedsadresse i minst 6 måneder.")
        assertThat(vilkårResultatSøker.begrunnelseForManuellKontroll).isNull()

        val vilkårResultatBarn =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultatBarn.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultatSøker.utdypendeVilkårsvurderinger).containsExactly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        assertThat(vilkårResultatBarn.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Bosatt i Norge siden fødsel.")
        assertThat(vilkårResultatBarn.begrunnelseForManuellKontroll).isNull()
    }
}
