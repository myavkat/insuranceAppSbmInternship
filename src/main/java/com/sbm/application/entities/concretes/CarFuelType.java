package com.sbm.application.entities.concretes;

import org.springframework.data.relational.core.mapping.Table;

import com.sbm.application.entities.abstracts.Parameter;

@Table("CarFuelTypes")
public class CarFuelType extends Parameter {

	private String name;

	public CarFuelType(double scaleFactor, double valueFactor, int id, String name) {
		super(id, scaleFactor, valueFactor);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
