ALTER TABLE locations ADD COLUMN IF NOT EXISTS location_time_zone_mode VARCHAR(16);

UPDATE locations SET location_time_zone = CASE location_time_zone
    WHEN 'Eastern Time (GMT-5)' THEN 'America/New_York'
    WHEN 'Central Time (GMT-6)' THEN 'America/Chicago'
    WHEN 'Mountain Time (GMT-7)' THEN 'America/Denver'
    WHEN 'Pacific Time (GMT-8)' THEN 'America/Los_Angeles'
    WHEN 'Alaska Time (GMT-9)' THEN 'America/Anchorage'
    WHEN 'Hawaii-Aleutian Time (GMT-10)' THEN 'Pacific/Honolulu'
    ELSE location_time_zone END;

UPDATE locations SET location_time_zone_mode = 'AUTO' WHERE location_time_zone_mode IS NULL;
ALTER TABLE locations ALTER COLUMN location_time_zone_mode SET DEFAULT 'AUTO';
ALTER TABLE locations ALTER COLUMN location_time_zone_mode SET NOT NULL;
