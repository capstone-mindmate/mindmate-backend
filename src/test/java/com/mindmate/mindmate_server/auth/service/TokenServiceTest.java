package com.mindmate.mindmate_server.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.auth.dto.TokenData;
import com.mindmate.mindmate_server.auth.service.TokenService;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.global.exception.CustomException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("리프레시 토큰 저장 테스트")
    class SaveRefreshTokenTest {
        @Test
        @DisplayName("리프레시 토큰 저장 성공")
        void saveRefreshToken_Success() throws JsonProcessingException {
            // given
            Long userId = 1L;
            String refreshToken = "refresh-token";
            String tokenFamily = "token-family";
            String tokenDataJson = "token-data-json";

            lenient().when(objectMapper.writeValueAsString(any(TokenData.class))).thenReturn(tokenDataJson);

            // when
            tokenService.saveRefreshToken(userId, refreshToken, tokenFamily);

            // then
            verify(valueOperations).set(
                    eq("RT:" + userId),
                    eq(tokenDataJson),
                    eq(7L),
                    eq(TimeUnit.DAYS)
            );
        }

        @Test
        @DisplayName("JSON 처리 실패시 예외 발생")
        void saveRefreshToken_JsonProcessingException() throws JsonProcessingException {
            // given
            lenient().when(objectMapper.writeValueAsString(any(TokenData.class)))
                    .thenThrow(new JsonProcessingException("Error"){});

            // when & then
            assertThrows(CustomException.class, () -> tokenService.saveRefreshToken(1L, "token", "family"));
        }
    }
    @Nested
    @DisplayName("리프레시 토큰 조회 테스트")
    class GetRefreshTokenTest {
        @Test
        @DisplayName("리프레시 토큰 조회 성공")
        void getRefreshToken_Success() throws JsonProcessingException {
            // given
            Long userId = 1L;
            String tokenDataJson = "token-data-json";
            TokenData expectedTokenData = TokenData.of("token", "family");

            when(valueOperations.get("RT:" + userId)).thenReturn(tokenDataJson);
            when(objectMapper.readValue(eq(tokenDataJson), eq(TokenData.class))).thenReturn(expectedTokenData);

            // when
            TokenData result = tokenService.getRefreshToken(userId);

            // then
            assertNotNull(result);
            assertEquals(expectedTokenData, result);
        }

        @Test
        @DisplayName("저장된 토큰이 없을 경우 null 반환")
        void getRefreshToken_NotFound() {
            // given
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            TokenData result = tokenService.getRefreshToken(1L);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("저장된 토큰 조회 시 JSON 처리 실패")
        void getRefreshToken_JsonProcessingException() throws JsonProcessingException{
            // given
            Long userId = 1L;
            String tokenDataJson = "invalid-json";
            when(valueOperations.get("RT:" + userId)).thenReturn(tokenDataJson);
            when(objectMapper.readValue(eq(tokenDataJson), eq(TokenData.class)))
                    .thenThrow(new JsonProcessingException("Error"){});

            // when & then
            assertThrows(CustomException.class, () -> tokenService.getRefreshToken(userId));
        }
    }

    @Nested
    @DisplayName("블랙리스트 관련 테스트")
    class BlacklistTest {
        @Test
        @DisplayName("토큰 블랙리스트 추가 성공")
        void addToBlacklist_Success() {
            // given
            String token = "test-token";
            long expiration = 3600000L;

            when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(expiration);

            // when
            tokenService.addToBlackList(token);

            // then
            verify(valueOperations).set(
                    eq("BL:" + token),
                    eq("blacklisted"),
                    eq(expiration),
                    eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("토큰 블랙리스트 확인")
        void isTokenBlacklisted_Success() {
            // given
            String token = "test-token";
            when(redisTemplate.hasKey("BL:" + token)).thenReturn(true);

            // when
            boolean result = tokenService.isTokenBlacklisted(token);

            // then
            assertTrue(result);
            verify(redisTemplate).hasKey("BL:" + token);
        }
    }

    @Nested
    @DisplayName("토큰 무효화 테스트")
    class InvalidateTokenTest {
        @Test
        @DisplayName("리프레시 토큰 무효화 성공")
        void invalidateRefreshToken_Success() {
            // given
            Long userId = 1L;

            // when
            tokenService.invalidateRefreshToken(userId);

            // then
            verify(redisTemplate).delete("RT:" + userId);
        }
    }

    @Nested
    @DisplayName("토큰 마스킹 테스트")
    class MaskTokenTest {
        @Test
        @DisplayName("긴 토큰 마스킹 처리")
        void maskToken_LongToken() {
            // given
            String token = "abcdefghijklmnopqrstuvwxyz";
            String expectedMask = "abcde****************vwxyz";

            // when
            String result = ReflectionTestUtils.invokeMethod(tokenService, "maskToken", token);

            // then
            assertEquals(expectedMask, result);
        }

        @Test
        @DisplayName("짧은 토큰 마스킹 처리")
        void maskToken_ShortToken() {
            // given
            String token = "abcde";
            String expectedMask = "*****";

            // when
            String result = ReflectionTestUtils.invokeMethod(tokenService, "maskToken", token);

            // then
            assertEquals(expectedMask, result);
        }

    }

}
