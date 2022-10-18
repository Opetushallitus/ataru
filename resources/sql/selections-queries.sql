--name: yesql-get-selections-query
-- All valid selections with selection group id
SELECT q.question_id AS "question-id",
       q.answer_id AS "answer-id",
       count(*) AS n FROM
((SELECT question_id, answer_id
 FROM initial_selections
 WHERE selection_group_id IN (:selection_group_ids)
   AND valid @> now()
   AND selection_id IS DISTINCT FROM :selection_id)
UNION ALL
(SELECT question_id, answer_id
 FROM selections
 WHERE selection_group_id IN (:selection_group_ids))) AS q
GROUP BY q.question_id, q.answer_id;

--name: yesql-get-selections-for-answer-query
-- All valid selections with selection group id
SELECT count(*) AS n FROM
((SELECT question_id, answer_id
 FROM initial_selections
 WHERE selection_group_id = :selection_group_id
   AND question_id = :question_id
   AND answer_id = :answer_id
   AND valid @> now())
UNION ALL
(SELECT question_id, answer_id
 FROM selections
 WHERE selection_group_id = :selection_group_id
   AND question_id = :question_id
   AND answer_id = :answer_id)) AS q;

-- name: yesql-remove-existing-initial-selection!
-- Remove existing initial selection
UPDATE initial_selections
SET valid = tstzrange(lower(valid), now(), '[)')
WHERE selection_id = :selection_id
  AND selection_group_id = :selection_group_id
  AND question_id = :question_id;

-- name: yesql-remove-existing-selection!
-- Remove existing selection
DELETE FROM selections
WHERE application_key = :application_key
  AND selection_group_id = :selection_group_id
  AND question_id = :question_id;

-- name: yesql-new-initial-selection!
-- Create new selection
INSERT INTO initial_selections (selection_id, selection_group_id, question_id, answer_id, valid)
VALUES (:selection_id, :selection_group_id, :question_id, :answer_id, tstzrange(now(), now() + INTERVAL '4 hour', '[)'))
ON CONFLICT (selection_group_id, selection_id, question_id)
DO
 UPDATE
   SET answer_id = EXCLUDED.answer_id,
       valid = tstzrange(now(), now() + INTERVAL '4 hour', '[)')
   WHERE EXCLUDED.selection_id = :selection_id
   AND EXCLUDED.selection_group_id = :selection_group_id
   AND EXCLUDED.question_id = :question_id;

-- name: yesql-new-selection!
-- Create new selection
INSERT INTO selections (selection_group_id, question_id, answer_id, application_key)
VALUES (:selection_group_id, :question_id, :answer_id, :application_key)
ON CONFLICT (application_key, selection_group_id, question_id)
DO
 UPDATE
   SET answer_id = EXCLUDED.answer_id
   WHERE EXCLUDED.application_key = :application_key
   AND EXCLUDED.selection_group_id = :selection_group_id
   AND EXCLUDED.question_id = :question_id;

--name: yesql-has-permanent-selection
SELECT count(*) as n
 FROM selections
 WHERE selection_group_id = :selection_group_id
   AND application_key = :application_key
   AND question_id = :question_id
   AND answer_id = :answer_id;
