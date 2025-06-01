CREATE DATABASE IF NOT EXISTS yeradinDB
  DEFAULT CHARACTER SET utf8mb4 -- 한글, 이모지, 유니코드까지 저장 가능한 문자셋
  DEFAULT COLLATE utf8mb4_general_ci; -- 대소문자 구분 없이 정렬하는 일반적인 Collation 방식
USE `yeradinDB` ;

-- -----------------------------------------------------
-- Table board, image
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS board (
  id               CHAR(36)      NOT NULL,  -- UUID
  title            VARCHAR(255)  NOT NULL,
  content          TEXT          NOT NULL,
  author           VARCHAR(100)  NOT NULL,
  img_forder_path  VARCHAR(500),
  thumbnail_url   VARCHAR(500),           -- ✅ 대표 이미지 이름 (NULL 허용)
  content_priview  VARCHAR(30),
  created_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS images (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  original_name   VARCHAR(255)   NOT NULL,        -- 사용자가 업로드한 원본 파일명
  stored_name     VARCHAR(255)   NOT NULL,        -- 실제 저장된 파일명 (UUID 충돌 방지용)
  board_id        CHAR(36)       NOT NULL,        -- 게시글 참조용 외래키
  created_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_image_board FOREIGN KEY (board_id) REFERENCES board(id) ON DELETE CASCADE -- 참조된 게시글 삭제시 관련 이미지 자동 삭제
) ENGINE = InnoDB;


-----
-- Table sidos, gugun , contenttypes , attractions
-----
CREATE TABLE IF NOT EXISTS sidos(
  `no` int NOT NULL AUTO_INCREMENT  comment '시도번호',
  `sido_code` int NOT NULL comment '시도코드',
  `sido_name` varchar(20) DEFAULT NULL comment '시도이름',
  PRIMARY KEY (`no`),
  UNIQUE INDEX `sido_code_UNIQUE` (`sido_code` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 18
COLLATE = utf8mb4_0900_ai_ci comment '시도정보테이블';


CREATE TABLE IF NOT EXISTS guguns (
  `no` int NOT NULL AUTO_INCREMENT comment '구군번호',
  `sido_code` int NOT NULL comment '시도코드',
  `gugun_code` int NOT NULL comment '구군코드',
  `gugun_name` varchar(20) DEFAULT NULL comment '구군이름',
  PRIMARY KEY (`no`),
  INDEX `guguns_sido_to_sidos_cdoe_fk_idx` (`sido_code` ASC) VISIBLE,
  INDEX `gugun_code_idx` (`gugun_code` ASC) VISIBLE,
  CONSTRAINT `guguns_sido_to_sidos_cdoe_fk`
    FOREIGN KEY (`sido_code`)
    REFERENCES `sidos` (`sido_code`))
ENGINE = InnoDB
AUTO_INCREMENT = 235
COLLATE = utf8mb4_0900_ai_ci comment '구군정보테이블';

DROP TABLE IF EXISTS contenttypes ;

CREATE TABLE IF NOT EXISTS contenttypes (
  `content_type_id` int NOT NULL comment '콘텐츠타입번호',
  `content_type_name` varchar(45) DEFAULT NULL comment '콘텐츠타입이름',
  PRIMARY KEY (`content_type_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci comment '콘텐츠타입정보테이블';


CREATE TABLE IF NOT EXISTS attractions (
  `no` int NOT NULL AUTO_INCREMENT  comment '명소코드',
  `content_id` int DEFAULT NULL comment '콘텐츠번호',
  `title` varchar(500) DEFAULT NULL comment '명소이름',
  `content_type_id` int DEFAULT NULL comment '콘텐츠타입',
  `area_code` int DEFAULT NULL comment '시도코드',
  `si_gun_gu_code` int DEFAULT NULL comment '구군코드',
  `first_image1` varchar(100) DEFAULT NULL comment '이미지경로1',
  `first_image2` varchar(100) DEFAULT NULL comment '이미지경로2',
  `map_level` int DEFAULT NULL comment '줌레벨',
  `latitude` decimal(20,17) DEFAULT NULL comment '위도',
  `longitude` decimal(20,17) DEFAULT NULL comment '경도',
  `tel` varchar(20) DEFAULT NULL comment '전화번호',
  `addr1` varchar(100) DEFAULT NULL comment '주소1',
  `addr2` varchar(100) DEFAULT NULL comment '주소2',
  `homepage` varchar(1000) DEFAULT NULL comment '홈페이지',
  `overview` varchar(10000) DEFAULT NULL comment '설명',
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
    REFERENCES `contenttypes` (`content_type_id`))
ENGINE = InnoDB
AUTO_INCREMENT = 56644
COLLATE = utf8mb4_0900_ai_ci
comment '명소정보테이블';


-----
-- Table member
-----



CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    pw VARCHAR(255),           -- 일반 유저만 사용
    provider VARCHAR(20) NOT NULL,   -- 'LOCAL', 'GOOGLE', 'KAKAO'
    provider_id VARCHAR(100),        -- OAuth 유저는 여기에 고유 ID 저장
    role VARCHAR(20) DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-----
-- JWT refresh token
-----

CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,          -- Member 테이블의 ID (외래 키)
    token_value VARCHAR(500) NOT NULL UNIQUE, -- Refresh Token 값 (길이는 JWT 길이에 맞게 조정)
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,          -- Refresh Token 만료 시간
    CONSTRAINT fk_member_refresh_token FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);


-----
-- Table shortform
-----
CREATE TABLE IF NOT EXISTS shortform (
	pk INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(200) NOT NULL,
    content TEXT,
    views INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    date DATE NOT NULL,
    -- favorite INT DEFAULT 0, -- 이전에 논의된 대로, member_favorite_shortform 테이블로 관리
    videofile VARCHAR(255) NOT NULL
);

-- -----------------------------------------------------
-- Table shortform_to_contenttype (숏폼-콘텐츠타입 연결 테이블 - 신규)
-- shortform과 contenttypes 간의 다대다 관계를 위한 중간 테이블
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS shortform_to_contenttype (
    shortform_pk INT NOT NULL,                      -- shortform 테이블의 pk 참조
    content_type_id INT NOT NULL,                   -- contenttypes 테이블의 content_type_id 참조
    PRIMARY KEY (shortform_pk, content_type_id),    -- 복합 기본 키
    FOREIGN KEY (shortform_pk) REFERENCES shortform(pk) ON DELETE CASCADE,
    FOREIGN KEY (content_type_id) REFERENCES contenttypes(content_type_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-----
-- Table shortform favorite
-----

CREATE TABLE IF NOT EXISTS member_favorite_shortform (
    member_id BIGINT NOT NULL,         -- member 테이블의 id를 참조하는 외래 키
    shortform_pk INT NOT NULL,      -- shortform 테이블의 pk를 참조하는 외래 키
    favorited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 언제 좋아요를 눌렀는지 (선택 사항)

    PRIMARY KEY (member_id, shortform_pk), -- 한 멤버는 한 게시물에 대해 한 번만 좋아요를 누를 수 있도록 복합 기본 키 설정
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE, -- 멤버가 삭제되면 관련 좋아요 기록도 삭제
    FOREIGN KEY (shortform_pk) REFERENCES shortform(pk) ON DELETE CASCADE -- 게시물이 삭제되면 관련 좋아요 기록도 삭제
);



-----
-- Table member per shortform category
-----
CREATE TABLE IF NOT EXISTS member_preferred_contenttype (
    member_id BIGINT NOT NULL,
    content_type_id INT NOT NULL,          -- category_id 에서 변경됨
    selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, content_type_id), -- 복합 기본 키 업데이트
    FOREIGN KEY (member_id) REFERENCES member(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (content_type_id) REFERENCES contenttypes(content_type_id) -- contenttypes 테이블 참조
        ON DELETE CASCADE -- 콘텐츠 타입 삭제 시 관련 선호도 정보 삭제 (또는 비즈니스 로직에 따라 ON DELETE RESTRICT 등)
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- 더미 로그인 파일
INSERT INTO member (name, email, pw, provider, role, created_at) VALUES ("1", "1", "$2a$10$b2zk3jxqY5u7tHgNS8IHuOhhUfWfAEEgJ34MC7PiPac4jm3SSEm76", "LOCAL", "ROLE_USER", CURRENT_TIMESTAMP)

-- mysql dump cmd 등록
-- mysql -u [사용자명] -p [데이터베이스명] < SSAFYTRIP_Dump.sql

