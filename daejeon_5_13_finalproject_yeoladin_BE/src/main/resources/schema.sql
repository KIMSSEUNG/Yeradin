CREATE DATABASE IF NOT EXISTS yeradinDB
  DEFAULT CHARACTER SET utf8mb4 -- 한글, 이모지, 유니코드까지 저장 가능한 문자셋
  DEFAULT COLLATE utf8mb4_general_ci; -- 대소문자 구분 없이 정렬하는 일반적인 Collation 방식
USE `yeradinDB` ;

-- -----------------------------------------------------
-- Table board, image
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Table sidos, gugun , contenttypes , attractions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sidos(
  `no` int NOT NULL AUTO_INCREMENT COMMENT '시도번호',
  `sido_code` int NOT NULL COMMENT '시도코드',
  `sido_name` varchar(20) DEFAULT NULL COMMENT '시도이름',
  PRIMARY KEY (`no`),
  UNIQUE INDEX `sido_code_UNIQUE` (`sido_code` ASC) VISIBLE
)
ENGINE = InnoDB
AUTO_INCREMENT = 18
COLLATE = utf8mb4_0900_ai_ci
COMMENT='시도정보테이블';

CREATE TABLE IF NOT EXISTS guguns (
  `no` int NOT NULL AUTO_INCREMENT COMMENT '구군번호',
  `sido_code` int NOT NULL COMMENT '시도코드',
  `gugun_code` int NOT NULL COMMENT '구군코드',
  `gugun_name` varchar(20) DEFAULT NULL COMMENT '구군이름',
  PRIMARY KEY (`no`),
  INDEX `guguns_sido_to_sidos_cdoe_fk_idx` (`sido_code` ASC) VISIBLE,
  INDEX `gugun_code_idx` (`gugun_code` ASC) VISIBLE,
  CONSTRAINT `guguns_sido_to_sidos_cdoe_fk`
    FOREIGN KEY (`sido_code`)
    REFERENCES `sidos` (`sido_code`)
)
ENGINE = InnoDB
AUTO_INCREMENT = 235
COLLATE = utf8mb4_0900_ai_ci
COMMENT='구군정보테이블';

DROP TABLE IF EXISTS contenttypes ;

CREATE TABLE IF NOT EXISTS contenttypes (
  `content_type_id` int NOT NULL COMMENT '콘텐츠타입번호',
  `content_type_name` varchar(45) DEFAULT NULL COMMENT '콘텐츠타입이름',
  PRIMARY KEY (`content_type_id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT='콘텐츠타입정보테이블';

CREATE TABLE IF NOT EXISTS attractions (
  `no` int NOT NULL AUTO_INCREMENT  COMMENT '명소코드',
  `content_id` int DEFAULT NULL COMMENT '콘텐츠번호',
  `title` varchar(500) DEFAULT NULL COMMENT '명소이름',
  `content_type_id` int DEFAULT NULL COMMENT '콘텐츠타입',
  `area_code` int DEFAULT NULL COMMENT '시도코드',
  `si_gun_gu_code` int DEFAULT NULL COMMENT '구군코드',
  `first_image1` varchar(100) DEFAULT NULL COMMENT '이미지경로1',
  `first_image2` varchar(100) DEFAULT NULL COMMENT '이미지경로2',
  `map_level` int DEFAULT NULL COMMENT '줌레벨',
  `latitude` decimal(20,17) DEFAULT NULL COMMENT '위도',
  `longitude` decimal(20,17) DEFAULT NULL COMMENT '경도',
  `tel` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `addr1` varchar(100) DEFAULT NULL COMMENT '주소1',
  `addr2` varchar(100) DEFAULT NULL COMMENT '주소2',
  `homepage` varchar(1000) DEFAULT NULL COMMENT '홈페이지',
  `overview` varchar(10000) DEFAULT NULL COMMENT '설명',
  PRIMARY KEY (`no`),
  INDEX `attractions_typeid_to_types_typeid_fk_idx` (`content_type_id` ASC) VISIBLE,
  INDEX `attractions_sido_to_sidos_code_fk_idx` (`area_code` ASC) VISIBLE,
  INDEX `attractions_sigungu_to_guguns_gugun_fk_idx` (`si_gun_gu_code` ASC) VISIBLE,
  CONSTRAINT `attractions_area_to_sidos_code_fk`
    FOREIGN KEY (`area_code`)
    REFERENCES `sidos` (`sido_code`),
  CONSTRAINT `attractions_sigungu_to_guguns_gugun_fk`
    FOREIGN KEY (`si_gun_gu_code`)
    REFERENCES `guguns` (`gugun_code`),
  CONSTRAINT `attractions_typeid_to_types_typeid_fk`
    FOREIGN KEY (`content_type_id`)
    REFERENCES `contenttypes` (`content_type_id`)
)
ENGINE = InnoDB
AUTO_INCREMENT = 56644
COLLATE = utf8mb4_0900_ai_ci
COMMENT='명소정보테이블';

-- -----------------------------------------------------
-- Table member
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    pw VARCHAR(255),           -- 일반 유저만 사용
    provider VARCHAR(20) NOT NULL,   -- 'LOCAL', 'GOOGLE', 'KAKAO'
    provider_id VARCHAR(100),        -- OAuth 유저는 여기에 고유 ID 저장
    role VARCHAR(20) DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------
-- JWT refresh token
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,                 -- Member 테이블의 ID (외래 키)
    token_value VARCHAR(500) NOT NULL UNIQUE,  -- Refresh Token
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,             -- 만료 시간
    CONSTRAINT fk_member_refresh_token FOREIGN KEY (member_id)
      REFERENCES member(id) ON DELETE CASCADE
);

-- -----------------------------------------------------
-- Table shortform
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS shortform (
    pk INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(200) NOT NULL,
    content TEXT,
    views INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- favorite INT DEFAULT 0, -- 별도 조인 테이블로 관리
    videofile VARCHAR(255) NOT NULL
);

-- -----------------------------------------------------
-- shortform_to_contenttype (M:N)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS shortform_to_contenttype (
    shortform_pk INT NOT NULL,
    content_type_id INT NOT NULL,
    PRIMARY KEY (shortform_pk, content_type_id),
    FOREIGN KEY (shortform_pk) REFERENCES shortform(pk) ON DELETE CASCADE,
    FOREIGN KEY (content_type_id) REFERENCES contenttypes(content_type_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------
-- shortform favorite
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS member_favorite_shortform (
    member_id BIGINT NOT NULL,
    shortform_pk INT NOT NULL,
    favorited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, shortform_pk),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (shortform_pk) REFERENCES shortform(pk) ON DELETE CASCADE
);

-- -----------------------------------------------------
-- member preferred contenttype
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS member_preferred_contenttype (
    member_id BIGINT NOT NULL,
    content_type_id INT NOT NULL,
    selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, content_type_id),
    FOREIGN KEY (member_id) REFERENCES member(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (content_type_id) REFERENCES contenttypes(content_type_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- -----------------------------------------------------
-- board, images, comment
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS board (
  id               CHAR(36)      NOT NULL,  -- UUID
  title            VARCHAR(255)  NOT NULL,
  content          TEXT          NOT NULL,
  member_id        BIGINT  		 NOT NULL,
  img_forder_path  VARCHAR(500),
  thumbnail_url    VARCHAR(500),            -- 대표 이미지 (NULL 허용)
  content_priview  VARCHAR(30),
  created_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_board_member FOREIGN KEY (member_id)
    REFERENCES member(id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS images (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  original_name   VARCHAR(255)   NOT NULL,
  stored_name     VARCHAR(255)   NOT NULL,
  board_id        CHAR(36)       NOT NULL,
  created_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_image_board FOREIGN KEY (board_id)
    REFERENCES board(id) ON DELETE CASCADE
) ENGINE = InnoDB;

DROP TABLE IF EXISTS comment ;

CREATE TABLE IF NOT EXISTS comment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id BIGINT NOT NULL,
  board_id CHAR(36) NOT NULL,
  content TEXT NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comment_member FOREIGN KEY (member_id)
    REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_board FOREIGN KEY (board_id)
    REFERENCES board(id) ON DELETE CASCADE
);

-- 더미 로그인 데이터
INSERT INTO member (name, email, pw, provider, role, created_at)
VALUES ("1", "1", "$2a$10$b2zk3jxqY5u7tHgNS8IHuOhhUfWfAEEgJ34MC7PiPac4jm3SSEm76", "LOCAL", "ROLE_USER", CURRENT_TIMESTAMP);
