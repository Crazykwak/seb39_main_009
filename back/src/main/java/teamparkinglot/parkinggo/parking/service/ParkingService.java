package teamparkinglot.parkinggo.parking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import teamparkinglot.parkinggo.advice.exception.BusinessException;
import teamparkinglot.parkinggo.advice.ExceptionCode;
import teamparkinglot.parkinggo.history.repository.HistoryRepositoryQueryDsl;
import teamparkinglot.parkinggo.member.entity.Member;
import teamparkinglot.parkinggo.member.repository.MemberRepository;
import teamparkinglot.parkinggo.member.service.MemberService;
import teamparkinglot.parkinggo.parking.dto.*;
import teamparkinglot.parkinggo.parking.entity.Address;
import teamparkinglot.parkinggo.parking.entity.Parking;
import teamparkinglot.parkinggo.parking.mapper.ParkingMapper;
import teamparkinglot.parkinggo.parking.repository.ParkingQueryDsl;
import teamparkinglot.parkinggo.parking.repository.ParkingRepository;
import teamparkinglot.parkinggo.parking_place.ParkingPlace;
import teamparkinglot.parkinggo.parking_place.ParkingPlaceRepository;
import teamparkinglot.parkinggo.reservation.entity.Reservation;
import teamparkinglot.parkinggo.reservation.repository.ReservationRepository;
import teamparkinglot.parkinggo.reservation.service.ReservationService;
import teamparkinglot.parkinggo.health_check.DbDto;
import teamparkinglot.parkinggo.health_check.Items;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingService {

    private final ParkingRepository parkingRepository;
    private final ParkingQueryDsl parkingQueryDsl;
    private final ReservationService reservationService;
    private final MemberService memberService;
    private final HistoryRepositoryQueryDsl historyRepositoryQueryDsl;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final ParkingPlaceRepository parkingPlaceRepository;

    private final ParkingMapper parkingMapper;

    public List<Parking> findByCond(ParkingCondDto parkingCondDto) {

        List<Parking> parkings = parkingQueryDsl.findParkingOnRegionAndReservationTime(parkingCondDto.getRegion(), parkingCondDto.getParkingStartTime(), parkingCondDto.getParkingEndTime());

        if (parkings.size() < 10) {
            List<Parking> parkingButNotPartnerShip = parkingQueryDsl.findParkingOnRegionButPartnerShipIsNot(parkingCondDto.getRegion());
            parkings.addAll(parkingButNotPartnerShip);
        }

        return parkings;
    }



    public Parking findVerifiedParking(long id) {
        return parkingRepository.findById(id).orElseThrow(
                () -> new BusinessException(ExceptionCode.PARKING_NOT_EXISTS)
        );
    }

    public ParkingMapDto findMap(long id, SelectTimeDto selectTimeDto) {

        Parking parking = findVerifiedParking(id);

        List<ValidNum> validNums = parkingPlaceRepository.findByParkingId(parking.getId(), selectTimeDto.getParkingStartTime(), selectTimeDto.getParkingEndTime()).stream()
                .map(e -> new ValidNum(e.getNumber()))
                .collect(Collectors.toList());

        return new ParkingMapDto(parking.getParkingMap(), validNums);
    }

    @Transactional
    public CreateReservDto createReservation(Long id, ParkingDateTimeDto parkingDateTimeDto, String email) {
        Parking parking = parkingRepository.findById(id).orElseThrow(
                () -> new BusinessException(ExceptionCode.PARKING_NOT_EXISTS)
        );
        Member member = memberRepository.findByEmail(email).orElseThrow(
                () -> new BusinessException(ExceptionCode.MEMBER_NOT_EXISTS)
        );
        ParkingPlace parkingPlace = parkingPlaceRepository.findParkingPlace(id, parkingDateTimeDto.getNumber());

        long time = ChronoUnit.MINUTES.between(parkingDateTimeDto.getParkingStartDateTime(), parkingDateTimeDto.getParkingEndDateTime());

        long price = parking.getBasicCharge() + ((time - parking.getBasicTime()) / parking.getAddUnitTime()) * parking.getAddUnitCharge();

        if(price >= parking.getDayMaxPrice()) price = parking.getDayMaxPrice();

        Reservation reservation = Reservation.builder()
                .reservationDate(LocalDateTime.now())
                .parkingStartDateTime(parkingDateTimeDto.getParkingStartDateTime())
                .parkingEndDateTime(parkingDateTimeDto.getParkingEndDateTime())
                .member(member)
                .parkingPlace(parkingPlace)
                .payOrNot(false)
                .price(price)
                .refundAgmt(true)
                .build();

        reservationRepository.save(reservation);

        return parkingMapper.reservationToCreateReservDto(reservation, member);
    }

    @Transactional
    public void saveAllItem(DbDto dbDto) {

        Member member = memberService.findVerifiedMember("kwj1830@naver.com");

        dbDto.getRecords().stream()
                .forEach(e -> {
                    Parking parking = Parking.builder()
                            .parkingManagementNumber(e.getPrkplceNo())
                            .parkingName(e.getPrkplceNm())
                            .parkingType(e.getPrkplceType())
                            .parkingSeparation(e.getPrkplceSe())
                            .spacialManagement(e.getSpacialManagement())
                            .parkingChargeInfo(e.getParkingChargeInfo())
                            .methodPay(methodPayHandle(e.getMetpay()))
                            .address(new Address("", e.getRdnmadr(), e.getLnmadr()))
                            .capacity(e.getPrkcmprt())
                            .weekdayOpen(e.getWeekdayOperOpenHhmm())
                            .weekdayClose(e.getWeekdayOperColseHhmm())
                            .satClose(e.getSatOperOperOpenHhmm())
                            .satOpen(e.getSatOperCloseHhmm())
                            .sunOpen(e.getHolidayOperOpenHhmm())
                            .sunClose(e.getHolidayCloseOpenHhmm())
                            .basicTime(e.getBasicTime())
                            .basicCharge(e.getBasicCharge())
                            .addUnitTime(e.getAddUnitTime())
                            .addUnitCharge(e.getAddUnitCharge())
                            .dayMaxPrice(e.getDayCmmtkt())
                            .latitude(e.getLatitude())
                            .longitude(e.getLongitude())
                            .member(member)
                            .phoneNumber(e.getPhoneNumber())
                            .partnership(false)
                            .build();

                    Optional<Parking> find = parkingRepository.findByParkingManagementNumber(parking.getParkingManagementNumber());

                    if (find.isEmpty()) {
                        parkingRepository.save(parking);
                    }
                });
    }

    private String methodPayHandle(String methodPay) {
        if (methodPay == null) {
            return null;
        }
        if (methodPay.equals("카드") || methodPay.equals("신용카드")) {
            return "신용카드";
        }
        if (methodPay.equals("현금")) {
            return "현금";
        }
        return null;
    }

    @Transactional
    public void updateAllDb(DbDto dbDto) {
        List<Items> records = dbDto.getRecords();

        records.stream()
                .forEach(e -> {

                    Optional<Parking> findParking = parkingRepository.findByParkingManagementNumber(e.getPrkplceNo());
                    if (findParking.isEmpty()) {
                        return;
                    }

                    Parking parking = findParking.get();
                    parking.setParkingType(e.getPrkplceType());
                });

    }
}