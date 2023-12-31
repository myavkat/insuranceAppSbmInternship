package com.sbm.application.business.concretes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sbm.application.business.abstracts.CarService;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.core.utilities.results.ErrorDataResult;
import com.sbm.application.core.utilities.results.ErrorResult;
import com.sbm.application.core.utilities.results.Result;
import com.sbm.application.core.utilities.results.SuccessDataResult;
import com.sbm.application.core.utilities.results.SuccessResult;
import com.sbm.application.entities.concretes.Car;
import com.sbm.application.entities.dtos.CarDetailDTO;
import com.sbm.application.repositories.concretes.CarRepository;

@Service
public class CarManager implements CarService {
	private final String entityName = "Araba";
	Logger logger = LoggerFactory.getLogger(CarManager.class);
	@Autowired
	private CarRepository carRepository;

	@Override
	public Result save(Car car) {
		try {
			if (car.getId() == 0) {
				carRepository.save(car).block(Duration.ofSeconds(1));
				return new SuccessResult("%s eklendi".formatted(entityName));
			}
			Car foundCarBrand = carRepository.findById(car.getId()).block(Duration.ofSeconds(1));
			if (foundCarBrand == null) {
				return new ErrorResult("%s idli %s bulunamadı".formatted(car.getId(), entityName));
			}
			carRepository.save(car).block(Duration.ofSeconds(1));
			return new SuccessResult("%s güncelleme başarılı!".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result delete(Car car) {
		try {
			carRepository.delete(car);
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
			carRepository.delete(result.getData()).block(Duration.ofSeconds(1));
			return new SuccessResult("%s silindi".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<Car> getById(int id) {
		Car car = new Car();
		try {
			car = carRepository.findById(id).block(Duration.ofSeconds(1));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<Car>(car, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		if (car == null) {
			return new ErrorDataResult<Car>(new Car(), "%s Bulunamadı!".formatted(entityName));
		}
		return new SuccessDataResult<Car>(car, "Başarılı");
	}

	@Override
	public DataResult<List<Car>> getAll() {
		List<Car> cars = new ArrayList<Car>();
		try {
			carRepository.findAll().doOnNext(cars::add).blockLast(Duration.ofSeconds(10));
			return new SuccessDataResult<List<Car>>(cars, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<Car>>(cars, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<List<CarDetailDTO>> getCarDetails() {
		List<CarDetailDTO> carDetails = new ArrayList<CarDetailDTO>();
		try {
			carRepository.findCarDetails().doOnNext(carDetails::add).blockLast(Duration.ofSeconds(3));
			return new SuccessDataResult<List<CarDetailDTO>>(carDetails, "Araba detayları listelendi");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<CarDetailDTO>>(carDetails, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<CarDetailDTO> getCarDetailById(int id) {
		try {
		return new SuccessDataResult<CarDetailDTO>(carRepository.findCarDetailById(id).block(Duration.ofSeconds(1)));
		}
		catch(RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<CarDetailDTO>(new CarDetailDTO(), "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

}
