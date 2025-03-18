# spring-traffic-flow-blog

## 주요 기능
- **목적**: 대용량 트래픽 운용+광고 노출+ 게시글 조회 운용 블로그
- **데이터베이스 테이블 생성**: 
- **도구 및 사욯한 기술**: 스프링부트 + 자바 + elastic - ELK + Redis + +RabbitMq + mysql + mongodb 
- **기능**: 광고 전환율 및 노출도를 분석하고 핵심 정보를 추출하는 기능을 스프링부트 어플리케이션에 제공

### 서비스 기술 기획서 및 구성 flow
https://www.notion.so/1a5f3ccd4fb581e8bbe6fdbbcb989075

![image](https://github.com/user-attachments/assets/b03d8d0d-3443-4c2e-9b0d-a70dc65ce356)

## 데이터 베이스 adarticle-db
### Article 테이블
- id (BIGINT, 자동 증가, 기본 키)
- title (VARCHAR, NOT NULL)
- content (TEXT, NOT NULL)
- author_id (BIGINT, 외래 키)
- board_id (BIGINT, 외래 키, JSON 직렬화 제외)
- isDeleted (BOOLEAN, NOT NULL, 기본값: false)
- createdDate (DATETIME, 자동 설정)
- updatedDate (DATETIME, 자동 업데이트)

### Board 테이블
- id (BIGINT, 자동 증가, 기본 키)
- title (VARCHAR, NOT NULL)
- description (VARCHAR, NOT NULL)
- createdDate (DATETIME, 자동 설정)
- updatedDate (DATETIME, 자동 업데이트)

### Comment 테이블
- id (BIGINT, 자동 증가, 기본 키)
- content (TEXT, NOT NULL)
- author_id (BIGINT, 외래 키)
- article_id (BIGINT, 외래 키, JSON 직렬화 제외)
- isDeleted (BOOLEAN, NOT NULL, 기본값: false)
- createdDate (DATETIME, 자동 설정)
- updatedDate (DATETIME, 자동 업데이트)

### User 테이블
- id (BIGINT, 자동 증가, 기본 키)
- username (VARCHAR, NOT NULL)
- password (VARCHAR, NOT NULL, JSON 직렬화 제외)
- email (VARCHAR, NOT NULL, 이메일 형식 유효성 검사, JSON 직렬화 제외)
- lastLogin (DATETIME, 선택적)
- deviceList (JSON, 기본값: 빈 리스트)
- createdDate (DATETIME, 자동 설정)
- updatedDate (DATETIME, 자동 업데이트)

### JwtBlacklist 테이블
- id (BIGINT, 자동 증가, 기본 키)
- token (VARCHAR, NOT NULL, 고유)
- expirationTime (DATETIME, NOT NULL)
- username (VARCHAR, NOT NULL)

### Device 클래스 (JSON 형태로 저장)
- deviceName (VARCHAR)
- token (VARCHAR)
