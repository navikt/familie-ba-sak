ALTER TABLE VEDTAK RENAME stonad_brev_markdown TO stonad_brev_html;
ALTER TABLE VEDTAK ADD COLUMN stonad_brev_pdf bytea;
ALTER TABLE VEDTAK ADD COLUMN ansvarlig_enhet varchar;