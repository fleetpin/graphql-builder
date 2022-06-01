package com.fleetpin.graphql.builder.type.directive;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.INPUT)
public class CaptureType {
	private String color;
	
	public CaptureType setColor(String color) {
		this.color = color;
		return this;
	}
	
	public String getColor() {
		return color;
	}
}