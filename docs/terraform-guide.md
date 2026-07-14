# Terraform 사용 가이드

LinkU 인프라(EC2, RDS, EIP, 보안그룹)는 `infra/` 디렉토리의 Terraform 코드로 관리합니다.
이 문서는 팀원 각자가 로컬에서 Terraform을 사용하기 위한 가이드입니다.

## 현재 인프라 현황 (2026-07 기준, 모두 Terraform 관리 중)

| | dev | prod |
|---|---|---|
| EC2 | `linku-ec2` / t3.small | `linku-prod` / t3.small |
| 고정 IP (EIP) | `43.203.60.237` | `54.116.121.179` |
| 도메인 | `dev.linkudeveloper.org` | (지정 예정) |
| DB | EC2 내 postgres 컨테이너 | **RDS PostgreSQL 16** (`linkUDB` / `linkU`) |
| Redis | EC2 내 redis 컨테이너 | EC2 내 redis 컨테이너 |
| 열린 포트 | 22, 80, 443 (nginx → 8080 프록시) | 22, 80, 443 (nginx → 8080 프록시) |
| compose 파일 | `docker-compose.remote.yml` | `docker-compose.prod.yml` (RDS 연결) |

- AWS 계정: `236058984744` — provider에 `allowed_account_ids`로 고정되어 있어 다른 계정에는 apply 자체가 거부됩니다.
- RDS 엔드포인트 등 상세 값은 각 환경 디렉토리에서 `terraform output`으로 확인.

## 디렉토리 구조

```
infra/
├── modules/          # 재사용 모듈 (직접 실행하지 않음)
│   ├── ec2/          # EC2 인스턴스 + 보안그룹 + EIP
│   └── rds/          # RDS PostgreSQL + 보안그룹 + 서브넷그룹
└── envs/             # 실제 실행하는 환경 디렉토리
    ├── dev/          # 기존 서버를 import해서 관리 중
    └── prod/         # Terraform으로 신규 생성 (EC2 + RDS)
```

Terraform 명령은 항상 `infra/envs/dev` 또는 `infra/envs/prod` 안에서 실행합니다.

## 최초 1회 세팅

### 1. 도구 설치

```bash
brew install terraform awscli
```

### 2. AWS 자격증명 등록

**액세스 키는 노션에 있는 값을 참고해주세요.**
프로파일 이름은 반드시 `linku`여야 합니다 (provider 설정에 `profile = "linku"`로 고정되어 있음).

```bash
aws configure --profile linku
# AWS Access Key ID     → 노션 참고
# AWS Secret Access Key → 노션 참고
# Default region name   → ap-northeast-2
# Default output format → json
```

키는 `~/.aws/credentials`에 저장되며 git에 올라가지 않습니다.
**액세스 키를 코드, tfvars, 커밋 메시지 등 저장소 어디에도 넣지 마세요.**

연결 확인:

```bash
aws sts get-caller-identity --profile linku   # Account가 236058984744면 정상
```

### 3. terraform.tfvars 작성

실제 값이 들어가는 `terraform.tfvars`는 gitignore 대상이라 저장소에 없습니다.
각 환경 디렉토리의 `terraform.tfvars.example`을 복사한 뒤, **노션에 정리된 값**으로 채워주세요.
(RDS 비밀번호 `db_password` 포함 — 노션 참고)

```bash
cd infra/envs/dev   # 또는 prod
cp terraform.tfvars.example terraform.tfvars
# 값 채우기 (노션 참고)
```

### 4. 초기화

```bash
terraform init
```

## 평소 작업 흐름

```bash
cd infra/envs/dev   # 또는 prod

terraform plan      # 무엇이 바뀌는지 미리 확인 (읽기 전용, 안전)
terraform apply     # 실제 반영 (yes 입력 필요)
terraform output    # 서버 IP, RDS 엔드포인트 등 확인
```

### plan 읽는 법

| 표시 | 의미 | 주의 |
|---|---|---|
| `+ create` | 새 리소스 생성 | |
| `~ update in-place` | 기존 리소스 수정 | |
| `- destroy` | 리소스 삭제 | ⚠️ 의도한 게 맞는지 확인 |
| `-/+ destroy and then create replacement` | **삭제 후 재생성** | ⚠️⚠️ IP·데이터가 날아갈 수 있음. 확신 없으면 apply 금지 |

### 자주 하는 변경 예시

- **인스턴스 타입 변경**: `terraform.tfvars`의 `instance_type` 수정 → apply
  (EC2가 중지→변경→시작되므로 몇 분 다운타임 발생, EIP 덕분에 IP는 유지됨)
- **포트 열기/닫기**: `envs/<env>/main.tf`의 `ingress_ports` 수정 → apply (다운타임 없음)
- **RDS 비밀번호 변경**: tfvars의 `db_password` 수정 → apply
  (서버의 `.env`에 있는 `DB_PASSWORD`도 같이 바꿔야 앱 연결이 유지됨)

## 규칙

- **apply 전에 plan을 반드시 읽습니다.** 특히 `destroy`, `replace`가 보이면 멈추고 팀에 공유하세요.
- `terraform.tfvars`, `*.tfstate`, `.terraform/`은 절대 커밋하지 않습니다 (gitignore로 막혀 있음).
- 인프라 변경은 콘솔에서 직접 하지 않고 Terraform 코드 수정 → PR → apply 순서로 합니다.
  콘솔에서 직접 바꾸면 코드와 실제 상태가 어긋나서(drift) 다음 apply 때 되돌아갑니다.
- `terraform destroy`는 환경 전체를 삭제하는 명령입니다. 실행 전 반드시 팀 합의를 거치세요.
  (prod RDS는 `skip_final_snapshot = true`라 destroy 시 데이터가 스냅샷 없이 사라집니다)

## 서버 SSH 접속

접속용 키(`linku-ec2-key.pem`)는 **노션을 참고**해서 받은 뒤:

```bash
chmod 400 ~/.ssh/linku-ec2-key.pem
ssh -i ~/.ssh/linku-ec2-key.pem ubuntu@43.203.60.237    # dev
ssh -i ~/.ssh/linku-ec2-key.pem ubuntu@54.116.121.179   # prod
```

참고: AWS에 등록된 키페어 이름은 `linku-ec2-key-20260525`입니다 (prod tfvars의 `key_name`).
dev 인스턴스에는 과거 이름(`linku-ec2-key`)이 박혀 있는데, 이미 삭제된 키페어라서
dev를 재생성할 일이 생기면 `key_name`을 `linku-ec2-key-20260525`로 바꿔야 합니다.

## 도메인 / nginx / HTTPS

- 각 서버는 nginx가 80/443으로 받아 내부 8080(Spring 컨테이너)으로 프록시합니다. 8080은 외부에 열려 있지 않습니다.
- 인증서는 서버별로 Let's Encrypt(certbot) 발급, 자동 갱신됩니다.
- **새 도메인을 붙이는 순서 (순서 중요!)**:
  1. DNS에 A 레코드 등록 (해당 서버 EIP로)
  2. nginx에 해당 도메인의 프록시 server 블록 작성 → `sudo nginx -t && sudo systemctl reload nginx`
  3. `sudo certbot --nginx -d <도메인>` (redirect 선택)
  - ⚠️ server 블록 없이 certbot부터 돌리면 인증서만 붙고 프록시가 없는 404 블록이 생깁니다.

## RDS (prod)

- prod 앱은 컨테이너 DB가 아닌 RDS를 사용합니다 (`docker-compose.prod.yml`).
- 접속 정보: DB명 `linkUDB`, 사용자 `linkU`, 비밀번호는 노션 참고.
- 엔드포인트는 `infra/envs/prod`에서 `terraform output rds_endpoint` / `app_db_url`로 확인.
- RDS는 외부 접근 불가(`publicly_accessible = false`)이며, prod EC2 보안그룹에서만 5432 접근이 허용됩니다.
  로컬에서 DB에 붙어야 하면 prod EC2를 통한 SSH 터널을 사용하세요:
  ```bash
  ssh -i ~/.ssh/linku-ec2-key.pem -L 5433:<rds-endpoint-host>:5432 ubuntu@54.116.121.179
  # 이후 localhost:5433으로 접속
  ```

## 상태(state) 관련 주의

Terraform은 "지금까지 만든 리소스 목록"을 state 파일로 관리합니다.

- 현재는 로컬 state를 사용 중이며, **S3 원격 state로 전환 예정**입니다.
  전환 후에는 팀원 모두가 같은 state를 공유하므로 별도 파일 전달이 필요 없습니다.
- 전환 전까지는 **여러 명이 동시에 apply하면 안 됩니다.** apply 전 팀 채널에 알려주세요.
- state 파일에는 DB 비밀번호가 평문으로 들어 있습니다. 외부에 공유 금지.

## 자주 겪는 문제

| 증상 | 원인/해결 |
|---|---|
| `Error: No valid credential sources` | `aws configure --profile linku` 미실행. 위 세팅 2번 참고 |
| `AWS account ID not allowed` | 다른 계정 자격증명으로 연결됨. `--profile linku` 확인 |
| plan에 예상 밖의 replace가 뜸 | tfvars 값이 실제 리소스와 다름. apply 하지 말고 팀에 공유 |
| `Backend initialization required` | 코드 업데이트 후 `terraform init` 재실행 |
| HTTPS는 되는데 404만 나옴 | nginx server 블록에 프록시 설정 없음. "도메인 / nginx" 섹션 참고 |
