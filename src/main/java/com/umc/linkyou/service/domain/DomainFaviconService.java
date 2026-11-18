package com.umc.linkyou.service.domain;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.RobotsTxtChecker;
import com.umc.linkyou.infra.parser.SiteMetaParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 외부 요청이 있어 호출자는 트랜잭션 밖에서 호출한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainFaviconService {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int HOME_FETCH_TIMEOUT_MS = 5000;
    private static final int ICON_FETCH_TIMEOUT_MS = 3000;
    private static final int MAX_ICON_CANDIDATES = 3;
    private static final int MAX_ICON_BYTES = 512 * 1024;
    private static final String ICON_FOLDER = "domain";
    // ICO, SVG는 앱 렌더링을 보장할 수 없어 받지 않는다.
    private static final Map<String, String> ICON_EXTENSIONS =
            Map.of(
                    "image/png", ".png",
                    "image/jpeg", ".jpg",
                    "image/webp", ".webp");

    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final AwsS3Service awsS3Service;

    public Optional<String> collect(Domain domain) {
        String homeUrl = "https://" + domain.getDomainTail();
        Document home = fetchHome(homeUrl);
        // 홈페이지를 못 받으면 관례 경로만 시도한다.
        List<String> candidates =
                home == null
                        ? List.of(homeUrl + "/apple-touch-icon.png")
                        : SiteMetaParser.iconCandidates(home, homeUrl);
        for (String iconUrl : candidates.stream().limit(MAX_ICON_CANDIDATES).toList()) {
            Optional<IconFile> icon = download(iconUrl);
            if (icon.isPresent()) {
                try {
                    return Optional.of(
                            awsS3Service.uploadBytes(
                                    icon.get().bytes(),
                                    icon.get().contentType(),
                                    ICON_FOLDER,
                                    icon.get().extension()));
                } catch (Exception e) {
                    log.warn("[파비콘 수집] S3 업로드 실패 {}: {}", iconUrl, e.getMessage());
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private Document fetchHome(String homeUrl) {
        try {
            if (!robotsTxtChecker.isAllowed(homeUrl, USER_AGENT)) {
                return null;
            }
            return safeUrlFetcher.fetchDocument(homeUrl, USER_AGENT, HOME_FETCH_TIMEOUT_MS);
        } catch (Exception e) {
            log.debug("[파비콘 수집] 홈페이지 조회 실패 {}: {}", homeUrl, e.getMessage());
            return null;
        }
    }

    private Optional<IconFile> download(String iconUrl) {
        HttpURLConnection conn = null;
        try {
            conn =
                    safeUrlFetcher.openConnection(
                            iconUrl, USER_AGENT, ICON_FETCH_TIMEOUT_MS, ICON_FETCH_TIMEOUT_MS);
            if (conn.getResponseCode() != 200) {
                return Optional.empty();
            }
            String contentType = mimeOf(conn.getContentType());
            String extension = ICON_EXTENSIONS.get(contentType);
            if (extension == null || conn.getContentLengthLong() > MAX_ICON_BYTES) {
                return Optional.empty();
            }
            // Content-Length가 없거나 틀릴 수 있어 한 바이트 더 읽어 크기를 다시 확인한다.
            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readNBytes(MAX_ICON_BYTES + 1);
            }
            if (bytes.length == 0 || bytes.length > MAX_ICON_BYTES) {
                return Optional.empty();
            }
            return Optional.of(new IconFile(bytes, contentType, extension));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String mimeOf(String contentType) {
        return contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private record IconFile(byte[] bytes, String contentType, String extension) {}
}
