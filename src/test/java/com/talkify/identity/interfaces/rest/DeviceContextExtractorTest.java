package com.talkify.identity.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.DevicePlatform;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceContextExtractor — device info extraction from HTTP request")
class DeviceContextExtractorTest {

    // DeviceContextExtractor is a simple @Component with no constructor deps
    private final DeviceContextExtractor extractor = new DeviceContextExtractor();

    @Mock
    private HttpServletRequest request;

    // Real-world User-Agent strings
    private static final String UA_CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final String UA_EDGE_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0";

    private static final String UA_OPERA_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 OPR/115.0.0.0";

    private static final String UA_FIREFOX_LINUX =
            "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0";

    private static final String UA_SAFARI_MACOS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/17.5 Safari/605.1.15";

    private static final String UA_CHROME_ANDROID =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    private static final String UA_SAFARI_IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1";

    private static final String UA_CHROME_IOS =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) CriOS/131.0.6778.154 Mobile/15E148 Safari/604.1";

    private static final String UA_CHROME_MACOS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    // ═════════════════════════════════════════════════════════════════════════
    //  1. IP Extraction
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IP extraction")
    class IpExtraction {

        @BeforeEach
        void stubUa() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_WINDOWS);
        }

        @Test
        @DisplayName("should take first IP from X-Forwarded-For when present")
        void usesFirstIpFromXForwardedFor() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 172.16.0.5");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.ipAddress()).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should fall back to X-Real-IP when X-Forwarded-For is absent")
        void usesXRealIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.42");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.ipAddress()).isEqualTo("203.0.113.42");
        }

        @Test
        @DisplayName("should fall back to remoteAddr when no proxy headers present")
        void usesRemoteAddr() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.ipAddress()).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("should ignore blank X-Forwarded-For and use X-Real-IP")
        void skipsBlankXForwardedFor() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
            when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.ipAddress()).isEqualTo("10.0.0.1");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  2. Platform Detection
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Platform detection")
    class PlatformDetection {

        @BeforeEach
        void stubIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("should detect ANDROID from Android User-Agent")
        void detectsAndroid() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_ANDROID);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.platform()).isEqualTo(DevicePlatform.ANDROID);
        }

        @Test
        @DisplayName("should detect IOS from iPhone User-Agent")
        void detectsIos() {
            when(request.getHeader("User-Agent")).thenReturn(UA_SAFARI_IPHONE);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.platform()).isEqualTo(DevicePlatform.IOS);
        }

        @Test
        @DisplayName("should detect WEB from desktop User-Agent")
        void detectsWeb() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_WINDOWS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.platform()).isEqualTo(DevicePlatform.WEB);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  3. Browser Detection (ordering — Edge/Opera before Chrome/Safari)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Browser detection")
    class BrowserDetection {

        @BeforeEach
        void stubIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("should detect Edge (contains both Chrome/ and Edg/ — Edge must win)")
        void detectsEdge() {
            when(request.getHeader("User-Agent")).thenReturn(UA_EDGE_WINDOWS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).startsWith("Edge");
        }

        @Test
        @DisplayName("should detect Opera (contains both Chrome/ and OPR/ — Opera must win)")
        void detectsOpera() {
            when(request.getHeader("User-Agent")).thenReturn(UA_OPERA_WINDOWS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).startsWith("Opera");
        }

        @Test
        @DisplayName("should detect Chrome on Windows")
        void detectsChromeWindows() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_WINDOWS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Chrome on Windows");
        }

        @Test
        @DisplayName("should detect Chrome on macOS")
        void detectsChromeOnMacos() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_MACOS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Chrome on macOS");
        }

        @Test
        @DisplayName("should detect CriOS (Chrome on iOS) as Chrome with IOS platform")
        void detectsCriosAsChrome() {
            when(request.getHeader("User-Agent")).thenReturn(UA_CHROME_IOS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Chrome on iOS");
            assertThat(info.platform()).isEqualTo(DevicePlatform.IOS);
        }

        @Test
        @DisplayName("should detect Firefox on Linux")
        void detectsFirefoxLinux() {
            when(request.getHeader("User-Agent")).thenReturn(UA_FIREFOX_LINUX);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Firefox on Linux");
        }

        @Test
        @DisplayName("should detect Safari on macOS (no Chrome/ token)")
        void detectsSafariMacos() {
            when(request.getHeader("User-Agent")).thenReturn(UA_SAFARI_MACOS);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Safari on macOS");
        }

        @Test
        @DisplayName("should detect Safari on iPhone")
        void detectsSafariIphone() {
            when(request.getHeader("User-Agent")).thenReturn(UA_SAFARI_IPHONE);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Safari on iOS");
            assertThat(info.platform()).isEqualTo(DevicePlatform.IOS);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  4. Edge Cases
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @BeforeEach
        void stubIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("should return 'Unknown Device' when User-Agent is empty string")
        void handlesEmptyUserAgent() {
            when(request.getHeader("User-Agent")).thenReturn("");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Unknown Device");
            assertThat(info.platform()).isEqualTo(DevicePlatform.WEB);
        }

        @Test
        @DisplayName("should return 'Unknown Device' when User-Agent header is missing (null)")
        void handlesMissingUserAgent() {
            when(request.getHeader("User-Agent")).thenReturn(null);

            DeviceInfo info = extractor.extract(request);

            assertThat(info.deviceName()).isEqualTo("Unknown Device");
            assertThat(info.platform()).isEqualTo(DevicePlatform.WEB);
        }

        @Test
        @DisplayName("should always set non-null platform even for unknown UA")
        void platformNeverNull() {
            when(request.getHeader("User-Agent")).thenReturn("SomeUnknownAgent/1.0");

            DeviceInfo info = extractor.extract(request);

            assertThat(info.platform()).isNotNull();
        }
    }
}
