UPDATE vedtak_begrunnelse
SET begrunnelse='OPPHØR_UTVANDRET'
WHERE begrunnelse = 'OPPHØR_BARN_UTVANDRET';
UPDATE vedtak_begrunnelse
SET begrunnelse='OPPHØR_UTVANDRET'
WHERE begrunnelse = 'OPPHØR_SØKER_UTVANDRET';
UPDATE vedtak_begrunnelse
SET begrunnelse='OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE'
WHERE begrunnelse = 'OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE';
UPDATE vedtak_begrunnelse
SET begrunnelse='OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE'
WHERE begrunnelse = 'OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE';

UPDATE VEDTAKSBEGRUNNELSE
SET vedtak_begrunnelse_spesifikasjon='OPPHØR_UTVANDRET'
WHERE vedtak_begrunnelse_spesifikasjon = 'OPPHØR_BARN_UTVANDRET';
UPDATE VEDTAKSBEGRUNNELSE
SET vedtak_begrunnelse_spesifikasjon='OPPHØR_UTVANDRET'
WHERE vedtak_begrunnelse_spesifikasjon = 'OPPHØR_SØKER_UTVANDRET';
UPDATE VEDTAKSBEGRUNNELSE
SET vedtak_begrunnelse_spesifikasjon='OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE'
WHERE vedtak_begrunnelse_spesifikasjon = 'OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE';
UPDATE VEDTAKSBEGRUNNELSE
SET vedtak_begrunnelse_spesifikasjon='OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE'
WHERE vedtak_begrunnelse_spesifikasjon = 'OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE';