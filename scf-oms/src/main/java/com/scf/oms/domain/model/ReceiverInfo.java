package com.scf.oms.domain.model;

import lombok.Value;

@Value
public class ReceiverInfo {
    String name;
    String phone;
    String province;
    String city;
    String district;
    String detailAddress;

    public ReceiverInfo(String name, String phone, String province, String city, String district, String detailAddress) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("receiver name must not be blank");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("receiver phone must not be blank");
        }
        if (detailAddress == null || detailAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("detail address must not be blank");
        }
        this.name = name;
        this.phone = phone;
        this.province = province;
        this.city = city;
        this.district = district;
        this.detailAddress = detailAddress;
    }
}
