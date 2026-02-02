-- Predata MSA 데이터베이스 초기화

-- 서비스별 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS predata_member;
CREATE DATABASE IF NOT EXISTS predata_question;
CREATE DATABASE IF NOT EXISTS predata_betting;
CREATE DATABASE IF NOT EXISTS predata_settlement;
CREATE DATABASE IF NOT EXISTS predata_data;
CREATE DATABASE IF NOT EXISTS predata_sports;
CREATE DATABASE IF NOT EXISTS predata_blockchain;

-- 권한 설정
GRANT ALL PRIVILEGES ON predata_member.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_question.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_betting.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_settlement.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_data.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_sports.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON predata_blockchain.* TO 'root'@'%';

FLUSH PRIVILEGES;
