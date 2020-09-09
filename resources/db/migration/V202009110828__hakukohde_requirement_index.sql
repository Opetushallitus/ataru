CREATE INDEX application_hakukohde_reviews_hakukohde_requirement_idx
    ON public.application_hakukohde_reviews
        USING btree (hakukohde, requirement);
