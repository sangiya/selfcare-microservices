-- Local dev only. Each service owns its own database (Doc 1 sec 4.2) -- this mirrors that in
-- one shared MySQL container purely to keep docker-compose small; a real environment runs
-- separate RDS instances/schemas per service.
CREATE DATABASE IF NOT EXISTS loyalty_service CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS reports_service CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS notification_service CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'loyalty_service'@'%' IDENTIFIED BY 'localdev';
CREATE USER IF NOT EXISTS 'reports_service'@'%' IDENTIFIED BY 'localdev';
CREATE USER IF NOT EXISTS 'notification_service'@'%' IDENTIFIED BY 'localdev';

GRANT ALL PRIVILEGES ON loyalty_service.* TO 'loyalty_service'@'%';
GRANT ALL PRIVILEGES ON reports_service.* TO 'reports_service'@'%';
GRANT ALL PRIVILEGES ON notification_service.* TO 'notification_service'@'%';
FLUSH PRIVILEGES;
