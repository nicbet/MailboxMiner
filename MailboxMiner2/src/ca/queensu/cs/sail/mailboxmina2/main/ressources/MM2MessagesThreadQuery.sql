SELECT DISTINCT ON (msg_id) * 
FROM messages
LEFT JOIN (
    SELECT msg_id, (CASE WHEN header_value is null THEN '' ELSE header_value END) AS messageid
    FROM headers
    WHERE lower(header_key) = 'message-id'
    GROUP BY msg_id, header_value
) AS data1 USING(msg_id)
LEFT JOIN (
    SELECT msg_id, (CASE WHEN header_value is null THEN '' ELSE header_value END) AS inreply
    FROM headers
    WHERE lower(header_key) = 'in-reply-to'
    GROUP BY msg_id, header_value
) AS data2 USING(msg_id)
LEFT JOIN (
    SELECT msg_id, (CASE WHEN header_value is null THEN '' ELSE header_value END) AS references
    FROM headers
    WHERE lower(header_key) = 'references'
    GROUP BY msg_id, header_value
) AS data3 USING(msg_id)
