package com.sbm.application.entities.concretes;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.sbm.application.entities.abstracts.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("RealEstateUnitConstructionCosts")
public class RealEstateUnitConstructionCost extends Entity {

	@Column("LuxuryClassId")
	private int luxuryClassId;
	@Column("ConstructionTypeId")
	private int constructionTypeId;
	@Column("UsageTypeId")
	private int usageTypeId;
	@Column("UnitConstructionCost")
	private double unitConstructionCost;
}
