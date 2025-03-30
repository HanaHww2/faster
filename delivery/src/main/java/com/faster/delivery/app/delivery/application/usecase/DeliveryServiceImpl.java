package com.faster.delivery.app.delivery.application.usecase;

import com.common.exception.CustomException;
import com.common.exception.type.ApiErrorCode;
import com.common.resolver.dto.CurrentUserInfoDto;
import com.common.resolver.dto.UserRole;
import com.common.response.PageResponse;
import com.faster.delivery.app.delivery.application.CompanyClient;
import com.faster.delivery.app.delivery.application.DeliveryManagerClient;
import com.faster.delivery.app.delivery.application.HubClient;
import com.faster.delivery.app.delivery.application.MessageClient;
import com.faster.delivery.app.delivery.application.OrderClient;
import com.faster.delivery.app.delivery.application.dto.AssignDeliveryManagerApplicationResponse;
import com.faster.delivery.app.delivery.application.dto.AssignedDeliveryRouteDto;
import com.faster.delivery.app.delivery.application.dto.CompanyDto;
import com.faster.delivery.app.delivery.application.dto.DeliveryDetailDto;
import com.faster.delivery.app.delivery.application.dto.DeliveryGetElementDto;
import com.faster.delivery.app.delivery.application.dto.DeliveryManagerDto;
import com.faster.delivery.app.delivery.application.dto.DeliveryRouteUpdateDto;
import com.faster.delivery.app.delivery.application.dto.DeliverySaveApplicationDto;
import com.faster.delivery.app.delivery.application.dto.DeliverySaveDto;
import com.faster.delivery.app.delivery.application.dto.DeliveryUpdateDto;
import com.faster.delivery.app.delivery.application.dto.HubDto;
import com.faster.delivery.app.delivery.application.dto.HubRouteDto;
import com.faster.delivery.app.delivery.application.dto.OrderUpdateApplicationRequestDto;
import com.faster.delivery.app.delivery.application.dto.OrderUpdateApplicationResponseDto;
import com.faster.delivery.app.delivery.application.dto.SendMessageApplicationRequestDto;
import com.faster.delivery.app.delivery.application.dto.SendMessageApplicationRequestDto.DeliveryManagerInfo;
import com.faster.delivery.app.delivery.application.usecase.strategy.SearchByRole;
import com.faster.delivery.app.delivery.domain.entity.Delivery;
import com.faster.delivery.app.delivery.domain.entity.Delivery.Status;
import com.faster.delivery.app.delivery.domain.entity.DeliveryRoute;
import com.faster.delivery.app.delivery.domain.repository.DeliveryRepository;
import com.faster.delivery.app.delivery.infrastructure.client.type.DeliveryManagerType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DeliveryServiceImpl implements DeliveryService {

  private final DeliveryRepository deliveryRepository;
  private final CompanyClient companyClient;
  private final HubClient hubClient;
  private final DeliveryManagerClient deliveryManagerClient;
  private final OrderClient orderClient;
  private final List<SearchByRole> searchByRole;
  private final MessageClient messageClient;

  @Transactional
  public UUID saveDelivery(DeliverySaveDto deliverySaveDto) {

    // 수취 업체 정보 조회
    CompanyDto companyData = companyClient.getCompanyData(deliverySaveDto.receiveCompanyId());

    // 허브 경로 조회
    List<HubRouteDto> hubRouteDataList = hubClient.getHubRouteDataList(
        deliverySaveDto.sourceHubId(), deliverySaveDto.destinationHubId());

    // 배송 경로 목록 구성
    List<DeliveryRoute> deliveryRouteList = IntStream.range(0, hubRouteDataList.size())
        .mapToObj(i -> hubRouteDataList.get(i).toDeliveryRoute(i+1)) // 인덱스를 함께 전달
        .toList();

    // 업체 배송 담당자 지정
    AssignDeliveryManagerApplicationResponse deliveryManagerDto =
        deliveryManagerClient.assignCompanyDeliveryManager(
            deliverySaveDto.destinationHubId(), DeliveryManagerType.COMPANY_DELIVERY, 1);

    // 배송 정보 구성
    AssignDeliveryManagerApplicationResponse.DeliveryManagerInfo assignCompanyDeliveryManager = deliveryManagerDto.deliveryManagers()
        .get(0);
    Delivery delivery =
        Delivery.builder()
        .orderId(deliverySaveDto.orderId())
        .sourceHubId(deliverySaveDto.sourceHubId())
        .companyDeliveryManagerId(assignCompanyDeliveryManager.deliveryManagerId())
        .destinationHubId(deliverySaveDto.destinationHubId())
        .receiptCompanyId(deliverySaveDto.receiveCompanyId())
        .receiptCompanyAddress(companyData.address())
        .recipientName(companyData.companyManagerName())
        .recipientSlackId(companyData.companyManagerSlackId())
        .status(Status.READY)
        .build();

    delivery.addDeliveryRouteList(deliveryRouteList);

    ArrayList<UUID> routeIds = new ArrayList<>();
    routeIds.add(deliverySaveDto.sourceHubId());
    for(DeliveryRoute route : deliveryRouteList){
      routeIds.add(route.getDestinationHubId());
    }

    Delivery savedDelivery = deliveryRepository.save(delivery);

    List<HubDto> hubListData = hubClient.getHubListData(routeIds);

    sendMessage(hubListData,
        deliverySaveDto.sourceHubId(), deliverySaveDto.destinationHubId(),
        savedDelivery, deliveryManagerDto.deliveryManagers());

    return savedDelivery.getId();
  }

  @Transactional(readOnly = true)
  public DeliveryDetailDto getDeliveryDetail(UUID deliveryId, CurrentUserInfoDto userInfoDto) {
    // 배송 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    // 권한 체크
    checkRole(userInfoDto, delivery);

    // 배송 담당자 정보 조회
    DeliveryManagerDto deliveryManagerData = deliveryManagerClient.getDeliveryManagerData(
        delivery.getCompanyDeliveryManagerId());

    // 허브 정보 조회
    List<HubDto> hubListData = hubClient.getHubListData(
        List.of(delivery.getSourceHubId(), delivery.getDestinationHubId()));
    Map<UUID, String> hubIdNameMap = hubListData.stream()
        .collect(Collectors.toMap(hubDto -> hubDto.hubId(), hubDto -> hubDto.name()));

    // 업체 정보 조회
    CompanyDto companyData = companyClient.getCompanyData(delivery.getReceiptCompanyId());

    // dto 변환
    DeliveryDetailDto deliveryDetailDto = DeliveryDetailDto.of(
        delivery, deliveryManagerData.deliveryManagerName(),
        hubIdNameMap.get(delivery.getSourceHubId()),
        hubIdNameMap.get(delivery.getDestinationHubId()),
        companyData.name());

    return deliveryDetailDto;
  }

  @Transactional(readOnly = true)
  @Override
  public PageResponse<DeliveryGetElementDto> getDeliveryList(
      Pageable pageable, String search, CurrentUserInfoDto userInfo) {

    Page<Delivery> deliveries = searchByRole.stream()
        .filter(s -> s.isSupport(userInfo))
        .findAny()
        .orElseThrow(() -> new CustomException(ApiErrorCode.FORBIDDEN))
        .getDeliveryList(pageable, Status.fromString(search), userInfo);

    return PageResponse.from(deliveries).map(DeliveryGetElementDto::from);
  }

  @Transactional
  public UUID updateDeliveryStatus (CurrentUserInfoDto userInfoDto, UUID deliveryId,
      DeliveryUpdateDto deliveryUpdateDto) {

    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    checkRole(userInfoDto, delivery);

    // 배송 정보 업데이트
    Status deliveryStatus = getDeliveryStatusByString(deliveryUpdateDto.status());
    delivery.updateStatus(deliveryStatus);

    Long newCompanyDeliveryManagerId = deliveryUpdateDto.companyDeliveryManagerId();
    if (newCompanyDeliveryManagerId != null) {
      DeliveryManagerDto deliveryManagerData = deliveryManagerClient
          .getDeliveryManagerData(newCompanyDeliveryManagerId);
      if (deliveryManagerData.hubId() == null) { // hubId 가 없으면 허브배송담당자
        throw new CustomException(ApiErrorCode.INVALID_REQUEST);
      }
    }
    delivery.updateCompanyDeliveryManagerId(newCompanyDeliveryManagerId);

    // 주문 정보 업데이트
    OrderUpdateApplicationResponseDto updateDto = updateOrderStatus(deliveryStatus, delivery);
    return delivery.getId();
  }

  @Transactional
  public UUID deleteDelivery(UUID deliveryId, CurrentUserInfoDto userInfoDto) {
    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    checkRole(userInfoDto, delivery);

    // 삭제
    delivery.delete(LocalDateTime.now(), userInfoDto.userId());

    return delivery.getId();
  }

  @Transactional
  public UUID saveDeliveryInternal(DeliverySaveApplicationDto deliverySaveDto) {

    // 업체 정보 조회
    CompanyDto supplierCompany = companyClient.getCompanyData(deliverySaveDto.supplierCompanyId());
    CompanyDto receiveCompany = companyClient.getCompanyData(deliverySaveDto.receiveCompanyId());

    // 허브 경로 조회
    List<HubRouteDto> hubRouteDataList = hubClient.getHubRouteDataList(
        supplierCompany.hubId(), receiveCompany.hubId());

    // 배송 경로 목록 구성
    List<DeliveryRoute> deliveryRouteList = IntStream.range(0, hubRouteDataList.size())
        .mapToObj(i -> hubRouteDataList.get(i).toDeliveryRoute(i+1)) // 인덱스를 함께 전달
        .toList();

    // 업체 배송 담당자 지정
    AssignDeliveryManagerApplicationResponse deliveryManagerDto = deliveryManagerClient.assignCompanyDeliveryManager(
        receiveCompany.hubId(),
        DeliveryManagerType.COMPANY_DELIVERY,
        1);

    ArrayList<UUID> routeIds = new ArrayList<>();
    routeIds.add(supplierCompany.hubId());
    for(DeliveryRoute route : deliveryRouteList){
      routeIds.add(route.getDestinationHubId());
    }

    Delivery savedDelivery = saveDelivery(deliverySaveDto,
        deliveryManagerDto.deliveryManagers().get(0), supplierCompany, receiveCompany, deliveryRouteList);

    List<HubDto> hubListData = hubClient.getHubListData(routeIds);

    sendMessage(hubListData, supplierCompany.hubId(), receiveCompany.hubId(),
        savedDelivery, List.of(deliveryManagerDto.deliveryManagers().get(0)));

    return savedDelivery.getId();
  }

  private Delivery saveDelivery(
      DeliverySaveApplicationDto deliverySaveDto,
      AssignDeliveryManagerApplicationResponse.DeliveryManagerInfo deliveryManagerDto,
      CompanyDto supplierCompany,
      CompanyDto receiveCompany,
      List<DeliveryRoute> deliveryRouteList
  ) {
    Delivery delivery = Delivery.builder()
        .orderId(deliverySaveDto.orderId())
        .companyDeliveryManagerId(deliveryManagerDto.deliveryManagerId())
        .sourceHubId(supplierCompany.hubId())
        .destinationHubId(receiveCompany.hubId())
        .receiptCompanyId(receiveCompany.companyId())
        .receiptCompanyAddress(receiveCompany.address())
        .recipientName(receiveCompany.companyManagerName())
        .recipientSlackId(receiveCompany.companyManagerSlackId())
        .build();

    delivery.addDeliveryRouteList(deliveryRouteList);

    // save
    Delivery savedDelivery = deliveryRepository.save(delivery);
    return savedDelivery;
  }

  // 이벤트 리스너로 변경하면 좋을 듯
  private void sendMessage(List<HubDto> hubListData, UUID sourceHubId,
      UUID destinationHubId, Delivery savedDelivery, List<AssignDeliveryManagerApplicationResponse.DeliveryManagerInfo> deliveryManagers) {
    StringBuilder waypointNames = new StringBuilder("|");
    String hubSourceName = null;
    String hubDestinationName = null;
    for(HubDto hubDto : hubListData){
      UUID hubId = hubDto.hubId();
      if(hubId.equals(sourceHubId)) {
        hubSourceName = hubDto.name();
        continue;
      }
      if(hubId.equals(destinationHubId)) {
        hubDestinationName = hubDto.name();
        continue;
      }
      waypointNames.append(hubDto.name()).append("|");
    }

    // 메시지 전송
    messageClient.sendMessage(
        SendMessageApplicationRequestDto.builder()
            .deliveryId(savedDelivery.getId())
            .hubSourceId(sourceHubId)
            .receiveHubId(destinationHubId)
            .hubSourceName(hubSourceName)
            .hubWaypointName(waypointNames.toString())
            .hubDestinationName(hubDestinationName)
            .orderInfo(SendMessageApplicationRequestDto.OrderInfo.from(savedDelivery))
            .deliveryManagers(deliveryManagers.stream().map(DeliveryManagerInfo::from).toList())
            .build());
  }

  @Transactional
  public UUID updateDeliveryStatusInternal(
      UUID deliveryId, DeliveryUpdateDto deliveryUpdateDto, CurrentUserInfoDto userInfoDto) {

    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    checkRole(userInfoDto, delivery);

    // 배송 정보 업데이트
    Status deliveryStatus = getDeliveryStatusByString(deliveryUpdateDto.status());
    delivery.updateStatus(deliveryStatus);

    // 주문 정보 업데이트
    OrderUpdateApplicationResponseDto updateDto = updateOrderStatus(deliveryStatus, delivery);
    return delivery.getId();
  }

  private OrderUpdateApplicationResponseDto updateOrderStatus(Status deliveryStatus, Delivery delivery) {

    if (deliveryStatus.isOrderUpdateRequired()) {
      return orderClient.updateOrderStatus(
          delivery.getOrderId(), OrderUpdateApplicationRequestDto.of(deliveryStatus.toString()));
    }
    return null;
  }

  @Transactional
  public UUID updateDeliverRoute(UUID deliveryId, UUID deliveryRouteId,
      DeliveryRouteUpdateDto deliveryRouteUpdateDto,
      CurrentUserInfoDto userInfoDto) {

    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    // route 조회
    DeliveryRoute deliveryRoute = delivery.findDeliveryRouteById(deliveryRouteId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    // 권한 검사
    if (!userInfoDto.role().equals(UserRole.ROLE_MASTER)) {
      if (!deliveryRoute.getDeliveryManagerId().equals(userInfoDto.userId())) {
        throw new CustomException(ApiErrorCode.FORBIDDEN);
      }
    }

    // 업데이트 데이터 반영
    updateDeliveryRouteDataFromDto(deliveryRouteUpdateDto, delivery, deliveryRoute);

    return delivery.getId();
  }

  public void updateDeliveryRouteDataFromDto(DeliveryRouteUpdateDto updateDto,
      Delivery delivery,
      DeliveryRoute deliveryRoute) {
    if (updateDto.isRealMeasurementUpdate()) {
      delivery.updateDeliveryRouteRealMeasurement(
          deliveryRoute,
          updateDto.realDistanceM(),
          updateDto.realTimeMin()
      );
    }
    if (updateDto.isStatusUpdate()) {
      delivery.updateDeliveryRouteStatus(
          deliveryRoute,
          getDeliveryRouteStatusByString(updateDto.status())
      );
    }
    if (updateDto.isDeliveryManagerUpdate()) {
      DeliveryManagerDto deliveryManagerData = deliveryManagerClient.getDeliveryManagerData(
          updateDto.deliveryManagerId());
      if (deliveryManagerData.hubId() != null) {
        throw new CustomException(ApiErrorCode.INVALID_REQUEST);
      }
      delivery.updateDeliveryRouteManager(
          deliveryRoute,
          deliveryManagerData.deliveryManagerId(),
          deliveryManagerData.deliveryManagerName()
      );
    }
  }

  @Override
  @Transactional
  public List<AssignedDeliveryRouteDto> assignHubDeliveryManagerScheduleService() {
    List<DeliveryRoute> routes = deliveryRepository.findRoutesWithMissingManager();

    // 출발지로 분리
    Map<UUID, List<DeliveryRoute>> deliveryRoutesOfSourceHubId = routes.stream()
        .collect(Collectors.groupingBy(DeliveryRoute::getSourceHubId));
    for(Entry<UUID, List<DeliveryRoute>> entry : deliveryRoutesOfSourceHubId.entrySet()){

      // 목적지로 분리
      Map<UUID, List<DeliveryRoute>> deliveryRoutesOfDestinationHubId = entry.getValue().stream()
          .collect(Collectors.groupingBy(DeliveryRoute::getDestinationHubId));

      // 배송 담당자 배정 요청 명수 N명 (같은 출발지에서 다른 배송지 N개)
      int requiredAssignManagerCount = deliveryRoutesOfDestinationHubId.keySet().size();
      List<AssignDeliveryManagerApplicationResponse.DeliveryManagerInfo> deliveryManagerInfos =
          deliveryManagerClient.assignCompanyDeliveryManager(
                  null, DeliveryManagerType.HUB_DELIVERY, requiredAssignManagerCount)
              .deliveryManagers();

      int deliveryManagerInfosIdx = 0;
      for(Entry<UUID, List<DeliveryRoute>> sameRoutesEntry : deliveryRoutesOfDestinationHubId.entrySet()){
        // 출발지 - 목적지 같으면 하나의 배정담당자에게 지정
        List<DeliveryRoute> sameRoutes = sameRoutesEntry.getValue();
        AssignDeliveryManagerApplicationResponse.DeliveryManagerInfo deliveryManagerInfo = deliveryManagerInfos.get(deliveryManagerInfosIdx);
        for(DeliveryRoute sameRoute : sameRoutes){
          sameRoute.updateManager(deliveryManagerInfo.deliveryManagerId(), deliveryManagerInfo.deliveryManagerName());
        }
        deliveryManagerInfosIdx++;
      }
    }

    return null;
  }

  @Override
  public UUID cancelDeliveryInternal(UUID deliveryId, CurrentUserInfoDto userInfo) {

    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNullFetchJoin(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    // 배송 정보 취소
    delivery.cancel();
    return delivery.getId();
  }

  @Override
  public UUID cancelRollbackDeliveryInternal(UUID deliveryId, CurrentUserInfoDto userInfo) {

    // 배송 정보 조회
    Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNullFetchJoin(deliveryId)
        .orElseThrow(() -> new CustomException(ApiErrorCode.NOT_FOUND));

    // 배송 정보 취소 롤백
    delivery.rollbackCancel();
    return delivery.getId();
  }

  private void checkRole(CurrentUserInfoDto userInfoDto, Delivery delivery) {
    switch (userInfoDto.role()) {
      case ROLE_COMPANY -> { // 업체 담당자 : 본인이 수령인인 배송만 조회
        UUID receiptCompanyId = delivery.getReceiptCompanyId();
        // 업체 담당자 정보 조회
        CompanyDto companyData = companyClient.getCompanyData(receiptCompanyId);
        Long companyManagerId = companyData.companyManagerUserId();
        if (!userInfoDto.userId().equals(companyManagerId)) {
          throw new CustomException(ApiErrorCode.FORBIDDEN);
        }
      }
      case ROLE_DELIVERY -> { // 배송 담당자 : 본인이 배송담당자인 배송만 조회
        Long companyDeliveryManagerId = delivery.getCompanyDeliveryManagerId();
        // 배송 담당자 정보 조회
        DeliveryManagerDto deliveryManagerData =
            deliveryManagerClient.getDeliveryManagerData(companyDeliveryManagerId);
        if (!userInfoDto.userId().equals(deliveryManagerData.deliveryManagerId())) {
          throw new CustomException(ApiErrorCode.FORBIDDEN);
        }
      }
      case ROLE_HUB -> { // 허브 담당자 : 소스허브 or 목적지 허브 인 배송만 조회
        UUID sourceHubId = delivery.getSourceHubId();
        UUID destinationHubId = delivery.getDestinationHubId();
        // 허브 정보(허브담당자 정보 포함) 조회
        List<HubDto> hubListData = hubClient.getHubListData(List.of(sourceHubId, destinationHubId));
        List<Long> hubManagerIdList = hubListData.stream().map(HubDto::hubManagerId).toList();
        if (!hubManagerIdList.contains(userInfoDto.userId())) {
          throw new CustomException(ApiErrorCode.FORBIDDEN);
        }
      }
    }
  }

  private DeliveryRoute.Status getDeliveryRouteStatusByString(String statusString) {
    try {
      return DeliveryRoute.Status.valueOf(statusString);
    } catch (Exception e) {
      throw new CustomException(ApiErrorCode.INVALID_REQUEST);
    }
  }

  private Delivery.Status getDeliveryStatusByString(String statusString) {
    try {
      return Delivery.Status.valueOf(statusString);
    } catch (Exception e) {
      throw new CustomException(ApiErrorCode.INVALID_REQUEST);
    }
  }
}
