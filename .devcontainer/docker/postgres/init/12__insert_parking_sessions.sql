-- DML: 駐車セッションテーブル初期データ挿入
INSERT INTO parking_sessions (id, space_number, entry_time, exit_time, fee, status) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '01', CURRENT_TIMESTAMP - INTERVAL '02 HOURS 00 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '02', CURRENT_TIMESTAMP - INTERVAL '04 HOURS 15 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '05', CURRENT_TIMESTAMP - INTERVAL '06 HOURS 30 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '09', CURRENT_TIMESTAMP - INTERVAL '07 HOURS 11 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '11', CURRENT_TIMESTAMP - INTERVAL '05 HOURS 22 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '12', CURRENT_TIMESTAMP - INTERVAL '03 HOURS 33 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '16', CURRENT_TIMESTAMP - INTERVAL '00 HOURS 01 MINUTES', NULL, NULL, 'IN_PROGRESS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '18', CURRENT_TIMESTAMP - INTERVAL '25 HOURS 45 MINUTES', NULL, NULL, 'IN_PROGRESS');
    
