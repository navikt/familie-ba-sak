package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.mai
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EndringIVilkårsvurderingUtilTest {
    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring dersom vilkårresultatene er helt like`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val vilkårResultater =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = MIN_MÅNED,
                ).tilPerioder()
                .filter { it.verdi == true }

        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har vært endringer i regelverk`() {
        val fødselsdato = 1.jan(2022)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = 31.mai(2022),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = 31.mai(2022),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = MIN_MÅNED,
                ).tilPerioder()
                .filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(1.feb(2022), perioderMedEndring.single().fom)
        assertEquals(31.mai(2022), perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke bry seg om endringer utført før relevant fom dato`() {
        val fødselsdato = 1.jan(2022)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = 31.mai(2022),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = 31.mai(2022),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = jun(2022),
                ).tilPerioder()
                .filter { it.verdi == true }

        assertEquals(0, perioderMedEndring.size)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har oppstått splitt i vilkårsvurderingen`() {
        val fødselsdato = 1.jan(2022)
        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = 7.mai(2022),
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = 8.mai(2022),
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = MIN_MÅNED,
                ).tilPerioder()
                .filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(1.jun(2022), perioderMedEndring.single().fom)
        assertNull(perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis det kun er opphørt`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = MIN_MÅNED,
                ).tilPerioder()
                .filter { it.verdi == true }

        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis eneste endring er å sette obligatoriske utdypende vilkårsvurderinger`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "migrering",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "migrering",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                    personIBehandling = person,
                    personIForrigeBehandling = person,
                    tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = MIN_MÅNED,
                ).tilPerioder()
                .filter { it.verdi == true }

        assertTrue(perioderMedEndring.isEmpty())
    }

    private fun lagPersonResultatFraVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
        aktør: Aktør,
    ): PersonResultat {
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = lagBehandling(), resultat = Resultat.OPPFYLT, søkerAktør = randomAktør())
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }
}
