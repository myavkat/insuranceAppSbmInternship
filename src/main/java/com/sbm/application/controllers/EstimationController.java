package com.sbm.application.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sbm.application.business.abstracts.CustomerService;
import com.sbm.application.business.abstracts.EstimationService;
import com.sbm.application.business.abstracts.RealEstateService;
import com.sbm.application.business.abstracts.VehicleService;
import com.sbm.application.core.utilities.results.DataResult;
import com.sbm.application.entities.dtos.EstimationDetailDTO;

@Controller
@RequestMapping("/estimations")
public class EstimationController {

	private final String controllerName = "estimations";
	@Autowired
	private EstimationService estimationService;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private RealEstateService realEstateService;

	@GetMapping("/kasko")
	public String estimateKasko(Model model, @RequestParam(required = false) Integer customerId,
			@RequestParam(required = false) Integer vehicleId) {
		model.addAttribute("controller", controllerName);
		model.addAttribute("page", "kasko");
		if (customerId == null && vehicleId == null) {// Müşteri ve araç seçilmemişse
			model.addAttribute("customers", customerService.getCustomerDetails().getData());
			return "app";
		} else if (vehicleId == null) { // Sadece araç seçilmemişse
			model.addAttribute("customers", customerService.getCustomerDetails().getData());
			model.addAttribute("vehicles", vehicleService.getVehiclesByCustomerId(customerId).getData());
			model.addAttribute("customerId", customerId);
			return "app";
		} else { // Araç seçilmiş ama müşteri değiştirilmişse
			var vehicleResult = vehicleService.getById(vehicleId);
			if (vehicleResult.isSuccess() && vehicleResult.getData().getCustomerId() != customerId) {
				model.addAttribute("customers", customerService.getCustomerDetails().getData());
				model.addAttribute("vehicles", vehicleService.getVehiclesByCustomerId(customerId).getData());
				model.addAttribute("customerId", customerId);
				return "app";
			}
		}
		var result = estimationService.estimateKaskoAllCompanies(vehicleId);
		if (!result.isSuccess()) {
			model.addAttribute("toastError", true);
			model.addAttribute("toastMessage", result.getMessage());
			return "app";
		}
		model.addAttribute("estimations", result.getData());
		return "app";
	}

	@GetMapping("/list")
	public String list(Model model, @RequestParam(required = false) Integer customerId,
			@RequestParam(required = false) Integer vehicleId) {
		DataResult<List<EstimationDetailDTO>> estimationResult;
		if (vehicleId != null) {
			estimationResult = estimationService.getKaskoDetailsByVehicleId(vehicleId);
		} else if (customerId != null) {
			estimationResult = estimationService.getDetailsByCustomerId(customerId);
		} else {
			estimationResult = estimationService.getDetails();
		}
		model.addAttribute("controller", controllerName);
		model.addAttribute("page", "list");
		if (!estimationResult.isSuccess()) {
			return "redirect:/";
		}
		model.addAttribute("estimationDetails", estimationResult.getData());
		return "app";
	}

	@GetMapping("/confirm/{id}")
	public String confirm(Model model, @PathVariable int id) {
		var confirmResult = estimationService.confirmById(id);
		if (confirmResult.isSuccess()) {
			model.addAttribute("toastSuccess", true);
			model.addAttribute("toastMessage", "Teklif kabul edildi");
			return list(model, null, null);
		}
		model.addAttribute("toastError", true);
		model.addAttribute("toastMessage", confirmResult.getMessage());
		return list(model, null, null);
	}

	@GetMapping("/revokeConfirmation/{id}")
	public String revoke(Model model, @PathVariable int id) {
		var confirmResult = estimationService.revokeConfirmationById(id);
		if (confirmResult.isSuccess()) {
			model.addAttribute("toastSuccess", true);
			model.addAttribute("toastMessage", "Teklif onayı geri alındı");
			return list(model, null, null);
		}
		model.addAttribute("toastError", true);
		model.addAttribute("toastMessage", confirmResult.getMessage());
		return list(model, null, null);
	}

	@GetMapping("/delete/{id}")
	public String delete(Model model, @PathVariable int id) {
		var deleteResult = estimationService.deleteById(id);
		if (!deleteResult.isSuccess()) {
			model.addAttribute("toastError", true);
			model.addAttribute("toastMessage", deleteResult.getMessage());
			return list(model, null, null);
		}
		model.addAttribute("toastSuccess", true);
		model.addAttribute("toastMessage", deleteResult.getMessage());
		return list(model, null, null);
	}
	
	@PostMapping("/checkOldOffers")
	@ResponseBody
	// returns true if there is kasko offers for vehicle
	// false otherwise
	public boolean checkOldOffers(@RequestBody int vehicleId) {
		var result = estimationService.getKaskoDetailsByVehicleId(vehicleId);
		if(!result.isSuccess() || result.getData().size()>0) {
			return true;
		}
		return false;
	}
	
	@GetMapping("/konut")
	public String estimateKonut(Model model, @RequestParam(required = false) Integer customerId,
			@RequestParam(required = false) Integer realEstateId) {
		model.addAttribute("controller", controllerName);
		model.addAttribute("page", "konut");
		if (customerId == null && realEstateId == null) {// Müşteri ve emlak seçilmemişse
			model.addAttribute("customers", customerService.getCustomerDetails().getData());
			return "app";
		} else if (realEstateId == null) { // Sadece emlak seçilmemişse
			model.addAttribute("customers", customerService.getCustomerDetails().getData());
			model.addAttribute("realEstates", realEstateService.getAllByCustomerId(customerId).getData());
			model.addAttribute("customerId", customerId);
			return "app";
		} else { // Emlak seçilmiş ama müşteri değiştirilmişse
			var realEstateResult = realEstateService.getById(realEstateId);
			if (realEstateResult.isSuccess() && realEstateResult.getData().getCustomerId() != customerId) {
				model.addAttribute("customers", customerService.getCustomerDetails().getData());
				model.addAttribute("realEstates", realEstateService.getAllByCustomerId(customerId).getData());
				model.addAttribute("customerId", customerId);
				return "app";
			}
		}
		var result = estimationService.estimateKonutAllCompanies(realEstateId);
		if (!result.isSuccess()) {
			model.addAttribute("toastError", true);
			model.addAttribute("toastMessage", result.getMessage());
			return "app";
		}
		model.addAttribute("estimations", result.getData());
		return "app";
	}
	
	@PostMapping("/konut/checkOldOffers")
	@ResponseBody
	// returns true if there is konut offers for real estate
	// false otherwise
	public boolean checkOldOffersKonut(@RequestBody int realEstateId) {
		var result = estimationService.getKonutDetailsByRealEstateId(realEstateId);
		if(!result.isSuccess() || result.getData().size()>0) {
			return true;
		}
		return false;
	}


}
