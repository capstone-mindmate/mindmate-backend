package com.mindmate.mindmate_server.emoticon.controller;

import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.service.EmoticonPopularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "이모티콘 인기 랭킹",
        description = "이모티콘의 구매, 조회, 사용, 종합 인기 랭킹 및 인기 점수 수동 갱신 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/emoticons/popular")
public class EmoticonPopularityController {
    private final EmoticonPopularityService emoticonPopularityService;

    @Operation(
            summary = "많이 구매한 이모티콘 랭킹 조회",
            description = "최근 기간(주간 등) 동안 가장 많이 구매된 이모티콘 TOP N을 인기순으로 조회합니다."
    )
    @GetMapping("/purchased")
    public ResponseEntity<List<EmoticonResponse>> getMostPurchasedEmoticons(
            @RequestParam(defaultValue = "10") int limit) {
        List<EmoticonResponse> result = emoticonPopularityService.getMostPurchasedEmoticons(limit);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "많이 조회한 이모티콘 랭킹 조회",
            description = "최근 기간(일간 등) 동안 가장 많이 조회된 이모티콘 TOP N을 인기순으로 조회합니다."
    )
    @GetMapping("/viewed")
    public ResponseEntity<List<EmoticonResponse>> getMostViewedEmoticons(
            @RequestParam(defaultValue = "10") int limit) {
        List<EmoticonResponse> result = emoticonPopularityService.getMostViewedEmoticons(limit);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "많이 사용한 이모티콘 랭킹 조회",
            description = "최근 기간 동안 가장 많이 채팅에 사용된 이모티콘 TOP N을 인기순으로 조회합니다."
    )
    @GetMapping("/used")
    public ResponseEntity<List<EmoticonResponse>> getMostUsedEmoticons(
            @RequestParam(defaultValue = "10") int limit) {
        List<EmoticonResponse> result = emoticonPopularityService.getMostUsedEmoticons(limit);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "종합 인기 이모티콘 랭킹 조회",
            description = """
                조회수, 사용수, 구매수, 최신성 등 여러 지표를 가중치로 합산한 종합 인기 점수 기준
                TOP N 이모티콘을 인기순으로 조회합니다.
            """
    )
    @GetMapping("/overall")
    public ResponseEntity<List<EmoticonResponse>> getOverallPopularEmoticons(
            @RequestParam(defaultValue = "10") int limit) {
        List<EmoticonResponse> result = emoticonPopularityService.getOverallPopularEmoticons(limit);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/calculate")
    public ResponseEntity<Void> calculateOverallPopularity() {
        emoticonPopularityService.calculatePopularity();
        return ResponseEntity.ok().build();
    }
}
