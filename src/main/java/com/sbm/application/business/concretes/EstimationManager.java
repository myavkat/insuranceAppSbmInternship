package com.sbm.application.business.concretes;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sbm.application.business.abstracts.CarService;
import com.sbm.application.business.abstracts.CityService;
import com.sbm.application.business.abstracts.CustomerService;
import com.sbm.application.business.abstracts.EstimationService;
import com.sbm.application.business.abstracts.InsuranceService;
import com.sbm.application.business.abstracts.RealEstateService;
import com.sbm.application.business.abstracts.VehicleService;
import com.sbm.application.core.helpers.DateHelper;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.core.utilities.results.ErrorDataResult;
import com.sbm.application.core.utilities.results.ErrorResult;
import com.sbm.application.core.utilities.results.Result;
import com.sbm.application.core.utilities.results.SuccessDataResult;
import com.sbm.application.core.utilities.results.SuccessResult;
import com.sbm.application.entities.concretes.Estimation;
import com.sbm.application.entities.concretes.Vehicle;
import com.sbm.application.entities.dtos.EstimationDetailDTO;
import com.sbm.application.entities.dtos.InsuranceDetailDTO;
import com.sbm.application.repositories.concretes.EstimationRepository;

@Service
public class EstimationManager implements EstimationService {
	private final String entityName = "Fiyat Teklifi";
	Logger logger = LoggerFactory.getLogger(EstimationManager.class);
	@Autowired
	private EstimationRepository estimationRepository;
	@Autowired
	private InsuranceService insuranceService;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private CarService carService;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private CityService cityService;
	@Autowired
	private RealEstateService realEstateService;

	@Override
	public Result save(Estimation estimation) {
		try {
			if (estimation.getId() == 0) {
				estimationRepository.save(estimation).block(Duration.ofSeconds(1));
				return new SuccessResult("%s eklendi".formatted(entityName));
			}
			Estimation foundEstimation = estimationRepository.findById(estimation.getId()).block(Duration.ofSeconds(1));
			if (foundEstimation == null) {
				return new ErrorResult("%s idli %s bulunamadı".formatted(estimation.getId(), entityName));
			}
			estimationRepository.save(estimation).block(Duration.ofSeconds(1));
			return new SuccessResult("%s güncelleme başarılı!".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result delete(Estimation estimation) {
		try {
			estimationRepository.delete(estimation);
			return new SuccessResult("Silindi");
		} catch (Exception ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result deleteById(int id) {
		var result = getById(id);
		if (!result.isSuccess()) {
			return new ErrorResult("%s bulunamadı".formatted(entityName));
		}
		try {
			estimationRepository.delete(result.getData()).block(Duration.ofSeconds(1));
			return new SuccessResult("%s silindi".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<Estimation> getById(int id) {
		Estimation estimation = new Estimation();
		try {
			estimation = estimationRepository.findById(id).block(Duration.ofSeconds(1));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<Estimation>(estimation, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		if (estimation == null) {
			return new ErrorDataResult<Estimation>(new Estimation(), "%s Bulunamadı!".formatted(entityName));
		}
		return new SuccessDataResult<Estimation>(estimation, "Başarılı");
	}

	@Override
	public DataResult<List<Estimation>> getAll() {
		List<Estimation> estimations = new ArrayList<Estimation>();
		try {
			estimationRepository.findAll().doOnNext(estimations::add).blockLast(Duration.ofSeconds(10));
			return new SuccessDataResult<List<Estimation>>(estimations, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<Estimation>>(estimations, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<Estimation> estimateKasko(int insuranceId, int vehicleId) {
		Estimation estimation = new Estimation();
		// Sigorta ve aracı getir hata varsa mesajla beraber dön
		var insuranceResult = insuranceService.getInsuranceDetailById(insuranceId);
		if (!insuranceResult.isSuccess()) {
			return new ErrorDataResult<Estimation>(estimation, insuranceResult.getMessage());
		}
		var vehicleResult = vehicleService.getById(vehicleId);
		if (!vehicleResult.isSuccess()) {
			return new ErrorDataResult<Estimation>(estimation, vehicleResult.getMessage());
		}
		InsuranceDetailDTO insurance = insuranceResult.getData();
		Vehicle vehicle = vehicleResult.getData();
		// Fiyat tahmini objesinin boş alanlarını doldur
		estimation.setEstimationDate(OffsetDateTime.now());
		estimation.setInsuranceId(insuranceId);
		estimation.setParameterId(vehicleId);
		estimation.setConfirmed(false);
		// Araca bağlı araba ve müşteri detaylarını getir
		var carDetail = carService.getCarDetailById(vehicle.getCarId()).getData();
		var customerDetail = customerService.getCustomerDetailById(vehicle.getCustomerId()).getData();
		double price = 0;
		// Arabanın TSB tarafından belirlenen kasko değeri
		// ile sigorta şirketinin birim fiyatını çarparak varsayılan fiyatı belirle
		price += insurance.getUnitPrice() * carDetail.getEstimatedValue();
		// Araç hasarlıysa fiyatı belirlenen oranda artır
		// TODO bunu başka bir yerden çekmek daha mantıklı gömülü yazmaktansa
		double damageScaleValue = 0.25;
		if (vehicle.isDamaged()) {
			price *= (1 + damageScaleValue);
		}
		// aracın plakasındaki şehri getir ve bulamazsa mesajla beraber dön
		var cityOfVehicleResult = cityService.getByPlateCode(vehicle.extractCityCode());
		if (!cityOfVehicleResult.isSuccess()) {
			return new ErrorDataResult<Estimation>(estimation, vehicleResult.getMessage());
		}
		var cityOfVehicle = cityOfVehicleResult.getData();
		// Aracın şehrinin yüzdelik oranını yarıya azalt (Müşterinin ikamet şehri daha
		// önemli)
		double cityOfVehicleScaleFactor = cityOfVehicle.getScaleFactor() * 0.5;
		price *= (1 + cityOfVehicleScaleFactor);
		price += (cityOfVehicle.getValueFactor() / 2);
		// Müşteri ehliyet tarihine göre fiyatta değişiklik yap
		double licenseAgeScaleFactor = (5 - customerDetail.getLicenseAge()) * 0.01;
		price *= (1 + licenseAgeScaleFactor);
		// Müşteri yaşına göre fiyatta değişiklik yap
		double ageScaleFactor = (45 - customerDetail.getAge()) * 0.01;
		price *= (1 + ageScaleFactor);
		// Müşteri ikamet şehri ve mesleğine göre fiyatta yüzdelik ve doğrusal
		// değişiklik yap
		price *= (1 + customerDetail.getCityScaleFactor());
		price *= (1 + customerDetail.getProfessionScaleFactor());
		price += customerDetail.getCityValueFactor() + customerDetail.getProfessionValueFactor();
		estimation.setPrice(price);
		return new SuccessDataResult<Estimation>(estimation);
	}

	public DataResult<List<EstimationDetailDTO>> estimateKaskoAllCompanies(int vehicleId) {
		List<Estimation> estimations = new ArrayList<Estimation>();
		List<Integer> oldEstimationIds = new ArrayList<Integer>();
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		var insurancesResult = insuranceService.getInsuranceDetailsByInsuranceTypeName("Kasko");
		if (!insurancesResult.isSuccess()) {
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, insurancesResult.getMessage());
		}
		for (InsuranceDetailDTO insurance : insurancesResult.getData()) {
			var estimationResult = estimateKasko(insurance.getId(), vehicleId);
			if (!estimationResult.isSuccess()) {
				return new ErrorDataResult<List<EstimationDetailDTO>>(details, estimationResult.getMessage());
			} else {
				estimations.add(estimationResult.getData());
			}
		}
		try {
			estimationRepository.findKaskoDetailsByVehicleId(vehicleId).doOnNext(e-> oldEstimationIds.add(e.getId())).blockLast();
			estimationRepository.deleteAllById(oldEstimationIds).block();
			estimationRepository.saveAll(estimations).blockLast(Duration.ofSeconds(3));
			for (Estimation estimation : estimations) {
				estimationRepository.findKaskoDetailById(estimation.getId()).doOnNext(details::add).block();
			}
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
	}

	@Override
	public DataResult<List<EstimationDetailDTO>> getDetails() {
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		try {
			estimationRepository.findKaskoDetails().doOnNext(details::add).blockLast(Duration.ofSeconds(5));
			estimationRepository.findKonutDetails().doOnNext(details::add).blockLast(Duration.ofSeconds(5));
			details.sort((EstimationDetailDTO detail1, EstimationDetailDTO detail2) -> OffsetDateTime.timeLineOrder().reversed().compare(detail1.getEstimationDate(),detail2.getEstimationDate()));
			return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<List<EstimationDetailDTO>> getDetailsByCustomerId(int customerId) {
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		try {
			estimationRepository.findKaskoDetailsByCustomerId(customerId).doOnNext(details::add)
					.blockLast(Duration.ofSeconds(5));
			estimationRepository.findKonutDetailsByCustomerId(customerId).doOnNext(details::add)
			.blockLast(Duration.ofSeconds(5));
			details.sort((EstimationDetailDTO detail1, EstimationDetailDTO detail2) -> OffsetDateTime.timeLineOrder().reversed().compare(detail1.getEstimationDate(),detail2.getEstimationDate()));
			return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<List<EstimationDetailDTO>> getKaskoDetailsByVehicleId(int vehicleId) {
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		try {
			estimationRepository.findKaskoDetailsByVehicleId(vehicleId).doOnNext(details::add)
					.blockLast(Duration.ofSeconds(5));
			return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result confirmById(int id) {
		var getResult = getById(id);
		if (!getResult.isSuccess()) {
			return new ErrorResult(getResult.getMessage());
		}
		var estimationToConfirm = getResult.getData();
		estimationToConfirm.setConfirmed(true);
		var saveResult = save(estimationToConfirm);
		if (saveResult.isSuccess()) {
			return new SuccessResult("%s onaylandı".formatted(entityName));
		}
		return new ErrorResult("%s onaylanamadı".formatted(entityName));

	}

	@Override
	public Result revokeConfirmationById(int id) {
		var getResult = getById(id);
		if (!getResult.isSuccess()) {
			return new ErrorResult(getResult.getMessage());
		}
		var estimationToConfirm = getResult.getData();
		estimationToConfirm.setConfirmed(false);
		var saveResult = save(estimationToConfirm);
		if (saveResult.isSuccess()) {
			return new SuccessResult("%s onayı geri alındı".formatted(entityName));
		}
		return new ErrorResult("%s onayı geri alınamadı".formatted(entityName));

	}

	@Override
	public DataResult<List<EstimationDetailDTO>> estimateKonutAllCompanies(int realEstateId) {
		List<Estimation> estimations = new ArrayList<Estimation>();
		List<Integer> oldEstimationIds = new ArrayList<Integer>();
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		var insurancesResult = insuranceService.getInsuranceDetailsByInsuranceTypeName("Konut");
		if (!insurancesResult.isSuccess()) {
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, insurancesResult.getMessage());
		}
		for (InsuranceDetailDTO insurance : insurancesResult.getData()) {
			var estimationResult = estimateKonut(insurance.getId(), realEstateId);
			if (!estimationResult.isSuccess()) {
				return new ErrorDataResult<List<EstimationDetailDTO>>(details, estimationResult.getMessage());
			} else {
				estimations.add(estimationResult.getData());
			}
		}
		try {
			estimationRepository.findKonutDetailsByRealEstateId(realEstateId).doOnNext(e-> oldEstimationIds.add(e.getId())).blockLast();
			estimationRepository.deleteAllById(oldEstimationIds).block();
			estimationRepository.saveAll(estimations).blockLast(Duration.ofSeconds(3));
			for (Estimation estimation : estimations) {
				estimationRepository.findKonutDetailById(estimation.getId()).doOnNext(details::add).block();
			}
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
	}

	@Override
	public DataResult<Estimation> estimateKonut(int insuranceId, int realEstateId) {
		Estimation estimation = new Estimation();
		// Fiyat tahmini objesinin boş alanlarını doldur
		estimation.setEstimationDate(OffsetDateTime.now());
		estimation.setInsuranceId(insuranceId);
		estimation.setParameterId(realEstateId);
		estimation.setConfirmed(false);
		var insurance = insuranceService.getById(insuranceId).getData();
		var realEstateDetail = realEstateService.getDetailById(realEstateId).getData();
		var customer = customerService.getCustomerDetailById(realEstateDetail.getCustomerId()).getData();
		double price = insurance.getUnitPrice() * (realEstateDetail.getFloorArea()*realEstateDetail.getUnitConstructionCost() + realEstateDetail.getValue())/2;
		price *= 1+realEstateDetail.getCityScaleFactor();
		price *= realEstateDetail.isUrbanLocated()? 0.9d : 1.5d;
		price *= 1 + (DateHelper.getYearDifferenceFromNow(realEstateDetail.getConstructionYear())*0.01d);
		price *= 1 + ((35-customer.getAge())*0.01d);// 35 yaş altında her yaş için %1 sürprim üstündeyse indirim
		price *= 1 + (customer.getProfessionScaleFactor());
		price += realEstateDetail.getCityValueFactor() + customer.getProfessionValueFactor();
		estimation.setPrice(price);
		return new SuccessDataResult<Estimation>(estimation);
	}

	@Override
	public DataResult<List<EstimationDetailDTO>> getKonutDetailsByRealEstateId(int realEstateId) {
		List<EstimationDetailDTO> details = new ArrayList<EstimationDetailDTO>();
		try {
			estimationRepository.findKonutDetailsByRealEstateId(realEstateId).doOnNext(details::add)
					.blockLast(Duration.ofSeconds(5));
			return new SuccessDataResult<List<EstimationDetailDTO>>(details, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<EstimationDetailDTO>>(details, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}
}
