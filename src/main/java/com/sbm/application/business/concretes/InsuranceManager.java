package com.sbm.application.business.concretes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sbm.application.business.abstracts.InsuranceService;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.core.utilities.results.ErrorDataResult;
import com.sbm.application.core.utilities.results.ErrorResult;
import com.sbm.application.core.utilities.results.Result;
import com.sbm.application.core.utilities.results.SuccessDataResult;
import com.sbm.application.core.utilities.results.SuccessResult;
import com.sbm.application.entities.concretes.Insurance;
import com.sbm.application.entities.dtos.InsuranceDetailDTO;
import com.sbm.application.repositories.concretes.InsuranceRepository;

@Service
public class InsuranceManager implements InsuranceService {

	private final String entityName = "Sigorta";
	Logger logger = LoggerFactory.getLogger(InsuranceManager.class);
	@Autowired
	private InsuranceRepository insuranceRepository;

	@Override
	public Result save(Insurance insurance) {
		try {
			if (insurance.getId() == 0) {
				insuranceRepository.save(insurance).block(Duration.ofSeconds(1));
				return new SuccessResult("%s eklendi".formatted(entityName));
			}
			Insurance foundInsurance = insuranceRepository.findById(insurance.getId()).block(Duration.ofSeconds(1));
			if (foundInsurance == null) {
				return new ErrorResult("%s idli %s bulunamadı".formatted(insurance.getId(), entityName));
			}
			insuranceRepository.save(insurance).block(Duration.ofSeconds(1));
			return new SuccessResult("%s güncelleme başarılı!".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result delete(Insurance insurance) {
		try {
			insuranceRepository.delete(insurance);
			return new SuccessResult("Silindi");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<Insurance> getById(int id) {
		Insurance insurance = new Insurance();
		try {
			insurance = insuranceRepository.findById(id).block(Duration.ofSeconds(1));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<Insurance>(insurance, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
		if (insurance == null) {
			return new ErrorDataResult<Insurance>(new Insurance(), "%s Bulunamadı!".formatted(entityName));
		}
		return new SuccessDataResult<Insurance>(insurance, "Başarılı");
	}

	@Override
	public DataResult<List<Insurance>> getAll() {
		List<Insurance> insurances = new ArrayList<Insurance>();
		try {
			insuranceRepository.findAll().doOnNext(insurances::add).blockLast(Duration.ofSeconds(10));
			return new SuccessDataResult<List<Insurance>>(insurances, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<Insurance>>(insurances, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public Result deleteById(int id) {
		var result = getById(id);
		if (!result.isSuccess()) {
			return new ErrorResult("%s bulunamadı".formatted(entityName));
		}
		try {
			insuranceRepository.delete(result.getData()).block(Duration.ofSeconds(1));
			return new SuccessResult("%s silindi".formatted(entityName));
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorResult("Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<InsuranceDetailDTO> getInsuranceDetailById(int id) {
		try {
			InsuranceDetailDTO dto = insuranceRepository.getInsuranceDetailById(id).block(Duration.ofSeconds(1));
			return new SuccessDataResult<InsuranceDetailDTO>(dto, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<InsuranceDetailDTO>(new InsuranceDetailDTO(), "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<List<InsuranceDetailDTO>> getInsuranceDetails() {
		List<InsuranceDetailDTO> dtos = new ArrayList<InsuranceDetailDTO>();
		try {
			insuranceRepository.getInsuranceDetails().doOnNext(dtos::add).blockLast(Duration.ofSeconds(3));
			return new SuccessDataResult<List<InsuranceDetailDTO>>(dtos, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<InsuranceDetailDTO>>(dtos, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

	@Override
	public DataResult<List<InsuranceDetailDTO>> getInsuranceDetailsByInsuranceTypeName(String insuranceTypeName) {
		List<InsuranceDetailDTO> dtos = new ArrayList<InsuranceDetailDTO>();
		try {
			insuranceRepository.getInsuranceDetails().doOnNext(dtos::add).blockLast(Duration.ofSeconds(3));
			dtos.removeIf(dto -> !dto.getInsuranceTypeName().equalsIgnoreCase(insuranceTypeName));
			return new SuccessDataResult<List<InsuranceDetailDTO>>(dtos, "Başarılı");
		} catch (RuntimeException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
			return new ErrorDataResult<List<InsuranceDetailDTO>>(dtos, "Beklenmeyen bir hatayla karşılaşıldı!");
		}
	}

}
