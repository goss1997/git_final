package com.githrd.figurium.order.vo;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data           // Getter + Setter
@Alias("shippingAddresses")
public class ShippingAddresses {

    // 배송지 정보 테이블 (Create table shipping_addresses)
    int id;             // primary key

    int orderId;       // foreign key
    String recipientName;
    String shippingPhone;
    String address;
    String deliveryRequest;

}
