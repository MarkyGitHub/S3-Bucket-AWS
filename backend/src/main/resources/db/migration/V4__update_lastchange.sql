WITH selected_auftraege AS (
    SELECT auftragid
    FROM (
        SELECT
            auftragid,
            kundeid,
            ROW_NUMBER() OVER (PARTITION BY kundeid ORDER BY lastchange DESC) AS rn
        FROM auftraege
    ) ranked
    WHERE rn <= 2
),
current_ts AS (
    SELECT CURRENT_TIMESTAMP AS ts
)
UPDATE auftraege a
SET lastchange = ct.ts
FROM selected_auftraege sa
CROSS JOIN current_ts ct
WHERE a.auftragid = sa.auftragid;


