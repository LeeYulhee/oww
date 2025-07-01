DROP TABLE IF EXISTS `MEMBER_GOAL_HISTORY`;
DROP TABLE IF EXISTS `GROUP_WORKOUT_LOGS`;
DROP TABLE IF EXISTS `GROUP_MEMBERS`;
DROP TABLE IF EXISTS `NOTIFICATION_SETTINGS`;
DROP TABLE IF EXISTS `WORKOUT_RECORDS`;
DROP TABLE IF EXISTS `EMAIL_VERIFICATIONS`;
DROP TABLE IF EXISTS `GROUPS`;
DROP TABLE IF EXISTS `USERS`;

CREATE TABLE `USERS` (
	`user_id`	        VARCHAR(36)	 NOT NULL    PRIMARY KEY,
	`user_login_id`	    VARCHAR(50)	 NOT NULL    UNIQUE 	            COMMENT '로그인용 ID',
	`email`	            VARCHAR(255) NOT NULL    UNIQUE 	            COMMENT '이메일',
	`password`	        VARCHAR(255) NOT NULL	                        COMMENT '암호화된 비밀번호(필요하면 추후에 비밀번호 만료일자도 별도 테이블로 추가)',
	`user_status`	    VARCHAR(20)	 NOT NULL	 DEFAULT 'NOT_VERIFIED'	COMMENT '계정 활성화 상태(ACTIVE, NOT_VERIFIED)',
	`email_verified_at`	DATETIME	 NULL	                            COMMENT '이메일 인증 완료 시간',
	`is_deleted`	    BOOLEAN	     NOT NULL    DEFAULT FALSE,
    `deleted_at`	    DATETIME	 NULL,
	`created_at`	    DATETIME	 NOT NULL,
	`updated_at`	    DATETIME	 NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_login_id (user_login_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='사용자 정보';

CREATE TABLE `GROUPS` (
	`group_id`       VARCHAR(36)    NOT NULL    PRIMARY KEY,
	`group_name`     VARCHAR(100)   NOT NULL                                 COMMENT '그룹명',
	`description`    TEXT           NULL                                     COMMENT '그룹 설명',
	`invite_code`    VARCHAR(20)    NOT NULL                                 COMMENT '초대 코드',
	`max_members`    INT            NULL                                     COMMENT '최대 인원수 (NULL이면 제한 없음)',
	`created_at`     DATETIME       NOT NULL,
	`updated_at`     DATETIME       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`is_deleted`     BOOLEAN        NOT NULL    DEFAULT FALSE,
    `deleted_at`	 DATETIME	    NULL,
	`is_active`	     BOOLEAN        NOT NULL    DEFAULT TRUE                 COMMENT '그룹 활성화 상태'
) ENGINE=InnoDB COMMENT='그룹 정보';

CREATE TABLE `EMAIL_VERIFICATIONS` (
	`verification_id`       VARCHAR(36)     NOT NULL    PRIMARY KEY,
	`verification_token`    VARCHAR(255)    NOT NULL	                    COMMENT '인증 토큰',
	`verification_type`     VARCHAR(20)     NOT NULL    DEFAULT 'SIGNUP'    COMMENT '인증 유형 : SIGNUP, PASSWORD_RESET, EMAIL_CHANGE',
	`email`	                VARCHAR(255)    NOT NULL                        COMMENT '인증할 이메일',
	`expires_at`	        DATETIME        NOT NULL	                    COMMENT '만료 시간',
	`verified_at`	        DATETIME        NULL                            COMMENT '인증 완료 시간',
	`created_at`	        DATETIME        NOT NULL,
	`updated_at`      DATETIME    NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`user_id`	            VARCHAR(36)     NOT NULL,
	FOREIGN KEY (user_id) REFERENCES USERS(user_id)
) ENGINE=InnoDB COMMENT='이메일 인증 (회원가입, 비밀번호 재설정 등)';


CREATE TABLE `WORKOUT_RECORDS` (
	`record_id`           VARCHAR(36)	  NOT NULL    PRIMARY KEY,
	`workout_date`        DATE	          NOT NULL                                 COMMENT '운동 날짜',
	`workout_memo`        TEXT	          NULL	                                   COMMENT '운동 메모',
	`image_url`           VARCHAR(500)    NULL	                                   COMMENT '운동 인증 사진 URL',
	`duration_minutes`    INT             NULL                                     COMMENT '운동 시간 (분)',
	`created_at`          DATETIME        NOT NULL,
	`updated_at`          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`user_id`             VARCHAR(36)     NOT NULL,
	FOREIGN KEY (user_id) REFERENCES USERS(user_id),
	INDEX idx_workout_date (workout_date)
) ENGINE=InnoDB COMMENT='운동 기록';

CREATE TABLE `NOTIFICATION_SETTINGS` (
	`setting_id`              VARCHAR(36)    NOT NULL    PRIMARY KEY,
	`workout_reminder`        BOOLEAN        NOT NULL    DEFAULT FALSE                COMMENT '운동 리마인더(아직 없는 기능)',
	`group_member_workout`    BOOLEAN        NOT NULL    DEFAULT TRUE                 COMMENT '그룹 멤버 운동 완료 알림',
	`weekly_report`           BOOLEAN        NOT NULL    DEFAULT FALSE                COMMENT '주간 리포트(아직 없는 기능)',
	`created_at`              DATETIME       NOT NULL,
	`updated_at`              DATETIME	     NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`group_id`                VARCHAR(36)    NOT NULL                                 COMMENT '그룹별 알림 설정',
	`user_id`                 VARCHAR(36)    NOT NULL,
	FOREIGN KEY (user_id) REFERENCES USERS(user_id),
	FOREIGN KEY (group_id) REFERENCES GROUPS(group_id)
) ENGINE=InnoDB COMMENT='알림 설정';

CREATE TABLE `GROUP_MEMBERS` (
	`member_id`       VARCHAR(36)    NOT NULL    PRIMARY KEY,
	`member_color`    VARCHAR(7)     NOT NULL                                  COMMENT '그룹 내 고유 색상 (#FF0000)',
	`nickname`        VARCHAR(50)    NOT NULL                                  COMMENT '그룹 내 닉네임',
	`role`            VARCHAR(10)    NOT NULL    DEFAULT 'MEMBER'              COMMENT '그룹 내 역할(MEMBER, OWNER, ADMIN)',
	`created_at`      DATETIME       NOT NULL	                               COMMENT '그룹 가입 일자',
	`updated_at`      DATETIME       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`is_deleted`      BOOLEAN        NOT NULL    DEFAULT FALSE                 COMMENT '그룹 탈퇴 여부(탈퇴 시, 운동 기록 관리 필요)',
    `deleted_at`	  DATETIME	     NULL,
	`group_id`        VARCHAR(36)    NOT NULL,
	`user_id`         VARCHAR(36)    NOT NULL,
	FOREIGN KEY (group_id) REFERENCES GROUPS(group_id),
    FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='그룹 구성원';


CREATE TABLE `GROUP_WORKOUT_LOGS` (
	`log_id`          VARCHAR(36)        NOT NULL    PRIMARY KEY,
	`record_id`       VARCHAR(36)        NOT NULL,
	`workout_date`    DATE               NOT NULL                    COMMENT '운동 날짜',
	`created_at`      DATETIME           NOT NULL,
	`updated_at`      DATETIME           NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`group_id`        VARCHAR(36)        NOT NULL,
	`user_id`         VARCHAR(36)        NOT NULL,
	FOREIGN KEY (group_id) REFERENCES GROUPS(group_id),
    FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    FOREIGN KEY (record_id) REFERENCES WORKOUT_RECORDS(record_id),
    INDEX idx_workout_date (workout_date)
) ENGINE=InnoDB COMMENT='그룹별 운동 로그';

CREATE TABLE `MEMBER_GOAL_HISTORY` (
	`goal_id`         VARCHAR(36) NOT NULL    PRIMARY KEY,
	`workout_days`    JSON        NOT NULL                    COMMENT '목표 요일 설정 월, 수, 금',
	`start_date`      DATE        NOT NULL                    COMMENT '목표 시작일',
	`end_date`        DATE        NULL                        COMMENT '목표 종료일 (NULL이면 현재 진행중)',
	`created_at`      DATETIME    NOT NULL,
	`updated_at`      DATETIME    NOT NULL    DEFAULT CURRENT_TIMESTAMP,
	`member_id`       VARCHAR(36) NOT NULL,
	FOREIGN KEY (member_id) REFERENCES GROUP_MEMBERS(member_id)
) ENGINE=InnoDB COMMENT='그룹원 개인 목표 이력';