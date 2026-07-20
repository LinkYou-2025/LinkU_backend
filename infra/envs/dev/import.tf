# 기존에 배포되어 있는 인스턴스를 Terraform 관리 하에 두기 위한 import 블록.
# 아래 id를 실제 인스턴스 ID로 바꾼 뒤 `terraform plan`을 실행하면 import가 계획에 포함된다.
# import가 완료(첫 apply)된 후에는 이 파일을 삭제해도 된다.
import {
  to = module.app.aws_instance.this
  id = "i-0d972f8a8a1ab1d78" # linku-ec2
}
