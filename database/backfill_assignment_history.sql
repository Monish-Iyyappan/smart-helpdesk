INSERT INTO ticket_assignment_history (ticket_id, from_agent, to_agent, changed_by, reason, changed_at)
SELECT t.ticket_id, NULL, t.assigned_to,
       (SELECT MIN(user_id) FROM users WHERE role = 'ADMIN'),
       'Initial assignment (demo data)', t.created_at
FROM tickets t
WHERE t.assigned_to IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ticket_assignment_history h WHERE h.ticket_id = t.ticket_id);
