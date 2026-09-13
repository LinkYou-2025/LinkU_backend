package com.umc.linkyou.infra.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SsrfGuard 테스트")
class SsrfGuardTest {

    private final SsrfGuard guard = new SsrfGuard();

    @Nested
    @DisplayName("스킴 검증")
    class Scheme {

        @Test
        @DisplayName("http, https는 허용된다")
        void http_https는_허용된다() {
            assertThatCode(() -> guard.validate("http://8.8.8.8/")).doesNotThrowAnyException();
            assertThatCode(() -> guard.validate("https://8.8.8.8/")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("file, ftp 스킴은 차단된다")
        void file_ftp_스킴은_차단된다() {
            assertThatThrownBy(() -> guard.validate("file:///etc/passwd"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("ftp://8.8.8.8/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }
    }

    @Nested
    @DisplayName("포트 검증")
    class Port {

        @Test
        @DisplayName("포트를 생략하거나 80/443이면 허용된다")
        void 기본_포트는_허용된다() {
            assertThatCode(() -> guard.validate("http://8.8.8.8/")).doesNotThrowAnyException();
            assertThatCode(() -> guard.validate("http://8.8.8.8:80/")).doesNotThrowAnyException();
            assertThatCode(() -> guard.validate("https://8.8.8.8:443/")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("80/443이 아닌 포트는 차단된다")
        void 비표준_포트는_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://8.8.8.8:8080/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://8.8.8.8:6379/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }
    }

    @Nested
    @DisplayName("목적지 IP 검증")
    class Destination {

        @Test
        @DisplayName("공인 IP는 허용된다")
        void 공인_IP는_허용된다() {
            assertThatCode(() -> guard.validate("http://8.8.8.8/")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("루프백 주소는 차단된다")
        void 루프백_주소는_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://127.0.0.1/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://[::1]/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }

        @Test
        @DisplayName("링크로컬 주소(AWS 메타데이터 엔드포인트 포함)는 차단된다")
        void 링크로컬_주소는_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://169.254.169.254/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }

        @Test
        @DisplayName("RFC1918 사설 대역은 차단된다")
        void RFC1918_사설대역은_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://10.0.0.5/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://172.16.0.1/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://192.168.1.1/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }

        @Test
        @DisplayName("IPv6 Unique Local Address(fc00::/7)는 차단된다")
        void IPv6_ULA는_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://[fc00::1]/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://[fd12:3456::1]/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }

        @Test
        @DisplayName("멀티캐스트/와일드카드 주소는 차단된다")
        void 멀티캐스트_와일드카드_주소는_차단된다() {
            assertThatThrownBy(() -> guard.validate("http://224.0.0.1/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
            assertThatThrownBy(() -> guard.validate("http://0.0.0.0/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }
    }

    @Nested
    @DisplayName("테스트 전용 loopback 허용 옵션")
    class LoopbackAllowedForTesting {

        private final SsrfGuard loopbackAllowedGuard = new SsrfGuard(true);

        @Test
        @DisplayName("allowLoopback이 true면 루프백 주소도 통과한다")
        void allowLoopback이_true면_루프백도_통과한다() {
            assertThatCode(() -> loopbackAllowedGuard.validate("http://127.0.0.1:80/"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("allowLoopback이 true여도 RFC1918 사설 대역은 여전히 차단된다")
        void allowLoopback이_true여도_사설대역은_차단된다() {
            assertThatThrownBy(() -> loopbackAllowedGuard.validate("http://10.0.0.5/"))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }
    }
}
