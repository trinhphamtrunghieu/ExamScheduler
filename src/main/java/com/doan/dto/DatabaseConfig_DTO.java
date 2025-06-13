package com.doan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConfig_DTO {
	private String dbHost;
	private String dbPort;
	private String dbName;
	private String dbUsername;
	private String dbPassword;
}