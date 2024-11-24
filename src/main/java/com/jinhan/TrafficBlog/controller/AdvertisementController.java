package com.jinhan.TrafficBlog.controller;

import com.jinhan.TrafficBlog.dto.AdvertisementDto;
import com.jinhan.TrafficBlog.entity.Advertisement;
import com.jinhan.TrafficBlog.service.AdvertisementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// 광고 관련 API 엔드포인트를 제공하는 REST 컨트롤러
@RestController
@RequestMapping("/api")
public class AdvertisementController {
    // 광고 서비스 의존성 주입
    private final AdvertisementService advertisementService;

    // 생성자를 통한 의존성 주입 (생성자 주입 방식)
    @Autowired
    public AdvertisementController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    /**
     * 새로운 광고 생성 API
     * 관리자 권한으로만 접근 가능한 엔드포인트
     *
     * @param advertisementDto 광고 정보를 담은 DTO
     * @return 생성된 광고 객체와 200 OK 상태
     */
    @PostMapping("/admin/ads")
    public ResponseEntity<Advertisement> writeAd(@RequestBody AdvertisementDto advertisementDto) {
        // 광고 서비스를 통해 새로운 광고 생성
        Advertisement advertisement = advertisementService.writeAd(advertisementDto);
        // 생성된 광고 정보 반환
        return ResponseEntity.ok(advertisement);
    }

    /**
     * 전체 광고 목록 조회 API
     * 모든 사용자가 접근 가능
     *
     * @return 광고 목록과 200 OK 상태
     */
    @GetMapping("/ads")
    public ResponseEntity<List<Advertisement>> getAdList() {
        // 모든 광고 목록 조회
        List<Advertisement> advertisementList = advertisementService.getAdList();
        // 광고 목록 반환
        return ResponseEntity.ok(advertisementList);
    }

    /**
     * 특정 광고 상세 조회 API
     * 광고 조회 시 뷰 카운트 및 IP 추적 기능 포함
     *
     * @param adId 조회할 광고 ID
     * @param request HTTP 요청 객체 (IP 주소 추출용)
     * @param isTrueView 진성 뷰 여부를 판단하는 파라미터 (선택적)
     * @return 광고 상세 정보 또는 404 Not Found
     */
    @GetMapping("/ads/{adId}")
    public Object getAdList(
            @PathVariable Long adId,
            HttpServletRequest request,
            @RequestParam(required = false) Boolean isTrueView
    ) {
        // 클라이언트 IP 주소 추출
        String ipAddress = request.getRemoteAddr();

        // 광고 조회 (IP 및 진성 뷰 여부 추적)
        Optional<Advertisement> advertisement = advertisementService.getAd(
                adId,
                ipAddress,
                isTrueView != null && isTrueView
        );

        // 광고가 존재하지 않을 경우 404 에러
        if (advertisement.isEmpty()) {
            return ResponseEntity.notFound();
        }

        // 광고 정보 반환
        return ResponseEntity.ok(advertisement);
    }

    /**
     * 광고 클릭 추적 API
     * 광고 클릭 시 IP 기반 추적
     *
     * @param adId 클릭된 광고 ID
     * @param request HTTP 요청 객체 (IP 주소 추출용)
     * @return 클릭 성공 메시지와 200 OK 상태
     */
    @PostMapping("/ads/{adId}")
    public Object clickAd(
            @PathVariable Long adId,
            HttpServletRequest request
    ) {
        // 클라이언트 IP 주소 추출
        String ipAddress = request.getRemoteAddr();

        // 광고 클릭 이벤트 처리
        advertisementService.clickAd(adId, ipAddress);

        // 클릭 성공 응답
        return ResponseEntity.ok("click");
    }

    /**
     * 광고 조회 이력 통계 API
     * 광고별 조회 이력을 그룹화하여 통계 생성 및 저장
     *
     * @return 광고 조회 통계 정보와 200 OK 상태
     */
//    @GetMapping("/ads/history")
//    public ResponseEntity<List<AdHistoryResult>> getAdHistory() {
//        // 광고 조회 이력을 광고 ID별로 그룹화
//        List<AdHistoryResult> result = advertisementService.getAdViewHistoryGroupedByAdId();
//
//        // 광고 조회 통계 정보 데이터베이스에 저장
//        advertisementService.insertAdViewStat(result);
//
//        // 광고 조회 통계 정보 반환
//        return ResponseEntity.ok(result);
//    }

}