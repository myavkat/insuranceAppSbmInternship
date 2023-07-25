package com.sbm.application.entities.concretes;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.sbm.application.entities.abstracts.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("RealEstateLuxuryClasses")
public class RealEstateLuxuryClass extends Entity {


	@Column("LuxuryClassName")
	public String LuxuryClassName;
}
