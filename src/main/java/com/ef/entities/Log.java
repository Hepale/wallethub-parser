package com.ef.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
 
@Entity
@Table
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="datetime")
    private Date dt;
    private String ip;
    private String method;
    @Column(name="status_resp")
    private String statusResp;
    private String client;
    
	public Log() {
	}

	public Log(Date dt, String ip, String method, String statusResp, String client) {
		super();
		this.dt = dt;
		this.ip = ip;
		this.method = method;
		this.statusResp = statusResp;
		this.client = client;
	}

	public Date getDt() {
		return dt;
	}

	public void setDt(Date dt) {
		this.dt = dt;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getStatusResp() {
		return statusResp;
	}

	public void setStatusResp(String statusResp) {
		this.statusResp = statusResp;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}
    
    
}

