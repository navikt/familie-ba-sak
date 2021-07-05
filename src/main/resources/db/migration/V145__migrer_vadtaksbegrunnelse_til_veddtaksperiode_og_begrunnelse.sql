INSERT INTO vedtaksperiode (id, fk_vedtak_id, fom, tom, type)
    (SELECT NEXTVAL('vedtaksperiode_seq'), fk_vedtak_id, fom, tom,
        CASE split_part(begrunnelse,'_', 1)
        WHEN 'INNVILGET' THEN 'UTBETALING'
        WHEN 'REDUKSJON' THEN 'UTBETALING'
        WHEN 'AVSLAG' THEN 'AVSLAG'
        WHEN 'OPPHØR' THEN 'OPPHØR'
        WHEN 'FORTSATT_INNVILGET' THEN 'FORTSATT_INNVILGET'
        END AS type
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtak v
    ON vb.fk_vedtak_id = v.id
    INNER JOIN behandling b
    ON v.fk_behandling_id = b.id
    WHERE NOT EXISTS(
        SELECT id
        FROM vedtaksperiode v
        WHERE v.fk_vedtak_id = vb.fk_vedtak_id AND
            v.fom = vb.fom AND
            v.tom = vb.tom AND
            v.type = v.type)
        AND b.status != 'FATTER_VEDTAK'
        AND b.resultat != 'FORTSATT_INNVILGET'
    GROUP BY fk_vedtak_id, fom, tom, type)

INSERT INTO vedtaksbegrunnelse(id, fk_vedtaksperiode_id, fom, tom, vedtak_begrunnelse_spesifikasjon, oppprette_av, opprettet_tid, endret_av, endret_tid)
    SELECT nextval('vedtaksbegrunnelse_seq'), vp.id, fom, tom, begrunnelse, oppprette_av, opprettet_tid, endret_av, endret_tid
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtaksperiode vp
        ON vb.fk_vedtak_id = vp.fk_vedtak_id AND
           vb.fom = vp.fom AND
           vb.tom = vp.tom AND
           split_part(vb.begrunnelse,'_', 1)  = vp.type
    WHERE vb.begrunnelse NOT LIKE '%FRITEKST%'

INSERT INTO vedtaksbegrunnelse_fritekst(id, fk_vedtaksperiode_id, fritekst, oppprette_av, opprettet_tid, endret_av, endret_tid)
    SELECT nextval('vedtaksbegrunnelse_fritekst_seq'), vp.id, brev_begrunnelse, oppprette_av, opprettet_tid, endret_av, endret_tid
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtaksperiode vp
        ON vb.fk_vedtak_id = vp.fk_vedtak_id AND
           vb.fom = vp.fom AND
           vb.tom = vp.tom AND
           split_part(vb.begrunnelse,'_', 1)  = vp.type
    WHERE vb.begrunnelse LIKE '%FRITEKST%'