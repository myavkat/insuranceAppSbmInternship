package com.sbm.application.business.concretes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sbm.application.business.abstracts.CarFuelTypeService;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.core.utilities.results.ErrorDataResult;
import com.sbm.application.core.utilities.results.ErrorResult;
import com.sbm.application.core.utilities.results.Result;
import com.sbm.application.core.utilities.results.SuccessDataResult;
import com.sbm.application.core.utilities.results.SuccessResult;
import com.sbm.application.entities.concretes.CarFuelType;
import com.sbm.application.repositories.concretes.CarFuelTypeRepository;

@Service
public class CarFuelTypeManager implements CarFuelTypeService {

	private final String entityName = "Araba Yakıt Tipi";
	Logger logger = LoggerFactory.getLogger(CarFuelTypeManager.class);
	@Autowired
	private CarFuelTypeRepository carFuelTypeRepository;

	@Override
	public Result save(CarFuelType carFuelType) {
		try {
			if (carFuelType.getId() == 0) {
				carFuelTypeRepository.save(carFuelType).block(Duration.ofSeconds(1));
				return new SuccessResult("%s eklendi".formatted(entityName));
			}
			CarFuelType foundCarBrand = carFuelTypeRepository.findById(carFuelType.getId())
					.block(Duration.ofSeconds(1));
			if (foundCarBrand == null) {
				return new ErrorResult("%s idli %s bulunamadı".formatted(carFuelType.getId(), entityName));
			}
			carFuelTypeRepository.save(carFuelType).block(Duration.ofSeconds(1));
			return new SuccessResult("%s güncelleme başarılı!".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result delete(CarFuelType carFuelType) {
		try {
			carFuelTypeRepository.delete(carFuelType);
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
			carFuelTypeRepository.delete(result.getData()).block(Duration.ofSeconds(1));
			return new SuccessResult("%s silindi".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<CarFuelType> getById(int id) {
		CarFuelType carFuelType = new CarFuelType();
		try {
			carFuelType = carFuelTypeRepository.findById(id).block(Duration.ofSeconds(1));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<CarFuelType>(carFuelType, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		if (carFuelType == null) {
			return new ErrorDataResult<CarFuelType>(new CarFuelType(), "%s Bulunamadı!".formatted(entityName));
		}
		return new SuccessDataResult<CarFuelType>(carFuelType, "Başarılı");
	}

	@Override
	public DataResult<List<CarFuelType>> getAll() {
		List<CarFuelType> carFuelTypes = new ArrayList<CarFuelType>();
		try {
			carFuelTypeRepository.findAll().doOnNext(carFuelTypes::add).blockLast(Duration.ofSeconds(10));
			return new SuccessDataResult<List<CarFuelType>>(carFuelTypes, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<CarFuelType>>(carFuelTypes, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

}
