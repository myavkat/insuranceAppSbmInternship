package com.sbm.application.business.concretes;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sbm.application.business.abstracts.CarService;
import com.sbm.application.business.abstracts.CustomerService;
import com.sbm.application.business.abstracts.EstimationService;
import com.sbm.application.business.abstracts.InsuranceService;
import com.sbm.application.business.abstracts.VehicleService;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.core.utilities.results.ErrorDataResult;
import com.sbm.application.core.utilities.results.ErrorResult;
import com.sbm.application.core.utilities.results.Result;
import com.sbm.application.core.utilities.results.SuccessDataResult;
import com.sbm.application.core.utilities.results.SuccessResult;
import com.sbm.application.entities.concretes.Estimation;
import com.sbm.application.entities.concretes.Vehicle;
import com.sbm.application.entities.dtos.InsuranceDetailDTO;
import com.sbm.application.repositories.concretes.EstimationRepository;

@Service
public class EstimationManager implements EstimationService {
	private final String entityName = "Fiyat Tahmini";
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

	@Override
	public Result save(Estimation estimation) {
		try {
			if (estimation.getId() == 0) {
				estimationRepository.save(estimation).block(Duration.ofSeconds(1));
				return new SuccessResult("%s eklendi".formatted(entityName));
			}
			Estimation foundCarBrand = estimationRepository.findById(estimation.getId()).block(Duration.ofSeconds(1));
			if (foundCarBrand == null) {
				return new ErrorResult("%s idli %s bulunamadı".formatted(estimation.getId(), entityName));
			}
			estimationRepository.save(estimation).block(Duration.ofSeconds(1));
			return new SuccessResult("%s güncelleme başarılı!".formatted(entityName));
		} catch (RuntimeException ex) {
			return new ErrorResult("İstek zaman aşımına uğradı!");
		}
	}

	@Override
	public Result delete(Estimation estimation) {
		try {
			estimationRepository.delete(estimation);
			return new SuccessResult("Silindi");
		} catch (Exception ex) {
			return new ErrorResult(ex.getMessage());
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
			return new ErrorResult("İstek zaman aşımına uğradı");
		}
	}

	@Override
	public DataResult<Estimation> getById(int id) {
		Estimation estimation = new Estimation();
		try {
			estimation = estimationRepository.findById(id).block(Duration.ofSeconds(1));
		} catch (RuntimeException ex) {
			System.out.println(ex.getMessage());
			return new ErrorDataResult<Estimation>(estimation, "İstek zaman aşımına uğradı!");
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
			System.out.println(ex.getMessage());
			return new ErrorDataResult<List<Estimation>>(estimations, "İstek zaman aşımına uğradı!");
		}
	}

	@Override
	public DataResult<Estimation> estimateKasko(int insuranceId, int vehicleId) {
		var insuranceResult = insuranceService.getInsuranceDetailById(insuranceId);
		if (!insuranceResult.isSuccess()) {
			return new ErrorDataResult<Estimation>(new Estimation(), insuranceResult.getMessage());
		}
		var vehicleResult = vehicleService.getById(vehicleId);
		if (!vehicleResult.isSuccess()) {
			return new ErrorDataResult<Estimation>(new Estimation(), vehicleResult.getMessage());
		}
		InsuranceDetailDTO insurance = insuranceResult.getData();
		Vehicle vehicle = vehicleResult.getData();
		Estimation estimation = new Estimation();
		estimation.setEstimationDate(new Timestamp(new Date().getTime()));
		estimation.setInsuranceId(insuranceId);
		estimation.setParameterId(vehicleId);
		var carDetail = carService.getCarDetailById(vehicle.getCarId()).getData();
		var customerDetail = customerService.getCustomerDetailById(vehicle.getCustomerId()).getData();
		double price = 0;
		price += insurance.getUnitPrice() * carDetail.getEstimatedValue();
		var customerAge = ageFromDateString(customerDetail.getBirthDate());
		double ageScaleFactor = (45 - customerAge) * 0.01;
		price *= (customerDetail.getCityScaleFactor() + customerDetail.getProfessionScaleFactor() + ageScaleFactor) / 3;
		price += customerDetail.getCityValueFactor() + customerDetail.getProfessionValueFactor();
		estimation.setPrice(price);
		return new SuccessDataResult<Estimation>(estimation);
	}

	private int ageFromDateString(String birthDateStr) {
		LocalDate birthDate = LocalDate.parse(birthDateStr); // string must be in iso format yyy-MM-dd
		return Period.between(birthDate, LocalDate.now()).getYears();

	}
}
