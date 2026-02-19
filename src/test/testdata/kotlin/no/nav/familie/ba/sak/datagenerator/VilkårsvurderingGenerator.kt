package no.nav.familie.ba.sak.datagenerator

import io.mockk.mockk
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import java.time.LocalDate
import java.time.YearMonth
import kotlin.collections.set

fun lagPersonResultaterForSøkerOgToBarn(
    vilkårsvurdering: Vilkårsvurdering,
    søkerAktør: Aktør,
    barn1Aktør: Aktør,
    barn2Aktør: Aktør,
    stønadFom: LocalDate,
    stønadTom: LocalDate,
    erDeltBosted: Boolean = false,
): Set<PersonResultat> =
    setOf(
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.SØKER, aktør = søkerAktør),
            resultat = OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person =
                lagPerson(
                    type = PersonType.BARN,
                    aktør = barn1Aktør,
                    fødselsdato = stønadFom,
                ),
            resultat = OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.BARN, aktør = barn2Aktør, fødselsdato = stønadFom),
            resultat = OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
    )

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    person: Person,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = BOSATT_I_RIKET,
    erDeltBosted: Boolean = false,
    erDeltBostedSkalIkkeDeles: Boolean = false,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): PersonResultat {
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør,
        )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår
                .hentVilkårFor(
                    personType = personType,
                    fagsakType = FagsakType.NORMAL,
                    behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                ).map {
                    VilkårResultat(
                        personResultat = personResultat,
                        periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else periodeFom,
                        periodeTom = periodeTom,
                        vilkårType = it,
                        resultat = resultat,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger =
                            listOfNotNull(
                                when {
                                    erDeltBosted && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED
                                    erDeltBostedSkalIkkeDeles && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
                                    else -> null
                                },
                            ),
                        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                    )
                }.toSet(),
        )
    } else {
        personResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = vilkårType,
                    resultat = resultat,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                ),
            ),
        )
    }
    return personResultat
}

// Setter alle vilkår til oppfylt.
fun vurderVilkårsvurderingTilInnvilget(
    vilkårsvurdering: Vilkårsvurdering,
    barn: Person,
    innvilgetFom: LocalDate? = null,
) {
    vilkårsvurdering.personResultater.filter { it.aktør == barn.aktør }.forEach { personResultat ->
        personResultat.vilkårResultater.forEach {
            if (it.vilkårType == UNDER_18_ÅR) {
                it.resultat = OPPFYLT
                it.periodeFom = barn.fødselsdato
                it.periodeTom = barn.fødselsdato.plusYears(18)
            } else {
                it.resultat = OPPFYLT
                it.periodeFom = innvilgetFom ?: it.periodeFom ?: barn.fødselsdato
            }
        }
    }
}

fun lagVilkårsvurdering(
    id: Long = 0L,
    behandling: Behandling = lagBehandling(),
    aktiv: Boolean = true,
    lagPersonResultater: (vilkårsvurdering: Vilkårsvurdering) -> Set<PersonResultat> = {
        setOf(
            lagPersonResultat(
                vilkårsvurdering = it,
                aktør = behandling.fagsak.aktør,
            ),
        )
    },
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            id = id,
            behandling = behandling,
            aktiv = aktiv,
        )
    vilkårsvurdering.personResultater = lagPersonResultater(vilkårsvurdering)
    return vilkårsvurdering
}

fun lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
    behandling: Behandling,
    person: Person,
    perioderMedUtdypendeVilkårsvurdering: List<Pair<LocalDate, LocalDate?>>,
    vilkårsvurdering: Vilkårsvurdering,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
): PersonResultat =
    lagPersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = person.aktør,
        lagVilkårResultater = { personResultat ->
            setOfNotNull(
                *perioderMedUtdypendeVilkårsvurdering
                    .map {
                        lagVilkårResultat(
                            personResultat = personResultat,
                            vilkårType = BOSATT_I_RIKET,
                            resultat = OPPFYLT,
                            periodeFom = it.first,
                            periodeTom = it.second,
                            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
                            behandlingId = behandling.id,
                        )
                    }.toTypedArray(),
                if (person.type == PersonType.BARN) {
                    lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = UNDER_18_ÅR,
                        resultat = OPPFYLT,
                        periodeFom = person.fødselsdato,
                        periodeTom = person.fødselsdato.plusYears(18),
                        behandlingId = behandling.id,
                    )
                } else {
                    null
                },
            )
        },
    )

fun lagPersonResultat(
    id: Long = 0L,
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør = randomAktør(),
    lagVilkårResultater: (personResultat: PersonResultat) -> Set<VilkårResultat> = {
        setOf(
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = BOSATT_I_RIKET,
                resultat = OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
        )
    },
    lagAnnenVurderinger: (personResultat: PersonResultat) -> Set<AnnenVurdering> = {
        setOf(
            lagAnnenVurdering(
                personResultat = it,
            ),
        )
    },
): PersonResultat {
    val personResultat =
        PersonResultat(
            id = id,
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør,
        )
    personResultat.setSortedVilkårResultater(lagVilkårResultater(personResultat))
    personResultat.andreVurderinger.addAll(lagAnnenVurderinger(personResultat))
    return personResultat
}

fun lagAnnenVurdering(
    id: Long = 0L,
    personResultat: PersonResultat,
    resultat: Resultat = OPPFYLT,
    type: AnnenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
    begrunnelse: String? = null,
): AnnenVurdering =
    AnnenVurdering(
        id = id,
        personResultat = personResultat,
        resultat = resultat,
        type = type,
        begrunnelse = begrunnelse,
    )

fun lagVilkårsvurdering(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2),
    medAndreVurderinger: Boolean = true,
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
    personResultat.setSortedVilkårResultater(
        setOf(
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = BOSATT_I_RIKET,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
        ),
    )
    if (medAndreVurderinger) {
        personResultat.andreVurderinger.add(
            AnnenVurdering(
                personResultat = personResultat,
                resultat = resultat,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = null,
            ),
        )
    }

    vilkårsvurdering.personResultater = setOf(personResultat)
    return vilkårsvurdering
}

fun lagVilkårResultat(
    vilkår: Vilkår,
    vilkårRegelverk: Regelverk? = null,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    behandlingId: Long = 0,
) = VilkårResultat(
    personResultat = null,
    vilkårType = vilkår,
    resultat = OPPFYLT,
    periodeFom = fom?.toLocalDate(),
    periodeTom = tom?.toLocalDate(),
    begrunnelse = "",
    sistEndretIBehandlingId = behandlingId,
    vurderesEtter = vilkårRegelverk,
)

fun lagVilkårResultat(
    id: Long = 0L,
    personResultat: PersonResultat? = null,
    vilkårType: Vilkår = BOSATT_I_RIKET,
    resultat: Resultat = OPPFYLT,
    periodeFom: LocalDate? = LocalDate.of(2009, 12, 24),
    periodeTom: LocalDate? = LocalDate.of(2010, 1, 31),
    begrunnelse: String = "",
    behandlingId: Long = lagBehandling().id,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    erEksplisittAvslagPåSøknad: Boolean = false,
    standardbegrunnelser: List<IVedtakBegrunnelse> = emptyList(),
    vurderesEtter: Regelverk? = null,
    erPreutfylt: Boolean = false,
) = VilkårResultat(
    id = id,
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    sistEndretIBehandlingId = behandlingId,
    utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
    standardbegrunnelser = standardbegrunnelser,
    vurderesEtter = vurderesEtter,
    erOpprinneligPreutfylt = erPreutfylt,
)

fun oppfyltVilkår(
    vilkår: Vilkår,
    regelverk: Regelverk? = null,
) = VilkårRegelverkResultat(
    vilkår = vilkår,
    regelverkResultat =
        when (regelverk) {
            Regelverk.NASJONALE_REGLER -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
            Regelverk.EØS_FORORDNINGEN -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
            else -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
        },
)

fun ikkeOppfyltVilkår(vilkår: Vilkår) =
    VilkårRegelverkResultat(
        vilkår = vilkår,
        regelverkResultat = RegelverkResultat.IKKE_OPPFYLT,
    )

fun lagPersonResultatAvOverstyrteResultater(
    person: Person,
    overstyrendeVilkårResultater: List<VilkårResultat>,
    vilkårsvurdering: Vilkårsvurdering,
    id: Long = 0,
): PersonResultat {
    val personResultat =
        PersonResultat(
            id = id,
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør,
        )

    val erUtvidet = overstyrendeVilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

    val vilkårResultater =
        Vilkår
            .hentVilkårFor(
                personType = person.type,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = if (erUtvidet) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR,
            ).foldIndexed(mutableListOf<VilkårResultat>()) { index, acc, vilkårType ->
                val overstyrteVilkårResultaterForVilkår: List<VilkårResultat> =
                    overstyrendeVilkårResultater
                        .filter { it.vilkårType == vilkårType }
                if (overstyrteVilkårResultaterForVilkår.isNotEmpty()) {
                    acc.addAll(overstyrteVilkårResultaterForVilkår)
                } else {
                    acc.add(
                        VilkårResultat(
                            id = if (id != 0L) index + 1L else 0L,
                            personResultat = personResultat,
                            periodeFom =
                                if (vilkårType == UNDER_18_ÅR) {
                                    person.fødselsdato
                                } else {
                                    maxOf(
                                        person.fødselsdato,
                                        LocalDate.now().minusYears(3),
                                    )
                                },
                            periodeTom = if (vilkårType == UNDER_18_ÅR) person.fødselsdato.plusYears(18) else null,
                            vilkårType = vilkårType,
                            resultat = OPPFYLT,
                            begrunnelse = "",
                            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    )
                }
                acc
            }.toSet()

    personResultat.setSortedVilkårResultater(vilkårResultater)

    return personResultat
}
typealias AktørId = String

/**
 * Setter vilkår som ikke er overstyrte til oppfylt fra det seneste av
 *      fødselsdato eller tre år tilbake i tid for personen som vurderes.
 * Dersom personen er et barn settes også tom-datoen til barnets attenårsdag.
 * Om du vil ha med utvidet vilkår og delt bosted må det sendes med uansett.
 **/
fun lagVilkårsvurderingMedOverstyrendeResultater(
    søker: Person,
    barna: List<Person>,
    behandling: Behandling? = null,
    id: Long = 0,
    overstyrendeVilkårResultater: Map<AktørId, List<VilkårResultat>>,
): Vilkårsvurdering {
    val vilkårsvurdering = Vilkårsvurdering(behandling = behandling ?: mockk(relaxed = true), id = id)

    val søkerPersonResultater =
        lagPersonResultatAvOverstyrteResultater(
            person = søker,
            overstyrendeVilkårResultater = overstyrendeVilkårResultater[søker.aktør.aktørId] ?: emptyList(),
            vilkårsvurdering = vilkårsvurdering,
            id = id,
        )

    val barnaPersonResultater =
        barna.map {
            lagPersonResultatAvOverstyrteResultater(
                person = it,
                overstyrendeVilkårResultater = overstyrendeVilkårResultater[it.aktør.aktørId] ?: emptyList(),
                vilkårsvurdering = vilkårsvurdering,
            )
        }

    vilkårsvurdering.personResultater = barnaPersonResultater.toSet() + søkerPersonResultater
    return vilkårsvurdering
}

fun lagVilkårsvurderingFraScenarioDto(
    scenario: ScenarioDto,
    overstyrendeVilkårResultater: Map<AktørId, List<VilkårResultat>>,
): Vilkårsvurdering {
    fun ScenarioPersonDto.lagAktør() =
        Aktør(
            this.aktørId,
            mutableSetOf(Personident(this.ident, mockk(relaxed = true))),
        )

    val søker =
        lagPerson(
            aktør = scenario.søker.lagAktør(),
            fødselsdato = LocalDate.parse(scenario.søker.fødselsdato),
            type = PersonType.SØKER,
        )
    val barna =
        scenario.barna.map {
            lagPerson(
                aktør = it.lagAktør(),
                fødselsdato = LocalDate.parse(it.fødselsdato),
                type = PersonType.BARN,
            )
        }
    return lagVilkårsvurderingMedOverstyrendeResultater(
        søker = søker,
        barna = barna,
        overstyrendeVilkårResultater = overstyrendeVilkårResultater,
    )
}

fun lagSøkerVilkårResultat(
    søkerPersonResultat: PersonResultat,
    periodeFom: LocalDate,
    periodeTom: LocalDate? = null,
    behandlingId: Long,
): Set<VilkårResultat> =
    setOf(
        lagVilkårResultat(
            personResultat = søkerPersonResultat,
            vilkårType = BOSATT_I_RIKET,
            resultat = OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = søkerPersonResultat,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            behandlingId = behandlingId,
        ),
    )

fun lagBarnVilkårResultat(
    barnPersonResultat: PersonResultat,
    barnetsFødselsdato: LocalDate,
    behandlingId: Long,
    periodeFom: LocalDate,
    flytteSak: Boolean = false,
): Set<VilkårResultat> =
    setOf(
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = UNDER_18_ÅR,
            resultat = OPPFYLT,
            periodeFom = barnetsFødselsdato,
            periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.GIFT_PARTNERSKAP,
            resultat = OPPFYLT,
            periodeFom = barnetsFødselsdato,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = BOSATT_I_RIKET,
            resultat = OPPFYLT,
            periodeFom = if (flytteSak) barnetsFødselsdato else periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = OPPFYLT,
            periodeFom = if (flytteSak) barnetsFødselsdato else periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
    )
